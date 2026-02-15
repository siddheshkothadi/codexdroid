package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.domain.repository.CodexSessionRepository
import me.siddheshkothadi.codexdroid.codex.CodexResponse
import me.siddheshkothadi.codexdroid.codex.TurnStartResult
import kotlinx.serialization.json.JsonElement
import javax.inject.Inject

class StartTurnUseCase @Inject constructor(
    private val codexSessionRepository: CodexSessionRepository,
) {
    suspend operator fun invoke(
        baseUrl: String,
        secret: String?,
        threadId: String,
        text: String,
        cwd: String?,
        model: String?,
        effort: String?,
        collaborationMode: JsonElement?,
    ): CodexResponse<TurnStartResult> {
        return codexSessionRepository.startTurn(
            baseUrl = baseUrl,
            secret = secret,
            threadId = threadId,
            text = text,
            cwd = cwd,
            model = model,
            effort = effort,
            collaborationMode = collaborationMode,
        )
    }
}


