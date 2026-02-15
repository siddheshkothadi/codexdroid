package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.domain.repository.SessionPreferencesRepository
import javax.inject.Inject

class GetApprovalAllowRulesUseCase @Inject constructor(
    private val repository: SessionPreferencesRepository,
) {
    suspend operator fun invoke(connectionId: String, workspaceKey: String): List<List<String>> {
        return repository.getApprovalAllowRules(connectionId = connectionId, workspaceKey = workspaceKey)
    }
}
