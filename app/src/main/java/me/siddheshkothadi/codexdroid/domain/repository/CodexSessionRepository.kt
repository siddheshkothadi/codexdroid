package me.siddheshkothadi.codexdroid.domain.repository

import kotlinx.serialization.json.JsonElement
import me.siddheshkothadi.codexdroid.codex.CodexResponse
import me.siddheshkothadi.codexdroid.codex.EmptyResult
import me.siddheshkothadi.codexdroid.codex.ThreadReadResult
import me.siddheshkothadi.codexdroid.codex.ThreadResumeResult
import me.siddheshkothadi.codexdroid.codex.ThreadStartResult
import me.siddheshkothadi.codexdroid.codex.TurnStartResult
import me.siddheshkothadi.codexdroid.domain.model.Connection

interface CodexSessionRepository {
    suspend fun ping(connection: Connection): Boolean

    suspend fun startThread(baseUrl: String, secret: String?, cwd: String?): CodexResponse<ThreadStartResult>

    suspend fun resumeThread(baseUrl: String, secret: String?, threadId: String): CodexResponse<ThreadResumeResult>

    suspend fun startTurn(
        baseUrl: String,
        secret: String?,
        threadId: String,
        text: String,
        cwd: String?,
        model: String?,
        effort: String?,
        collaborationMode: JsonElement?,
    ): CodexResponse<TurnStartResult>

    suspend fun readThread(baseUrl: String, secret: String?, threadId: String): CodexResponse<ThreadReadResult>

    suspend fun listModels(baseUrl: String, secret: String?): CodexResponse<JsonElement>

    suspend fun listSkills(baseUrl: String, secret: String?, cwd: String?): CodexResponse<JsonElement>

    suspend fun readConfig(baseUrl: String, secret: String?): CodexResponse<JsonElement>

    suspend fun interruptTurn(
        baseUrl: String,
        secret: String?,
        threadId: String,
        turnId: String,
    ): CodexResponse<EmptyResult>

    suspend fun respondToApprovalRequest(baseUrl: String, secret: String?, requestId: Long, decision: String)

    suspend fun respondToUserInputRequest(
        baseUrl: String,
        secret: String?,
        requestId: Long,
        answers: Map<String, List<String>>,
    )
}
