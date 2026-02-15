package me.siddheshkothadi.codexdroid.domain.usecase

import kotlinx.serialization.json.JsonElement
import me.siddheshkothadi.codexdroid.codex.CodexResponse
import me.siddheshkothadi.codexdroid.domain.repository.CodexSessionRepository
import javax.inject.Inject

class ListExperimentalFeaturesUseCase @Inject constructor(
    private val codexSessionRepository: CodexSessionRepository,
) {
    suspend operator fun invoke(
        baseUrl: String,
        secret: String?,
        cursor: String?,
        limit: Int?,
    ): CodexResponse<JsonElement> {
        return codexSessionRepository.listExperimentalFeatures(
            baseUrl = baseUrl,
            secret = secret,
            cursor = cursor,
            limit = limit,
        )
    }
}
