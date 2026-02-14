package me.siddheshkothadi.codexdroid.domain.usecase

import kotlinx.coroutines.flow.Flow
import me.siddheshkothadi.codexdroid.domain.model.SarvamTtsSettings
import me.siddheshkothadi.codexdroid.domain.repository.SpeechSettingsRepository
import javax.inject.Inject

class ObserveSarvamTtsSettingsUseCase @Inject constructor(
    private val repository: SpeechSettingsRepository,
) {
    operator fun invoke(): Flow<SarvamTtsSettings> = repository.observeSarvamTtsSettings()
}

