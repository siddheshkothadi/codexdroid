package me.siddheshkothadi.codexdroid.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import me.siddheshkothadi.codexdroid.ui.components.CodexDroidDrawerContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onAddConnectionClick: () -> Unit = {},
    onEditConnectionClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val connections by viewModel.connections.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            CodexDroidDrawerContent(
                historyState = uiState,
                connections = connections,
                activeThreadId = null, // No active thread tracked here yet
                onThreadClick = { thread ->
                    // Handle thread click (e.g., navigate to thread details)
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
                connectionStatus = connectionStatus,
                isSyncing = isRefreshing,
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val activeConnection = connections.firstOrNull()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ConnectionDot(connectionStatus)
                            Spacer(Modifier.width(8.dp))
                            if (isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(activeConnection?.name ?: "Codex")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Main content area - currently just shows status or a placeholder
                when (val state = uiState) {
                    is HistoryUiState.Loading -> HistorySkeleton()
                    is HistoryUiState.Empty -> Text("Connect to a server to get started")
                    is HistoryUiState.Success -> {
                        if (state.threads.isEmpty() && isRefreshing) {
                            HistorySkeleton()
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Select a thread from the menu to view details",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionDot(status: me.siddheshkothadi.codexdroid.codex.ConnectionStatus) {
    val color =
        when (status) {
            me.siddheshkothadi.codexdroid.codex.ConnectionStatus.Healthy -> Color(0xFF2E7D32)
            me.siddheshkothadi.codexdroid.codex.ConnectionStatus.Unhealthy -> Color(0xFFC62828)
            me.siddheshkothadi.codexdroid.codex.ConnectionStatus.Checking -> MaterialTheme.colorScheme.outline
            me.siddheshkothadi.codexdroid.codex.ConnectionStatus.Unknown -> MaterialTheme.colorScheme.outline
        }
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color, CircleShape)
    )
}

@Composable
private fun HistorySkeleton() {
    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        repeat(6) {
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
