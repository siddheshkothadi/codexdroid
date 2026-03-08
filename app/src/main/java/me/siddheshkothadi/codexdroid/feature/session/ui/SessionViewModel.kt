package me.siddheshkothadi.codexdroid.feature.session.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.siddheshkothadi.codexdroid.codex.*
import me.siddheshkothadi.codexdroid.codex.requests.ApprovalPendingRequest
import me.siddheshkothadi.codexdroid.codex.requests.InMemoryPendingRequestQueue
import me.siddheshkothadi.codexdroid.codex.requests.PendingRequestParser
import me.siddheshkothadi.codexdroid.codex.requests.PendingRequestQueue
import me.siddheshkothadi.codexdroid.codex.requests.ApprovalRules
import me.siddheshkothadi.codexdroid.codex.requests.UnknownPendingRequest
import me.siddheshkothadi.codexdroid.codex.requests.UserInputPendingRequest
import me.siddheshkothadi.codexdroid.domain.model.Connection
import me.siddheshkothadi.codexdroid.domain.usecase.*
import javax.inject.Inject

enum class FollowUpMessageBehavior {
    Queue,
    Steer,
}

enum class ComposerSubmitIntent {
    Default,
    Queue,
    Steer,
}

data class QueuedMessageUi(
    val id: String,
    val text: String,
    val createdAt: Long,
)

data class CollaborationModeOptionUi(
    val id: String,
    val mode: String,
    val label: String,
    val developerInstructions: String? = null,
)

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
    val pendingApproval: ApprovalPendingRequest? = null,
    val pendingUserInput: UserInputPendingRequest? = null,
    val pendingUnknownRequest: UnknownPendingRequest? = null,
    val models: List<ModelOptionUi> = emptyList(),
    val skills: List<SkillOptionUi> = emptyList(),
    val selectedModelId: String? = null,
    val selectedEffort: String? = null,
    val planModeEnabled: Boolean = false,
    val followUpMessageBehavior: FollowUpMessageBehavior = FollowUpMessageBehavior.Queue,
    val queuedMessages: List<QueuedMessageUi> = emptyList(),
    val queuePausedReason: String? = null,
    val collaborationModes: List<CollaborationModeOptionUi> = emptyList(),
    val collaborationModeSupported: Boolean = false,
    val controlsError: String? = null,
    val ttsNotice: String? = null,
    val lastPlanTurnId: String? = null,
    val planReadyTurnId: String? = null,
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
    private val getThreadUseCase: GetThreadUseCase,
    private val observeThreadUseCase: ObserveThreadUseCase,
    private val upsertThreadUseCase: UpsertThreadUseCase,
    private val renameThreadUseCase: RenameThreadUseCase,
    private val archiveThreadUseCase: ArchiveThreadUseCase,
    private val startThreadUseCase: StartThreadUseCase,
    private val resumeThreadUseCase: ResumeThreadUseCase,
    private val startTurnUseCase: StartTurnUseCase,
    private val readThreadUseCase: ReadThreadUseCase,
    private val interruptTurnUseCase: InterruptTurnUseCase,
    private val listModelsUseCase: ListModelsUseCase,
    private val listSkillsUseCase: ListSkillsUseCase,
    private val listCollaborationModesUseCase: ListCollaborationModesUseCase,
    private val readConfigUseCase: ReadConfigUseCase,
    private val steerTurnUseCase: SteerTurnUseCase,
    private val respondToApprovalRequestUseCase: RespondToApprovalRequestUseCase,
    private val respondToUserInputRequestUseCase: RespondToUserInputRequestUseCase,
    private val getSessionControlDefaultsUseCase: GetSessionControlDefaultsUseCase,
    private val saveSessionControlDefaultsUseCase: SaveSessionControlDefaultsUseCase,
    private val getApprovalAllowRulesUseCase: GetApprovalAllowRulesUseCase,
    private val addApprovalAllowRuleUseCase: AddApprovalAllowRuleUseCase,
    private val pingConnectionUseCase: PingConnectionUseCase,
    private val markConnectionUsedUseCase: MarkConnectionUsedUseCase,
    private val deleteConnectionUseCase: DeleteConnectionUseCase,
    private val synthesizeSarvamSpeechUseCase: SynthesizeSarvamSpeechUseCase,
    private val eventRouter: CodexEventRouter,
    private val codexAppLifecycle: CodexAppLifecycle,
) : ViewModel() {
    private val tag = "SessionViewModel"

    // Used by the poll fallback to avoid turning off the typing indicator while WS events are still flowing.
    private var lastEventAtMs: Long = 0L
    private var sendingStartedAtMs: Long = 0L
    private var autoSelectedConnectionId: String? = null
    private var didInitialHistorySyncForConnectionId: String? = null
    private var lastControlsKey: String? = null
    private val ttsNoticeLock = Any()
    private var didShowSarvamFallbackNotice: Boolean = false
    private val pendingRequestQueue: PendingRequestQueue = InMemoryPendingRequestQueue()
    private val queuedMessagesByThread = mutableMapOf<String, MutableList<QueuedMessageUi>>()

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
        observeForegroundReconnect()
        observeServerRequests()
    }

    fun setSelectedModelId(modelId: String?) {
        val normalized = modelId?.takeIf { v -> v.isNotBlank() }
        _uiState.update { it.copy(selectedModelId = normalized) }
    }

    fun setSelectedEffort(effort: String?) {
        val normalized = effort?.takeIf { v -> v.isNotBlank() }
        _uiState.update { it.copy(selectedEffort = normalized) }
    }

    fun setPlanModeEnabled(enabled: Boolean) {
        _uiState.update { it.copy(planModeEnabled = enabled) }
    }

    fun setFollowUpMessageBehavior(behavior: FollowUpMessageBehavior) {
        _uiState.update { it.copy(followUpMessageBehavior = behavior) }
        val modelSlug = resolveModelSlug(_uiState.value.selectedModelId)
        persistControlPreferences(model = modelSlug, effort = _uiState.value.selectedEffort, followUpMessageBehavior = behavior)
    }

    fun applyModelSelection(modelId: String?) {
        val normalizedModel = modelId?.takeIf { it.isNotBlank() }
        val models = _uiState.value.models
        val nextModel =
            normalizedModel?.let { id ->
                models.firstOrNull { option -> option.id == id || option.model == id }
            }
        val supportedEfforts =
            nextModel?.supportedReasoningEfforts
                ?.map { it.reasoningEffort }
                ?.filter { it.isNotBlank() }
                .orEmpty()
        val defaultEffort = nextModel?.defaultReasoningEffort?.takeIf { it.isNotBlank() }
        val currentEffort = _uiState.value.selectedEffort?.takeIf { it.isNotBlank() }
        val nextEffort =
            when {
                currentEffort != null &&
                    (supportedEfforts.isEmpty() || currentEffort in supportedEfforts) ->
                    currentEffort
                defaultEffort != null &&
                    (supportedEfforts.isEmpty() || defaultEffort in supportedEfforts) ->
                    defaultEffort
                supportedEfforts.isNotEmpty() ->
                    supportedEfforts.first()
                else -> null
            }

        _uiState.update {
            it.copy(
                selectedModelId = normalizedModel,
                selectedEffort = nextEffort,
            )
        }
        val modelSlug = resolveModelSlug(modelId = normalizedModel)
        persistControlPreferences(model = modelSlug, effort = nextEffort)
    }

    fun applyEffortSelection(effort: String?) {
        val normalizedEffort = effort?.takeIf { it.isNotBlank() }
        val selectedModel =
            _uiState.value.selectedModelId?.let { id ->
                _uiState.value.models.firstOrNull { option -> option.id == id || option.model == id }
            }
        val supportedEfforts =
            selectedModel?.supportedReasoningEfforts
                ?.map { it.reasoningEffort }
                ?.filter { it.isNotBlank() }
                .orEmpty()
        val nextEffort =
            when {
                normalizedEffort == null -> null
                supportedEfforts.isEmpty() || normalizedEffort in supportedEfforts -> normalizedEffort
                else -> _uiState.value.selectedEffort?.takeIf { it.isNotBlank() }
            }
        _uiState.update { it.copy(selectedEffort = nextEffort) }
        val modelSlug = resolveModelSlug(_uiState.value.selectedModelId)
        persistControlPreferences(model = modelSlug, effort = nextEffort)
    }

    fun saveControlsSelection(
        modelId: String?,
        effort: String?,
        planModeEnabled: Boolean,
        followUpMessageBehavior: FollowUpMessageBehavior = _uiState.value.followUpMessageBehavior,
    ) {
        val normalizedModel = modelId?.takeIf { it.isNotBlank() }
        val normalizedEffort = effort?.takeIf { it.isNotBlank() }
        _uiState.update {
            it.copy(
                selectedModelId = normalizedModel,
                selectedEffort = normalizedEffort,
                planModeEnabled = planModeEnabled,
                followUpMessageBehavior = followUpMessageBehavior,
            )
        }
        val modelSlug = resolveModelSlug(modelId = normalizedModel)
        persistControlPreferences(model = modelSlug, effort = normalizedEffort, followUpMessageBehavior = followUpMessageBehavior)
    }

    private fun persistControlPreferences(
        model: String?,
        effort: String?,
        followUpMessageBehavior: FollowUpMessageBehavior = _uiState.value.followUpMessageBehavior,
    ) {
        viewModelScope.launch {
            runCatching {
                saveSessionControlDefaultsUseCase(
                    model,
                    effort,
                    followUpMessageBehavior = followUpMessageBehavior.name.lowercase(),
                )
            }
                .onFailure { Log.w(tag, "Failed to persist session control defaults", it) }

            val conn = connections.value.firstOrNull() ?: return@launch
            val thread = _uiState.value.currentThread ?: return@launch
            val updated = thread.copy(clientModel = model, clientEffort = effort)
            upsertThreadUseCase(conn.id, updated)
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
            val modelsResp = runCatching { listModelsUseCase(connection.baseUrl, connection.secret) }.getOrNull()
            val skillsResp = runCatching { listSkillsUseCase(connection.baseUrl, connection.secret, cwd) }.getOrNull()
            val collaborationModesResp =
                runCatching { listCollaborationModesUseCase(connection.baseUrl, connection.secret) }.getOrNull()
            val configResp = runCatching { readConfigUseCase(connection.baseUrl, connection.secret) }.getOrNull()
            val appDefaults = runCatching { getSessionControlDefaultsUseCase() }.getOrNull()

            val models = modelsResp?.result?.let { SessionControlsParser.parseModels(it) }.orEmpty()
            val skills = skillsResp?.result?.let { SessionControlsParser.parseSkills(it) }.orEmpty()
            val collaborationModes = collaborationModesResp?.result?.let(::parseCollaborationModes).orEmpty()
            val (configModel, configEffort) =
                configResp?.result?.let { SessionControlsParser.parseConfigPreferences(it) } ?: (null to null)

            val error =
                when {
                    modelsResp?.error != null -> modelsResp.error?.message
                    skillsResp?.error != null -> skillsResp.error?.message
                    modelsResp == null && skillsResp == null -> "Failed to load session controls."
                    else -> null
                }

            _uiState.update { state ->
                var next =
                    state.copy(
                        models = models,
                        skills = skills,
                        collaborationModes = collaborationModes,
                        collaborationModeSupported = collaborationModesResp?.error == null && collaborationModes.isNotEmpty(),
                        controlsError = error,
                    )

                val threadPreferredModel =
                    next.currentThread?.clientModel
                        ?.takeIf { it.isNotBlank() }
                        ?: appDefaults?.model?.takeIf { it.isNotBlank() }
                        ?: configModel

                val resolvedModelId =
                    SessionControlsParser.resolveModelId(models, next.selectedModelId)
                        ?: SessionControlsParser.resolveModelId(models, threadPreferredModel)
                        ?: models.firstOrNull { it.isDefault }?.id
                        ?: models.firstOrNull()?.id

                if (resolvedModelId != next.selectedModelId) {
                    next = next.copy(selectedModelId = resolvedModelId)
                }

                val selectedModel =
                    next.selectedModelId?.let { id ->
                        models.firstOrNull { it.id == id || it.model == id }
                    }
                val supportedEfforts =
                    selectedModel?.supportedReasoningEfforts?.map { it.reasoningEffort }?.filter { it.isNotBlank() }.orEmpty()
                val defaultEffort = selectedModel?.defaultReasoningEffort?.takeIf { it.isNotBlank() }

                val threadPreferredEffort =
                    next.currentThread?.clientEffort?.takeIf { it.isNotBlank() }
                        ?: appDefaults?.effort?.takeIf { it.isNotBlank() }
                        ?: configEffort
                val resolvedEffort =
                    when {
                        !next.selectedEffort.isNullOrBlank() &&
                            (supportedEfforts.isEmpty() || next.selectedEffort in supportedEfforts) ->
                            next.selectedEffort
                        threadPreferredEffort != null && (supportedEfforts.isEmpty() || threadPreferredEffort in supportedEfforts) ->
                            threadPreferredEffort
                        defaultEffort != null && (supportedEfforts.isEmpty() || defaultEffort in supportedEfforts) ->
                            defaultEffort
                        supportedEfforts.isNotEmpty() ->
                            supportedEfforts.firstOrNull()
                        else -> null
                    }

                if (resolvedEffort != next.selectedEffort) {
                    next = next.copy(selectedEffort = resolvedEffort)
                }

                val resolvedFollowUpBehavior =
                    appDefaults?.followUpMessageBehavior
                        ?.trim()
                        ?.lowercase()
                        ?.let {
                            if (it == "steer") FollowUpMessageBehavior.Steer else FollowUpMessageBehavior.Queue
                        }
                        ?: next.followUpMessageBehavior
                if (resolvedFollowUpBehavior != next.followUpMessageBehavior) {
                    next = next.copy(followUpMessageBehavior = resolvedFollowUpBehavior)
                }

                next
            }
            lastControlsKey = key
        } finally {
            _uiState.update { it.copy(isControlsSyncing = false) }
        }
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
        val cached = getThreadUseCase(connection.id, threadId)
        if (cached == null) {
            runCatching {
                val resp = readThreadUseCase(connection.baseUrl, connection.secret, threadId)
                resp.result?.thread?.let { upsertThreadUseCase(connection.id, it) }
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
                                selected = getThreadUseCase(active.id, threads.first().id) ?: threads.first()
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
                        checkConnection(active, retryOnFailure = true)
                    } else {
                        _uiState.update { it.copy(connectionStatus = ConnectionStatus.Unknown) }
                    }
                }
        }
    }

    private fun observeForegroundReconnect() {
        viewModelScope.launch {
            codexAppLifecycle.isForeground
                .filter { it }
                .collect {
                    val active = connections.value.firstOrNull() ?: return@collect
                    checkConnection(active, retryOnFailure = true)
                }
        }
    }

    private fun observeServerRequests() {
        viewModelScope.launch {
            eventRouter.observeServerRequests().collect { req ->
                val parsed = PendingRequestParser.parse(req)
                if (parsed is ApprovalPendingRequest && shouldAutoAcceptApproval(parsed)) {
                    val conn = connections.value.firstOrNull()
                    if (conn != null) {
                        runCatching {
                            respondToApprovalRequestUseCase(
                                conn.baseUrl,
                                conn.secret,
                                parsed.requestId,
                                "accept",
                            )
                        }.onFailure {
                            Log.w(tag, "Failed to auto-accept approval request", it)
                            pendingRequestQueue.enqueue(parsed)
                        }
                    } else {
                        pendingRequestQueue.enqueue(parsed)
                    }
                } else {
                    pendingRequestQueue.enqueue(parsed)
                }
                maybeShowNextPending()
            }
        }
    }

    private suspend fun shouldAutoAcceptApproval(request: ApprovalPendingRequest): Boolean {
        val conn = connections.value.firstOrNull() ?: return false
        val command = ApprovalRules.extractCommandTokens(request.params) ?: return false
        val rules =
            runCatching {
                getApprovalAllowRulesUseCase(
                    connectionId = conn.id,
                    workspaceKey = currentWorkspaceKey(),
                )
            }.getOrElse {
                Log.w(tag, "Failed to read approval allow rules", it)
                emptyList()
            }
        return ApprovalRules.matchesCommandPrefix(command, rules)
    }

    private fun maybeShowNextPending() {
        _uiState.update { state ->
            val next =
                state.copy(
                pendingApproval = pendingRequestQueue.nextApproval(state.pendingApproval),
                pendingUserInput = pendingRequestQueue.nextUserInput(state.pendingUserInput),
                pendingUnknownRequest = pendingRequestQueue.nextUnknown(state.pendingUnknownRequest),
                )
            next.copy(queuePausedReason = queuePausedReason(next))
        }
    }

    fun decideApproval(decision: String) {
        val conn = connections.value.firstOrNull() ?: return
        val pending = _uiState.value.pendingApproval ?: return
        viewModelScope.launch {
            try {
                respondToApprovalRequestUseCase(conn.baseUrl, conn.secret, pending.requestId, decision)
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
                respondToUserInputRequestUseCase(conn.baseUrl, conn.secret, pending.requestId, answers)
            } catch (e: Exception) {
                Log.w(tag, "Failed to respond to user input request", e)
            } finally {
                _uiState.update { it.copy(pendingUserInput = null) }
                maybeShowNextPending()
            }
        }
    }

    fun dismissUnknownRequest() {
        _uiState.update { it.copy(pendingUnknownRequest = null) }
        maybeShowNextPending()
    }

    suspend fun synthesizeTurnSpeechOrNull(text: String): ByteArray? {
        return try {
            synthesizeSarvamSpeechUseCase(text).audioBytes
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(tag, "Sarvam TTS failed, using Android fallback", e)
            maybePublishSarvamFallbackNotice()
            null
        }
    }

    fun onTtsNoticeShown() {
        _uiState.update { it.copy(ttsNotice = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun maybePublishSarvamFallbackNotice() {
        val shouldNotify =
            synchronized(ttsNoticeLock) {
                if (didShowSarvamFallbackNotice) false
                else {
                    didShowSarvamFallbackNotice = true
                    true
                }
            }
        if (!shouldNotify) return
        _uiState.update { it.copy(ttsNotice = "Sarvam unavailable. Using Android TTS.") }
    }

    // --- Actions ---

    fun sendMessage(text: String, submitIntent: ComposerSubmitIntent = ComposerSubmitIntent.Default) {
        val connection = connections.value.firstOrNull() ?: return
        viewModelScope.launch {
            val trimmed = text.trim()
            if (trimmed.isBlank()) return@launch
            if (_uiState.value.connectionStatus != ConnectionStatus.Healthy) {
                _uiState.update { it.copy(error = "Not connected to server", isSending = false) }
                return@launch
            }
            val currentThreadId = _uiState.value.currentThread?.id
            if (_uiState.value.isSending && currentThreadId != null) {
                when (resolveFollowUpIntent(submitIntent)) {
                    ComposerSubmitIntent.Queue -> {
                        enqueueQueuedMessage(currentThreadId, trimmed)
                        return@launch
                    }
                    ComposerSubmitIntent.Steer -> {
                        if (attemptSteer(connection, currentThreadId, trimmed)) return@launch
                        enqueueQueuedMessage(currentThreadId, trimmed)
                        return@launch
                    }
                    ComposerSubmitIntent.Default -> Unit
                }
            }
            pollJob?.cancel()
            // Prime the "last event" timestamp so the poll fallback doesn't immediately treat the stream as quiet
            // before we receive the first WS notification (this race is common on brand-new threads).
            val now = System.currentTimeMillis()
            sendingStartedAtMs = now
            lastEventAtMs = now
            val shouldHidePendingMessage = trimmed.startsWith("[[cm_plan_ready:")
            _uiState.update { it.copy(isSending = true, error = null, activeTurnId = null, pendingUserMessage = trimmed) }
            if (shouldHidePendingMessage) {
                _uiState.update { it.copy(pendingUserMessage = null, planReadyTurnId = null) }
            }
            
            try {
                val resolvedModel = resolveModelSlug(_uiState.value.selectedModelId)
                val resolvedEffort = _uiState.value.selectedEffort?.takeIf { it.isNotBlank() }
                runCatching {
                    saveSessionControlDefaultsUseCase(
                        resolvedModel,
                        resolvedEffort,
                        followUpMessageBehavior = _uiState.value.followUpMessageBehavior.name.lowercase(),
                    )
                }

                // 1. Ensure thread
                var thread = _uiState.value.currentThread
                if (thread == null) {
                    val resp =
                        startThreadUseCase(
                            connection.baseUrl,
                            connection.secret,
                            cwd = _uiState.value.selectedCwd
                        )
                    thread = resp.result?.thread ?: throw Exception(resp.error?.message)
                    thread =
                        thread.copy(
                            clientModel = resolvedModel,
                            clientEffort = resolvedEffort,
                        )
                    upsertThreadUseCase(connection.id, thread)
                    selectThreadId(connection.id, thread.id)
                } else {
                    resumeThreadUseCase(connection.baseUrl, connection.secret, thread.id)
                }

                // 2. Start turn
                val effectiveCwd = thread.cwd.takeIf { it.isNotBlank() } ?: _uiState.value.selectedCwd
                val collaborationMode =
                    if (_uiState.value.planModeEnabled) {
                        buildPlanCollaborationMode(
                            model = resolvedModel,
                            effort = resolvedEffort,
                        )
                    } else {
                        null
                    }
                Log.d(
                    tag,
                    "Starting turn threadId=${thread.id} planMode=${_uiState.value.planModeEnabled} collaborationMode=${collaborationMode?.toString() ?: "null"}"
                )
                val turnResp =
                    startTurnUseCase(
                        baseUrl = connection.baseUrl,
                        secret = connection.secret,
                        threadId = thread.id,
                        text = trimmed,
                        cwd = effectiveCwd,
                        model = resolvedModel,
                        effort = resolvedEffort,
                        collaborationMode = collaborationMode,
                    )
                if (turnResp.error != null) {
                    Log.w(
                        tag,
                        "turn/start rpc error threadId=${thread.id} message=${turnResp.error.message}"
                    )
                } else {
                    Log.d(
                        tag,
                        "turn/start ok threadId=${thread.id} turnId=${turnResp.result?.turn?.id.orEmpty()}"
                    )
                }
                val turnId = turnResp.result?.turn?.id ?: throw Exception(turnResp.error?.message)
                _uiState.update {
                    it.copy(
                        activeTurnId = turnId,
                        planReadyTurnId = null,
                    )
                }

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
                Log.w(tag, "sendMessage failed", e)
                val maybeCapabilityError = e.message?.takeIf { it.contains("experimentalApi capability", ignoreCase = true) }
                _uiState.update {
                    it.copy(
                        isSending = false,
                        error = maybeCapabilityError ?: e.message,
                        activeTurnId = null,
                        pendingUserMessage = null,
                        collaborationModeSupported =
                            if (maybeCapabilityError != null) false else it.collaborationModeSupported,
                    )
                }
            }
        }
    }

    fun sendPlanReadyAcceptance() {
        _uiState.update { it.copy(planModeEnabled = false, planReadyTurnId = null) }
        sendMessage("[[cm_plan_ready:accept]] Implement this plan.")
    }

    fun sendPlanReadyChanges(changes: String) {
        val trimmed = changes.trim()
        if (trimmed.isBlank()) return
        _uiState.update { it.copy(planModeEnabled = true, planReadyTurnId = null) }
        sendMessage("[[cm_plan_ready:changes]] Update the plan with these changes:\n\n$trimmed")
    }

    fun steerQueuedMessage(messageId: String) {
        val connection = connections.value.firstOrNull() ?: return
        val threadId = _uiState.value.currentThread?.id ?: return
        val message =
            queuedMessagesByThread[threadId]
                ?.firstOrNull { it.id == messageId }
                ?: return
        viewModelScope.launch {
            if (attemptSteer(connection, threadId, message.text)) {
                removeQueuedMessage(threadId, messageId)
            } else {
                _uiState.update { it.copy(error = "Steer is unavailable for the current turn. Message kept in queue.") }
            }
        }
    }

    fun removeQueuedMessage(messageId: String) {
        val threadId = _uiState.value.currentThread?.id ?: return
        removeQueuedMessage(threadId, messageId)
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
            val cached = getThreadUseCase(conn.id, thread.id)
            if (cached == null) {
                upsertThreadUseCase(conn.id, thread)
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

    private fun buildPlanCollaborationMode(model: String?, effort: String?): JsonObject {
        val planMode =
            _uiState.value.collaborationModes.firstOrNull { mode ->
                mode.id.equals("plan", ignoreCase = true) || mode.mode.equals("plan", ignoreCase = true)
            }
        val normalizedModel = model?.takeIf { it.isNotBlank() }
        val normalizedEffort = effort?.takeIf { it.isNotBlank() }
        val settings =
            buildJsonObject {
                put("developer_instructions", JsonNull)
                normalizedModel?.let { put("model", it) }
                normalizedEffort?.let { put("reasoning_effort", it) }
            }
        return buildJsonObject {
            put("mode", planMode?.mode ?: "plan")
            put("settings", settings)
        }
    }

    private fun resolveModelSlug(modelId: String?): String? {
        val normalized = modelId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val selected =
            _uiState.value.models.firstOrNull { option ->
                option.id == normalized || option.model == normalized
            }
        return selected?.model?.takeIf { it.isNotBlank() } ?: normalized
    }

    private fun currentWorkspaceKey(): String {
        return _uiState.value.currentThread?.cwd?.takeIf { it.isNotBlank() }
            ?: _uiState.value.currentThread?.path?.takeIf { it.isNotBlank() }
            ?: _uiState.value.selectedCwd?.takeIf { it.isNotBlank() }
            ?: "__default__"
    }

    fun renameThread(threadId: String, newName: String) {
        val connection = connections.value.firstOrNull() ?: return
        val normalizedName = newName.trim()
        if (normalizedName.isBlank()) return

        viewModelScope.launch {
            try {
                renameThreadUseCase(connection, threadId, normalizedName)
                refreshThreadsUseCase(connection)
            } catch (e: Exception) {
                Log.w(tag, "Failed to rename thread", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to rename thread.") }
            }
        }
    }

    fun allowApprovalAlways(command: List<String>) {
        val conn = connections.value.firstOrNull() ?: return
        val pending = _uiState.value.pendingApproval ?: return
        val normalized = ApprovalRules.normalizeCommandTokens(command)
        if (normalized.isEmpty()) {
            decideApproval("accept")
            return
        }

        viewModelScope.launch {
            runCatching {
                addApprovalAllowRuleUseCase(
                    connectionId = conn.id,
                    workspaceKey = currentWorkspaceKey(),
                    command = normalized,
                )
            }.onFailure {
                Log.w(tag, "Failed to persist approval allow rule", it)
            }

            try {
                respondToApprovalRequestUseCase(conn.baseUrl, conn.secret, pending.requestId, "accept")
            } catch (e: Exception) {
                Log.w(tag, "Failed to respond to approval request", e)
            } finally {
                _uiState.update { it.copy(pendingApproval = null) }
                maybeShowNextPending()
            }
        }
    }

    fun archiveThread(thread: Thread) {
        val connection = connections.value.firstOrNull() ?: return
        viewModelScope.launch {
            try {
                archiveThreadUseCase(connection, thread.id)
                if (_uiState.value.currentThread?.id == thread.id) {
                    clearCurrentThreadSelection()
                }
                refreshThreadsUseCase(connection)
            } catch (e: Exception) {
                Log.w(tag, "Failed to archive thread", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to delete thread.") }
            }
        }
    }

    fun archiveThreadsInDirectory(cwd: String) {
        val connection = connections.value.firstOrNull() ?: return
        val targetCwd = cwd.trim()
        if (targetCwd.isBlank()) return

        viewModelScope.launch {
            val threadsInDirectory =
                _uiState.value.historyThreads.filter { thread ->
                    thread.cwd.trim() == targetCwd
                }
            if (threadsInDirectory.isEmpty()) return@launch

            var failed = 0
            threadsInDirectory.forEach { thread ->
                try {
                    archiveThreadUseCase(connection, thread.id)
                } catch (e: Exception) {
                    failed += 1
                    Log.w(tag, "Failed to archive thread ${thread.id} in directory $targetCwd", e)
                }
            }

            if (threadsInDirectory.any { it.id == _uiState.value.currentThread?.id }) {
                clearCurrentThreadSelection()
            }

            try {
                refreshThreadsUseCase(connection)
            } catch (e: Exception) {
                Log.w(tag, "Failed to refresh threads after directory delete", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to refresh sessions.") }
                return@launch
            }

            if (failed > 0) {
                _uiState.update {
                    it.copy(error = "Deleted ${threadsInDirectory.size - failed}/${threadsInDirectory.size} sessions.")
                }
            }
        }
    }

    private fun clearCurrentThreadSelection() {
        pollJob?.cancel()
        selectedThreadId = null
        observeThreadJob?.cancel()
        observeThreadJob = null
        _uiState.update {
            it.copy(
                currentThread = null,
                activeTurnId = null,
                isSending = false,
                pendingUserMessage = null,
                scrollToTurnId = null,
                queuedMessages = emptyList(),
                queuePausedReason = null,
                planReadyTurnId = null,
            )
        }
    }

    private fun selectThreadId(connectionId: String, threadId: String) {
        selectedThreadId = threadId
        observeThreadJob?.cancel()
        observeThreadJob =
            viewModelScope.launch {
                observeThreadUseCase(connectionId, threadId).collect { t ->
                    if (t == null) return@collect
                    val now = System.currentTimeMillis()
                    val runningTurn = t.turns.lastOrNull { it.status == TurnStatus.inProgress }
                    val latestPlanTurnId = latestPlanTurnId(t)

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

                        val threadChanged = state.currentThread?.id != t.id
                        var next =
                            state.copy(
                                currentThread = t,
                                selectedCwd = t.cwd.takeIf { it.isNotBlank() },
                                queuedMessages = queuedMessagesByThread[t.id].orEmpty().toList(),
                                lastPlanTurnId = latestPlanTurnId,
                            )

                        if (threadChanged) {
                            next =
                                next.copy(
                                    selectedModelId = t.clientModel?.takeIf { it.isNotBlank() },
                                    selectedEffort = t.clientEffort?.takeIf { it.isNotBlank() },
                                )
                        } else {
                            if (next.selectedModelId.isNullOrBlank() && !t.clientModel.isNullOrBlank()) {
                                next = next.copy(selectedModelId = t.clientModel)
                            }
                            if (next.selectedEffort.isNullOrBlank() && !t.clientEffort.isNullOrBlank()) {
                                next = next.copy(selectedEffort = t.clientEffort)
                            }
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

                        if (latestPlanTurnId != null && latestPlanTurnId != state.lastPlanTurnId) {
                            next = next.copy(planReadyTurnId = latestPlanTurnId)
                        }
                        next = next.copy(queuePausedReason = queuePausedReason(next))

                        next
                    }

                    if (runningTurn == null) {
                        flushQueuedMessages(connectionId = connectionId, thread = t)
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
                val resp = readThreadUseCase(connection.baseUrl, connection.secret, threadId)
                val serverThread = resp.result?.thread ?: return@launch
                val existing = getThreadUseCase(connection.id, threadId)
                val mergedTurns =
                    if (existing != null && existing.turns.isNotEmpty()) existing.turns else serverThread.turns
                upsertThreadUseCase(
                    connection.id,
                    serverThread.copy(
                        turns = mergedTurns,
                        clientName = existing?.clientName,
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

    private fun checkConnection(connection: Connection, retryOnFailure: Boolean = false) {
        connectionCheckJob?.cancel()
        _uiState.update { it.copy(connectionStatus = ConnectionStatus.Checking) }
        connectionCheckJob = viewModelScope.launch {
            var attempt = 0
            while (true) {
                val ok = pingConnectionUseCase(connection)
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

                if (ok || !retryOnFailure) return@launch
                if (connections.value.firstOrNull()?.id != connection.id) return@launch

                attempt += 1
                val backoffMs = minOf(15_000L, 1_000L * (1L shl minOf(attempt, 4)))
                delay(backoffMs)
                if (connections.value.firstOrNull()?.id != connection.id) return@launch
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.Checking) }
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
                val resp = readThreadUseCase(connection.baseUrl, connection.secret, threadId)
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
            markConnectionUsedUseCase(connection.id)
            _uiState.update {
                it.copy(
                    currentThread = null,
                    activeTurnId = null,
                    isSending = false,
                    pendingUserMessage = null,
                    selectedModelId = null,
                    selectedEffort = null,
                    queuedMessages = emptyList(),
                )
            }
        }
    }

    fun deleteConnection(connectionId: String) {
        viewModelScope.launch {
            try {
                deleteConnectionUseCase(connectionId)
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
                interruptTurnUseCase(connection.baseUrl, connection.secret, threadId, turnId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun currentRunningTurnId(): String? =
        _uiState.value.currentThread?.turns?.firstOrNull { it.status == TurnStatus.inProgress }?.id

    private fun latestPlanTurnId(thread: Thread): String? {
        return thread.turns
            .asReversed()
            .firstOrNull { turn ->
                turn.items.any { item ->
                    when (item) {
                        is ThreadItem.PlanUpdate -> item.plan.isNotEmpty() || !item.explanation.isNullOrBlank()
                        is ThreadItem.Plan -> item.text.isNotBlank() || item.status.isNotBlank()
                        else -> false
                    }
                }
            }
            ?.id
    }

    private fun flushQueuedMessages(connectionId: String, thread: Thread) {
        val queue = queuedMessagesByThread[thread.id].orEmpty()
        if (queue.isEmpty()) return
        val state = _uiState.value
        if (state.queuePausedReason != null || state.isSending) return
        val next = queue.firstOrNull() ?: return
        removeQueuedMessage(thread.id, next.id)
        if (connections.value.firstOrNull()?.id != connectionId) return
        viewModelScope.launch {
            sendMessage(next.text, ComposerSubmitIntent.Default)
        }
    }

    private fun resolveFollowUpIntent(submitIntent: ComposerSubmitIntent): ComposerSubmitIntent {
        if (!_uiState.value.isSending) return ComposerSubmitIntent.Default
        return when (submitIntent) {
            ComposerSubmitIntent.Queue -> ComposerSubmitIntent.Queue
            ComposerSubmitIntent.Steer -> ComposerSubmitIntent.Steer
            ComposerSubmitIntent.Default ->
                if (_uiState.value.followUpMessageBehavior == FollowUpMessageBehavior.Steer) {
                    ComposerSubmitIntent.Steer
                } else {
                    ComposerSubmitIntent.Queue
                }
        }
    }

    private suspend fun attemptSteer(connection: Connection, threadId: String, text: String): Boolean {
        val turnId = _uiState.value.activeTurnId ?: currentRunningTurnId() ?: return false
        return runCatching {
            val response =
                steerTurnUseCase(
                    baseUrl = connection.baseUrl,
                    secret = connection.secret,
                    threadId = threadId,
                    turnId = turnId,
                    text = text,
                )
            response.error == null
        }.getOrElse {
            Log.w(tag, "turn/steer failed", it)
            false
        }
    }

    private fun enqueueQueuedMessage(threadId: String, text: String) {
        val queue = queuedMessagesByThread.getOrPut(threadId) { mutableListOf() }
        queue += QueuedMessageUi(
            id = "${System.currentTimeMillis()}-${queue.size}",
            text = text,
            createdAt = System.currentTimeMillis(),
        )
        syncQueuedMessagesForCurrentThread()
    }

    private fun removeQueuedMessage(threadId: String, messageId: String) {
        queuedMessagesByThread[threadId]?.removeAll { it.id == messageId }
        syncQueuedMessagesForCurrentThread()
    }

    private fun syncQueuedMessagesForCurrentThread() {
        val threadId = _uiState.value.currentThread?.id
        val activeQueue =
            if (threadId == null) emptyList()
            else queuedMessagesByThread[threadId].orEmpty().toList()
        _uiState.update {
            it.copy(
                queuedMessages = activeQueue,
                queuePausedReason = queuePausedReason(it),
            )
        }
    }

    private fun queuePausedReason(state: SessionUiState): String? {
        return when {
            state.pendingUserInput != null -> "Waiting for a response to the current input request."
            !state.planReadyTurnId.isNullOrBlank() -> "Waiting for your plan follow-up."
            else -> null
        }
    }

    private fun parseCollaborationModes(result: JsonElement): List<CollaborationModeOptionUi> {
        val root = result as? JsonObject ?: return emptyList()
        val payload = (root["result"] as? JsonObject) ?: root
        val rawModes =
            listOf("data", "items", "modes")
                .firstNotNullOfOrNull { key -> payload[key] as? kotlinx.serialization.json.JsonArray }
                ?: return emptyList()
        return rawModes.mapNotNull { entry ->
            val obj = entry as? JsonObject ?: return@mapNotNull null
            val id = obj["id"]?.toString()?.trim('"').orEmpty().ifBlank {
                obj["mode"]?.toString()?.trim('"').orEmpty()
            }
            if (id.isBlank()) return@mapNotNull null
            val mode = obj["mode"]?.toString()?.trim('"').orEmpty().ifBlank { id }
            val label =
                obj["label"]?.toString()?.trim('"')
                    ?.takeIf { it.isNotBlank() }
                    ?: id.replaceFirstChar { it.uppercase() }
            val developerInstructions =
                (obj["developerInstructions"] ?: obj["developer_instructions"])
                    ?.toString()
                    ?.trim('"')
            CollaborationModeOptionUi(
                id = id,
                mode = mode,
                label = label,
                developerInstructions = developerInstructions,
            )
        }
    }

    override fun onCleared() {
        observeThreadJob?.cancel()
        super.onCleared()
    }
}

