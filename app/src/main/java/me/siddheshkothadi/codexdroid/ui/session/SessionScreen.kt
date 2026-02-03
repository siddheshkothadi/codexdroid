package me.siddheshkothadi.codexdroid.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import me.siddheshkothadi.codexdroid.codex.*
import me.siddheshkothadi.codexdroid.ui.components.CodexDroidDrawerContent
import me.siddheshkothadi.codexdroid.ui.history.HistoryUiState
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val connections by viewModel.connections.collectAsState()
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
    var inputText by remember { mutableStateOf("") }
    var showNewSessionDialog by remember { mutableStateOf(false) }
    var newSessionCwd by rememberSaveable { mutableStateOf("") }

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
                onSetupClick = {
                    onAddConnectionClick()
                    scope.launch { drawerState.close() }
                },
                connectionStatus = uiState.connectionStatus,
                isSyncing = uiState.isHistorySyncing || uiState.isThreadSyncing
            )
        }
    ) {
        val borderColor = MaterialTheme.colorScheme.outline
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
                ChatInput(
                    text = inputText,
                    onTextChange = { inputText = it },
                    onSend = {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    },
                    onStop = { viewModel.stopCurrentTurn() },
                    onControls = {
                        viewModel.refreshControls()
                        showControlsSheet = true
                    },
                    enabled = !uiState.isSending,
                    isSending = uiState.isSending
                    ,
                    canSend = canSend
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when {
                    uiState.error != null -> ErrorView(uiState.error!!) { viewModel.sendMessage(inputText) }
                    uiState.currentThread != null && uiState.currentThread!!.turns.isEmpty() && uiState.isThreadSyncing -> SessionSkeleton()
                    uiState.currentThread == null && uiState.pendingUserMessage == null -> EmptyView()
                    uiState.currentThread == null && uiState.pendingUserMessage != null -> PendingConversationView(uiState.pendingUserMessage!!)
                    else ->
                        MessageList(
                            thread = uiState.currentThread!!,
                            isSending = uiState.isSending,
                            pendingUserMessage = uiState.pendingUserMessage,
                            activeTurnId = uiState.activeTurnId,
                            scrollToTurnId = uiState.scrollToTurnId,
                            onScrollToTurnHandled = { viewModel.clearScrollTarget() },
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            onRefresh = { viewModel.refreshControls() },
            onSelectModelId = { viewModel.setSelectedModelId(it) },
            onSelectEffort = { viewModel.setSelectedEffort(it) },
        )
    }

    uiState.pendingApproval?.let { approval ->
        AlertDialog(
            onDismissRequest = { /* keep until decision */ },
            title = { Text("Approval needed") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(approval.method, style = MaterialTheme.typography.labelMedium)
                    val rendered = remember(approval.params) { renderJsonForUi(approval.params) }
                    if (rendered.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
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
                TextButton(onClick = { viewModel.decideApproval("accept") }) { Text("Approve") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.decideApproval("decline") }) { Text("Decline") }
            }
        )
    }

    uiState.pendingUserInput?.let { req ->
        val requestKey = "userInput:${req.requestId}"
        val selections = rememberSaveable(requestKey) { mutableStateMapOf<String, String>() }
        AlertDialog(
            onDismissRequest = { /* keep until answered */ },
            title = { Text("Input needed") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    req.questions.forEach { q ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (q.header.isNotBlank()) {
                                Text(q.header, style = MaterialTheme.typography.labelMedium)
                            }
                            if (q.question.isNotBlank()) {
                                Text(q.question, style = MaterialTheme.typography.bodyMedium)
                            }

                            val selected = selections[q.id].orEmpty()
                            q.options.forEach { opt ->
                                val label = opt.label.ifBlank { opt.description }
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable { selections[q.id] = label },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selected == label,
                                        onClick = { selections[q.id] = label }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(opt.label.ifBlank { "(option)" }, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        if (opt.description.isNotBlank()) {
                                            Text(
                                                opt.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val canSubmit =
                    req.questions.all { q ->
                        selections[q.id]?.isNotBlank() == true
                    }
                TextButton(
                    enabled = canSubmit,
                    onClick = {
                        val answers =
                            req.questions.associate { q ->
                                q.id to listOf(selections[q.id].orEmpty())
                            }
                        viewModel.submitUserInput(answers)
                    }
                ) { Text("Submit") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // Best-effort: empty answers signals "no response"; server may treat this as decline/cancel.
                        viewModel.submitUserInput(emptyMap())
                    }
                ) { Text("Cancel") }
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
    CenterAlignedTopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ConnectionDot(connectionStatus)
                Spacer(Modifier.width(8.dp))
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(text = title, style = MaterialTheme.typography.titleLarge)
            }
        },
        navigationIcon = {
            Surface(
                modifier = Modifier.padding(start = 8.dp).size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Open menu")
                }
            }
        },
        actions = {
            if (showNewSessionButton) {
                Surface(
                    modifier = Modifier.padding(end = 8.dp).size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    IconButton(onClick = onNewSessionClick) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New Session",
                            modifier = Modifier.size(24.dp), // Slightly larger for the plus icon
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlsBottomSheet(
    uiState: SessionUiState,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onSelectModelId: (String?) -> Unit,
    onSelectEffort: (String?) -> Unit,
) {
    val models = uiState.models
    val selectedModel = uiState.selectedModelId?.let { id -> models.firstOrNull { it.id == id } }
    val supportedEfforts =
        selectedModel?.supportedReasoningEfforts?.map { it.reasoningEffort }?.filter { it.isNotBlank() }.orEmpty()
    val defaultEffort = selectedModel?.defaultReasoningEffort?.takeIf { it.isNotBlank() }
    val effortOptions =
        when {
            supportedEfforts.isNotEmpty() -> supportedEfforts
            defaultEffort != null -> listOf(defaultEffort)
            else -> emptyList()
        }

    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Session controls", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = onRefresh, enabled = !uiState.isControlsSyncing) { Text("Refresh") }
                    }
                    if (uiState.isControlsSyncing) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    if (!uiState.controlsError.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = uiState.controlsError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    TabRow(
                        selectedTabIndex = tabIndex,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Model") })
                        Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Skills") })
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            when (tabIndex) {
                0 -> {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        ModelDropdown(
                            models = models,
                            selectedModelId = uiState.selectedModelId,
                            onSelectModelId = onSelectModelId
                        )
                        Spacer(Modifier.height(12.dp))
                        EffortDropdown(
                            efforts = effortOptions,
                            selectedEffort = uiState.selectedEffort,
                            enabled = effortOptions.isNotEmpty(),
                            onSelect = onSelectEffort
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
                1 -> {
                    if (uiState.skills.isEmpty()) {
                        Text(
                            text = if (uiState.isControlsSyncing) "Loading…" else "No skills found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.skills, key = { it.path }) { skill ->
                                ElevatedCard {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(skill.name, style = MaterialTheme.typography.titleSmall)
                                        if (!skill.description.isNullOrBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                skill.description!!,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            skill.path,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelDropdown(
    models: List<ModelOptionUi>,
    selectedModelId: String?,
    onSelectModelId: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = models.firstOrNull { it.id == selectedModelId }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected?.displayName ?: (selectedModelId ?: ""),
            onValueChange = {},
            readOnly = true,
            enabled = models.isNotEmpty(),
            label = { Text("Model") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (models.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No models") },
                    onClick = { expanded = false }
                )
            }
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelectModelId(model.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EffortDropdown(
    efforts: List<String>,
    selectedEffort: String?,
    enabled: Boolean,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val current = selectedEffort?.takeIf { it.isNotBlank() } ?: efforts.firstOrNull().orEmpty()
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = current,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Reasoning effort") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            efforts.forEach { effort ->
                DropdownMenuItem(
                    text = { Text(effort) },
                    onClick = {
                        onSelect(effort)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ConnectionDot(status: ConnectionStatus) {
    val color =
        when (status) {
            ConnectionStatus.Healthy -> Color(0xFF2E7D32)
            ConnectionStatus.Unhealthy -> Color(0xFFC62828)
            ConnectionStatus.Checking -> MaterialTheme.colorScheme.outline
            ConnectionStatus.Unknown -> MaterialTheme.colorScheme.outline
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
                color = MaterialTheme.colorScheme.surfaceVariant
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
    scrollToTurnId: String?,
    onScrollToTurnHandled: () -> Unit,
) {
    val allItems = thread.turns.withIndex().flatMap { (turnIdx, turn) ->
        turn.items.withIndex().map { (itemIdx, item) -> Triple(turnIdx, itemIdx, item) }
    }.filter { (_, _, item) ->
        if (item is ThreadItem.Reasoning) {
            item.summary.any { it.isNotBlank() } || item.content.any { it.isNotBlank() }
        } else {
            true
        }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showScrollToBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    LaunchedEffect(allItems.size, isSending) {
        // Only auto-scroll if the user is already at the bottom; otherwise show the FAB.
        if (!showScrollToBottom && allItems.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(scrollToTurnId, allItems.size) {
        val targetTurnId = scrollToTurnId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        // Items are displayed as `allItems.asReversed()` in a reverseLayout list.
        val display = allItems.asReversed()
        val firstIndex = display.indexOfFirst { (turnIdx, _, _) -> thread.turns.getOrNull(turnIdx)?.id == targetTurnId }
        if (firstIndex == -1) return@LaunchedEffect

        val offset = (if (isSending) 1 else 0) + (if (!pendingUserMessage.isNullOrBlank()) 1 else 0)
        val targetIndex = offset + firstIndex
        listState.animateScrollToItem(targetIndex)
        onScrollToTurnHandled()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom),
            reverseLayout = true
        ) {
            if (isSending) {
                item(key = "typing-indicator") {
                    AgentTypingIndicator()
                }
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
            items(allItems.asReversed(), key = { (t, i, item) -> "$t-$i-${item.id}" }) { (_, _, item) ->
                ThreadItemBubble(item)
            }
        }

        if (showScrollToBottom) {
            SmallFloatingActionButton(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Scroll to bottom")
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
private fun PendingConversationView(pendingUserMessage: String) {
    // Brand-new thread: show the user message immediately, even before thread/start returns.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom),
        reverseLayout = true
    ) {
        item { AgentTypingIndicator() }
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
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            TypingDots(color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun ThreadItemBubble(item: ThreadItem) {
    val isUser = item is ThreadItem.UserMessage
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val background = if (isUser) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val isCardLikeItem =
        item is ThreadItem.CommandExecution ||
            item is ThreadItem.Reasoning ||
            item is ThreadItem.FileChange ||
            item is ThreadItem.McpToolCall ||
            item is ThreadItem.WebSearch ||
            item is ThreadItem.ImageView ||
            item is ThreadItem.EnteredReviewMode ||
            item is ThreadItem.ExitedReviewMode ||
            item is ThreadItem.CollabAgentToolCall
    
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(background)
                .padding(
                    if (isCardLikeItem) 0.dp else 14.dp
                )
                .widthIn(max = if (isUser) 280.dp else 340.dp)
        ) {
            when (item) {
                is ThreadItem.UserMessage -> {
                    Text(
                        text = item.content.joinToString { it.text.orEmpty() },
                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onPrimaryContainer),
                    )
                }
                is ThreadItem.AgentMessage -> {
                    MarkdownText(
                        markdown = item.text
                    )
                }
                is ThreadItem.Reasoning -> ReasoningItem(item)
                is ThreadItem.PlanUpdate -> PlanUpdateItem(item)
                is ThreadItem.CommandExecution -> CommandExecutionItem(item)
                is ThreadItem.McpToolCall -> McpToolCallItem(item)
                is ThreadItem.FileChange -> FileChangeItem(item)
                is ThreadItem.WebSearch -> InfoItem(id = item.id, title = "Web search", body = item.query)
                is ThreadItem.ImageView -> InfoItem(id = item.id, title = "Image", body = item.path)
                is ThreadItem.EnteredReviewMode -> InfoItem(id = item.id, title = "Review started", body = item.review)
                is ThreadItem.ExitedReviewMode -> InfoItem(id = item.id, title = "Review finished", body = item.review)
                is ThreadItem.CollabAgentToolCall ->
                    InfoItem(id = item.id, title = "Collab tool call", body = "${item.tool} (${item.status})")
            }
        }
    }
}

@Composable
fun ReasoningItem(item: ThreadItem.Reasoning) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    Column {
        val toggle = { expanded = !expanded }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = toggle)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Reasoning",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        if (expanded) {
            Column(modifier = Modifier.padding(12.dp)) {
                item.summary.forEach {
                    MarkdownText(
                        markdown = it,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic)
                    )
                }
                item.content.forEach {
                    MarkdownText(
                        markdown = it,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic)
                    )
                }
            }
        }
    }
}

@Composable
fun CommandExecutionItem(item: ThreadItem.CommandExecution) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    Column {
        val toggle = { expanded = !expanded }
        val isRunning = item.status == CommandExecutionStatus.inProgress || item.status == CommandExecutionStatus.unknown
        val title = if (isRunning) "Running" else "Ran"
        val dotColor =
            when (item.status) {
                CommandExecutionStatus.inProgress -> Color(0xFFF9A825) // yellow
                CommandExecutionStatus.completed -> Color(0xFF2E7D32) // green
                CommandExecutionStatus.failed, CommandExecutionStatus.declined -> Color(0xFFC62828) // red
                CommandExecutionStatus.unknown -> MaterialTheme.colorScheme.outline
            }

        val commandOneLine = item.command.lineSequence().firstOrNull().orEmpty().trim()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = toggle)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    commandOneLine,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (expanded) {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(10.dp)) {
                if (item.command.isNotBlank()) {
                    Text("Command", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        item.command,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item.aggregatedOutput?.let { out ->
                    if (out.isNotBlank()) {
                        if (item.command.isNotBlank()) Spacer(Modifier.height(8.dp))
                        Text("Output", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text(
                            out,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlanUpdateItem(item: ThreadItem.PlanUpdate) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    Column {
        val toggle = { expanded = !expanded }
        val total = item.plan.size
        val done = item.plan.count { it.status == PlanEntryStatus.completed }
        val summary =
            when {
                total == 0 -> ""
                done == 0 -> "$total"
                else -> "$done/$total"
            }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = toggle)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "To-dos",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (summary.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        if (expanded) {
            val markdown =
                buildString {
                    val explanation = item.explanation?.trim().orEmpty()
                    if (explanation.isNotBlank()) {
                        appendLine(explanation)
                        appendLine()
                    }
                    item.plan.forEach { entry ->
                        val step = entry.step.trim()
                        if (step.isBlank()) return@forEach
                        val line =
                            when (entry.status) {
                                PlanEntryStatus.completed -> "- [x] $step"
                                PlanEntryStatus.inProgress -> "- [ ] (in progress) $step"
                                PlanEntryStatus.failed -> "- [ ] (failed) $step"
                                PlanEntryStatus.cancelled -> "- [ ] (cancelled) $step"
                                PlanEntryStatus.pending, PlanEntryStatus.unknown -> "- [ ] $step"
                            }
                        appendLine(line)
                    }
                }

            Column(modifier = Modifier.padding(12.dp)) {
                if (markdown.isNotBlank()) {
                    MarkdownText(
                        markdown = markdown,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun McpToolCallItem(item: ThreadItem.McpToolCall) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    Column {
        val toggle = { expanded = !expanded }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = toggle)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${item.server} :: ${item.tool}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                item.status.name,
                style = MaterialTheme.typography.labelSmall,
                color = when (item.status) {
                    McpToolCallStatus.completed -> Color.Green
                    McpToolCallStatus.failed -> MaterialTheme.colorScheme.error
                    else -> Color.Yellow
                }
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (expanded) {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(10.dp)) {
                if (item.progress.isNotEmpty()) {
                    Text("Progress", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    item.progress.takeLast(8).forEach { msg ->
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Text("Arguments", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text(item.arguments.toString(), style = MaterialTheme.typography.bodySmall)

                item.result?.let { res ->
                    Spacer(Modifier.height(8.dp))
                    Text("Result", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(res.toString(), style = MaterialTheme.typography.bodySmall)
                }

                item.error?.let { err ->
                    Spacer(Modifier.height(8.dp))
                    Text("Error", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    Text(err.toString(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun FileChangeItem(item: ThreadItem.FileChange) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    Column {
        val toggle = { expanded = !expanded }
        val dotColor =
            when (item.status) {
                PatchApplyStatus.inProgress -> Color(0xFFF9A825) // yellow
                PatchApplyStatus.completed -> Color(0xFF2E7D32) // green
                PatchApplyStatus.failed, PatchApplyStatus.declined -> Color(0xFFC62828) // red
                PatchApplyStatus.unknown -> MaterialTheme.colorScheme.outline
            }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = toggle)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "File changes (${item.changes.size})",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        if (expanded) {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(10.dp)) {
                item.changes.forEach { change ->
                    if (change.path.isNotBlank()) {
                        Text(change.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    if (change.diff.isNotBlank()) {
                        Text(change.diff, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                item.output?.let { out ->
                    if (out.isNotBlank()) {
                        Text("Output", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text(out, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoItem(id: String, title: String, body: String) {
    var expanded by rememberSaveable(id) { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        val toggle = { expanded = !expanded }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = toggle)
                    .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!expanded && body.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        if (expanded) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (body.isNotBlank()) {
                    MarkdownText(
                        markdown = body,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = CodexLogo,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isSystemInDarkTheme()) 0.10f else 0.06f),
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
            Text("Error: $message", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onControls: () -> Unit,
    enabled: Boolean,
    isSending: Boolean,
    canSend: Boolean,
) {
    Surface(
        color = MaterialTheme.colorScheme.background
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
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                IconButton(onClick = onControls, enabled = enabled) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Session controls",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(10.dp))

            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(24.dp)),
                placeholder = { Text("Ask something...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                maxLines = 5,
                enabled = enabled,
                trailingIcon = {
                    if (isSending) {
                        FilledIconButton(
                            onClick = onStop,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Stop session"
                            )
                        }
                    } else {
                        val isEnabled = enabled && canSend && text.trim().isNotEmpty()
                        FilledIconButton(
                            onClick = onSend,
                            enabled = isEnabled,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            ),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send"
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
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
