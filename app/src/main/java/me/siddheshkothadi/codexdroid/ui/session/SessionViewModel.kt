package me.siddheshkothadi.codexdroid.ui.session

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.siddheshkothadi.codexdroid.codex.*
import me.siddheshkothadi.codexdroid.data.local.Connection
import me.siddheshkothadi.codexdroid.data.repository.ConnectionRepository
import me.siddheshkothadi.codexdroid.data.repository.ThreadRepository
import me.siddheshkothadi.codexdroid.domain.usecase.GetConnectionsUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.GetThreadsUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.RefreshThreadsUseCase
import javax.inject.Inject

/**
 * UI State for the Session Screen.
 */
data class SessionUiState(
    val currentThread: Thread? = null,
    val historyThreads: List<Thread> = emptyList(),
    val isSending: Boolean = false,
    val isHistorySyncing: Boolean = false,
    val isHistoryInitialized: Boolean = false,
    val isThreadSyncing: Boolean = false,
    val isControlsSyncing: Boolean = false,
    val error: String? = null,
    val activeTurnId: String? = null,
    val scrollToTurnId: String? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Unknown,
    // Used to render an immediate local echo when starting a brand-new thread (before thread/start returns).
    val pendingUserMessage: String? = null,
    // Workspace key for new threads on a shared app-server (maps to Codex thread "cwd").
    val selectedCwd: String? = null,
    val pendingApproval: PendingApproval? = null,
    val pendingUserInput: PendingUserInput? = null,
    val models: List<ModelOptionUi> = emptyList(),
    val skills: List<SkillOptionUi> = emptyList(),
    val selectedModelId: String? = null,
    val selectedEffort: String? = null,
    val controlsError: String? = null,
)

data class PendingApproval(
    val requestId: Long,
    val method: String,
    val params: JsonElement? = null,
)

data class PendingUserInput(
    val requestId: Long,
    val threadId: String? = null,
    val turnId: String? = null,
    val itemId: String? = null,
    val questions: List<UserInputQuestion> = emptyList(),
)

data class UserInputQuestion(
    val id: String,
    val header: String = "",
    val question: String = "",
    val options: List<UserInputOption> = emptyList(),
)

data class UserInputOption(
    val label: String,
    val description: String,
)

data class ModelOptionUi(
    val id: String,
    val model: String,
    val displayName: String,
    val description: String = "",
    val supportedReasoningEfforts: List<ReasoningEffortUi> = emptyList(),
    val defaultReasoningEffort: String? = null,
    val isDefault: Boolean = false,
)

data class ReasoningEffortUi(
    val reasoningEffort: String,
    val description: String = "",
)

data class SkillOptionUi(
    val name: String,
    val path: String,
    val description: String? = null,
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val getConnectionsUseCase: GetConnectionsUseCase,
    private val getThreadsUseCase: GetThreadsUseCase,
    private val refreshThreadsUseCase: RefreshThreadsUseCase,
    private val connectionRepository: ConnectionRepository,
    private val apiService: CodexApiService,
    private val threadRepository: ThreadRepository,
    private val eventRouter: CodexEventRouter,
) : ViewModel() {
    private val tag = "SessionViewModel"

    // Used by the poll fallback to avoid turning off the typing indicator while WS events are still flowing.
    private var lastEventAtMs: Long = 0L
    private var sendingStartedAtMs: Long = 0L
    private var autoSelectedConnectionId: String? = null
    private var didInitialHistorySyncForConnectionId: String? = null
    private var lastControlsKey: String? = null
    private val approvalQueue = ArrayDeque<PendingApproval>()
    private val userInputQueue = ArrayDeque<PendingUserInput>()

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    // Observed connections
    val connections: StateFlow<List<Connection>> = getConnectionsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var pollJob: Job? = null
    private var connectionCheckJob: Job? = null
    private var observeThreadJob: Job? = null
    private var selectedThreadId: String? = null

    init {
        observeHistory()
        observeActiveSession()
        observeServerRequests()
    }

    fun setSelectedModelId(modelId: String?) {
        val normalized = modelId?.takeIf { v -> v.isNotBlank() }
        _uiState.update { it.copy(selectedModelId = normalized) }
        persistThreadPreferences(model = normalized, effort = null)
    }

    fun setSelectedEffort(effort: String?) {
        val normalized = effort?.takeIf { v -> v.isNotBlank() }
        _uiState.update { it.copy(selectedEffort = normalized) }
        persistThreadPreferences(model = null, effort = normalized)
    }

    private fun persistThreadPreferences(model: String?, effort: String?) {
        val conn = connections.value.firstOrNull() ?: return
        val thread = _uiState.value.currentThread ?: return
        viewModelScope.launch {
            val updated =
                thread.copy(
                    clientModel = model ?: thread.clientModel,
                    clientEffort = effort ?: thread.clientEffort,
                )
            threadRepository.upsertThread(conn.id, updated)
        }
    }

    fun refreshControls() {
        val connection = connections.value.firstOrNull() ?: return
        val cwd =
            _uiState.value.currentThread?.cwd?.takeIf { it.isNotBlank() }
                ?: _uiState.value.currentThread?.path?.takeIf { it.isNotBlank() }
                ?: _uiState.value.selectedCwd
        viewModelScope.launch { refreshControlsForConnection(connection, cwd, force = true) }
    }

    private suspend fun refreshControlsForConnection(connection: Connection, cwd: String?, force: Boolean) {
        val key = "${connection.id}|${cwd.orEmpty()}"
        if (!force && key == lastControlsKey && _uiState.value.models.isNotEmpty()) return
        _uiState.update { it.copy(isControlsSyncing = true, controlsError = null) }
        try {
            val modelsResp = runCatching { apiService.listModels(connection.baseUrl, connection.secret) }.getOrNull()
            val skillsResp = runCatching { apiService.listSkills(connection.baseUrl, connection.secret, cwd) }.getOrNull()

            val models = modelsResp?.result?.let { parseModels(it) }.orEmpty()
            val skills = skillsResp?.result?.let { parseSkills(it) }.orEmpty()

            val error =
                when {
                    modelsResp?.error != null -> modelsResp.error?.message
                    skillsResp?.error != null -> skillsResp.error?.message
                    modelsResp == null && skillsResp == null -> "Failed to load session controls."
                    else -> null
                }

            _uiState.update { state ->
                var next = state.copy(models = models, skills = skills, controlsError = error)

                val threadPreferredModel =
                    next.currentThread?.clientModel
                        ?.takeIf { it.isNotBlank() }
                        ?: next.currentThread?.modelProvider?.takeIf { it.isNotBlank() }

                val resolvedModelId =
                    when {
                        !next.selectedModelId.isNullOrBlank() && models.any { it.id == next.selectedModelId } ->
                            next.selectedModelId
                        !threadPreferredModel.isNullOrBlank() ->
                            models.firstOrNull { it.id == threadPreferredModel || it.model == threadPreferredModel }?.id
                        else ->
                            models.firstOrNull { it.isDefault }?.id ?: models.firstOrNull()?.id
                    }

                if (resolvedModelId != next.selectedModelId) {
                    next = next.copy(selectedModelId = resolvedModelId)
                }

                val selectedModel = next.selectedModelId?.let { id -> models.firstOrNull { it.id == id } }
                val supportedEfforts =
                    selectedModel?.supportedReasoningEfforts?.map { it.reasoningEffort }?.filter { it.isNotBlank() }.orEmpty()
                val defaultEffort = selectedModel?.defaultReasoningEffort?.takeIf { it.isNotBlank() }

                val threadPreferredEffort = next.currentThread?.clientEffort?.takeIf { it.isNotBlank() }
                val resolvedEffort =
                    when {
                        !next.selectedEffort.isNullOrBlank() &&
                            (supportedEfforts.isEmpty() || next.selectedEffort in supportedEfforts) ->
                            next.selectedEffort
                        threadPreferredEffort != null && (supportedEfforts.isEmpty() || threadPreferredEffort in supportedEfforts) ->
                            threadPreferredEffort
                        supportedEfforts.isNotEmpty() ->
                            supportedEfforts.firstOrNull()
                        else -> defaultEffort
                    }

                if (resolvedEffort != next.selectedEffort) {
                    next = next.copy(selectedEffort = resolvedEffort)
                }

                next
            }
            lastControlsKey = key
        } finally {
            _uiState.update { it.copy(isControlsSyncing = false) }
        }
    }

    private fun parseModels(result: JsonElement): List<ModelOptionUi> {
        val root = result as? JsonObject
        val items =
            when {
                root != null -> {
                    val data = root["data"] ?: root["models"] ?: root["items"]
                    (data as? JsonArray)?.jsonArray
                }
                result is JsonArray -> result.jsonArray
                else -> null
            } ?: return emptyList()

        return items.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val id = readString(obj, "id").ifBlank { readString(obj, "model") }
            if (id.isBlank()) return@mapNotNull null
            val model = readString(obj, "model").ifBlank { id }
            val displayName =
                readString(obj, "displayName", "display_name").ifBlank { model }
            val description = readString(obj, "description")
            val isDefault = readBoolean(obj, "isDefault", "is_default")

            val supported =
                (obj["supportedReasoningEfforts"] as? JsonArray)?.jsonArray
                    ?: (obj["supported_reasoning_efforts"] as? JsonArray)?.jsonArray
            val supportedEfforts =
                supported
                    ?.mapNotNull { e ->
                        val eo = e as? JsonObject ?: return@mapNotNull null
                        val effort = readString(eo, "reasoningEffort", "reasoning_effort")
                        if (effort.isBlank()) return@mapNotNull null
                        ReasoningEffortUi(
                            reasoningEffort = effort,
                            description = readString(eo, "description"),
                        )
                    }
                    .orEmpty()

            val defaultEffort =
                readString(obj, "defaultReasoningEffort", "default_reasoning_effort").takeIf { it.isNotBlank() }

            ModelOptionUi(
                id = id,
                model = model,
                displayName = displayName,
                description = description,
                supportedReasoningEfforts = supportedEfforts,
                defaultReasoningEffort = defaultEffort,
                isDefault = isDefault,
            )
        }
    }

    private fun parseSkills(result: JsonElement): List<SkillOptionUi> {
        val root = result as? JsonObject
        val items =
            when {
                root != null -> {
                    val data = root["data"] ?: root["skills"] ?: root["items"]
                    (data as? JsonArray)?.jsonArray
                }
                result is JsonArray -> result.jsonArray
                else -> null
            } ?: return emptyList()

        return items.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val name = readString(obj, "name")
            val path = readString(obj, "path")
            if (name.isBlank() || path.isBlank()) return@mapNotNull null
            SkillOptionUi(
                name = name,
                path = path,
                description = readString(obj, "description").takeIf { it.isNotBlank() },
            )
        }
    }

    private fun readString(obj: JsonObject, vararg keys: String): String {
        for (k in keys) {
            val v = obj[k] as? JsonPrimitive ?: continue
            val s = runCatching { v.content }.getOrNull()?.trim().orEmpty()
            if (s.isNotBlank()) return s
        }
        return ""
    }

    private fun readBoolean(obj: JsonObject, vararg keys: String): Boolean {
        for (k in keys) {
            val v = obj[k] as? JsonPrimitive ?: continue
            val s = runCatching { v.content }.getOrNull()?.trim()?.lowercase()
            if (s == "true") return true
            if (s == "false") return false
            // Some servers may send numeric booleans.
            val n = s?.toIntOrNull()
            if (n != null) return n != 0
        }
        return false
    }

    fun handleAppLink(connectionId: String?, threadId: String?, turnId: String?, openLatest: Boolean) {
        if (connectionId.isNullOrBlank() && threadId.isNullOrBlank() && !openLatest) return
        viewModelScope.launch {
            val list = connections.value
            val desiredConnection =
                when {
                    !connectionId.isNullOrBlank() -> list.firstOrNull { it.id == connectionId }
                    else -> list.firstOrNull()
                } ?: return@launch

            // Make sure the desired connection becomes the "active" connection (connections.firstOrNull()).
            if (connections.value.firstOrNull()?.id != desiredConnection.id) {
                selectConnection(desiredConnection)
                withTimeoutOrNull(3_000) {
                    connections
                        .map { it.firstOrNull()?.id }
                        .filter { it == desiredConnection.id }
                        .first()
                }
            }

            if (!threadId.isNullOrBlank()) {
                openThreadOnConnection(desiredConnection, threadId, turnId)
                return@launch
            }

            if (openLatest) {
                val threads = getThreadsUseCase(desiredConnection.id).first()
                val latest = threads.firstOrNull() ?: return@launch
                openThreadOnConnection(desiredConnection, latest.id, null)
            }
        }
    }

    private suspend fun openThreadOnConnection(connection: Connection, threadId: String, turnId: String?) {
        val cached = threadRepository.getThread(connection.id, threadId)
        if (cached == null) {
            runCatching {
                val resp = apiService.readThread(connection.baseUrl, connection.secret, threadId)
                resp.result?.thread?.let { threadRepository.upsertThread(connection.id, it) }
            }
        }
        _uiState.update { it.copy(scrollToTurnId = turnId) }
        selectThreadId(connection.id, threadId)
        refreshThreadFromServer(connection, threadId)
    }

    fun clearScrollTarget() {
        _uiState.update { it.copy(scrollToTurnId = null) }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            connections.collectLatest { list ->
                val active = list.firstOrNull() ?: return@collectLatest
                coroutineScope {
                    launch {
                        getThreadsUseCase(active.id).collect { threads ->
                            var didAutoSelect = false
                            var selected: Thread? = null
                            if (_uiState.value.currentThread == null && threads.isNotEmpty() && autoSelectedConnectionId != active.id) {
                                selected = threadRepository.getThread(active.id, threads.first().id) ?: threads.first()
                                didAutoSelect = true
                            }

                            _uiState.update { state ->
                                var next = state.copy(historyThreads = threads, isHistoryInitialized = true)
                                if (didAutoSelect && selected != null) {
                                    next =
                                        next.copy(
                                            currentThread = selected,
                                            selectedCwd = selected.cwd.takeIf { it.isNotBlank() }
                                        )
                                    autoSelectedConnectionId = active.id
                                }
                                next
                            }

                                if (didAutoSelect && selected != null) {
                                    selectThreadId(active.id, selected.id)
                                    refreshThreadFromServer(active, selected.id)
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun observeActiveSession() {
        viewModelScope.launch {
            connections
                .map { it.firstOrNull() }
                .distinctUntilChangedBy { it?.id }
                .collect { active ->
                    if (active != null) {
                        checkConnection(active)
                    } else {
                        _uiState.update { it.copy(connectionStatus = ConnectionStatus.Unknown) }
                    }
                }
        }
    }

    private fun observeServerRequests() {
        viewModelScope.launch {
            eventRouter.observeServerRequests().collect { req ->
                when {
                    req.method.contains("requestApproval") -> {
                        approvalQueue.addLast(
                            PendingApproval(
                                requestId = req.id,
                                method = req.method,
                                params = req.params,
                            )
                        )
                        maybeShowNextPending()
                    }
                    req.method == "item/tool/requestUserInput" -> {
                        parseUserInputRequest(req)?.let {
                            userInputQueue.addLast(it)
                            maybeShowNextPending()
                        }
                    }
                }
            }
        }
    }

    private fun maybeShowNextPending() {
        _uiState.update { state ->
            var next = state
            if (next.pendingApproval == null && approvalQueue.isNotEmpty()) {
                next = next.copy(pendingApproval = approvalQueue.removeFirst())
            }
            if (next.pendingUserInput == null && userInputQueue.isNotEmpty()) {
                next = next.copy(pendingUserInput = userInputQueue.removeFirst())
            }
            next
        }
    }

    private fun parseUserInputRequest(req: ServerRequest): PendingUserInput? {
        val params = req.params as? JsonObject ?: return null
        val threadId = params["threadId"]?.jsonPrimitive?.content
        val turnId = params["turnId"]?.jsonPrimitive?.content
        val itemId = params["itemId"]?.jsonPrimitive?.content
        val questionsRaw = params["questions"] as? JsonArray
        val questions =
            questionsRaw
                ?.jsonArray
                ?.mapNotNull { entry ->
                    val obj = entry as? JsonObject ?: return@mapNotNull null
                    val id = obj["id"]?.jsonPrimitive?.content?.trim().orEmpty()
                    if (id.isBlank()) return@mapNotNull null
                    val header = obj["header"]?.jsonPrimitive?.content?.trim().orEmpty()
                    val question = obj["question"]?.jsonPrimitive?.content?.trim().orEmpty()
                    val optionsRaw = obj["options"] as? JsonArray
                    val options =
                        optionsRaw
                            ?.jsonArray
                            ?.mapNotNull { opt ->
                                val o = opt as? JsonObject ?: return@mapNotNull null
                                val label = o["label"]?.jsonPrimitive?.content?.trim().orEmpty()
                                val description = o["description"]?.jsonPrimitive?.content?.trim().orEmpty()
                                if (label.isBlank() && description.isBlank()) return@mapNotNull null
                                UserInputOption(label = label, description = description)
                            }
                            .orEmpty()
                    UserInputQuestion(id = id, header = header, question = question, options = options)
                }
                .orEmpty()
                .filter { it.options.isNotEmpty() }

        if (questions.isEmpty()) return null

        return PendingUserInput(
            requestId = req.id,
            threadId = threadId,
            turnId = turnId,
            itemId = itemId,
            questions = questions
        )
    }

    fun decideApproval(decision: String) {
        val conn = connections.value.firstOrNull() ?: return
        val pending = _uiState.value.pendingApproval ?: return
        viewModelScope.launch {
            try {
                apiService.respondToApprovalRequest(conn.baseUrl, conn.secret, pending.requestId, decision)
            } catch (e: Exception) {
                Log.w(tag, "Failed to respond to approval request", e)
            } finally {
                _uiState.update { it.copy(pendingApproval = null) }
                maybeShowNextPending()
            }
        }
    }

    fun submitUserInput(answers: Map<String, List<String>>) {
        val conn = connections.value.firstOrNull() ?: return
        val pending = _uiState.value.pendingUserInput ?: return
        viewModelScope.launch {
            try {
                apiService.respondToUserInputRequest(conn.baseUrl, conn.secret, pending.requestId, answers)
            } catch (e: Exception) {
                Log.w(tag, "Failed to respond to user input request", e)
            } finally {
                _uiState.update { it.copy(pendingUserInput = null) }
                maybeShowNextPending()
            }
        }
    }

    // --- Actions ---

    fun sendMessage(text: String) {
        val connection = connections.value.firstOrNull() ?: return
        viewModelScope.launch {
            if (_uiState.value.connectionStatus != ConnectionStatus.Healthy) {
                _uiState.update { it.copy(error = "Not connected to server", isSending = false) }
                return@launch
            }
            pollJob?.cancel()
            // Prime the "last event" timestamp so the poll fallback doesn't immediately treat the stream as quiet
            // before we receive the first WS notification (this race is common on brand-new threads).
            val now = System.currentTimeMillis()
            sendingStartedAtMs = now
            lastEventAtMs = now
            _uiState.update { it.copy(isSending = true, error = null, activeTurnId = null, pendingUserMessage = text) }
            
            try {
                // 1. Ensure thread
                var thread = _uiState.value.currentThread
                if (thread == null) {
                    val resp =
                        apiService.startThread(
                            connection.baseUrl,
                            connection.secret,
                            cwd = _uiState.value.selectedCwd
                        )
                    thread = resp.result?.thread ?: throw Exception(resp.error?.message)
                    thread =
                        thread.copy(
                            clientModel = _uiState.value.selectedModelId,
                            clientEffort = _uiState.value.selectedEffort,
                        )
                    threadRepository.upsertThread(connection.id, thread)
                    selectThreadId(connection.id, thread.id)
                } else {
                    apiService.resumeThread(connection.baseUrl, connection.secret, thread.id)
                }

                // 2. Start turn
                val effectiveCwd = thread.cwd.takeIf { it.isNotBlank() } ?: _uiState.value.selectedCwd
                val turnResp =
                    apiService.startTurn(
                        connection.baseUrl,
                        connection.secret,
                        thread.id,
                        text,
                        cwd = effectiveCwd,
                        model = _uiState.value.selectedModelId,
                        effort = _uiState.value.selectedEffort,
                    )
                val turnId = turnResp.result?.turn?.id ?: throw Exception(turnResp.error?.message)
                _uiState.update { it.copy(activeTurnId = turnId) }

                // 3. Fallback poll
                pollJob = viewModelScope.launch {
                    pollUntilCompleted(
                        connection = connection,
                        threadId = thread.id,
                        turnId = turnId
                    )
                }
                _uiState.update { it.copy(isHistorySyncing = true) }
                try {
                    refreshThreadsUseCase(connection)
                } catch (e: Exception) {
                    // Refreshing history is non-critical; don't clear the typing indicator if this fails.
                    Log.w(tag, "History refresh failed", e)
                } finally {
                    _uiState.update { it.copy(isHistorySyncing = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSending = false, error = e.message, activeTurnId = null, pendingUserMessage = null) }
            }
        }
    }

    fun selectThread(thread: Thread) {
        val conn = connections.value.firstOrNull()
        if (conn == null) {
            _uiState.update { it.copy(currentThread = thread, selectedCwd = thread.cwd.takeIf { it.isNotBlank() }) }
            return
        }

        viewModelScope.launch {
            // Switching threads should not keep showing the previous thread's typing indicator / pending echo.
            _uiState.update { state ->
                val isDifferent = state.currentThread?.id != thread.id
                if (!isDifferent) state
                else state.copy(isSending = false, activeTurnId = null, pendingUserMessage = null, scrollToTurnId = null)
            }

            // Ensure we have a DB row (history list may be metadata-only).
            val cached = threadRepository.getThread(conn.id, thread.id)
            if (cached == null) {
                threadRepository.upsertThread(conn.id, thread)
            }
            selectThreadId(conn.id, thread.id)
            refreshThreadFromServer(conn, thread.id)
        }
    }

    fun startNewSession(cwd: String? = null) {
        val normalized = cwd?.takeIf { it.isNotBlank() }
        selectedThreadId = null
        observeThreadJob?.cancel()
        observeThreadJob = null
        _uiState.update {
            it.copy(
                currentThread = null,
                activeTurnId = null,
                isSending = false,
                pendingUserMessage = null,
                selectedCwd = normalized ?: it.selectedCwd
            )
        }
    }

    private fun selectThreadId(connectionId: String, threadId: String) {
        selectedThreadId = threadId
        observeThreadJob?.cancel()
        observeThreadJob =
            viewModelScope.launch {
                threadRepository.observeThread(connectionId, threadId).collect { t ->
                    if (t == null) return@collect
                    val now = System.currentTimeMillis()
                    val runningTurn = t.turns.lastOrNull { it.status == TurnStatus.inProgress }

                    if (_uiState.value.isSending || runningTurn != null) {
                        lastEventAtMs = now
                    }

                    _uiState.update { state ->
                        val activeTurnId = state.activeTurnId
                        val activeTurn = activeTurnId?.let { id -> t.turns.firstOrNull { it.id == id } }
                        val isTerminal =
                            activeTurn?.status == TurnStatus.completed ||
                                activeTurn?.status == TurnStatus.interrupted ||
                                activeTurn?.status == TurnStatus.failed

                        var next =
                            state.copy(
                                currentThread = t,
                                selectedCwd = t.cwd.takeIf { it.isNotBlank() }
                            )

                        if (next.selectedModelId.isNullOrBlank() && !t.clientModel.isNullOrBlank()) {
                            next = next.copy(selectedModelId = t.clientModel)
                        }
                        if (next.selectedEffort.isNullOrBlank() && !t.clientEffort.isNullOrBlank()) {
                            next = next.copy(selectedEffort = t.clientEffort)
                        }

                        // Clear the local echo once the authoritative user message is present.
                        if (!next.pendingUserMessage.isNullOrBlank()) {
                            val inTurnId = activeTurnId ?: runningTurn?.id
                            if (threadHasUserMessage(t, inTurnId, next.pendingUserMessage.orEmpty())) {
                                next = next.copy(pendingUserMessage = null)
                            }
                        }

                        // Keep typing indicator / "Stop" available while a turn is in progress for this thread.
                        if (!isTerminal) {
                            if (runningTurn != null) {
                                next =
                                    next.copy(
                                        isSending = true,
                                        activeTurnId = activeTurnId ?: runningTurn.id
                                    )
                            }
                        } else {
                            next = next.copy(isSending = false, activeTurnId = null, pendingUserMessage = null)
                        }

                        next
                    }

                    val activeConnection = connections.value.firstOrNull()
                    if (activeConnection != null && activeConnection.id == connectionId) {
                        val cwd =
                            t.cwd.takeIf { it.isNotBlank() }
                                ?: t.path.takeIf { it.isNotBlank() }
                        viewModelScope.launch { refreshControlsForConnection(activeConnection, cwd, force = false) }
                    }
                }
            }
    }

    private fun threadHasUserMessage(thread: Thread, turnId: String?, expectedText: String): Boolean {
        val needle = expectedText.trim()
        if (needle.isBlank()) return false

        val turns =
            when {
                !turnId.isNullOrBlank() -> thread.turns.filter { it.id == turnId }
                else -> thread.turns
            }
        return turns.any { turn ->
            turn.items.any { item ->
                val msg = item as? ThreadItem.UserMessage ?: return@any false
                msg.content.joinToString(separator = "") { it.text.orEmpty() }.trim() == needle
            }
        }
    }

    private fun refreshThreadFromServer(connection: Connection, threadId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isThreadSyncing = true) }
            try {
                val resp = apiService.readThread(connection.baseUrl, connection.secret, threadId)
                val serverThread = resp.result?.thread ?: return@launch
                val existing = threadRepository.getThread(connection.id, threadId)
                val mergedTurns =
                    if (existing != null && existing.turns.isNotEmpty()) existing.turns else serverThread.turns
                threadRepository.upsertThread(
                    connection.id,
                    serverThread.copy(
                        turns = mergedTurns,
                        clientModel = existing?.clientModel,
                        clientEffort = existing?.clientEffort,
                    )
                )
            } catch (e: Exception) {
                Log.w(tag, "Thread refresh failed", e)
            } finally {
                _uiState.update { it.copy(isThreadSyncing = false) }
            }
        }
    }

    // --- Connection health ---

    private fun checkConnection(connection: Connection) {
        connectionCheckJob?.cancel()
        _uiState.update { it.copy(connectionStatus = ConnectionStatus.Checking) }
        connectionCheckJob = viewModelScope.launch {
            val ok =
                try {
                    apiService.ping(connection.baseUrl, connection.secret)
                } catch (_: Exception) {
                    false
                }
            _uiState.update { it.copy(connectionStatus = if (ok) ConnectionStatus.Healthy else ConnectionStatus.Unhealthy) }

            if (ok) {
                // Populate controls (models/skills) as soon as the server is reachable.
                val cwd =
                    _uiState.value.currentThread?.cwd?.takeIf { it.isNotBlank() }
                        ?: _uiState.value.currentThread?.path?.takeIf { it.isNotBlank() }
                        ?: _uiState.value.selectedCwd
                refreshControlsForConnection(connection, cwd, force = false)
            }

            // Always do one initial history sync per connection on app start, even if the local DB
            // has already emitted (and set isHistoryInitialized=true).
            if (ok && didInitialHistorySyncForConnectionId != connection.id) {
                didInitialHistorySyncForConnectionId = connection.id
                try {
                    _uiState.update { it.copy(isHistorySyncing = true) }
                    refreshThreadsUseCase(connection)
                } catch (e: Exception) {
                    Log.w(tag, "History sync failed", e)
                } finally {
                    withContext(NonCancellable) {
                        _uiState.update { it.copy(isHistorySyncing = false) }
                    }
                }
            }
        }
    }

    private suspend fun pollUntilCompleted(
        connection: Connection,
        threadId: String,
        turnId: String
    ) {
        repeat(30) {
            delay(2000)

            // Fallback: thread/read exposes turn completion status even if /events dropped.
            val done = try {
                val resp = apiService.readThread(connection.baseUrl, connection.secret, threadId)
                val serverThread = resp.result?.thread
                val turns = serverThread?.turns.orEmpty()

                // Only consider completion once the specific turn exists and is terminal.
                val t = turns.firstOrNull { it.id == turnId }
                if (t == null) {
                    false
                } else {
                    t.status == TurnStatus.completed ||
                        t.status == TurnStatus.interrupted ||
                        t.status == TurnStatus.failed
                }
            } catch (_: Exception) {
                false
            }

            if (done) {
                // If WS is still actively streaming events, keep the indicator on until we see turn/completed.
                // This prevents premature stops when thread/read observes completion earlier than the event stream.
                val now = System.currentTimeMillis()
                val last = if (lastEventAtMs != 0L) lastEventAtMs else sendingStartedAtMs
                val quietForMs = now - last
                if (quietForMs >= 8_000L) {
                    _uiState.update { it.copy(isSending = false, activeTurnId = null, pendingUserMessage = null) }
                    return@repeat
                }
            }
        }
        _uiState.update { it.copy(isSending = false, activeTurnId = null, pendingUserMessage = null) }
    }

    fun selectConnection(connection: Connection) {
        viewModelScope.launch {
            connectionRepository.updateLastUsed(connection.id)
            _uiState.update { it.copy(currentThread = null, activeTurnId = null, isSending = false, pendingUserMessage = null) }
        }
    }

    fun deleteConnection(connectionId: String) {
        viewModelScope.launch {
            try {
                connectionRepository.deleteConnection(connectionId)
            } catch (e: Exception) {
                Log.w(tag, "Failed to delete connection", e)
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun stopCurrentTurn() {
        val connection = connections.value.firstOrNull() ?: return
        val threadId = _uiState.value.currentThread?.id ?: return
        val turnId = _uiState.value.activeTurnId ?: currentRunningTurnId() ?: return
        viewModelScope.launch {
            try {
                pollJob?.cancel()
                apiService.interruptTurn(connection.baseUrl, connection.secret, threadId, turnId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun currentRunningTurnId(): String? =
        _uiState.value.currentThread?.turns?.firstOrNull { it.status == TurnStatus.inProgress }?.id

    override fun onCleared() {
        observeThreadJob?.cancel()
        super.onCleared()
    }
}
