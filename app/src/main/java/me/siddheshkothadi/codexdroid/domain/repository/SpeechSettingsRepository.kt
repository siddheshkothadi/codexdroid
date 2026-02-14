package me.siddheshkothadi.codexdroid.domain.repository

import kotlinx.coroutines.flow.Flow
import me.siddheshkothadi.codexdroid.domain.model.SarvamTtsSettings

interface SpeechSettingsRepository {
    fun observeSarvamTtsSettings(): Flow<SarvamTtsSettings>

    suspend fun updateSarvamTtsSettings(settings: SarvamTtsSettings)

    suspend fun updateSarvamApiKey(apiKey: String?)

    suspend fun getSarvamApiKey(): String?
}

