package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.domain.model.SarvamTtsSettings
import me.siddheshkothadi.codexdroid.domain.repository.SpeechSettingsRepository
import javax.inject.Inject

class UpdateSarvamTtsSettingsUseCase @Inject constructor(
    private val repository: SpeechSettingsRepository,
) {
    suspend operator fun invoke(settings: SarvamTtsSettings) {
        repository.updateSarvamTtsSettings(SarvamTtsSettings.sanitize(settings))
    }
}

