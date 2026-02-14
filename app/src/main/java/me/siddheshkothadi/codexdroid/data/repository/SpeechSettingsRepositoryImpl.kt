package me.siddheshkothadi.codexdroid.data.repository

import kotlinx.coroutines.flow.Flow
import me.siddheshkothadi.codexdroid.data.local.AppSettingsManager
import me.siddheshkothadi.codexdroid.domain.model.SarvamTtsSettings
import me.siddheshkothadi.codexdroid.domain.repository.SpeechSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechSettingsRepositoryImpl @Inject constructor(
    private val appSettingsManager: AppSettingsManager,
) : SpeechSettingsRepository {
    override fun observeSarvamTtsSettings(): Flow<SarvamTtsSettings> {
        return appSettingsManager.observeSarvamTtsSettings()
    }

    override suspend fun updateSarvamTtsSettings(settings: SarvamTtsSettings) {
        appSettingsManager.updateSarvamTtsSettings(settings)
    }

    override suspend fun updateSarvamApiKey(apiKey: String?) {
        appSettingsManager.updateSarvamApiKey(apiKey)
    }

    override suspend fun getSarvamApiKey(): String? {
        return appSettingsManager.getSarvamApiKey()
    }
}

