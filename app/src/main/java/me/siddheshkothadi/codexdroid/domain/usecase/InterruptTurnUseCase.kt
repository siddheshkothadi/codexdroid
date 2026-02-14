package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.domain.repository.CodexSessionRepository
import javax.inject.Inject

class InterruptTurnUseCase @Inject constructor(
    private val codexSessionRepository: CodexSessionRepository,
) {
    suspend operator fun invoke(
        baseUrl: String,
        secret: String?,
        threadId: String,
        turnId: String,
    ) {
        codexSessionRepository.interruptTurn(baseUrl, secret, threadId, turnId)
    }
}


