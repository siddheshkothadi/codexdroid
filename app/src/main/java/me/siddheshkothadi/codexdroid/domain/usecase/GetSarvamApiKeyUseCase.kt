package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.domain.repository.SpeechSettingsRepository
import javax.inject.Inject

class GetSarvamApiKeyUseCase @Inject constructor(
    private val repository: SpeechSettingsRepository,
) {
    suspend operator fun invoke(): String? = repository.getSarvamApiKey()
}

