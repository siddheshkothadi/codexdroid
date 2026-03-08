package me.siddheshkothadi.codexdroid.data.source.remote

import kotlinx.serialization.json.JsonElement
import me.siddheshkothadi.codexdroid.codex.CodexApiService
import me.siddheshkothadi.codexdroid.codex.CodexResponse
import me.siddheshkothadi.codexdroid.codex.EmptyResult
import me.siddheshkothadi.codexdroid.codex.ThreadReadResult
import me.siddheshkothadi.codexdroid.codex.ThreadResumeResult
import me.siddheshkothadi.codexdroid.codex.ThreadStartResult
import me.siddheshkothadi.codexdroid.codex.TurnSteerResult
import me.siddheshkothadi.codexdroid.codex.TurnStartResult
import me.siddheshkothadi.codexdroid.domain.model.Connection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CodexSessionRemoteDataSource @Inject constructor(
    private val apiService: CodexApiService,
) {
    suspend fun ping(connection: Connection): Boolean {
        return apiService.ping(connection.baseUrl, connection.secret)
    }

    suspend fun startThread(
        baseUrl: String,
        secret: String?,
        cwd: String?,
    ): CodexResponse<ThreadStartResult> {
        return apiService.startThread(baseUrl, secret, cwd)
    }

    suspend fun resumeThread(
        baseUrl: String,
        secret: String?,
        threadId: String,
    ): CodexResponse<ThreadResumeResult> {
        return apiService.resumeThread(baseUrl, secret, threadId)
    }

    suspend fun startTurn(
        baseUrl: String,
        secret: String?,
        threadId: String,
        text: String,
        cwd: String?,
        model: String?,
        effort: String?,
        collaborationMode: JsonElement?,
    ): CodexResponse<TurnStartResult> {
        return apiService.startTurn(baseUrl, secret, threadId, text, cwd, model, effort, collaborationMode)
    }

    suspend fun steerTurn(
        baseUrl: String,
        secret: String?,
        threadId: String,
        turnId: String,
        text: String,
    ): CodexResponse<TurnSteerResult> {
        return apiService.steerTurn(baseUrl, secret, threadId, turnId, text)
    }

    suspend fun readThread(
        baseUrl: String,
        secret: String?,
        threadId: String,
    ): CodexResponse<ThreadReadResult> {
        return apiService.readThread(baseUrl, secret, threadId)
    }

    suspend fun listModels(baseUrl: String, secret: String?): CodexResponse<JsonElement> {
        return apiService.listModels(baseUrl, secret)
    }

    suspend fun listCollaborationModes(baseUrl: String, secret: String?): CodexResponse<JsonElement> {
        return apiService.listCollaborationModes(baseUrl, secret)
    }

    suspend fun listExperimentalFeatures(
        baseUrl: String,
        secret: String?,
        cursor: String?,
        limit: Int?,
    ): CodexResponse<JsonElement> {
        return apiService.listExperimentalFeatures(
            baseUrl = baseUrl,
            secret = secret,
            cursor = cursor,
            limit = limit,
        )
    }

    suspend fun listSkills(baseUrl: String, secret: String?, cwd: String?): CodexResponse<JsonElement> {
        return apiService.listSkills(baseUrl, secret, cwd)
    }

    suspend fun readConfig(baseUrl: String, secret: String?): CodexResponse<JsonElement> {
        return apiService.readConfig(baseUrl, secret)
    }

    suspend fun writeConfigValue(
        baseUrl: String,
        secret: String?,
        key: String,
        value: JsonElement,
    ): CodexResponse<JsonElement> {
        return apiService.writeConfigValue(baseUrl, secret, key, value)
    }

    suspend fun interruptTurn(
        baseUrl: String,
        secret: String?,
        threadId: String,
        turnId: String,
    ): CodexResponse<EmptyResult> {
        return apiService.interruptTurn(baseUrl, secret, threadId, turnId)
    }

    suspend fun respondToApprovalRequest(baseUrl: String, secret: String?, requestId: Long, decision: String) {
        apiService.respondToApprovalRequest(baseUrl, secret, requestId, decision)
    }

    suspend fun respondToUserInputRequest(
        baseUrl: String,
        secret: String?,
        requestId: Long,
        answers: Map<String, List<String>>,
    ) {
        apiService.respondToUserInputRequest(baseUrl, secret, requestId, answers)
    }
}
