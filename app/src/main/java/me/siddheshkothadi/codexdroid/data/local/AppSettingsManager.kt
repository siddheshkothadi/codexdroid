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
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager,
) {
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
}

