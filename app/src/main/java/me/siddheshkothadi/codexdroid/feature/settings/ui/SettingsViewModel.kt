package me.siddheshkothadi.codexdroid.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.siddheshkothadi.codexdroid.codex.CodexResponse
import me.siddheshkothadi.codexdroid.domain.model.Connection
import me.siddheshkothadi.codexdroid.domain.model.SarvamTtsSettings
import me.siddheshkothadi.codexdroid.domain.usecase.GetConnectionsUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.GetSarvamApiKeyUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.ListExperimentalFeaturesUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.ObserveSarvamTtsSettingsUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.SetRemoteFeatureFlagUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.UpdateSarvamApiKeyUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.UpdateSarvamTtsSettingsUseCase
import javax.inject.Inject

data class CodexFeatureUi(
    val name: String,
    val stage: String,
    val enabled: Boolean,
    val defaultEnabled: Boolean,
    val displayName: String? = null,
    val description: String? = null,
    val announcement: String? = null,
)

data class SettingsUiState(
    val settings: SarvamTtsSettings = SarvamTtsSettings(),
    val sarvamApiKeyDraft: String = "",
    val isLoading: Boolean = true,
    val activeConnectionName: String? = null,
    val featuresLoading: Boolean = false,
    val featuresError: String? = null,
    val featureUpdatingKey: String? = null,
    val stableFeatures: List<CodexFeatureUi> = emptyList(),
    val experimentalFeatures: List<CodexFeatureUi> = emptyList(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSarvamTtsSettingsUseCase: ObserveSarvamTtsSettingsUseCase,
    getConnectionsUseCase: GetConnectionsUseCase,
    private val updateSarvamTtsSettingsUseCase: UpdateSarvamTtsSettingsUseCase,
    private val updateSarvamApiKeyUseCase: UpdateSarvamApiKeyUseCase,
    private val getSarvamApiKeyUseCase: GetSarvamApiKeyUseCase,
    private val listExperimentalFeaturesUseCase: ListExperimentalFeaturesUseCase,
    private val setRemoteFeatureFlagUseCase: SetRemoteFeatureFlagUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private var persistApiKeyJob: Job? = null
    private var activeConnection: Connection? = null

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
        viewModelScope.launch {
            getConnectionsUseCase().collectLatest { list ->
                val next = list.firstOrNull()
                val changed = next?.id != activeConnection?.id
                activeConnection = next

                if (!changed) return@collectLatest
                if (next == null) {
                    _uiState.update {
                        it.copy(
                            activeConnectionName = null,
                            featuresLoading = false,
                            featuresError = null,
                            featureUpdatingKey = null,
                            stableFeatures = emptyList(),
                            experimentalFeatures = emptyList(),
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            activeConnectionName = next.name.takeIf { name -> name.isNotBlank() },
                        )
                    }
                    refreshCodexFeatures()
                }
            }
        }
    }

    fun onVoiceChanged(voice: String) {
        updateSettings { it.copy(voice = voice) }
    }

    fun onPaceChanged(pace: Float) {
        updateSettings { it.copy(pace = pace) }
    }

    fun onTemperatureChanged(temperature: Float) {
        updateSettings { it.copy(temperature = temperature) }
    }

    fun onSarvamApiKeyDraftChanged(value: String) {
        _uiState.update { it.copy(sarvamApiKeyDraft = value) }
        persistApiKeyJob?.cancel()
        persistApiKeyJob =
            viewModelScope.launch {
                delay(350)
                updateSarvamApiKeyUseCase(value)
            }
    }

    fun refreshCodexFeatures() {
        val connection = activeConnection
        if (connection == null) {
            _uiState.update {
                it.copy(
                    featuresLoading = false,
                    featuresError = "Connect to a Codex server to manage feature flags.",
                    stableFeatures = emptyList(),
                    experimentalFeatures = emptyList(),
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(featuresLoading = true, featuresError = null) }
            try {
                val loaded = loadCodexFeatures(connection)
                val stable =
                    loaded.filter { feature ->
                        feature.stage == "stable" && feature.name != "personality"
                    }
                val experimental =
                    loaded.filter { feature ->
                        feature.stage == "beta" || feature.stage == "under_development"
                    }
                _uiState.update {
                    it.copy(
                        featuresLoading = false,
                        featuresError = null,
                        stableFeatures = stable,
                        experimentalFeatures = experimental,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        featuresLoading = false,
                        featuresError = e.message ?: "Unable to load feature flags.",
                        stableFeatures = emptyList(),
                        experimentalFeatures = emptyList(),
                    )
                }
            }
        }
    }

    fun onToggleCodexFeature(feature: CodexFeatureUi) {
        val connection = activeConnection ?: return
        viewModelScope.launch {
            val nextEnabled = !feature.enabled
            _uiState.update { it.copy(featureUpdatingKey = feature.name, featuresError = null) }
            try {
                val response =
                    setRemoteFeatureFlagUseCase(
                        baseUrl = connection.baseUrl,
                        secret = connection.secret,
                        featureKey = feature.name,
                        enabled = nextEnabled,
                    )
                throwOnError(response, "Unable to update feature ${feature.name}.")
                _uiState.update { state ->
                    state.copy(
                        featureUpdatingKey = null,
                        stableFeatures =
                            state.stableFeatures.map { item ->
                                if (item.name == feature.name) item.copy(enabled = nextEnabled) else item
                            },
                        experimentalFeatures =
                            state.experimentalFeatures.map { item ->
                                if (item.name == feature.name) item.copy(enabled = nextEnabled) else item
                            },
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        featureUpdatingKey = null,
                        featuresError = e.message ?: "Unable to update feature ${feature.name}.",
                    )
                }
            }
        }
    }

    private fun updateSettings(transform: (SarvamTtsSettings) -> SarvamTtsSettings) {
        val updated = transform(_uiState.value.settings)
        viewModelScope.launch {
            updateSarvamTtsSettingsUseCase(updated)
        }
    }

    private suspend fun loadCodexFeatures(connection: Connection): List<CodexFeatureUi> {
        val loaded = mutableListOf<CodexFeatureUi>()
        val seen = mutableSetOf<String>()
        var cursor: String? = null

        var pageIndex = 0
        while (pageIndex < 20) {
            var response =
                listExperimentalFeaturesUseCase(
                    baseUrl = connection.baseUrl,
                    secret = connection.secret,
                    cursor = cursor,
                    limit = 100,
                )
            val errorMessage = response.error?.message?.trim().orEmpty()
            if (
                cursor == null &&
                errorMessage.contains("invalid cursor", ignoreCase = true) &&
                errorMessage.contains("null", ignoreCase = true)
            ) {
                response =
                    listExperimentalFeaturesUseCase(
                        baseUrl = connection.baseUrl,
                        secret = connection.secret,
                        cursor = "",
                        limit = 100,
                    )
            }
            throwOnError(response, "Unable to load feature flags.")
            val (pageItems, nextCursor) = parseFeaturePage(response.result)
            pageItems.forEach { feature ->
                if (seen.add(feature.name)) loaded += feature
            }
            if (nextCursor.isNullOrBlank()) break
            cursor = nextCursor
            pageIndex += 1
        }

        return loaded.sortedBy { it.name.lowercase() }
    }

    private fun throwOnError(response: CodexResponse<*>, fallbackMessage: String) {
        val error = response.error ?: return
        val message = error.message.trim().ifEmpty { fallbackMessage }
        throw IllegalStateException(message)
    }

    private fun parseFeaturePage(result: JsonElement?): Pair<List<CodexFeatureUi>, String?> {
        val root = result as? JsonObject ?: return emptyList<CodexFeatureUi>() to null
        val payload = readObject(root, "result") ?: root
        val dataRaw = readArray(payload, "data", "items", "features").orEmpty()
        val items =
            dataRaw.mapNotNull { entry ->
                val obj = entry as? JsonObject ?: return@mapNotNull null
                parseFeature(obj)
            }
        val nextCursor = readString(payload, "nextCursor", "next_cursor").takeIf { it.isNotBlank() }
        return items to nextCursor
    }

    private fun parseFeature(item: JsonObject): CodexFeatureUi? {
        val name = readString(item, "name")
        if (name.isBlank()) return null
        val stage = normalizeStage(readString(item, "stage")) ?: return null
        return CodexFeatureUi(
            name = name,
            stage = stage,
            enabled = readBoolean(item, "enabled"),
            defaultEnabled = readBoolean(item, "defaultEnabled", "default_enabled"),
            displayName = readString(item, "displayName", "display_name").takeIf { it.isNotBlank() },
            description = readString(item, "description").takeIf { it.isNotBlank() },
            announcement = readString(item, "announcement").takeIf { it.isNotBlank() },
        )
    }

    private fun normalizeStage(raw: String): String? {
        return when (raw.trim().lowercase()) {
            "stable" -> "stable"
            "beta", "experimental" -> "beta"
            "underdevelopment", "under_development" -> "under_development"
            "deprecated" -> "deprecated"
            "removed" -> "removed"
            else -> null
        }
    }

    private fun readString(obj: JsonObject, vararg keys: String): String {
        return keys.firstNotNullOfOrNull { key ->
            (obj[key] as? JsonPrimitive)?.let { primitive ->
                runCatching { primitive.jsonPrimitive.content.trim() }.getOrNull()?.takeIf { it.isNotEmpty() }
            }
        }.orEmpty()
    }

    private fun readBoolean(obj: JsonObject, vararg keys: String): Boolean {
        keys.forEach { key ->
            val normalized =
                runCatching { (obj[key] as? JsonPrimitive)?.jsonPrimitive?.content?.trim()?.lowercase() }
                    .getOrNull()
                    ?: return@forEach
            when (normalized) {
                "true", "1" -> return true
                "false", "0" -> return false
            }
        }
        return false
    }

    private fun readArray(obj: JsonObject, vararg keys: String): JsonArray? {
        return keys.firstNotNullOfOrNull { key ->
            obj[key] as? JsonArray
        }?.jsonArray
    }

    private fun readObject(obj: JsonObject, vararg keys: String): JsonObject? {
        return keys.firstNotNullOfOrNull { key ->
            obj[key] as? JsonObject
        }?.jsonObject
    }
}
