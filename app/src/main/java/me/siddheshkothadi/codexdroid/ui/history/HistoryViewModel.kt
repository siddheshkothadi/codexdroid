package me.siddheshkothadi.codexdroid.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.siddheshkothadi.codexdroid.codex.ConnectionStatus
import me.siddheshkothadi.codexdroid.codex.Thread
import me.siddheshkothadi.codexdroid.domain.model.Connection
import me.siddheshkothadi.codexdroid.domain.usecase.DeleteConnectionUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.GetConnectionsUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.GetThreadsUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.MarkConnectionUsedUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.PingConnectionUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.RefreshThreadsUseCase
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getConnectionsUseCase: GetConnectionsUseCase,
    private val getThreadsUseCase: GetThreadsUseCase,
    private val refreshThreadsUseCase: RefreshThreadsUseCase,
    private val markConnectionUsedUseCase: MarkConnectionUsedUseCase,
    private val deleteConnectionUseCase: DeleteConnectionUseCase,
    private val pingConnectionUseCase: PingConnectionUseCase,
) : ViewModel() {
    private val tag = "HistoryViewModel"

    val connections: StateFlow<List<Connection>> =
        getConnectionsUseCase().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Unknown)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val activeConnectionFlow: Flow<Connection?> =
        connections
            .map { it.firstOrNull() }
            .distinctUntilChangedBy { it?.id }

    init {
        observeActiveConnection()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HistoryUiState> =
        activeConnectionFlow
            .flatMapLatest { activeConnection ->
                if (activeConnection == null) {
                    flowOf(HistoryUiState.Empty)
                } else {
                    getThreadsUseCase(activeConnection.id).map { threads ->
                        HistoryUiState.Success(threads)
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState.Loading)

    private fun observeActiveConnection() {
        viewModelScope.launch {
            activeConnectionFlow.collectLatest { activeConnection ->
                if (activeConnection == null) {
                    _connectionStatus.value = ConnectionStatus.Unknown
                    return@collectLatest
                }
                checkConnection(activeConnection)
                refreshHistory(activeConnection)
            }
        }
    }

    fun refreshHistory(connection: Connection? = null) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            try {
                val activeConnection = connection ?: connections.value.firstOrNull()
                activeConnection?.let {
                    refreshThreadsUseCase(it)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to refresh history"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun checkConnection(connection: Connection) {
        _connectionStatus.value = ConnectionStatus.Checking
        viewModelScope.launch {
            val ok = pingConnectionUseCase(connection)
            _connectionStatus.value = if (ok) ConnectionStatus.Healthy else ConnectionStatus.Unhealthy
        }
    }

    fun selectConnection(connection: Connection) {
        viewModelScope.launch {
            markConnectionUsedUseCase(connection.id)
        }
    }

    fun deleteConnection(connectionId: String) {
        viewModelScope.launch {
            try {
                deleteConnectionUseCase(connectionId)
            } catch (e: Exception) {
                Log.w(tag, "Failed to delete connection", e)
                _error.value = e.message ?: "Failed to delete connection"
            }
        }
    }
}

sealed interface HistoryUiState {
    object Loading : HistoryUiState
    object Empty : HistoryUiState
    data class Success(val threads: List<Thread>) : HistoryUiState
}
