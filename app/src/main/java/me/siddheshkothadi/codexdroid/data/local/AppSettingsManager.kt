package me.siddheshkothadi.codexdroid.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.siddheshkothadi.codexdroid.domain.model.SarvamTtsSettings
import me.siddheshkothadi.codexdroid.domain.model.SessionControlDefaults
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val lastSessionModelKey = stringPreferencesKey("session_last_model")
    private val lastSessionEffortKey = stringPreferencesKey("session_last_effort")
    private val approvalAllowRulesKey = stringPreferencesKey("approval_allow_rules_json")

    private val voiceKey = stringPreferencesKey("sarvam_voice")
    private val targetLanguageCodeKey = stringPreferencesKey("sarvam_target_language_code")
    private val paceKey = floatPreferencesKey("sarvam_pace")
    private val speechSampleRateKey = intPreferencesKey("sarvam_speech_sample_rate")
    private val temperatureKey = floatPreferencesKey("sarvam_temperature")
    private val encryptedSarvamApiKey = stringPreferencesKey("sarvam_api_key")

    fun observeSarvamTtsSettings(): Flow<SarvamTtsSettings> {
        return context.appSettingsDataStore.data.map { preferences ->
            val settings =
                SarvamTtsSettings(
                    voice = preferences[voiceKey] ?: SarvamTtsSettings.DEFAULT_VOICE,
                    targetLanguageCode =
                        preferences[targetLanguageCodeKey]
                            ?: SarvamTtsSettings.DEFAULT_TARGET_LANGUAGE_CODE,
                    pace = preferences[paceKey] ?: SarvamTtsSettings.DEFAULT_PACE,
                    speechSampleRate =
                        preferences[speechSampleRateKey]
                            ?: SarvamTtsSettings.DEFAULT_SPEECH_SAMPLE_RATE,
                    temperature = preferences[temperatureKey] ?: SarvamTtsSettings.DEFAULT_TEMPERATURE,
                    apiKeyPresent = !preferences[encryptedSarvamApiKey].isNullOrBlank(),
                )
            SarvamTtsSettings.sanitize(settings).copy(apiKeyPresent = settings.apiKeyPresent)
        }
    }

    suspend fun updateSarvamTtsSettings(settings: SarvamTtsSettings) {
        val sanitized = SarvamTtsSettings.sanitize(settings)
        context.appSettingsDataStore.edit { preferences ->
            preferences[voiceKey] = sanitized.voice
            preferences[targetLanguageCodeKey] = sanitized.targetLanguageCode
            preferences[paceKey] = sanitized.pace
            preferences[speechSampleRateKey] = sanitized.speechSampleRate
            preferences[temperatureKey] = sanitized.temperature
        }
    }

    suspend fun updateSarvamApiKey(apiKey: String?) {
        context.appSettingsDataStore.edit { preferences ->
            val normalized = apiKey?.trim().orEmpty()
            if (normalized.isBlank()) {
                preferences.remove(encryptedSarvamApiKey)
            } else {
                preferences[encryptedSarvamApiKey] = cryptoManager.encrypt(normalized)
            }
        }
    }

    suspend fun getSarvamApiKey(): String? {
        val encrypted = context.appSettingsDataStore.data.first()[encryptedSarvamApiKey].orEmpty()
        if (encrypted.isBlank()) return null
        return runCatching { cryptoManager.decrypt(encrypted) }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    }

    suspend fun updateSessionControlDefaults(model: String?, effort: String?) {
        val normalizedModel = model?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedEffort = effort?.trim()?.takeIf { it.isNotEmpty() }
        context.appSettingsDataStore.edit { preferences ->
            if (normalizedModel == null) preferences.remove(lastSessionModelKey)
            else preferences[lastSessionModelKey] = normalizedModel

            if (normalizedEffort == null) preferences.remove(lastSessionEffortKey)
            else preferences[lastSessionEffortKey] = normalizedEffort
        }
    }

    suspend fun getSessionControlDefaults(): SessionControlDefaults {
        val preferences = context.appSettingsDataStore.data.first()
        return SessionControlDefaults(
            model = preferences[lastSessionModelKey]?.trim()?.takeIf { it.isNotEmpty() },
            effort = preferences[lastSessionEffortKey]?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    suspend fun getApprovalAllowRules(connectionId: String, workspaceKey: String): List<List<String>> {
        val normalizedWorkspace = normalizeWorkspaceKey(workspaceKey)
        val preferences = context.appSettingsDataStore.data.first()
        val parsed = parseApprovalAllowRules(preferences[approvalAllowRulesKey])
        val scopedKey = approvalScopeKey(connectionId, normalizedWorkspace)
        val defaultKey = approvalScopeKey(connectionId, DEFAULT_WORKSPACE_SCOPE)

        val scoped = parsed[scopedKey].orEmpty()
        if (scoped.isNotEmpty()) return scoped
        if (normalizedWorkspace != DEFAULT_WORKSPACE_SCOPE) {
            return parsed[defaultKey].orEmpty()
        }
        return emptyList()
    }

    suspend fun addApprovalAllowRule(connectionId: String, workspaceKey: String, command: List<String>) {
        val normalizedCommand = normalizeCommandTokens(command)
        if (normalizedCommand.isEmpty()) return

        val scope = approvalScopeKey(connectionId, normalizeWorkspaceKey(workspaceKey))
        context.appSettingsDataStore.edit { preferences ->
            val parsed = parseApprovalAllowRules(preferences[approvalAllowRulesKey]).toMutableMap()
            val existing = parsed[scope].orEmpty().toMutableList()
            val exists =
                existing.any { entry ->
                    entry.size == normalizedCommand.size && entry.indices.all { idx -> entry[idx] == normalizedCommand[idx] }
                }
            if (!exists) {
                existing.add(normalizedCommand)
                parsed[scope] = existing
                preferences[approvalAllowRulesKey] = encodeApprovalAllowRules(parsed)
            }
        }
    }

    private fun normalizeWorkspaceKey(value: String): String {
        return value.trim().takeIf { it.isNotEmpty() } ?: DEFAULT_WORKSPACE_SCOPE
    }

    private fun approvalScopeKey(connectionId: String, workspaceKey: String): String {
        return "${connectionId.trim()}|${workspaceKey.trim()}"
    }

    private fun normalizeCommandTokens(command: List<String>): List<String> {
        return command.map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun parseApprovalAllowRules(raw: String?): Map<String, List<List<String>>> {
        if (raw.isNullOrBlank()) return emptyMap()
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return emptyMap()
        return root.mapValues { (_, value) ->
            (value as? JsonArray)
                ?.jsonArray
                ?.mapNotNull { entry ->
                    val arr = entry as? JsonArray ?: return@mapNotNull null
                    arr.jsonArray
                        .mapNotNull { token ->
                            runCatching { token.jsonPrimitive.content.trim() }.getOrNull()?.takeIf { it.isNotEmpty() }
                        }
                        .takeIf { it.isNotEmpty() }
                }
                .orEmpty()
        }
    }

    private fun encodeApprovalAllowRules(rules: Map<String, List<List<String>>>): String {
        val payload =
            buildJsonObject {
                rules.forEach { (scope, prefixes) ->
                    put(
                        scope,
                        buildJsonArray {
                            prefixes.forEach { prefix ->
                                add(
                                    buildJsonArray {
                                        prefix.forEach { token -> add(JsonPrimitive(token)) }
                                    }
                                )
                            }
                        },
                    )
                }
            }
        return payload.toString()
    }

    private companion object {
        private const val DEFAULT_WORKSPACE_SCOPE = "__default__"
    }
}
