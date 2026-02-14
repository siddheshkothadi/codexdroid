package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.domain.repository.CodexSessionRepository
import javax.inject.Inject

class RespondToApprovalRequestUseCase @Inject constructor(
    private val codexSessionRepository: CodexSessionRepository,
) {
    suspend operator fun invoke(
        baseUrl: String,
        secret: String?,
        requestId: Long,
        decision: String,
    ) {
        codexSessionRepository.respondToApprovalRequest(baseUrl, secret, requestId, decision)
    }
}


