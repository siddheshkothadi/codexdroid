package me.siddheshkothadi.codexdroid.ui.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.siddheshkothadi.codexdroid.data.local.Connection
import me.siddheshkothadi.codexdroid.domain.usecase.AddConnectionUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.GetConnectionsUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.UpdateConnectionUseCase
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val getConnectionsUseCase: GetConnectionsUseCase,
    private val addConnectionUseCase: AddConnectionUseCase,
    private val updateConnectionUseCase: UpdateConnectionUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val connectionId: String? = savedStateHandle.get<String>("connectionId")

    val connections: StateFlow<List<Connection>> = getConnectionsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val connectionToEdit: StateFlow<Connection?> = connections.map { list ->
        list.find { it.id == connectionId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow<SetupUiState>(SetupUiState.Idle)
    val uiState: StateFlow<SetupUiState> = _uiState

    fun saveConnection(name: String, url: String, secret: String) {
        viewModelScope.launch {
            _uiState.value = SetupUiState.Loading
            try {
                if (connectionId != null) {
                    updateConnectionUseCase(connectionId, name, url, secret)
                } else {
                    addConnectionUseCase(name, url, secret)
                }
                _uiState.value = SetupUiState.Success
            } catch (e: Exception) {
                _uiState.value = SetupUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface SetupUiState {
    object Idle : SetupUiState
    object Loading : SetupUiState
    object Success : SetupUiState
    data class Error(val message: String) : SetupUiState
}
