package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.domain.model.SessionControlDefaults
import me.siddheshkothadi.codexdroid.domain.repository.SessionPreferencesRepository
import javax.inject.Inject

class GetSessionControlDefaultsUseCase @Inject constructor(
    private val repository: SessionPreferencesRepository,
) {
    suspend operator fun invoke(): SessionControlDefaults {
        return repository.getSessionControlDefaults()
    }
}
