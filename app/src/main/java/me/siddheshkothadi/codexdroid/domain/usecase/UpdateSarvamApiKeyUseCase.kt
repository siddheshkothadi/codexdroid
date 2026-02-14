package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.domain.repository.SpeechSettingsRepository
import javax.inject.Inject

class UpdateSarvamApiKeyUseCase @Inject constructor(
    private val repository: SpeechSettingsRepository,
) {
    suspend operator fun invoke(apiKey: String?) {
        repository.updateSarvamApiKey(apiKey?.trim()?.takeIf { it.isNotEmpty() })
    }
}

