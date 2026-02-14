package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.domain.repository.CodexSessionRepository
import me.siddheshkothadi.codexdroid.codex.CodexResponse
import me.siddheshkothadi.codexdroid.codex.ThreadReadResult
import javax.inject.Inject

class ReadThreadUseCase @Inject constructor(
    private val codexSessionRepository: CodexSessionRepository,
) {
    suspend operator fun invoke(
        baseUrl: String,
        secret: String?,
        threadId: String,
    ): CodexResponse<ThreadReadResult> {
        return codexSessionRepository.readThread(baseUrl, secret, threadId)
    }
}


