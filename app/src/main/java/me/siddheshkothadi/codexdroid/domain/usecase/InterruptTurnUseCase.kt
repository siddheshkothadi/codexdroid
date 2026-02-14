package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.codex.CodexApiService
import javax.inject.Inject

class InterruptTurnUseCase @Inject constructor(
    private val apiService: CodexApiService,
) {
    suspend operator fun invoke(
        baseUrl: String,
        secret: String?,
        threadId: String,
        turnId: String,
    ) {
        apiService.interruptTurn(baseUrl, secret, threadId, turnId)
    }
}
