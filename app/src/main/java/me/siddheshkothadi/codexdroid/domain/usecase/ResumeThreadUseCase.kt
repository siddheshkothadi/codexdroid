package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.codex.CodexApiService
import me.siddheshkothadi.codexdroid.codex.CodexResponse
import me.siddheshkothadi.codexdroid.codex.ThreadResumeResult
import javax.inject.Inject

class ResumeThreadUseCase @Inject constructor(
    private val apiService: CodexApiService,
) {
    suspend operator fun invoke(
        baseUrl: String,
        secret: String?,
        threadId: String,
    ): CodexResponse<ThreadResumeResult> {
        return apiService.resumeThread(baseUrl, secret, threadId)
    }
}
