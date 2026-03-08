package me.siddheshkothadi.codexdroid.feature.session.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.siddheshkothadi.codexdroid.codex.*
import me.siddheshkothadi.codexdroid.feature.shared.ui.components.CodexDroidDrawerContent
import me.siddheshkothadi.codexdroid.feature.history.ui.HistoryUiState
import me.siddheshkothadi.codexdroid.ui.theme.CodexTheme
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import me.siddheshkothadi.codexdroid.codex.requests.ApprovalRules
import me.siddheshkothadi.codexdroid.codex.requests.UserInputOption

/**
 * Main session screen for chatting with Codex.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    viewModel: SessionViewModel = hiltViewModel(),
    onAddConnectionClick: () -> Unit = {},
    onEditConnectionClick: (String) -> Unit = {},
    onNoConnections: () -> Unit = {},
    onSkillsClick: (String?) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val connections by viewModel.connections.collectAsStateWithLifecycle()
    var hasSeenConnection by remember { mutableStateOf(false) }
    var showControlsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(connections.size) {
        if (connections.isNotEmpty()) {
            hasSeenConnection = true
        } else if (hasSeenConnection) {
            onNoConnections()
        }
    }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val turnSpeechController =
        rememberTurnTextToSpeechController(
            synthesizeSarvam = { text -> viewModel.synthesizeTurnSpeechOrNull(text) }
        )
    val snackbarHostState = remember { SnackbarHostState() }
    var inputText by remember { mutableStateOf("") }
    var showNewSessionDialog by remember { mutableStateOf(false) }
    var newSessionCwd by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.ttsNotice) {
        val notice = uiState.ttsNotice ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(notice)
        viewModel.onTtsNoticeShown()
    }

    LaunchedEffect(uiState.error) {
        val message = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

    val cwdPresets =
        remember(uiState.historyThreads) {
            uiState.historyThreads
                .mapNotNull { it.cwd.takeIf { cwd -> cwd.isNotBlank() } }
                .distinct()
                .sorted()
        }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            val historyState =
                when {
                    connections.isEmpty() -> HistoryUiState.Empty
                    !uiState.isHistoryInitialized && uiState.isHistorySyncing -> HistoryUiState.Loading
                    uiState.historyThreads.isEmpty() -> HistoryUiState.Empty
                    else -> HistoryUiState.Success(uiState.historyThreads)
                }

            CodexDroidDrawerContent(
                historyState = historyState,
                connections = connections,
                activeThreadId = uiState.currentThread?.id,
                onThreadClick = { thread ->
                    viewModel.selectThread(thread)
                    scope.launch { drawerState.close() }
                },
                onConnectionSelect = { connection ->
                    viewModel.selectConnection(connection)
                    scope.launch { drawerState.close() }
                },
                onEditClick = { connectionId ->
                    onEditConnectionClick(connectionId)
                    scope.launch { drawerState.close() }
                },
                onDeleteClick = { connectionId ->
                    viewModel.deleteConnection(connectionId)
                    scope.launch { drawerState.close() }
                },
                onThreadRename = { thread, newName ->
                    viewModel.renameThread(thread.id, newName)
                },
                onThreadDelete = { thread ->
                    viewModel.archiveThread(thread)
                },
                onStartThreadInCwd = { cwd ->
                    viewModel.startNewSession(cwd = cwd)
                    scope.launch { drawerState.close() }
                },
                onDeleteThreadsInCwd = { cwd ->
                    viewModel.archiveThreadsInDirectory(cwd)
                },
                onSetupClick = {
                    onAddConnectionClick()
                    scope.launch { drawerState.close() }
                },
                connectionStatus = uiState.connectionStatus,
                isSyncing = uiState.isHistorySyncing || uiState.isThreadSyncing
            )
        }
    ) {
        val borderColor = CodexTheme.colors.borderDefault
        val workspaceTitle = remember(uiState.currentThread?.cwd) {
            workspaceFolderName(uiState.currentThread?.cwd)
        }
        val contextLeftPercent =
            remember(uiState.currentThread?.tokenUsage) {
                computeContextLeftPercent(uiState.currentThread?.tokenUsage)
            }
        val topBarTitle =
            when {
                workspaceTitle.isNotBlank() -> workspaceTitle
                else -> connections.firstOrNull()?.name.orEmpty()
            }
        val topBarTitleWithContext =
            remember(topBarTitle, contextLeftPercent) {
                if (contextLeftPercent == null) topBarTitle
                else "$topBarTitle \u00B7 ${contextLeftPercent}%"
            }
        val runningStatus =
            remember(uiState.currentThread, uiState.activeTurnId, uiState.isSending) {
                computeRunningStatusUi(
                    thread = uiState.currentThread,
                    activeTurnId = uiState.activeTurnId,
                    isSending = uiState.isSending,
                )
            }
        val showLiveActivity = uiState.isSending || runningStatus != null
        var runningSinceMs by rememberSaveable { mutableStateOf(0L) }
        LaunchedEffect(showLiveActivity, uiState.activeTurnId) {
            when {
                showLiveActivity && runningSinceMs == 0L -> runningSinceMs = System.currentTimeMillis()
                !showLiveActivity -> runningSinceMs = 0L
            }
        }
        Scaffold(
            modifier = modifier,
            topBar = {
                SessionTopBar(
                    title = topBarTitleWithContext,
                    connectionStatus = uiState.connectionStatus,
                    isSyncing = uiState.isHistorySyncing || uiState.isThreadSyncing,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNewSessionClick = {
                        newSessionCwd =
                            uiState.currentThread?.cwd?.takeIf { it.isNotBlank() }
                                ?: uiState.selectedCwd.orEmpty()
                        showNewSessionDialog = true
                    },
                    showNewSessionButton = uiState.currentThread != null,
                    borderColor = borderColor
                )
            },
            bottomBar = {
                val canSend = uiState.connectionStatus == ConnectionStatus.Healthy
                val selectedModelLabel =
                    uiState.selectedModelId?.let { id ->
                        uiState.models.firstOrNull { it.id == id || it.model == id }?.displayName ?: id
                    }
                val selectedEffortLabel = uiState.selectedEffort?.takeIf { it.isNotBlank() }
                ChatInput(
                    text = inputText,
                    onTextChange = { inputText = it },
                    onSend = { intent ->
                        viewModel.sendMessage(inputText, intent)
                        inputText = ""
                    },
                    onStop = { viewModel.stopCurrentTurn() },
                    onControls = {
                        viewModel.refreshControls()
                        showControlsSheet = true
                    },
                    enabled = canSend,
                    isSending = uiState.isSending
                    ,
                    canSend = canSend,
                    selectedModelLabel = selectedModelLabel,
                    selectedEffortLabel = selectedEffortLabel,
                    planModeEnabled = uiState.planModeEnabled,
                    onDisablePlanMode = { viewModel.setPlanModeEnabled(false) },
                    followUpMessageBehavior = uiState.followUpMessageBehavior,
                    queuedMessages = uiState.queuedMessages,
                    queuePausedReason = uiState.queuePausedReason,
                    onSteerQueued = viewModel::steerQueuedMessage,
                    onDeleteQueued = viewModel::removeQueuedMessage,
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize().background(CodexTheme.colors.bgPrimary)) {
                when {
                    uiState.currentThread != null && uiState.currentThread!!.turns.isEmpty() && uiState.isThreadSyncing -> SessionSkeleton()
                    uiState.currentThread == null && uiState.pendingUserMessage == null -> EmptyView()
                    uiState.currentThread == null && uiState.pendingUserMessage != null ->
                        PendingConversationView(
                            pendingUserMessage = uiState.pendingUserMessage!!,
                            showTypingIndicator = false,
                            runningStatus = runningStatus,
                            runningSinceMs = runningSinceMs,
                        )
                    else ->
                        MessageList(
                            thread = uiState.currentThread!!,
                            isSending = uiState.isSending,
                            pendingUserMessage = uiState.pendingUserMessage,
                            activeTurnId = uiState.activeTurnId,
                            showLiveActivityInList = true,
                            runningStatus = runningStatus,
                            runningSinceMs = runningSinceMs,
                            scrollToTurnId = uiState.scrollToTurnId,
                            turnSpeechController = turnSpeechController,
                            onScrollToTurnHandled = { viewModel.clearScrollTarget() },
                            planReadyTurnId = uiState.planReadyTurnId,
                            onPlanAccept = { viewModel.sendPlanReadyAcceptance() },
                            onPlanSubmitChanges = { viewModel.sendPlanReadyChanges(it) },
                        )
                }
            }
        }
    }

    if (showNewSessionDialog) {
        AlertDialog(
            onDismissRequest = { showNewSessionDialog = false },
            title = { Text("New session workspace") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newSessionCwd,
                        onValueChange = { newSessionCwd = it },
                        label = { Text("Working directory (cwd)") },
                        placeholder = { Text("e.g. /Users/me/projects/repo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (cwdPresets.isNotEmpty()) {
                        Text(
                            text = "Recent",
                            style = MaterialTheme.typography.labelMedium,
                            color = CodexTheme.colors.textSecondary
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(cwdPresets) { cwd ->
                                AssistChip(
                                    onClick = { newSessionCwd = cwd },
                                    label = { Text(cwd, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.startNewSession(cwd = newSessionCwd.trim())
                        showNewSessionDialog = false
                    }
                ) { Text("Start") }
            },
            dismissButton = {
                TextButton(onClick = { showNewSessionDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showControlsSheet) {
        ControlsBottomSheet(
            uiState = uiState,
            onDismiss = { showControlsSheet = false },
            onPlanModeChange = { enabled ->
                viewModel.setPlanModeEnabled(enabled)
            },
            onModelSelect = { modelId ->
                viewModel.applyModelSelection(modelId)
            },
            onEffortSelect = { effort ->
                viewModel.applyEffortSelection(effort)
            },
            onSkillsClick = {
                val cwd =
                    uiState.currentThread?.cwd?.takeIf { it.isNotBlank() }
                        ?: uiState.currentThread?.path?.takeIf { it.isNotBlank() }
                        ?: uiState.selectedCwd
                showControlsSheet = false
                onSkillsClick(cwd)
            },
            onSettingsClick = {
                showControlsSheet = false
                onSettingsClick()
            },
        )
    }

    uiState.pendingApproval?.let { approval ->
        val approvalCommand = remember(approval.params) { ApprovalRules.extractCommandTokens(approval.params) }
        AlertDialog(
            onDismissRequest = { /* keep until decision */ },
            title = { Text("Approval needed") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(approval.method, style = MaterialTheme.typography.labelMedium)
                    val rendered = remember(approval.params) { renderJsonForUi(approval.params) }
                    if (rendered.isNotBlank()) {
                        Surface(
                            color = CodexTheme.colors.bgSecondary,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                rendered,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (!approvalCommand.isNullOrEmpty()) {
                        TextButton(
                            onClick = { viewModel.allowApprovalAlways(approvalCommand) },
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text("Always allow")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.decideApproval("accept") }) { Text("Allow once") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.decideApproval("decline") }) { Text("Decline") }
            }
        )
    }

    uiState.pendingUserInput?.let { req ->
        val requestKey = "userInput:${req.requestId}"
        val selections =
            remember(requestKey) {
                mutableStateMapOf<String, String>().apply {
                    req.questions.forEach { question ->
                        val firstOption = question.options.firstOrNull()
                        val defaultValue =
                            firstOption
                                ?.let { optionSelectionValue(it) }
                                ?.takeIf { it.isNotBlank() }
                        if (defaultValue != null) {
                            this[question.id] = defaultValue
                        }
                    }
                }
            }
        val notes = remember(requestKey) { mutableStateMapOf<String, String>() }
        var questionIndex by remember(requestKey) { mutableStateOf(0) }
        val currentQuestion = req.questions.getOrNull(questionIndex)
        AlertDialog(
            onDismissRequest = { /* keep until answered */ },
            title = { Text("Input needed") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (req.questions.size > 1) {
                        Text(
                            text = "Question ${questionIndex + 1} of ${req.questions.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CodexTheme.colors.textSecondary,
                        )
                    }
                    currentQuestion?.let { q ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (q.header.isNotBlank()) {
                                Text(q.header, style = MaterialTheme.typography.labelMedium)
                            }
                            if (q.question.isNotBlank()) {
                                Text(q.question, style = MaterialTheme.typography.bodyMedium)
                            }

                            val selected = selections[q.id].orEmpty()
                            if (q.options.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.heightIn(max = 240.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    q.options.forEach { opt ->
                                        val optionValue = optionSelectionValue(opt)
                                        Row(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selections[q.id] = optionValue },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = selected == optionValue,
                                                onClick = { selections[q.id] = optionValue }
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    opt.label.ifBlank { "(option)" },
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                if (opt.description.isNotBlank()) {
                                                    Text(
                                                        opt.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = CodexTheme.colors.textSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            val notePlaceholder =
                                when {
                                    q.options.isEmpty() || q.isOther -> "Type your answer (optional)"
                                    else -> "Add notes (optional)"
                                }
                            OutlinedTextField(
                                value = notes[q.id].orEmpty(),
                                onValueChange = { notes[q.id] = it },
                                placeholder = { Text(notePlaceholder) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 1,
                                maxLines = 3,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                val canAdvance =
                    currentQuestion?.let { q ->
                        val selected = selections[q.id]?.isNotBlank() == true
                        val hasNotes = notes[q.id]?.isNotBlank() == true
                        if (q.options.isEmpty()) true else selected || (q.isOther && hasNotes)
                    } ?: false
                val isLastQuestion = questionIndex >= req.questions.lastIndex
                TextButton(
                    enabled = canAdvance,
                    onClick = {
                        if (!isLastQuestion) {
                            questionIndex += 1
                            return@TextButton
                        }
                        val answers =
                            req.questions.associate { q ->
                                val answerList = mutableListOf<String>()
                                val selected = selections[q.id]?.trim().orEmpty()
                                if (selected.isNotBlank()) {
                                    answerList += selected
                                }
                                val note = notes[q.id]?.trim().orEmpty()
                                if (note.isNotBlank()) {
                                    if (q.options.isEmpty()) answerList += note
                                    else answerList += "user_note: $note"
                                }
                                q.id to answerList
                            }
                        viewModel.submitUserInput(answers)
                    }
                ) { Text(if (isLastQuestion) "Submit" else "Next") }
            },
            dismissButton = {
                Row {
                    if (questionIndex > 0) {
                        TextButton(onClick = { questionIndex -= 1 }) { Text("Back") }
                    }
                    TextButton(
                        onClick = {
                            // Best-effort: empty answers signals "no response"; server may treat this as decline/cancel.
                            viewModel.submitUserInput(emptyMap())
                        }
                    ) { Text("Cancel") }
                }
            }
        )
    }

    uiState.pendingUnknownRequest?.let { req ->
        AlertDialog(
            onDismissRequest = { /* keep until acknowledged */ },
            title = { Text("Needs attention") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(req.method, style = MaterialTheme.typography.labelMedium)
                    val rendered = remember(req.params) { renderJsonForUi(req.params) }
                    if (rendered.isNotBlank()) {
                        Surface(
                            color = CodexTheme.colors.bgSecondary,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                rendered,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissUnknownRequest() }) { Text("Dismiss") }
            }
        )
    }
}

private fun renderJsonForUi(element: JsonElement?): String {
    if (element == null || element is JsonNull) return ""
    return when (element) {
        is JsonPrimitive -> element.content
        is JsonObject -> element.toString()
        else -> element.toString()
    }.take(2000)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionTopBar(
    title: String,
    connectionStatus: ConnectionStatus,
    isSyncing: Boolean,
    onMenuClick: () -> Unit,
    onNewSessionClick: () -> Unit,
    showNewSessionButton: Boolean,
    borderColor: Color
) {
    val colors = CodexTheme.colors
    val appBarButtonBackground = colors.neutralIconButtonBackground
    val appBarButtonIcon = colors.neutralIconButtonContent

    CenterAlignedTopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ConnectionDot(connectionStatus)
                Spacer(Modifier.width(8.dp))
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(text = title, style = MaterialTheme.typography.titleLarge)
            }
        },
        navigationIcon = {
            Surface(
                modifier = Modifier.padding(start = 8.dp).size(40.dp),
                shape = CircleShape,
                color = appBarButtonBackground
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open menu",
                        tint = appBarButtonIcon
                    )
                }
            }
        },
        actions = {
            if (showNewSessionButton) {
                Surface(
                    modifier = Modifier.padding(end = 8.dp).size(40.dp),
                    shape = CircleShape,
                    color = appBarButtonBackground
                ) {
                    IconButton(onClick = onNewSessionClick) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New Session",
                            modifier = Modifier.size(24.dp), // Slightly larger for the plus icon
                            tint = appBarButtonIcon
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = colors.bgPrimary,
            titleContentColor = colors.textPrimary,
            navigationIconContentColor = colors.textPrimary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlsBottomSheet(
    uiState: SessionUiState,
    onDismiss: () -> Unit,
    onPlanModeChange: (Boolean) -> Unit,
    onModelSelect: (String?) -> Unit,
    onEffortSelect: (String?) -> Unit,
    onSkillsClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val colors = CodexTheme.colors
    val models = uiState.models
    val selectedModel =
        uiState.selectedModelId?.let { id -> models.firstOrNull { it.id == id || it.model == id } }
    val supportedEfforts =
        selectedModel?.supportedReasoningEfforts?.map { it.reasoningEffort }?.filter { it.isNotBlank() }.orEmpty()
    val defaultEffort = selectedModel?.defaultReasoningEffort?.takeIf { it.isNotBlank() }
    val effortOptions =
        when {
            supportedEfforts.isNotEmpty() -> supportedEfforts
            defaultEffort != null -> listOf(defaultEffort)
            else -> emptyList()
        }
    val selectedModelLabel =
        selectedModel?.displayName
            ?: uiState.selectedModelId?.takeIf { it.isNotBlank() }
            ?: "No model selected"
    val selectedEffortLabel =
        uiState.selectedEffort?.takeIf { it.isNotBlank() }
            ?: effortOptions.firstOrNull()
            ?: "No reasoning effort available"
    val skillsSubtitle =
        when {
            uiState.isControlsSyncing && uiState.skills.isEmpty() -> "Loading skills for this workspace."
            uiState.skills.isEmpty() -> "Browse skills available for this workspace."
            else -> {
                val count = uiState.skills.size
                "$count skill${if (count == 1) "" else "s"} available for this workspace."
            }
        }

    var modelExpanded by rememberSaveable { mutableStateOf(false) }
    var effortExpanded by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.bgPrimary,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (uiState.isControlsSyncing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (!uiState.controlsError.isNullOrBlank()) {
                Text(
                    text = uiState.controlsError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.accentError
                )
            }

            ControlToggleRow(
                icon = Icons.Default.AccountTree,
                title = "Plan mode",
                subtitle = "Ask Codex to explicitly plan steps before execution.",
                checked = uiState.planModeEnabled,
                enabled = !uiState.isControlsSyncing,
                onCheckedChange = onPlanModeChange,
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                ControlDropdownRow(
                    icon = Icons.Default.Memory,
                    title = "Model",
                    subtitle = selectedModelLabel,
                    enabled = models.isNotEmpty(),
                    expanded = modelExpanded,
                    onClick = { modelExpanded = true },
                )
                DropdownMenu(
                    expanded = modelExpanded,
                    onDismissRequest = { modelExpanded = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = colors.bgSecondary,
                    tonalElevation = 0.dp,
                ) {
                    if (models.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No models") },
                            onClick = { modelExpanded = false }
                        )
                    } else {
                        models.forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(model.displayName)
                                        if (model.description.isNotBlank()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                model.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textSecondary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onModelSelect(model.id)
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                ControlDropdownRow(
                    icon = Icons.Default.Psychology,
                    title = "Reasoning effort",
                    subtitle = selectedEffortLabel,
                    enabled = effortOptions.isNotEmpty(),
                    expanded = effortExpanded,
                    onClick = { effortExpanded = true },
                )
                DropdownMenu(
                    expanded = effortExpanded,
                    onDismissRequest = { effortExpanded = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = colors.bgSecondary,
                    tonalElevation = 0.dp,
                ) {
                    effortOptions.forEach { effort ->
                        DropdownMenuItem(
                            text = { Text(effort) },
                            onClick = {
                                onEffortSelect(effort)
                                effortExpanded = false
                            }
                        )
                    }
                }
            }

            NavigableControlRow(
                icon = Icons.Default.Build,
                title = "Skills",
                subtitle = skillsSubtitle,
                onClick = onSkillsClick,
            )

            NavigableControlRow(
                icon = Icons.Default.Settings,
                title = "Settings",
                subtitle = "Open application settings.",
                onClick = onSettingsClick,
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ControlToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = CodexTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = colors.controlStrongOn,
                    checkedTrackColor = colors.controlStrong,
                    uncheckedThumbColor = colors.textSecondary,
                    uncheckedTrackColor = colors.bgSecondary,
                ),
        )
    }
}

@Composable
private fun ControlDropdownRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val colors = CodexTheme.colors
    val iconTint =
        if (enabled) {
            colors.textSecondary
        } else {
            colors.textSecondary.copy(alpha = 0.5f)
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) colors.textPrimary else colors.textSecondary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = iconTint,
        )
    }
}

@Composable
private fun NavigableControlRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = CodexTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textSecondary,
        )
    }
}

@Composable
private fun ConnectionDot(status: ConnectionStatus) {
    val colors = CodexTheme.colors
    val color =
        when (status) {
            ConnectionStatus.Healthy -> colors.accentSuccess
            ConnectionStatus.Unhealthy -> colors.accentError
            ConnectionStatus.Checking -> colors.borderDefault
            ConnectionStatus.Unknown -> colors.borderDefault
        }
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color, CircleShape)
    )
}

@Composable
private fun SessionSkeleton() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        repeat(6) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(if (it % 2 == 0) 0.8f else 0.6f)
                    .height(20.dp),
                shape = RoundedCornerShape(8.dp),
                color = CodexTheme.colors.bgSecondary
            ) {}
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MessageList(
    thread: Thread,
    isSending: Boolean,
    pendingUserMessage: String?,
    activeTurnId: String?,
    showLiveActivityInList: Boolean,
    runningStatus: RunningStatusUi?,
    runningSinceMs: Long,
    scrollToTurnId: String?,
    turnSpeechController: TurnTextToSpeechController,
    onScrollToTurnHandled: () -> Unit,
    planReadyTurnId: String?,
    onPlanAccept: () -> Unit,
    onPlanSubmitChanges: (String) -> Unit,
) {
    val showLiveIndicators = showLiveActivityInList && (isSending || runningStatus != null)
    val timelineEntries = remember(thread.turns) { buildTimelineEntries(thread.turns) }
    val displayEntries = remember(timelineEntries) { timelineEntries.asReversed() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showScrollToBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    LaunchedEffect(displayEntries.size, isSending) {
        // Only auto-scroll if the user is already at the bottom; otherwise show the FAB.
        if (!showScrollToBottom && displayEntries.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(scrollToTurnId, displayEntries.size) {
        val targetTurnId = scrollToTurnId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        // Entries are displayed as `displayEntries` in a reverseLayout list.
        val firstIndex = displayEntries.indexOfFirst { it.turnId == targetTurnId }
        if (firstIndex == -1) return@LaunchedEffect

        val offset =
            (if (showLiveIndicators && runningStatus != null && runningSinceMs > 0L) 1 else 0) +
                (if (showLiveIndicators) 1 else 0) +
                (if (!pendingUserMessage.isNullOrBlank()) 1 else 0)
        val targetIndex = offset + firstIndex
        listState.animateScrollToItem(targetIndex)
        onScrollToTurnHandled()
    }

    val colors = CodexTheme.colors
    val buttonBackground = colors.neutralIconButtonBackground
    val buttonIcon = colors.neutralIconButtonContent

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom),
            reverseLayout = true
        ) {
            if (showLiveIndicators) {
                if (runningStatus != null && runningSinceMs > 0L) {
                    item(key = "live-status-inline") {
                        LiveStatusInlineRow(
                            status = runningStatus,
                            startedAtMs = runningSinceMs,
                        )
                    }
                }
                // Temporarily disabled for visual QA:
                // item(key = "typing-indicator") { AgentTypingIndicator() }
            }
            if (!pendingUserMessage.isNullOrBlank() && !threadHasUserMessage(thread, activeTurnId, pendingUserMessage)) {
                item(key = "pending-user-message") {
                    ThreadItemBubble(
                        ThreadItem.UserMessage(
                            id = "pending-user-message",
                            content = listOf(UserInput(text = pendingUserMessage))
                        )
                    )
                }
            }
            items(displayEntries, key = { it.key }) { entry ->
                when (entry) {
                    is ThreadTimelineEntry.ItemEntry -> ThreadItemBubble(entry.item)
                    is ThreadTimelineEntry.SpeechControlEntry ->
                        TurnSpeechControlRow(
                            entry = entry,
                            turnSpeechController = turnSpeechController,
                        )
                }
            }
            if (!planReadyTurnId.isNullOrBlank()) {
                item(key = "plan-ready-$planReadyTurnId") {
                    PlanReadyCard(
                        onAccept = onPlanAccept,
                        onSubmitChanges = onPlanSubmitChanges,
                    )
                }
            }
        }

        if (showScrollToBottom) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(40.dp),
                shape = CircleShape,
                color = buttonBackground
            ) {
                IconButton(onClick = { scope.launch { listState.animateScrollToItem(0) } }) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Scroll to bottom",
                        tint = buttonIcon
                    )
                }
            }
        }
    }
}

private data class RunningStatusUi(
    val header: String,
    val inlineMessage: String? = null,
)

private fun computeRunningStatusUi(
    thread: Thread?,
    activeTurnId: String?,
    isSending: Boolean,
): RunningStatusUi? {
    val activeTurn =
        thread?.let {
            when {
                !activeTurnId.isNullOrBlank() -> it.turns.firstOrNull { turn -> turn.id == activeTurnId }
                else ->
                    it.turns.lastOrNull { turn -> turn.status == TurnStatus.inProgress }
                        ?: it.turns.lastOrNull(::turnHasInFlightItems)
                        ?: it.turns.lastOrNull()
            }
        }
    val hasInFlightItems = activeTurn?.let(::turnHasInFlightItems) == true
    val hasInProgressTurn = activeTurn?.status == TurnStatus.inProgress
    if (!isSending && !hasInProgressTurn && !hasInFlightItems) return null

    val header =
        when {
            activeTurn == null -> "Working"
            else -> {
                val waitedInteraction =
                    activeTurn.items
                        .asReversed()
                        .mapNotNull { item -> item as? ThreadItem.TerminalInteraction }
                        .firstOrNull { it.waited || it.stdin.isBlank() }
                if (waitedInteraction != null) {
                    val command = waitedInteraction.command.takeIf { it.isNotBlank() }
                    if (command != null) "Waiting for background terminal · $command"
                    else "Waiting for background terminal"
                } else {
                    val runningCommand =
                        activeTurn.items
                            .asReversed()
                            .mapNotNull { item -> item as? ThreadItem.CommandExecution }
                            .firstOrNull { it.status == CommandExecutionStatus.inProgress || it.status == CommandExecutionStatus.unknown }
                    val runningMcp =
                        activeTurn.items
                            .asReversed()
                            .mapNotNull { item -> item as? ThreadItem.McpToolCall }
                            .firstOrNull { it.status == McpToolCallStatus.inProgress || it.status == McpToolCallStatus.unknown }
                    val runningSearch =
                        activeTurn.items
                            .asReversed()
                            .mapNotNull { item -> item as? ThreadItem.WebSearch }
                            .firstOrNull { it.action == null }
                    val runningCompaction =
                        activeTurn.items
                            .asReversed()
                            .mapNotNull { item -> item as? ThreadItem.ContextCompaction }
                            .firstOrNull { it.status == ContextCompactionStatus.inProgress || it.status == ContextCompactionStatus.unknown }
                    when {
                        runningSearch != null -> "Searching the web"
                        runningMcp != null -> "Calling ${runningMcp.server}.${runningMcp.tool}"
                        runningCompaction != null -> "Compacting context"
                        runningCommand != null && runningCommand.commandActions.isNotEmpty() -> "Exploring"
                        runningCommand != null -> "Running"
                        else -> extractLiveReasoningHeader(thread, activeTurnId) ?: "Working"
                    }
                }
            }
        }

    val backgroundCount =
        activeTurn
            ?.items
            ?.mapNotNull { item -> item as? ThreadItem.CommandExecution }
            ?.count { it.status == CommandExecutionStatus.inProgress || it.status == CommandExecutionStatus.unknown }
            ?: 0

    val inlineMessage =
        if (backgroundCount > 0) {
            "$backgroundCount background terminal${if (backgroundCount == 1) "" else "s"} running"
        } else {
            null
        }

    return RunningStatusUi(header = header, inlineMessage = inlineMessage)
}

private fun turnHasInFlightItems(turn: Turn): Boolean {
    return turn.items.any { item ->
        when (item) {
            is ThreadItem.CommandExecution ->
                item.status == CommandExecutionStatus.inProgress || item.status == CommandExecutionStatus.unknown
            is ThreadItem.McpToolCall ->
                item.status == McpToolCallStatus.inProgress || item.status == McpToolCallStatus.unknown
            is ThreadItem.WebSearch -> item.action == null
            is ThreadItem.ContextCompaction ->
                item.status == ContextCompactionStatus.inProgress || item.status == ContextCompactionStatus.unknown
            is ThreadItem.FileChange ->
                item.status == PatchApplyStatus.inProgress || item.status == PatchApplyStatus.unknown
            else -> false
        }
    }
}

@Composable
private fun LiveStatusInlineRow(
    status: RunningStatusUi,
    startedAtMs: Long,
    modifier: Modifier = Modifier,
) {
    val nowMs by produceState(
        initialValue = System.currentTimeMillis(),
        key1 = startedAtMs,
        key2 = status.header,
        key3 = status.inlineMessage,
    ) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val elapsedSeconds = ((nowMs - startedAtMs).coerceAtLeast(0L)) / 1000L
    val elapsed = formatElapsedCompact(elapsedSeconds)
    val line =
        buildString {
            append(status.header)
            append(" (")
            append(elapsed)
            append(")")
            status.inlineMessage?.takeIf { it.isNotBlank() }?.let {
                append(" · ")
                append(it)
            }
        }
    ReasoningLiveHeader(
        text = line,
        modifier = modifier,
        maxLines = 2,
    )
}

private fun formatElapsedCompact(elapsedSecs: Long): String {
    if (elapsedSecs < 60) return "${elapsedSecs}s"
    if (elapsedSecs < 3600) {
        val minutes = elapsedSecs / 60
        val seconds = elapsedSecs % 60
        return "${minutes}m ${seconds.toString().padStart(2, '0')}s"
    }
    val hours = elapsedSecs / 3600
    val minutes = (elapsedSecs % 3600) / 60
    val seconds = elapsedSecs % 60
    return "${hours}h ${minutes.toString().padStart(2, '0')}m ${seconds.toString().padStart(2, '0')}s"
}

private sealed interface ThreadTimelineEntry {
    val key: String
    val turnId: String

    data class ItemEntry(
        override val key: String,
        override val turnId: String,
        val item: ThreadItem,
    ) : ThreadTimelineEntry

    data class SpeechControlEntry(
        override val key: String,
        override val turnId: String,
        val rawText: String,
        val speechText: String,
    ) : ThreadTimelineEntry
}

private fun buildTimelineEntries(turns: List<Turn>): List<ThreadTimelineEntry> {
    val entries = mutableListOf<ThreadTimelineEntry>()
    turns.withIndex().forEach { (turnIdx, turn) ->
        val visibleItems =
            turn.items.withIndex().mapNotNull { (itemIdx, item) ->
                if (item is ThreadItem.Reasoning) {
                    val hasContent = item.summary.any { it.isNotBlank() } || item.content.any { it.isNotBlank() }
                    if (!hasContent) return@mapNotNull null
                }
                if (isInternalTaggedMessage(item)) return@mapNotNull null
                ThreadTimelineEntry.ItemEntry(
                    key = "$turnIdx-$itemIdx-${item.id}",
                    turnId = turn.id,
                    item = item,
                )
            }
        entries.addAll(visibleItems)

        val speechPayload = buildTurnSpeechPayload(turn)
        if (turn.status == TurnStatus.completed && speechPayload.rawText.isNotBlank()) {
            entries.add(
                ThreadTimelineEntry.SpeechControlEntry(
                    key = "tts-${turn.id}",
                    turnId = turn.id,
                    rawText = speechPayload.rawText,
                    speechText = speechPayload.speechText,
                )
            )
        }
    }
    return entries
}

private fun isInternalTaggedMessage(item: ThreadItem): Boolean {
    val message = item as? ThreadItem.UserMessage ?: return false
    val text = message.content.joinToString(separator = "") { it.text.orEmpty() }
    return text.trimStart().startsWith("[[cm_plan_ready:")
}

private data class TurnSpeechPayload(
    val rawText: String,
    val speechText: String,
)

private fun buildTurnSpeechPayload(turn: Turn): TurnSpeechPayload {
    val raw =
        turn.items
        .mapNotNull { item ->
            val message = item as? ThreadItem.AgentMessage ?: return@mapNotNull null
            message.text.trim().takeIf { it.isNotBlank() }
        }
        .joinToString(separator = "\n\n")
        .trim()
    return TurnSpeechPayload(
        rawText = raw,
        speechText = normalizeTextForSpeech(raw),
    )
}

private fun extractLiveReasoningHeader(thread: Thread, activeTurnId: String?): String? {
    val candidateTurns =
        buildList {
            if (!activeTurnId.isNullOrBlank()) {
                thread.turns.firstOrNull { it.id == activeTurnId }?.let { add(it) }
            }
            thread.turns.lastOrNull { it.status == TurnStatus.inProgress }?.let { turn ->
                if (none { it.id == turn.id }) add(turn)
            }
        }
    if (candidateTurns.isEmpty()) return null

    for (turn in candidateTurns) {
        val reasoningItems =
            turn.items.asReversed().mapNotNull { item ->
                item as? ThreadItem.Reasoning
            }
        for (reasoning in reasoningItems) {
            val summaryText =
                reasoning.summary
                    .asReversed()
                    .firstOrNull { it.isNotBlank() }
                    ?.trim()
            if (!summaryText.isNullOrBlank()) {
                return normalizeReasoningHeader(summaryText)
            }
        }
    }
    return null
}

private fun normalizeReasoningHeader(raw: String): String {
    if (raw.isBlank()) return ""
    return raw
        .replace(Regex("`([^`]*)`"), "$1")
        .replace(Regex("[*_~]"), "")
        .replace(Regex("(?m)^\\s{0,3}#{1,6}\\s*"), "")
        .replace(Regex("(?m)^\\s*[-*+]\\s+"), "")
        .replace(Regex("(?m)^\\s*\\d+\\.\\s+"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

@Composable
private fun ReasoningLiveHeader(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 3,
) {
    val glowColor = CodexTheme.colors.bgPrimary
    val textColor = CodexTheme.colors.textSecondary

    val transition = rememberInfiniteTransition(label = "reasoning_live_glow")
    val sweep by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "reasoning_live_sweep",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "reasoning_live_pulse",
    )

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp).offset(y = (-6).dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = text,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = textColor,
            modifier =
                Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .drawWithContent {
                        drawContent()
                        val centerX = size.width * sweep
                        val spread = max(size.width * 0.30f, 56.dp.toPx())
                        drawRect(
                            brush =
                                Brush.horizontalGradient(
                                    colors =
                                        listOf(
                                            Color.Transparent,
                                            glowColor.copy(alpha = 0.18f * pulse),
                                            glowColor.copy(alpha = 0.70f * pulse),
                                            glowColor.copy(alpha = 0.22f * pulse),
                                            Color.Transparent,
                                        ),
                                    startX = centerX - spread,
                                    endX = centerX + spread,
                                )
                        )
                    },
        )
    }
}

private fun normalizeTextForSpeech(input: String): String {
    if (input.isBlank()) return ""
    return input
        // Remove fenced code blocks to keep speech natural.
        .replace(Regex("```[\\s\\S]*?```"), " code snippet omitted ")
        // Convert markdown links/images to readable labels.
        .replace(Regex("!\\[([^\\]]*)]\\(([^)]+)\\)"), "$1")
        .replace(Regex("\\[([^\\]]+)]\\(([^)]+)\\)"), "$1")
        // Keep inline code content, drop backticks.
        .replace(Regex("`([^`]*)`"), "$1")
        .replace("`", " ")
        // Strip common markdown prefix syntax.
        .replace(Regex("(?m)^\\s{0,3}#{1,6}\\s*"), "")
        .replace(Regex("(?m)^\\s*[-*+]\\s+"), "")
        .replace(Regex("(?m)^\\s*\\d+\\.\\s+"), "")
        .replace(Regex("(?m)^\\s*>+\\s?"), "")
        // Expand common contractions so models pronounce phrases more naturally.
        .let(::expandCommonSpeechContractions)
        // Drop symbols that are read awkwardly by Android/Sarvam TTS.
        .replace("/", " ")
        .replace("(", " ")
        .replace(")", " ")
        // Remove emphasis/control markers that sound awkward in TTS.
        .replace(Regex("[*_~]"), "")
        .replace(Regex("<[^>]+>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun expandCommonSpeechContractions(text: String): String {
    var out = text
    val replacements =
        listOf(
            "\\b[Ii]['’]m\\b" to "I am",
            "\\b[Ii]['’]ll\\b" to "I will",
            "\\b[Ii]['’]ve\\b" to "I have",
            "\\b[Ii]['’]d\\b" to "I would",
            "\\b[Yy]ou['’]re\\b" to "you are",
            "\\b[Yy]ou['’]ll\\b" to "you will",
            "\\b[Yy]ou['’]ve\\b" to "you have",
            "\\b[Yy]ou['’]d\\b" to "you would",
            "\\b[Ww]e['’]re\\b" to "we are",
            "\\b[Ww]e['’]ll\\b" to "we will",
            "\\b[Ww]e['’]ve\\b" to "we have",
            "\\b[Ww]e['’]d\\b" to "we would",
            "\\b[Tt]hey['’]re\\b" to "they are",
            "\\b[Tt]hey['’]ll\\b" to "they will",
            "\\b[Tt]hey['’]ve\\b" to "they have",
            "\\b[Tt]hey['’]d\\b" to "they would",
            "\\b[Hh]e['’]s\\b" to "he is",
            "\\b[Hh]e['’]ll\\b" to "he will",
            "\\b[Hh]e['’]d\\b" to "he would",
            "\\b[Ss]he['’]s\\b" to "she is",
            "\\b[Ss]he['’]ll\\b" to "she will",
            "\\b[Ss]he['’]d\\b" to "she would",
            "\\b[Ii]t['’]s\\b" to "it is",
            "\\b[Tt]hat['’]s\\b" to "that is",
            "\\b[Tt]here['’]s\\b" to "there is",
            "\\b[Hh]ere['’]s\\b" to "here is",
            "\\b[Ww]hat['’]s\\b" to "what is",
            "\\b[Ww]ho['’]s\\b" to "who is",
            "\\b[Ll]et['’]s\\b" to "let us",
            "\\b[Cc]an['’]t\\b" to "cannot",
            "\\b[Ww]on['’]t\\b" to "will not",
            "\\b[Dd]on['’]t\\b" to "do not",
            "\\b[Dd]oesn['’]t\\b" to "does not",
            "\\b[Dd]idn['’]t\\b" to "did not",
            "\\b[Ii]sn['’]t\\b" to "is not",
            "\\b[Aa]ren['’]t\\b" to "are not",
            "\\b[Ww]asn['’]t\\b" to "was not",
            "\\b[Ww]eren['’]t\\b" to "were not",
            "\\b[Hh]aven['’]t\\b" to "have not",
            "\\b[Hh]asn['’]t\\b" to "has not",
            "\\b[Hh]adn['’]t\\b" to "had not",
            "\\b[Ww]ouldn['’]t\\b" to "would not",
            "\\b[Ss]houldn['’]t\\b" to "should not",
            "\\b[Cc]ouldn['’]t\\b" to "could not",
            "\\b[Mm]ustn['’]t\\b" to "must not",
            "\\b[Mm]ightn['’]t\\b" to "might not",
            "\\b[Nn]eedn['’]t\\b" to "need not",
        )
    replacements.forEach { (pattern, replacement) ->
        out = out.replace(Regex(pattern), replacement)
    }
    return out
}

@Composable
private fun TurnSpeechControlRow(
    entry: ThreadTimelineEntry.SpeechControlEntry,
    turnSpeechController: TurnTextToSpeechController,
) {
    val context = LocalContext.current
    val colors = CodexTheme.colors
    val isActiveTurn = turnSpeechController.activeTurnId == entry.turnId
    val state =
        if (isActiveTurn) turnSpeechController.playbackState else TurnSpeechPlaybackState.Idle
    val isSynthesizing = state == TurnSpeechPlaybackState.Synthesizing
    val (ttsIcon, ttsLabel, onTtsClick) =
        when (state) {
            TurnSpeechPlaybackState.Speaking ->
                Triple(Icons.Default.Pause, "Pause", { turnSpeechController.pause() })
            TurnSpeechPlaybackState.Paused ->
                Triple(Icons.Default.PlayArrow, "Resume", { turnSpeechController.resume() })
            TurnSpeechPlaybackState.Idle,
            TurnSpeechPlaybackState.Synthesizing ->
                Triple(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    "Listen",
                    { turnSpeechController.start(entry.turnId, entry.speechText) }
                )
        }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).offset(y = (-8).dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("turn", entry.rawText))
                },
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy agent response",
                    modifier = Modifier.size(18.dp),
                )
            }
            if (isSynthesizing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = colors.textSecondary,
                )
                IconButton(
                    onClick = { turnSpeechController.cancelSynthesis() },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop speech request",
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else {
                IconButton(onClick = onTtsClick, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = ttsIcon,
                        contentDescription = ttsLabel,
                        modifier = Modifier.size(18.dp),
                    )
                }
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

@Composable
private fun PendingConversationView(
    pendingUserMessage: String,
    showTypingIndicator: Boolean,
    runningStatus: RunningStatusUi?,
    runningSinceMs: Long,
) {
    // Brand-new thread: show the user message immediately, even before thread/start returns.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom),
        reverseLayout = true
    ) {
        if (runningStatus != null && runningSinceMs > 0L) {
            item(key = "pending-live-status-inline") {
                LiveStatusInlineRow(
                    status = runningStatus,
                    startedAtMs = runningSinceMs,
                )
            }
        }
        if (showTypingIndicator) {
            item { AgentTypingIndicator() }
        }
        item {
            ThreadItemBubble(
                ThreadItem.UserMessage(
                    id = "pending-user-message",
                    content = listOf(UserInput(text = pendingUserMessage))
                )
            )
        }
    }
}

@Composable
private fun AgentTypingIndicator() {
    // Render a "typing" bubble on the assistant side (left), near where the next agent message will appear.
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CodexTheme.colors.bgSecondary)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            TypingDots(color = CodexTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun TypingDots(color: Color) {
    val transition = rememberInfiniteTransition(label = "typing")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "typing_phase"
    )

    fun dotAlpha(phaseOffset: Float): Float {
        // 3 dots that "pulse" in sequence; keep it subtle so it feels polished.
        val x = (t + phaseOffset) % 1f
        return when {
            x < 0.2f -> 0.35f + (x / 0.2f) * 0.55f
            x < 0.5f -> 0.90f
            x < 0.8f -> 0.90f - ((x - 0.5f) / 0.3f) * 0.55f
            else -> 0.35f
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = dotAlpha(0f)))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = dotAlpha(0.18f)))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = dotAlpha(0.36f)))
        )
    }
}

@Composable
private fun EmptyView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = CodexLogo,
            contentDescription = null,
            tint = CodexTheme.colors.textPrimary.copy(alpha = CodexTheme.colors.emptyStateLogoAlpha),
            modifier = Modifier.size(180.dp),
        )
    }
}

private const val CODEX_LOGO_PATH =
    "M16.585 10a6.585 6.585 0 1 0-13.17 0 6.585 6.585 0 0 0 13.17 0m-3.252 1.418.135.014a.665.665 0 0 1 0 1.302l-.135.014h-2.5a.665.665 0 0 1 0-1.33zm-5.68 1.008a.665.665 0 0 1-1.14-.685zm1.25-2.768a.66.66 0 0 1 0 .684l-1.25 2.084-.57-.343-.57-.342L7.557 10 6.513 8.259l.57-.342.57-.343zM6.741 7.347a.665.665 0 0 1 .912.227l-1.14.685a.665.665 0 0 1 .228-.912M17.915 10a7.915 7.915 0 1 1-15.83 0 7.915 7.915 0 0 1 15.83 0"

private val CodexLogo: ImageVector by lazy {
    ImageVector.Builder(
        name = "CodexLogo",
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(CODEX_LOGO_PATH).toNodes(),
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero,
        )
    }.build()
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Error: $message", color = CodexTheme.colors.accentError, modifier = Modifier.padding(16.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

private fun optionSelectionValue(option: UserInputOption): String {
    return option.label.ifBlank { option.description }.ifBlank { "(option)" }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: (ComposerSubmitIntent) -> Unit,
    onStop: () -> Unit,
    onControls: () -> Unit,
    enabled: Boolean,
    isSending: Boolean,
    canSend: Boolean,
    selectedModelLabel: String?,
    selectedEffortLabel: String?,
    planModeEnabled: Boolean,
    onDisablePlanMode: () -> Unit,
    followUpMessageBehavior: FollowUpMessageBehavior,
    queuedMessages: List<QueuedMessageUi>,
    queuePausedReason: String?,
    onSteerQueued: (String) -> Unit,
    onDeleteQueued: (String) -> Unit,
) {
    val colors = CodexTheme.colors
    val toolsButtonBackground = colors.neutralIconButtonBackground
    val toolsButtonIcon = colors.neutralIconButtonContent
    val hasMetaChips = planModeEnabled || !selectedModelLabel.isNullOrBlank() || !selectedEffortLabel.isNullOrBlank()
    Surface(
        color = colors.bgPrimary
    ) {
        Row(
            modifier =
                Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = toolsButtonBackground
            ) {
                IconButton(onClick = onControls, enabled = enabled) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Session controls",
                        tint = toolsButtonIcon
                    )
                }
            }
            Spacer(Modifier.width(10.dp))

            Surface(
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(width = 1.dp, color = colors.inputFieldBorder, shape = RoundedCornerShape(24.dp)),
                color = colors.inputFieldBackground,
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                ) {
                    if (queuedMessages.isNotEmpty()) {
                        QueuedMessagesStrip(
                            messages = queuedMessages,
                            pausedReason = queuePausedReason,
                            onSteerQueued = onSteerQueued,
                            onDeleteQueued = onDeleteQueued,
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    if (hasMetaChips) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (planModeEnabled) {
                                PlanModeChip(
                                    enabled = enabled,
                                    onClose = onDisablePlanMode,
                                )
                            }
                            selectedModelLabel
                                ?.takeIf { it.isNotBlank() }
                                ?.let {
                                    StaticMetaChip(
                                        label = it,
                                        icon = Icons.Default.Memory,
                                    )
                                }
                            selectedEffortLabel
                                ?.takeIf { it.isNotBlank() }
                                ?.let {
                                    StaticMetaChip(
                                        label = it,
                                        icon = Icons.Default.Psychology,
                                    )
                                }
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    TextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 40.dp)
                                .offset(x = (-6).dp),
                        placeholder = { Text("Ask something", color = colors.textSecondary) },
                        maxLines = 5,
                        enabled = enabled,
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSending) {
                                    FilledIconButton(
                                        onClick = onStop,
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = toolsButtonBackground,
                                            contentColor = toolsButtonIcon
                                        ),
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Stop,
                                            contentDescription = "Stop session"
                                        )
                                    }
                                }
                                val canShowSend = enabled && canSend && text.trim().isNotEmpty()
                                if (canShowSend) {
                                    FilledIconButton(
                                        onClick = {
                                            onSend(
                                                if (isSending) {
                                                    if (followUpMessageBehavior == FollowUpMessageBehavior.Steer) {
                                                        ComposerSubmitIntent.Steer
                                                    } else {
                                                        ComposerSubmitIntent.Queue
                                                    }
                                                } else {
                                                    ComposerSubmitIntent.Default
                                                }
                                            )
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = colors.accentAction,
                                            contentColor = colors.onAccentAction
                                        ),
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription =
                                                if (isSending && followUpMessageBehavior == FollowUpMessageBehavior.Steer) {
                                                    "Steer"
                                                } else if (isSending) {
                                                    "Queue"
                                                } else {
                                                    "Send"
                                                }
                                        )
                                    }
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = colors.accentAction,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            disabledTextColor = colors.textPrimary,
                            disabledPlaceholderColor = colors.textSecondary,
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun QueuedMessagesStrip(
    messages: List<QueuedMessageUi>,
    pausedReason: String?,
    onSteerQueued: (String) -> Unit,
    onDeleteQueued: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Queued messages",
            style = MaterialTheme.typography.labelSmall,
            color = CodexTheme.colors.textSecondary,
        )
        messages.take(3).forEach { message ->
            Surface(
                color = CodexTheme.colors.bgSecondary,
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = message.text,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = { onSteerQueued(message.id) }) { Text("Steer") }
                    TextButton(onClick = { onDeleteQueued(message.id) }) { Text("Remove") }
                }
            }
        }
        pausedReason?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = CodexTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun PlanReadyCard(
    onAccept: () -> Unit,
    onSubmitChanges: (String) -> Unit,
) {
    var changes by rememberSaveable { mutableStateOf("") }
    Surface(
        color = CodexTheme.colors.bgSecondary,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Plan ready", style = MaterialTheme.typography.titleSmall, color = CodexTheme.colors.textPrimary)
            Text(
                "Start building from this plan, or describe changes to the plan.",
                style = MaterialTheme.typography.bodySmall,
                color = CodexTheme.colors.textSecondary,
            )
            OutlinedTextField(
                value = changes,
                onValueChange = { changes = it },
                placeholder = { Text("Describe what you want to change in the plan...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        onSubmitChanges(changes)
                        changes = ""
                    },
                    enabled = changes.trim().isNotEmpty(),
                ) {
                    Text("Send changes")
                }
                Button(onClick = onAccept) {
                    Text("Implement this plan")
                }
            }
        }
    }
}

@Composable
private fun PlanModeChip(enabled: Boolean, onClose: () -> Unit) {
    val colors = CodexTheme.colors
    Surface(
        color = colors.chipAccentActiveBackground,
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
        ) {
            Icon(
                imageVector = Icons.Default.AccountTree,
                contentDescription = null,
                tint = colors.chipAccent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Plan",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.chipAccent,
            )
            Spacer(Modifier.width(4.dp))
            Box(
                modifier =
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .clickable(enabled = enabled, onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Disable plan mode",
                    tint = colors.chipAccent,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun StaticMetaChip(label: String, icon: ImageVector) {
    val colors = CodexTheme.colors
    Surface(
        color = colors.chipAccentActiveBackground,
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.chipAccent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.chipAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun workspaceFolderName(cwd: String?): String {
    val raw = cwd?.trim().orEmpty()
    if (raw.isBlank()) return ""
    val normalized = raw.trimEnd('/', '\\')
    if (normalized.isBlank()) return ""
    val slash = normalized.lastIndexOf('/')
    val backslash = normalized.lastIndexOf('\\')
    val idx = maxOf(slash, backslash)
    return if (idx == -1) normalized else normalized.substring(idx + 1)
}

private fun computeContextLeftPercent(tokenUsage: ThreadTokenUsage?): Int? {
    val window = tokenUsage?.modelContextWindow ?: return null
    if (window <= 0) return null

    val lastTokens = tokenUsage.last.totalTokens
    val totalTokens = tokenUsage.total.totalTokens
    val usedTokens = if (lastTokens > 0) lastTokens else totalTokens
    if (usedTokens <= 0) return null

    val usedPct = (usedTokens.toDouble() / window.toDouble()) * 100.0
    val clampedUsed = usedPct.coerceIn(0.0, 100.0)
    val free = (100.0 - clampedUsed).coerceAtLeast(0.0)
    return free.roundToInt()
}
