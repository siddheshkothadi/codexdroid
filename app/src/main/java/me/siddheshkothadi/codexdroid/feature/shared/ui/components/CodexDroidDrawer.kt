package me.siddheshkothadi.codexdroid.feature.shared.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import me.siddheshkothadi.codexdroid.codex.Thread
import me.siddheshkothadi.codexdroid.codex.ConnectionStatus
import me.siddheshkothadi.codexdroid.codex.TurnStatus
import me.siddheshkothadi.codexdroid.domain.model.Connection
import me.siddheshkothadi.codexdroid.feature.history.ui.HistoryUiState
import me.siddheshkothadi.codexdroid.ui.theme.CodexTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CodexDroidDrawerContent(
    historyState: HistoryUiState,
    connections: List<Connection>,
    onThreadClick: (Thread) -> Unit,
    onConnectionSelect: (Connection) -> Unit,
    onEditClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onThreadRename: (Thread, String) -> Unit = { _, _ -> },
    onThreadDelete: (Thread) -> Unit = {},
    onStartThreadInCwd: (String) -> Unit = {},
    onDeleteThreadsInCwd: (String) -> Unit = {},
    onSetupClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    activeThreadId: String? = null,
    connectionStatus: ConnectionStatus = ConnectionStatus.Unknown,
    isSyncing: Boolean = false,
) {
    ModalDrawerSheet(
        modifier = modifier,
        drawerContainerColor = CodexTheme.colors.bgPrimary
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // History List (90% height approximately)
            Box(modifier = Modifier.weight(0.9f)) {
                when (historyState) {
                    is HistoryUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            HistorySkeleton()
                        }
                    }
                    is HistoryUiState.Empty -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No history found")
                        }
                    }
                    is HistoryUiState.Success -> {
                        var searchQuery by rememberSaveable { mutableStateOf("") }
                        var threadMenuTargetId by rememberSaveable { mutableStateOf<String?>(null) }
                        var renamingThreadId by rememberSaveable { mutableStateOf<String?>(null) }
                        var renameDraft by rememberSaveable { mutableStateOf("") }
                        var deleteCwdTarget by rememberSaveable { mutableStateOf<String?>(null) }
                        val isDark = isSystemInDarkTheme()
                        val actionButtonBackground =
                            if (isDark) CodexTheme.colors.bgSecondary else CodexTheme.colors.inputButtonBackground
                        val actionButtonIcon =
                            if (isDark) CodexTheme.colors.textSecondary else CodexTheme.colors.inputButtonContent

                        val grouped =
                            remember(historyState.threads) {
                                historyState.threads.groupBy { thread ->
                                    thread.cwd.takeIf { it.isNotBlank() } ?: "(no cwd)"
                                }
                            }
                        val sortedKeys =
                            remember(grouped) {
                                grouped.keys.sortedWith(compareBy<String> { it == "(no cwd)" }.thenBy { it })
                            }
                        val activeThreadCwd =
                            remember(historyState.threads, activeThreadId) {
                                historyState.threads.firstOrNull { it.id == activeThreadId }?.let { activeThread ->
                                    activeThread.cwd.takeIf { it.isNotBlank() } ?: "(no cwd)"
                                }
                            }
                        val defaultCollapsedGroups =
                            remember(sortedKeys, activeThreadCwd) {
                                if (activeThreadCwd == null) sortedKeys
                                else sortedKeys.filter { key -> key != activeThreadCwd }
                            }
                        val collapseSeed = remember(sortedKeys, activeThreadCwd) {
                            buildString {
                                append(activeThreadCwd.orEmpty())
                                append('|')
                                append(sortedKeys.joinToString(separator = "|"))
                            }
                        }
                        var collapsedGroups by
                            rememberSaveable(collapseSeed) {
                                mutableStateOf(defaultCollapsedGroups)
                            }
                        val collapsedSet = remember(collapsedGroups) { collapsedGroups.toSet() }

                        val query = searchQuery.trim().lowercase()
                        val filteredKeys =
                            remember(sortedKeys, grouped, query) {
                                if (query.isBlank()) sortedKeys
                                else {
                                    sortedKeys.filter { cwd ->
                                        val folder = workspaceFolderName(cwd).lowercase()
                                        folder.contains(query) ||
                                            cwd.lowercase().contains(query) ||
                                            grouped[cwd]
                                                .orEmpty()
                                                .any { t ->
                                                    threadDisplayName(t).lowercase().contains(query) ||
                                                    t.preview.lowercase().contains(query) ||
                                                        t.id.lowercase().contains(query)
                                                }
                                    }
                                }
                            }
                        val threadIds = remember(historyState.threads) { historyState.threads.map { it.id }.toSet() }
                        LaunchedEffect(threadIds, threadMenuTargetId, renamingThreadId) {
                            if (threadMenuTargetId != null && !threadIds.contains(threadMenuTargetId)) {
                                threadMenuTargetId = null
                            }
                            if (renamingThreadId != null && !threadIds.contains(renamingThreadId)) {
                                renamingThreadId = null
                                renameDraft = ""
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            item {
                                DrawerSearchField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            filteredKeys.forEach { cwd ->
                                val isCollapsed = collapsedSet.contains(cwd)
                                val folderLabel = workspaceFolderName(cwd).ifBlank { cwd }
                                val folderThreads = grouped[cwd].orEmpty()

                                item(key = "cwd:$cwd") {
                                    Surface(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .combinedClickable(
                                                    onClick = {},
                                                    onLongClick = {
                                                        if (cwd != "(no cwd)" && folderThreads.isNotEmpty()) {
                                                            deleteCwdTarget = cwd
                                                        }
                                                    }
                                                ),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.Transparent
                                    ) {
                                        Row(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = CodexTheme.colors.accentUi.copy(alpha = 0.78f),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                text = folderLabel,
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                                color = CodexTheme.colors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                modifier = Modifier.size(32.dp),
                                                shape = CircleShape,
                                                color = actionButtonBackground
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        val next =
                                                            collapsedSet.toMutableSet().apply {
                                                                if (isCollapsed) remove(cwd) else add(cwd)
                                                            }
                                                        collapsedGroups = next.toList()
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector =
                                                            if (isCollapsed) Icons.Default.ExpandMore
                                                            else Icons.Default.ExpandLess,
                                                        contentDescription =
                                                            if (isCollapsed) "Expand folder"
                                                            else "Collapse folder",
                                                        tint = actionButtonIcon
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.width(6.dp))
                                            Surface(
                                                modifier = Modifier.size(32.dp),
                                                shape = CircleShape,
                                                color = actionButtonBackground
                                            ) {
                                                val canStartInFolder = cwd != "(no cwd)"
                                                IconButton(
                                                    enabled = canStartInFolder,
                                                    onClick = {
                                                        if (canStartInFolder) {
                                                            onStartThreadInCwd(cwd)
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Start session in folder",
                                                        tint = actionButtonIcon
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (!isCollapsed) {
                                    items(folderThreads, key = { it.id }) { thread ->
                                        val hasRunningTurn =
                                            remember(thread.turns) {
                                                thread.turns.any { turn -> turn.status == TurnStatus.inProgress }
                                            }
                                        val isRenaming = renamingThreadId == thread.id
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            if (isRenaming) {
                                                Row(
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .padding(NavigationDrawerItemDefaults.ItemPadding),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    TextField(
                                                        value = renameDraft,
                                                        onValueChange = { renameDraft = it },
                                                        singleLine = true,
                                                        modifier =
                                                            Modifier
                                                                .weight(1f)
                                                                .heightIn(min = 52.dp)
                                                                .clip(RoundedCornerShape(24.dp))
                                                                .border(
                                                                    width = 1.dp,
                                                                    color = CodexTheme.colors.inputFieldBorder,
                                                                    shape = RoundedCornerShape(24.dp)
                                                                ),
                                                        placeholder = { Text("New name", color = CodexTheme.colors.textSecondary) },
                                                        colors =
                                                            TextFieldDefaults.colors(
                                                                focusedContainerColor = CodexTheme.colors.inputFieldBackground,
                                                                unfocusedContainerColor = CodexTheme.colors.inputFieldBackground,
                                                                disabledContainerColor = CodexTheme.colors.inputFieldBackground,
                                                                focusedIndicatorColor = Color.Transparent,
                                                                unfocusedIndicatorColor = Color.Transparent,
                                                                disabledIndicatorColor = Color.Transparent,
                                                                focusedTextColor = CodexTheme.colors.textPrimary,
                                                                unfocusedTextColor = CodexTheme.colors.textPrimary,
                                                                disabledTextColor = CodexTheme.colors.textPrimary,
                                                                disabledPlaceholderColor = CodexTheme.colors.textSecondary,
                                                            )
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Surface(
                                                        modifier = Modifier.size(34.dp),
                                                        shape = CircleShape,
                                                        color = CodexTheme.colors.bgSecondary
                                                    ) {
                                                        IconButton(
                                                            onClick = {
                                                                renamingThreadId = null
                                                                renameDraft = ""
                                                            }
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Close,
                                                                contentDescription = "Cancel rename",
                                                                tint = CodexTheme.colors.textSecondary
                                                            )
                                                        }
                                                    }
                                                    Spacer(Modifier.width(6.dp))
                                                    Surface(
                                                        modifier = Modifier.size(34.dp),
                                                        shape = CircleShape,
                                                        color = CodexTheme.colors.bgSecondary
                                                    ) {
                                                        IconButton(
                                                            onClick = {
                                                                val normalized = renameDraft.trim()
                                                                if (normalized.isNotBlank()) {
                                                                    onThreadRename(thread, normalized)
                                                                }
                                                                renamingThreadId = null
                                                                renameDraft = ""
                                                            }
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Check,
                                                                contentDescription = "Save thread name",
                                                                tint = CodexTheme.colors.textSecondary
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                Surface(
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                                                            .clip(RoundedCornerShape(28.dp))
                                                            .combinedClickable(
                                                                onClick = {
                                                                    threadMenuTargetId = null
                                                                    renamingThreadId = null
                                                                    onThreadClick(thread)
                                                                },
                                                                onLongClick = {
                                                                    threadMenuTargetId = thread.id
                                                                }
                                                            ),
                                                    color =
                                                        if (thread.id == activeThreadId) {
                                                            CodexTheme.colors.bgSecondary
                                                        } else {
                                                            Color.Transparent
                                                        }
                                                ) {
                                                    Row(
                                                        modifier =
                                                            Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier.width(20.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (hasRunningTurn) {
                                                                CircularProgressIndicator(
                                                                    modifier = Modifier.size(14.dp),
                                                                    strokeWidth = 2.dp
                                                                )
                                                            }
                                                        }
                                                        Spacer(Modifier.width(10.dp))
                                                        Text(
                                                            text = threadDisplayLabel(thread),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            color = CodexTheme.colors.textPrimary,
                                                            fontWeight = FontWeight.Medium,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                }
                                            }

                                            DropdownMenu(
                                                expanded = threadMenuTargetId == thread.id,
                                                onDismissRequest = { threadMenuTargetId = null }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Rename") },
                                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                                    onClick = {
                                                        threadMenuTargetId = null
                                                        renamingThreadId = thread.id
                                                        renameDraft =
                                                            thread.clientName?.takeIf { it.isNotBlank() }
                                                                ?: thread.preview
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Delete") },
                                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                                    onClick = {
                                                        threadMenuTargetId = null
                                                        if (renamingThreadId == thread.id) {
                                                            renamingThreadId = null
                                                            renameDraft = ""
                                                        }
                                                        onThreadDelete(thread)
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    item(key = "cwd:spacer:$cwd") {
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }
                        }

                        deleteCwdTarget?.let { cwd ->
                            val threadCount = grouped[cwd].orEmpty().size
                            val folderName = workspaceFolderName(cwd).ifBlank { cwd }
                            AlertDialog(
                                onDismissRequest = { deleteCwdTarget = null },
                                title = { Text("Delete all sessions?") },
                                text = {
                                    Text(
                                        "Delete $threadCount session(s) in \"$folderName\"? This cannot be undone."
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            onDeleteThreadsInCwd(cwd)
                                            if (
                                                renamingThreadId != null &&
                                                    grouped[cwd].orEmpty().any { it.id == renamingThreadId }
                                            ) {
                                                renamingThreadId = null
                                                renameDraft = ""
                                            }
                                            threadMenuTargetId = null
                                            deleteCwdTarget = null
                                        }
                                    ) { Text("Delete") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { deleteCwdTarget = null }) { Text("Cancel") }
                                }
                            )
                        }

                    }
                }
            }

            HorizontalDivider()

            // Connection Selector at the bottom
            ConnectionSelector(
                connections = connections,
                onConnectionSelect = onConnectionSelect,
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick,
                onSetupClick = onSetupClick,
                onSettingsClick = onSettingsClick,
                connectionStatus = connectionStatus,
                isSyncing = isSyncing,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun ConnectionSelector(
    connections: List<Connection>,
    onConnectionSelect: (Connection) -> Unit,
    onEditClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onSetupClick: () -> Unit,
    onSettingsClick: () -> Unit,
    connectionStatus: ConnectionStatus,
    isSyncing: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val activeConnection = connections.firstOrNull()
    var confirmDelete by remember { mutableStateOf<Connection?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Active Connection",
            style = MaterialTheme.typography.labelSmall,
            color = CodexTheme.colors.textSecondary
        )
        
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { expanded = true }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ConnectionDot(connectionStatus)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = activeConnection?.name ?: "No connection",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isSyncing) {
                            Spacer(Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    Text(
                        text = activeConnection?.baseUrl ?: "Configure a server",
                        style = MaterialTheme.typography.labelMedium,
                        color = CodexTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (activeConnection != null) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = CodexTheme.colors.bgSecondary
                        ) {
                            IconButton(onClick = onSettingsClick) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    modifier = Modifier.size(16.dp),
                                    tint = CodexTheme.colors.textSecondary
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = CodexTheme.colors.bgSecondary
                        ) {
                            IconButton(onClick = { onEditClick(activeConnection.id) }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Connection",
                                    modifier = Modifier.size(16.dp),
                                    tint = CodexTheme.colors.textSecondary
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(onClick = { expanded = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Switch connection")
                    }
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                connections.forEach { connection ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Only show status for active connection.
                                    if (connection.id == activeConnection?.id) {
                                        ConnectionDot(connectionStatus)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(connection.name)
                                }
                                Text(
                                    connection.baseUrl,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CodexTheme.colors.textSecondary
                                )
                            }
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    confirmDelete = connection
                                    expanded = false
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete connection",
                                    tint = CodexTheme.colors.accentError
                                )
                            }
                        },
                        onClick = {
                            onConnectionSelect(connection)
                            expanded = false
                        }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Add new connection") },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    onClick = {
                        onSetupClick()
                        expanded = false
                    }
                )
            }
        }
    }

    confirmDelete?.let { connection ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete connection?") },
            text = {
                Text(
                    "Delete “${connection.name}” (${connection.baseUrl})? This only removes it from your device."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick(connection.id)
                        confirmDelete = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
            }
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
private fun HistorySkeleton() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        repeat(8) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                shape = MaterialTheme.shapes.small,
                color = CodexTheme.colors.bgSecondary
            ) {}
            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun workspaceFolderName(cwd: String): String {
    val raw = cwd.trim()
    if (raw.isBlank() || raw == "(no cwd)") return raw
    val normalized = raw.trimEnd('/', '\\')
    if (normalized.isBlank()) return ""
    val slash = normalized.lastIndexOf('/')
    val backslash = normalized.lastIndexOf('\\')
    val idx = maxOf(slash, backslash)
    return if (idx == -1) normalized else normalized.substring(idx + 1)
}

private fun threadDisplayName(thread: Thread): String {
    return thread.clientName?.takeIf { it.isNotBlank() }
        ?: thread.preview.takeIf { it.isNotBlank() }
        ?: "Untitled Thread"
}

private const val ThreadLabelMaxChars = 24

private fun threadDisplayLabel(thread: Thread): String {
    val value = threadDisplayName(thread).trim()
    if (value.length <= ThreadLabelMaxChars) return value
    return value.take(ThreadLabelMaxChars - 1).trimEnd() + "…"
}

@Composable
private fun DrawerSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    val colors = CodexTheme.colors

    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = { Text("Search threads or folders") },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = colors.textSecondary
            )
        },
        modifier =
            modifier
                .clip(shape)
                .border(width = 1.dp, color = colors.inputFieldBorder, shape = shape),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = colors.inputFieldBackground,
                unfocusedContainerColor = colors.inputFieldBackground,
                disabledContainerColor = colors.inputFieldBackground,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                disabledTextColor = colors.textPrimary,
                focusedPlaceholderColor = colors.textSecondary,
                unfocusedPlaceholderColor = colors.textSecondary,
                disabledPlaceholderColor = colors.textSecondary,
            )
    )
}

