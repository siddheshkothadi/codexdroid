package me.siddheshkothadi.codexdroid.domain.usecase

import kotlinx.coroutines.flow.first
import me.siddheshkothadi.codexdroid.domain.model.SarvamSynthesisResult
import me.siddheshkothadi.codexdroid.domain.repository.SpeechSettingsRepository
import me.siddheshkothadi.codexdroid.domain.repository.SpeechSynthesisRepository
import javax.inject.Inject

class MissingSarvamApiKeyException : IllegalStateException("Sarvam API key is missing.")

class SynthesizeSarvamSpeechUseCase @Inject constructor(
    private val speechSettingsRepository: SpeechSettingsRepository,
    private val speechSynthesisRepository: SpeechSynthesisRepository,
) {
    suspend operator fun invoke(text: String): SarvamSynthesisResult {
        val normalized = text.trim()
        require(normalized.isNotBlank()) { "Text cannot be blank for speech synthesis." }

        val apiKey = speechSettingsRepository.getSarvamApiKey()?.trim()
        if (apiKey.isNullOrBlank()) throw MissingSarvamApiKeyException()

        val settings = speechSettingsRepository.observeSarvamTtsSettings().first()
        return speechSynthesisRepository.synthesizeWithSarvam(normalized, settings, apiKey)
    }
}

