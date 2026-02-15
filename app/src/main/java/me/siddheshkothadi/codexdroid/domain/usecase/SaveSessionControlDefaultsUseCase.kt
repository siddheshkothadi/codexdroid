package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.domain.repository.SessionPreferencesRepository
import javax.inject.Inject

class SaveSessionControlDefaultsUseCase @Inject constructor(
    private val repository: SessionPreferencesRepository,
) {
    suspend operator fun invoke(model: String?, effort: String?) {
        repository.saveSessionControlDefaults(model = model, effort = effort)
    }
}
