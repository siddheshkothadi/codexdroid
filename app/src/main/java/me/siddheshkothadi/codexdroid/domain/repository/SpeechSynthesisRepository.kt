package me.siddheshkothadi.codexdroid.domain.repository

import me.siddheshkothadi.codexdroid.domain.model.SarvamSynthesisResult
import me.siddheshkothadi.codexdroid.domain.model.SarvamTtsSettings

interface SpeechSynthesisRepository {
    suspend fun synthesizeWithSarvam(
        text: String,
        settings: SarvamTtsSettings,
        apiKey: String,
    ): SarvamSynthesisResult
}

