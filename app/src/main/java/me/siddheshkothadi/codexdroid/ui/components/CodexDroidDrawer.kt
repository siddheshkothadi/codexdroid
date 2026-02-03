package me.siddheshkothadi.codexdroid.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.text.AnnotatedString
import me.siddheshkothadi.codexdroid.codex.Thread
import me.siddheshkothadi.codexdroid.codex.ConnectionStatus
import me.siddheshkothadi.codexdroid.codex.TurnStatus
import me.siddheshkothadi.codexdroid.data.local.Connection
import me.siddheshkothadi.codexdroid.ui.history.HistoryUiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CodexDroidDrawerContent(
    historyState: HistoryUiState,
    connections: List<Connection>,
    onThreadClick: (Thread) -> Unit,
    onConnectionSelect: (Connection) -> Unit,
    onEditClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onSetupClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeThreadId: String? = null,
    connectionStatus: ConnectionStatus = ConnectionStatus.Unknown,
    isSyncing: Boolean = false,
) {
    ModalDrawerSheet(modifier = modifier) {
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
                        var collapsedGroups by rememberSaveable { mutableStateOf(emptyList<String>()) }
                        val collapsedSet = remember(collapsedGroups) { collapsedGroups.toSet() }
                        var showCwdDialog by remember { mutableStateOf<String?>(null) }
                        val clipboard = LocalClipboardManager.current

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
                                                    t.preview.lowercase().contains(query) ||
                                                        t.id.lowercase().contains(query)
                                                }
                                    }
                                }
                            }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            item {
                                Text(
                                    text = "Recent Threads",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

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

                                item(key = "cwd:$cwd") {
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .combinedClickable(
                                                    onClick = {
                                                        val next =
                                                            collapsedSet.toMutableSet().apply {
                                                                if (isCollapsed) remove(cwd) else add(cwd)
                                                            }
                                                        collapsedGroups = next.toList()
                                                    },
                                                    onLongClick = {
                                                        if (cwd != "(no cwd)") showCwdDialog = cwd
                                                    }
                                                )
                                                .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = folderLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (!isCollapsed) {
                                    val threads = grouped[cwd].orEmpty()
                                    items(threads, key = { it.id }) { thread ->
                                        val hasRunningTurn =
                                            remember(thread.turns) {
                                                thread.turns.any { turn -> turn.status == TurnStatus.inProgress }
                                            }
                                        NavigationDrawerItem(
                                            label = {
                                                Text(
                                                    text = thread.preview.ifBlank { "Untitled Thread" },
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            badge =
                                                if (hasRunningTurn) {
                                                    {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(14.dp),
                                                            strokeWidth = 2.dp
                                                        )
                                                    }
                                                } else {
                                                    null
                                                },
                                            selected = thread.id == activeThreadId,
                                            onClick = { onThreadClick(thread) },
                                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                        )
                                    }

                                    item(key = "cwd:spacer:$cwd") {
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }
                        }

                        showCwdDialog?.let { cwd ->
                            AlertDialog(
                                onDismissRequest = { showCwdDialog = null },
                                title = { Text(workspaceFolderName(cwd).ifBlank { "Workspace" }) },
                                text = {
                                    Text(
                                        text = cwd,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(cwd))
                                            showCwdDialog = null
                                        }
                                    ) { Text("Copy path") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showCwdDialog = null }) { Text("Close") }
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
            color = MaterialTheme.colorScheme.secondary
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (activeConnection != null) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            IconButton(onClick = { onEditClick(activeConnection.id) }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Connection",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    tint = MaterialTheme.colorScheme.error
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
private fun HistorySkeleton() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        repeat(8) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant
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

@Composable
private fun DrawerSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val borderColor =
        if (focused) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.0f)

    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        interactionSource = interactionSource,
        placeholder = { Text("Search threads or folders") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        modifier =
            modifier
                .clip(shape)
                .border(width = 1.dp, color = borderColor, shape = shape),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            )
    )
}
