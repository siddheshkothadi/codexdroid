package me.siddheshkothadi.codexdroid.domain.usecase

import kotlinx.serialization.json.JsonElement
import me.siddheshkothadi.codexdroid.domain.repository.CodexSessionRepository
import me.siddheshkothadi.codexdroid.codex.CodexResponse
import javax.inject.Inject

class ListSkillsUseCase @Inject constructor(
    private val codexSessionRepository: CodexSessionRepository,
) {
    suspend operator fun invoke(
        baseUrl: String,
        secret: String?,
        cwd: String?,
    ): CodexResponse<JsonElement> {
        return codexSessionRepository.listSkills(baseUrl, secret, cwd)
    }
}


