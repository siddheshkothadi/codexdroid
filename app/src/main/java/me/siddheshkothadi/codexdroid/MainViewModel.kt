package me.siddheshkothadi.codexdroid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import me.siddheshkothadi.codexdroid.domain.usecase.GetConnectionsUseCase
import me.siddheshkothadi.codexdroid.navigation.CodexDroidAppLink
import me.siddheshkothadi.codexdroid.ui.navigation.Screen
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getConnectionsUseCase: GetConnectionsUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _startDestination = MutableStateFlow(Screen.Setup.createRoute())
    val startDestination = _startDestination.asStateFlow()

    private val _appLinks = MutableSharedFlow<CodexDroidAppLink>(replay = 1, extraBufferCapacity = 8)
    val appLinks = _appLinks.asSharedFlow()

    init {
        viewModelScope.launch {
            // We verify if there are any connections.
            // We use .first() to get the current state from DataStore.
            // DataStore usually emits immediately.
            val connections = getConnectionsUseCase().first()
            
            if (connections.isNotEmpty()) {
                _startDestination.value = Screen.Session.createRoute()
            } else {
                _startDestination.value = Screen.Setup.createRoute()
            }
            
            _isLoading.value = false
        }
    }

    fun onAppLink(link: CodexDroidAppLink) {
        _appLinks.tryEmit(link)
    }
}
