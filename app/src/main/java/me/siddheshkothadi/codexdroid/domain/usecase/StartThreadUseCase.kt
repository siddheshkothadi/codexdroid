package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.domain.repository.CodexSessionRepository
import me.siddheshkothadi.codexdroid.codex.CodexResponse
import me.siddheshkothadi.codexdroid.codex.ThreadStartResult
import javax.inject.Inject

class StartThreadUseCase @Inject constructor(
    private val codexSessionRepository: CodexSessionRepository,
) {
    suspend operator fun invoke(
        baseUrl: String,
        secret: String?,
        cwd: String? = null,
    ): CodexResponse<ThreadStartResult> {
        return codexSessionRepository.startThread(baseUrl, secret, cwd)
    }
}


