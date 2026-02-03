package me.siddheshkothadi.codexdroid.codex

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import java.io.Closeable
import java.net.URI
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

/**
 * Persistent WebSocket client for codex-app-server.
 *
 * Transport:
 * - client -> server: { "id": 1, "method": "...", "params": { ... } }
 * - server -> client: responses (with id) + notifications (without id)
 */
class CodexWsClient(
    private val baseUrl: String,
    private val secret: String? = null,
) : Closeable {
    private val tag = "CodexWsClient"

    private val client = HttpClient(OkHttp) {
        install(WebSockets)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionLock = Mutex()
    private val connectLock = Mutex()
    private var session: WebSocketSession? = null
    private var readerJob: Job? = null

    // Use a high starting point to reduce the chance of colliding with server-initiated request IDs
    // in full-duplex JSON-RPC streams.
    private val nextId = AtomicLong(System.currentTimeMillis() * 1_000L)
    private val pendingLock = Mutex()
    private val pending = mutableMapOf<Long, CompletableDeferred<CodexResponse<JsonElement>>>()

    private val events =
        MutableSharedFlow<CodexEvent>(
            extraBufferCapacity = 256,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

    private var started = false
    private var stopping = false

    fun observeEvents(): Flow<CodexEvent> = events.asSharedFlow()

    suspend fun start() {
        if (started) return
        started = true
        scope.launch { maintainConnectionLoop() }
    }

    private suspend fun maintainConnectionLoop() {
        var attempt = 0
        while (scope.isActive && !stopping) {
            try {
                ensureConnected()
                attempt = 0
                val job = sessionLock.withLock { readerJob } ?: continue
                job.join()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(tag, "WS loop error (attempt=${attempt + 1})", e)
            } finally {
                closeSession()
            }

            attempt += 1
            val delayMs = min(15_000L, 1_000L * (1L shl min(attempt, 4)))
            delay(delayMs)
        }
    }

    private fun toWsUrl(baseUrl: String): String {
        val uri = URI(baseUrl.trim())
        val scheme =
            when (uri.scheme?.lowercase()) {
                "https" -> "wss"
                "http" -> "ws"
                "wss" -> "wss"
                "ws" -> "ws"
                else -> "ws"
            }
        val authority = uri.rawAuthority ?: throw IllegalArgumentException("Invalid baseUrl (missing host): $baseUrl")
        return "$scheme://$authority/"
    }

    private suspend fun ensureConnected() {
        // Fast path without taking the connect lock.
        sessionLock.withLock {
            if (session != null && readerJob?.isActive == true) return
        }

        connectLock.withLock {
            // Re-check once serialized.
            sessionLock.withLock {
                if (session != null && readerJob?.isActive == true) return
            }

            // Tear down any previous session before opening a new one.
            // Important: we must not let a canceled previous readerJob "closeSession()" the new session.
            closeSession()

            val url = toWsUrl(baseUrl)
            val newSession =
                client.webSocketSession(urlString = url) {
                    if (!secret.isNullOrEmpty()) {
                        headers.append("x-codex-secret", secret)
                    }
                }

            val newReaderJob =
                scope.launch {
                    try {
                        readLoop(newSession)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(tag, "WS read loop error", e)
                    } finally {
                        closeSession(expectedSession = newSession)
                    }
                }

            sessionLock.withLock {
                session = newSession
                readerJob = newReaderJob
            }
            Log.d(tag, "Connected WS: $url")
        }
    }

    private suspend fun readLoop(s: WebSocketSession) {
        for (frame in s.incoming) {
            if (frame !is Frame.Text) continue
            handleIncoming(frame.readText())
        }
    }

    private suspend fun handleIncoming(text: String) {
        val element = try {
            CodexJson.parseToJsonElement(text)
        } catch (e: Exception) {
            Log.w(tag, "Failed to parse WS message: $text", e)
            return
        }

        val obj = try {
            element.jsonObject
        } catch (_: Exception) {
            return
        }

        val hasId = obj["id"] != null && obj["id"] !is JsonNull
        val hasMethod = obj.containsKey("method")
        val hasResult = obj.containsKey("result")
        val hasError = obj.containsKey("error")

        // 1) Response: has id + (result|error)
        if (hasId && (hasResult || hasError) && !hasMethod) {
            val resp =
                try {
                    CodexJson.decodeFromJsonElement<CodexResponse<JsonElement>>(element)
                } catch (e: Exception) {
                    Log.w(tag, "Failed to decode response: $text", e)
                    return
                }

            val deferred = pendingLock.withLock { pending.remove(resp.id) }
            deferred?.complete(resp)
            return
        }

        // 2) Server-initiated request: has id + method (no result/error)
        if (hasId && hasMethod && !hasResult && !hasError) {
            val req =
                try {
                    CodexJson.decodeFromJsonElement<ServerRequest>(element)
                } catch (e: Exception) {
                    Log.w(tag, "Failed to decode server request: $text", e)
                    return
                }
            events.tryEmit(req)
            return
        }

        // 3) Notification: has method and no id
        if (!hasId && hasMethod) {
            val notification =
                try {
                    CodexJson.decodeFromJsonElement<CodexNotification<JsonObject>>(element)
                } catch (e: Exception) {
                    Log.w(tag, "Failed to decode notification: $text", e)
                    return
                }
            events.tryEmit(notification)
            return
        }

        // Fallback: best-effort decode. Some servers include "method" in responses or other shapes.
        if (hasId && (hasResult || hasError)) {
            val resp =
                try {
                    CodexJson.decodeFromJsonElement<CodexResponse<JsonElement>>(element)
                } catch (e: Exception) {
                    Log.w(tag, "Failed to decode response (fallback): $text", e)
                    return
                }
            val deferred = pendingLock.withLock { pending.remove(resp.id) }
            deferred?.complete(resp)
            return
        }

        Log.w(tag, "Unrecognized WS message shape: $text")
    }

    internal suspend inline fun <reified P, reified R> send(request: CodexRequest<P>): CodexResponse<R> {
        start()
        ensureConnected()

        val id = nextId.incrementAndGet()
        val reqWithId = request.copy(id = id)

        val deferred = CompletableDeferred<CodexResponse<JsonElement>>()
        pendingLock.withLock { pending[id] = deferred }

        try {
            val s = sessionLock.withLock { session } ?: throw IllegalStateException("WS not connected")

            val payload = CodexJson.encodeToString(CodexRequest.serializer(serializer<P>()), reqWithId)
            s.send(Frame.Text(payload))

            val resp = withTimeout(120_000) { deferred.await() }
            val decodedResult =
                if (resp.result != null && resp.result !is JsonNull) {
                    CodexJson.decodeFromJsonElement<R>(resp.result)
                } else {
                    null
                }
            return CodexResponse(id = resp.id, result = decodedResult, error = resp.error)
        } catch (e: Exception) {
            pendingLock.withLock { pending.remove(id) }
            throw e
        }
    }

    internal suspend inline fun <reified P, reified R> send(method: String, params: P? = null): CodexResponse<R> {
        return send(CodexRequest(method = method, params = params))
    }

    suspend fun sendResponse(id: Long, result: JsonElement) {
        start()
        ensureConnected()

        val s = sessionLock.withLock { session } ?: throw IllegalStateException("WS not connected")
        val payload =
            buildJsonObject {
                put("id", id)
                put("result", result)
            }
        s.send(Frame.Text(payload.toString()))
    }

    private suspend fun closeSession(expectedSession: WebSocketSession? = null) {
        var s: WebSocketSession? = null
        var job: Job? = null

        val shouldClose =
            sessionLock.withLock {
                val cur = session
                if (expectedSession != null && cur !== expectedSession) return@withLock false

                s = cur
                job = readerJob
                session = null
                readerJob = null
                true
            }

        if (!shouldClose) return

        job?.cancel()
        if (s == null) return

        try {
            s?.close(CloseReason(CloseReason.Codes.NORMAL, "closing"))
        } catch (_: Exception) {
        }

        // Fail all pending calls.
        val toFail =
            pendingLock.withLock {
                val all = pending.values.toList()
                pending.clear()
                all
            }
        toFail.forEach { it.cancel() }
    }

    override fun close() {
        stopping = true
        scope.launch {
            closeSession()
            client.close()
        }
    }
}
