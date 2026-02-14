package me.siddheshkothadi.codexdroid.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.siddheshkothadi.codexdroid.domain.model.SarvamTtsSettings
import me.siddheshkothadi.codexdroid.domain.usecase.GetSarvamApiKeyUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.ObserveSarvamTtsSettingsUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.UpdateSarvamApiKeyUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.UpdateSarvamTtsSettingsUseCase
import javax.inject.Inject

data class SettingsUiState(
    val settings: SarvamTtsSettings = SarvamTtsSettings(),
    val sarvamApiKeyDraft: String = "",
    val isSarvamApiKeyVisible: Boolean = false,
    val isLoading: Boolean = true,
    val statusMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSarvamTtsSettingsUseCase: ObserveSarvamTtsSettingsUseCase,
    private val updateSarvamTtsSettingsUseCase: UpdateSarvamTtsSettingsUseCase,
    private val updateSarvamApiKeyUseCase: UpdateSarvamApiKeyUseCase,
    private val getSarvamApiKeyUseCase: GetSarvamApiKeyUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSarvamTtsSettingsUseCase().collectLatest { settings ->
                _uiState.update {
                    it.copy(
                        settings = settings,
                        isLoading = false,
                    )
                }
            }
        }
        viewModelScope.launch {
            val apiKey = getSarvamApiKeyUseCase().orEmpty()
            _uiState.update { it.copy(sarvamApiKeyDraft = apiKey) }
        }
    }

    fun onVoiceChanged(voice: String) {
        updateSettings { it.copy(voice = voice) }
    }

    fun onTargetLanguageChanged(targetLanguageCode: String) {
        updateSettings { it.copy(targetLanguageCode = targetLanguageCode) }
    }

    fun onPaceChanged(pace: Float) {
        updateSettings { it.copy(pace = pace) }
    }

    fun onSpeechSampleRateChanged(speechSampleRate: Int) {
        updateSettings { it.copy(speechSampleRate = speechSampleRate) }
    }

    fun onTemperatureChanged(temperature: Float) {
        updateSettings { it.copy(temperature = temperature) }
    }

    fun onSarvamApiKeyDraftChanged(value: String) {
        _uiState.update { it.copy(sarvamApiKeyDraft = value, statusMessage = null) }
    }

    fun toggleSarvamApiKeyVisibility() {
        _uiState.update { it.copy(isSarvamApiKeyVisible = !it.isSarvamApiKeyVisible) }
    }

    fun saveSarvamApiKey() {
        val candidate = _uiState.value.sarvamApiKeyDraft
        viewModelScope.launch {
            updateSarvamApiKeyUseCase(candidate)
            _uiState.update {
                it.copy(
                    statusMessage =
                        if (candidate.isBlank()) {
                            "Sarvam API key removed."
                        } else {
                            "Sarvam API key updated."
                        }
                )
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun updateSettings(transform: (SarvamTtsSettings) -> SarvamTtsSettings) {
        val updated = transform(_uiState.value.settings)
        viewModelScope.launch {
            updateSarvamTtsSettingsUseCase(updated)
            _uiState.update { it.copy(statusMessage = null) }
        }
    }
}

