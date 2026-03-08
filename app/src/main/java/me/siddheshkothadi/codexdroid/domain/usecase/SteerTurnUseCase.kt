package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.codex.CodexResponse
import me.siddheshkothadi.codexdroid.codex.TurnSteerResult
import me.siddheshkothadi.codexdroid.domain.repository.CodexSessionRepository
import javax.inject.Inject

class SteerTurnUseCase @Inject constructor(
    private val codexSessionRepository: CodexSessionRepository,
) {
    suspend operator fun invoke(
        baseUrl: String,
        secret: String?,
        threadId: String,
        turnId: String,
        text: String,
    ): CodexResponse<TurnSteerResult> {
        return codexSessionRepository.steerTurn(
            baseUrl = baseUrl,
            secret = secret,
            threadId = threadId,
            turnId = turnId,
            text = text,
        )
    }
}
