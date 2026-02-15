package me.siddheshkothadi.codexdroid.feature.history.ui

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.siddheshkothadi.codexdroid.feature.shared.ui.components.CodexDroidDrawerContent
import me.siddheshkothadi.codexdroid.ui.theme.CodexTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onAddConnectionClick: () -> Unit = {},
    onEditConnectionClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val connections by viewModel.connections.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
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
                                    color = CodexTheme.colors.textSecondary
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
    val colors = CodexTheme.colors
    val color =
        when (status) {
            me.siddheshkothadi.codexdroid.codex.ConnectionStatus.Healthy -> colors.accentSuccess
            me.siddheshkothadi.codexdroid.codex.ConnectionStatus.Unhealthy -> colors.accentError
            me.siddheshkothadi.codexdroid.codex.ConnectionStatus.Checking -> colors.borderDefault
            me.siddheshkothadi.codexdroid.codex.ConnectionStatus.Unknown -> colors.borderDefault
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
                color = CodexTheme.colors.bgSecondary
            ) {}
            Spacer(Modifier.height(12.dp))
        }
    }
}

