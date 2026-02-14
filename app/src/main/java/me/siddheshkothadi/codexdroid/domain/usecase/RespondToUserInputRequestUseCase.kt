package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.codex.CodexApiService
import javax.inject.Inject

class RespondToUserInputRequestUseCase @Inject constructor(
    private val apiService: CodexApiService,
) {
    suspend operator fun invoke(
        baseUrl: String,
        secret: String?,
        requestId: Long,
        answers: Map<String, List<String>>,
    ) {
        apiService.respondToUserInputRequest(baseUrl, secret, requestId, answers)
    }
}
