package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.codex.CodexApiService
import me.siddheshkothadi.codexdroid.codex.CodexResponse
import me.siddheshkothadi.codexdroid.codex.ThreadReadResult
import javax.inject.Inject

class ReadThreadUseCase @Inject constructor(
    private val apiService: CodexApiService,
) {
    suspend operator fun invoke(
        baseUrl: String,
        secret: String?,
        threadId: String,
    ): CodexResponse<ThreadReadResult> {
        return apiService.readThread(baseUrl, secret, threadId)
    }
}
