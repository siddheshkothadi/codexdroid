package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.codex.CodexApiService
import me.siddheshkothadi.codexdroid.codex.CodexResponse
import me.siddheshkothadi.codexdroid.codex.TurnStartResult
import javax.inject.Inject

class StartTurnUseCase @Inject constructor(
    private val apiService: CodexApiService,
) {
    suspend operator fun invoke(
        baseUrl: String,
        secret: String?,
        threadId: String,
        text: String,
        cwd: String?,
        model: String?,
        effort: String?,
    ): CodexResponse<TurnStartResult> {
        return apiService.startTurn(
            baseUrl = baseUrl,
            secret = secret,
            threadId = threadId,
            text = text,
            cwd = cwd,
            model = model,
            effort = effort,
        )
    }
}
