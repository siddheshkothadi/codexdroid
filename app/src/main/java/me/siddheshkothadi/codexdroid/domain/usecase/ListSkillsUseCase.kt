package me.siddheshkothadi.codexdroid.domain.usecase

import kotlinx.serialization.json.JsonElement
import me.siddheshkothadi.codexdroid.codex.CodexApiService
import me.siddheshkothadi.codexdroid.codex.CodexResponse
import javax.inject.Inject

class ListSkillsUseCase @Inject constructor(
    private val apiService: CodexApiService,
) {
    suspend operator fun invoke(
        baseUrl: String,
        secret: String?,
        cwd: String?,
    ): CodexResponse<JsonElement> {
        return apiService.listSkills(baseUrl, secret, cwd)
    }
}
