package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.domain.repository.SessionPreferencesRepository
import javax.inject.Inject

class AddApprovalAllowRuleUseCase @Inject constructor(
    private val repository: SessionPreferencesRepository,
) {
    suspend operator fun invoke(connectionId: String, workspaceKey: String, command: List<String>) {
        repository.addApprovalAllowRule(
            connectionId = connectionId,
            workspaceKey = workspaceKey,
            command = command,
        )
    }
}
