package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.codex.CodexApiService
import me.siddheshkothadi.codexdroid.codex.CodexResponse
import me.siddheshkothadi.codexdroid.codex.ThreadStartResult
import javax.inject.Inject

class StartThreadUseCase @Inject constructor(
    private val apiService: CodexApiService,
) {
    suspend operator fun invoke(
        baseUrl: String,
        secret: String?,
        cwd: String? = null,
    ): CodexResponse<ThreadStartResult> {
        return apiService.startThread(baseUrl, secret, cwd)
    }
}
