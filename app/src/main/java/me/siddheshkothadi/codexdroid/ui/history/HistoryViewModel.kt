package me.siddheshkothadi.codexdroid.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log
import me.siddheshkothadi.codexdroid.codex.ConnectionStatus
import me.siddheshkothadi.codexdroid.codex.CodexApiService
import me.siddheshkothadi.codexdroid.codex.Thread
import me.siddheshkothadi.codexdroid.data.local.Connection
import me.siddheshkothadi.codexdroid.data.repository.ConnectionRepository
import me.siddheshkothadi.codexdroid.domain.usecase.GetConnectionsUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.GetThreadsUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.RefreshThreadsUseCase
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getConnectionsUseCase: GetConnectionsUseCase,
    private val getThreadsUseCase: GetThreadsUseCase,
    private val refreshThreadsUseCase: RefreshThreadsUseCase,
    private val connectionRepository: ConnectionRepository,
    private val apiService: CodexApiService,
) : ViewModel() {
    private val tag = "HistoryViewModel"

    val connections: StateFlow<List<Connection>> = getConnectionsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Unknown)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HistoryUiState> = connections
        .flatMapLatest { connections ->
            if (connections.isEmpty()) {
                flowOf(HistoryUiState.Empty)
            } else {
                val activeConnection = connections.first()
                checkConnection(activeConnection)
                refreshHistory(activeConnection)
                getThreadsUseCase(activeConnection.id).map { threads ->
                    HistoryUiState.Success(threads)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState.Loading)

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
            val ok =
                try {
                    apiService.ping(connection.baseUrl, connection.secret)
                } catch (_: Exception) {
                    false
                }
            _connectionStatus.value = if (ok) ConnectionStatus.Healthy else ConnectionStatus.Unhealthy
        }
    }

    fun selectConnection(connection: Connection) {
        viewModelScope.launch {
            connectionRepository.updateLastUsed(connection.id)
        }
    }

    fun deleteConnection(connectionId: String) {
        viewModelScope.launch {
            try {
                connectionRepository.deleteConnection(connectionId)
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
