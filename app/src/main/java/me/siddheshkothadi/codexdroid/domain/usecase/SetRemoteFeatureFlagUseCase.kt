package me.siddheshkothadi.codexdroid.domain.usecase

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import me.siddheshkothadi.codexdroid.codex.CodexResponse
import me.siddheshkothadi.codexdroid.domain.repository.CodexSessionRepository
import javax.inject.Inject

class SetRemoteFeatureFlagUseCase @Inject constructor(
    private val codexSessionRepository: CodexSessionRepository,
) {
    suspend operator fun invoke(
        baseUrl: String,
        secret: String?,
        featureKey: String,
        enabled: Boolean,
    ): CodexResponse<JsonElement> {
        return codexSessionRepository.writeConfigValue(
            baseUrl = baseUrl,
            secret = secret,
            key = "features.${featureKey.trim()}",
            value = JsonPrimitive(enabled),
        )
    }
}
