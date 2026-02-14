package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.codex.CodexApiService
import javax.inject.Inject

class RespondToApprovalRequestUseCase @Inject constructor(
    private val apiService: CodexApiService,
) {
    suspend operator fun invoke(
        baseUrl: String,
        secret: String?,
        requestId: Long,
        decision: String,
    ) {
        apiService.respondToApprovalRequest(baseUrl, secret, requestId, decision)
    }
}
