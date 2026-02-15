package me.siddheshkothadi.codexdroid.data.repository

import kotlinx.serialization.json.JsonElement
import me.siddheshkothadi.codexdroid.codex.CodexResponse
import me.siddheshkothadi.codexdroid.codex.EmptyResult
import me.siddheshkothadi.codexdroid.codex.ThreadReadResult
import me.siddheshkothadi.codexdroid.codex.ThreadResumeResult
import me.siddheshkothadi.codexdroid.codex.ThreadStartResult
import me.siddheshkothadi.codexdroid.codex.TurnStartResult
import me.siddheshkothadi.codexdroid.data.source.remote.CodexSessionRemoteDataSource
import me.siddheshkothadi.codexdroid.domain.model.Connection
import me.siddheshkothadi.codexdroid.domain.repository.CodexSessionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CodexSessionRepositoryImpl @Inject constructor(
    private val remoteDataSource: CodexSessionRemoteDataSource,
) : CodexSessionRepository {
    override suspend fun ping(connection: Connection): Boolean {
        return runCatching { remoteDataSource.ping(connection) }.getOrDefault(false)
    }

    override suspend fun startThread(baseUrl: String, secret: String?, cwd: String?): CodexResponse<ThreadStartResult> {
        return remoteDataSource.startThread(baseUrl, secret, cwd)
    }

    override suspend fun resumeThread(
        baseUrl: String,
        secret: String?,
        threadId: String,
    ): CodexResponse<ThreadResumeResult> {
        return remoteDataSource.resumeThread(baseUrl, secret, threadId)
    }

    override suspend fun startTurn(
        baseUrl: String,
        secret: String?,
        threadId: String,
        text: String,
        cwd: String?,
        model: String?,
        effort: String?,
        collaborationMode: JsonElement?,
    ): CodexResponse<TurnStartResult> {
        return remoteDataSource.startTurn(baseUrl, secret, threadId, text, cwd, model, effort, collaborationMode)
    }

    override suspend fun readThread(baseUrl: String, secret: String?, threadId: String): CodexResponse<ThreadReadResult> {
        return remoteDataSource.readThread(baseUrl, secret, threadId)
    }

    override suspend fun listModels(baseUrl: String, secret: String?): CodexResponse<JsonElement> {
        return remoteDataSource.listModels(baseUrl, secret)
    }

    override suspend fun listExperimentalFeatures(
        baseUrl: String,
        secret: String?,
        cursor: String?,
        limit: Int?,
    ): CodexResponse<JsonElement> {
        return remoteDataSource.listExperimentalFeatures(
            baseUrl = baseUrl,
            secret = secret,
            cursor = cursor,
            limit = limit,
        )
    }

    override suspend fun listSkills(baseUrl: String, secret: String?, cwd: String?): CodexResponse<JsonElement> {
        return remoteDataSource.listSkills(baseUrl, secret, cwd)
    }

    override suspend fun readConfig(baseUrl: String, secret: String?): CodexResponse<JsonElement> {
        return remoteDataSource.readConfig(baseUrl, secret)
    }

    override suspend fun writeConfigValue(
        baseUrl: String,
        secret: String?,
        key: String,
        value: JsonElement,
    ): CodexResponse<JsonElement> {
        return remoteDataSource.writeConfigValue(
            baseUrl = baseUrl,
            secret = secret,
            key = key,
            value = value,
        )
    }

    override suspend fun interruptTurn(
        baseUrl: String,
        secret: String?,
        threadId: String,
        turnId: String,
    ): CodexResponse<EmptyResult> {
        return remoteDataSource.interruptTurn(baseUrl, secret, threadId, turnId)
    }

    override suspend fun respondToApprovalRequest(baseUrl: String, secret: String?, requestId: Long, decision: String) {
        remoteDataSource.respondToApprovalRequest(baseUrl, secret, requestId, decision)
    }

    override suspend fun respondToUserInputRequest(
        baseUrl: String,
        secret: String?,
        requestId: Long,
        answers: Map<String, List<String>>,
    ) {
        remoteDataSource.respondToUserInputRequest(baseUrl, secret, requestId, answers)
    }
}
