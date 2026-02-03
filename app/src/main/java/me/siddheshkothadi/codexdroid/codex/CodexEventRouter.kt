package me.siddheshkothadi.codexdroid.codex

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.TaskStackBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.siddheshkothadi.codexdroid.MainActivity
import me.siddheshkothadi.codexdroid.data.local.Connection
import me.siddheshkothadi.codexdroid.data.repository.ThreadRepository
import me.siddheshkothadi.codexdroid.domain.usecase.GetConnectionsUseCase
import me.siddheshkothadi.codexdroid.navigation.CodexDroidAppLinkKeys
import me.siddheshkothadi.codexdroid.notifications.CodexDroidNotifications
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Background router for inbound Codex WS messages.
 *
 * Goal: move toward CodexMonitor’s “event hub + routing” model so that:
 * - events keep updating the local DB even when a thread is not open,
 * - server-initiated requests can be surfaced globally (approvals, user-input),
 * - future multi-workspace routing (by `cwd`) has a single home.
 *
 * This is intentionally conservative for now:
 * - It does NOT try to apply streaming deltas to arbitrary threads.
 * - It does refresh + merge on key lifecycle events (turn completion), which is enough to keep
 *   history lists correct and avoid depending on “send first message”.
 */
@Singleton
class CodexEventRouter @Inject constructor(
    getConnectionsUseCase: GetConnectionsUseCase,
    @ApplicationContext private val appContext: Context,
    private val clientManager: CodexClientManager,
    private val apiService: CodexApiService,
    private val threadRepository: ThreadRepository,
    private val codexAppLifecycle: CodexAppLifecycle,
) {
    private val tag = "CodexEventRouter"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var activeConnection: Connection? = null
    private var job: Job? = null

    private val cache = mutableMapOf<String, Thread>()
    private val pendingPersistJobs = mutableMapOf<String, Job>()
    private val threadByTurnId = mutableMapOf<String, String>()
    private val notifiedTurnIds = LinkedHashSet<String>()

    private val serverRequests = MutableSharedFlow<ServerRequest>(extraBufferCapacity = 64)
    fun observeServerRequests() = serverRequests.asSharedFlow()

    init {
        scope.launch {
            getConnectionsUseCase()
                .map { it.firstOrNull() }
                .distinctUntilChangedBy { it?.id }
                .collectLatest { conn ->
                    activeConnection = conn
                    job?.cancel()
                    cache.clear()
                    pendingPersistJobs.values.forEach { it.cancel() }
                    pendingPersistJobs.clear()
                    threadByTurnId.clear()
                    notifiedTurnIds.clear()
                    if (conn == null) return@collectLatest

                    job =
                        launch {
                            try {
                                val client = clientManager.get(conn.baseUrl, conn.secret)
                                client.observeEvents().collect { onEvent(conn, it) }
                            } catch (e: Exception) {
                                Log.w(tag, "Router loop failed", e)
                            }
                        }
                }
        }
    }

    private suspend fun onEvent(connection: Connection, event: CodexEvent) {
        when (event) {
            is ServerRequest -> {
                serverRequests.tryEmit(event)
            }
            is CodexNotification<*> -> {
                @Suppress("UNCHECKED_CAST")
                val n = event as CodexNotification<kotlinx.serialization.json.JsonObject>

                val threadId = extractThreadId(n.method, n.params)
                val turnId = extractTurnId(n.params)
                if (threadId != null && turnId != null) {
                    threadByTurnId[turnId] = threadId
                }

                if (threadId != null) {
                    applyNotification(connection, threadId, n.method, n.params)
                }

                // Occasionally reconcile from server for authoritative status/metadata.
                if (n.method == "turn/completed" && threadId != null) {
                    refreshAndMergeThread(connection, threadId)
                    maybeNotifyTurnCompleted(connection, threadId, turnId)
                }
            }
        }
    }

    private fun extractTurnId(params: kotlinx.serialization.json.JsonObject): String? {
        return try {
            params["turnId"]?.jsonPrimitive?.content
                ?: params["turn_id"]?.jsonPrimitive?.content
                ?: params["turn"]?.jsonObject?.get("id")?.jsonPrimitive?.content
        } catch (_: Exception) {
            null
        }
    }

    private fun extractThreadId(method: String, params: kotlinx.serialization.json.JsonObject): String? {
        return try {
            params["threadId"]?.jsonPrimitive?.content
                ?: params["thread_id"]?.jsonPrimitive?.content
                ?: params["turn"]?.jsonObject?.get("threadId")?.jsonPrimitive?.content
                ?: params["turn"]?.jsonObject?.get("thread_id")?.jsonPrimitive?.content
                ?: if (method == "turn/plan/updated") {
                    val turnId =
                        params["turnId"]?.jsonPrimitive?.content
                            ?: params["turn_id"]?.jsonPrimitive?.content
                    if (turnId != null) threadByTurnId[turnId] else null
                } else {
                    null
                }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun applyNotification(
        connection: Connection,
        threadId: String,
        method: String,
        params: kotlinx.serialization.json.JsonObject,
    ) {
        val key = "${connection.id}|$threadId"
        val current =
            cache[key]
                ?: threadRepository.getThread(connection.id, threadId)
                ?: runCatching {
                    val resp = apiService.readThread(connection.baseUrl, connection.secret, threadId)
                    resp.result?.thread
                }.getOrNull()
                ?: return

        val updated =
            runCatching { ThreadEventReducer.applyNotification(current, method, params) }
                .getOrElse {
                    Log.w(tag, "Failed to apply notification method=$method", it)
                    current
                }

        cache[key] = updated
        schedulePersist(connection, key, updated)
    }

    private fun schedulePersist(connection: Connection, cacheKey: String, thread: Thread) {
        pendingPersistJobs[cacheKey]?.cancel()
        pendingPersistJobs[cacheKey] =
            scope.launch {
                delay(250)
                threadRepository.upsertThread(connection.id, thread)
            }
    }

    private suspend fun refreshAndMergeThread(connection: Connection, threadId: String) {
        val key = "${connection.id}|$threadId"
        val existing = cache[key] ?: threadRepository.getThread(connection.id, threadId)
        val resp =
            runCatching {
                apiService.readThread(connection.baseUrl, connection.secret, threadId)
            }.getOrNull() ?: return
        val serverThread = resp.result?.thread ?: return

        val mergedTurns =
            if (existing != null && existing.turns.isNotEmpty()) existing.turns else serverThread.turns
        val merged =
            serverThread.copy(
                turns = mergedTurns,
                clientModel = existing?.clientModel,
                clientEffort = existing?.clientEffort,
            )
        cache[key] = merged
        threadRepository.upsertThread(connection.id, merged)
    }

    private fun maybeNotifyTurnCompleted(connection: Connection, threadId: String, turnId: String?) {
        if (codexAppLifecycle.isForeground.value) return
        if (turnId.isNullOrBlank()) return

        val notificationKey = "${connection.id}|$threadId|$turnId"
        if (!notifiedTurnIds.add(notificationKey)) return
        while (notifiedTurnIds.size > 256) {
            notifiedTurnIds.remove(notifiedTurnIds.first())
        }

        val key = "${connection.id}|$threadId"
        val thread = cache[key] ?: return

        val workspaceName = folderNameFromPath(thread.cwd).ifBlank { "Codex" }
        val snippet =
            lastAgentSnippet(thread, turnId).ifBlank {
                "Turn completed."
            }

        val pendingIntent =
            TaskStackBuilder.create(appContext)
                .addNextIntentWithParentStack(
                    Intent(appContext, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(CodexDroidAppLinkKeys.EXTRA_CONNECTION_ID, connection.id)
                        putExtra(CodexDroidAppLinkKeys.EXTRA_THREAD_ID, threadId)
                        putExtra(CodexDroidAppLinkKeys.EXTRA_TURN_ID, turnId)
                    }
                )
                .getPendingIntent(
                    (threadId.hashCode() xor turnId.hashCode()),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                ?: return

        CodexDroidNotifications.notifyTurnCompleted(
            context = appContext,
            notificationId = (threadId.hashCode() xor turnId.hashCode()),
            title = workspaceName,
            text = snippet,
            contentIntent = pendingIntent
        )
    }

    private fun lastAgentSnippet(thread: Thread, turnId: String): String {
        val turn =
            thread.turns.firstOrNull { it.id == turnId }
                ?: thread.turns.lastOrNull()
                ?: return ""
        val text =
            turn.items
                .asReversed()
                .firstNotNullOfOrNull { item ->
                    when (item) {
                        is ThreadItem.AgentMessage -> item.text
                        else -> null
                    }
                }
                ?.trim()
                .orEmpty()
        if (text.isBlank()) return ""
        return if (text.length <= 160) text else text.take(157) + "…"
    }

    private fun folderNameFromPath(path: String): String {
        val p = path.trim().trimEnd('/', '\\')
        if (p.isBlank()) return ""
        val lastSlash = p.lastIndexOf('/')
        val lastBackslash = p.lastIndexOf('\\')
        val idx = maxOf(lastSlash, lastBackslash)
        return if (idx == -1) p else p.substring(idx + 1)
    }
}
