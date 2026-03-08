package me.siddheshkothadi.codexdroid.domain.usecase

import kotlinx.serialization.json.JsonElement
import me.siddheshkothadi.codexdroid.codex.CodexResponse
import me.siddheshkothadi.codexdroid.domain.repository.CodexSessionRepository
import javax.inject.Inject

class ListCollaborationModesUseCase @Inject constructor(
    private val codexSessionRepository: CodexSessionRepository,
) {
    suspend operator fun invoke(baseUrl: String, secret: String?): CodexResponse<JsonElement> {
        return codexSessionRepository.listCollaborationModes(baseUrl, secret)
    }
}
