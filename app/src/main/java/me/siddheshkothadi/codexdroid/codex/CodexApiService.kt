package me.siddheshkothadi.codexdroid.codex

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that abstracts specific Codex RPC methods.
 */
@Singleton
class CodexApiService @Inject constructor(
    private val clientManager: CodexClientManager
) {

    suspend fun ping(baseUrl: String, secret: String?): Boolean {
        val client = clientManager.get(baseUrl, secret)
        val resp = client.send<ThreadListParams, ThreadListResult>(
            CodexRequest("thread/list", params = ThreadListParams(limit = 1))
        )
        return resp.error == null
    }

    suspend fun listThreads(baseUrl: String, secret: String?): CodexResponse<ThreadListResult> {
        val client = clientManager.get(baseUrl, secret)
        return client.send<ThreadListParams, ThreadListResult>(
            CodexRequest("thread/list", params = ThreadListParams(limit = 50))
        )
    }

    suspend fun readThread(baseUrl: String, secret: String?, threadId: String): CodexResponse<ThreadReadResult> {
        val client = clientManager.get(baseUrl, secret)
        return client.send<ThreadReadParams, ThreadReadResult>(
            CodexRequest("thread/read", params = ThreadReadParams(threadId))
        )
    }

    suspend fun startThread(
        baseUrl: String,
        secret: String?,
        cwd: String? = null
    ): CodexResponse<ThreadStartResult> {
        val client = clientManager.get(baseUrl, secret)
        return client.send<ThreadStartParams, ThreadStartResult>(
            CodexRequest(
                "thread/start",
                params = ThreadStartParams(
                    cwd = cwd?.takeIf { it.isNotBlank() },
                    source = SessionSource.appServer
                )
            )
        )
    }

    suspend fun resumeThread(baseUrl: String, secret: String?, threadId: String): CodexResponse<ThreadResumeResult> {
        val client = clientManager.get(baseUrl, secret)
        return client.send<ThreadResumeParams, ThreadResumeResult>(
            CodexRequest("thread/resume", params = ThreadResumeParams(threadId))
        )
    }

    suspend fun archiveThread(baseUrl: String, secret: String?, threadId: String): CodexResponse<EmptyResult> {
        val client = clientManager.get(baseUrl, secret)
        return client.send<ThreadArchiveParams, EmptyResult>(
            CodexRequest("thread/archive", params = ThreadArchiveParams(threadId))
        )
    }

    suspend fun setThreadName(baseUrl: String, secret: String?, threadId: String, name: String): CodexResponse<JsonElement> {
        val client = clientManager.get(baseUrl, secret)
        return client.send<ThreadNameSetParams, JsonElement>(
            CodexRequest("thread/name/set", params = ThreadNameSetParams(threadId = threadId, name = name))
        )
    }

    suspend fun startTurn(baseUrl: String, secret: String?, threadId: String, text: String): CodexResponse<TurnStartResult> {
        val client = clientManager.get(baseUrl, secret)
        val params = TurnStartParams(threadId = threadId, input = listOf(UserInput(text = text)))
        return client.send<TurnStartParams, TurnStartResult>(
            CodexRequest("turn/start", params = params)
        )
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
        val client = clientManager.get(baseUrl, secret)
        val params =
            TurnStartParams(
                threadId = threadId,
                input = listOf(UserInput(text = text)),
                cwd = cwd?.takeIf { it.isNotBlank() },
                model = model?.takeIf { it.isNotBlank() },
                effort = effort?.takeIf { it.isNotBlank() },
                collaborationMode = collaborationMode,
            )
        return client.send<TurnStartParams, TurnStartResult>(
            CodexRequest("turn/start", params = params)
        )
    }

    suspend fun listModels(baseUrl: String, secret: String?): CodexResponse<JsonElement> {
        val client = clientManager.get(baseUrl, secret)
        return client.send<EmptyParams, JsonElement>(CodexRequest("model/list", params = EmptyParams))
    }

    suspend fun listExperimentalFeatures(
        baseUrl: String,
        secret: String?,
        cursor: String?,
        limit: Int?,
    ): CodexResponse<JsonElement> {
        val client = clientManager.get(baseUrl, secret)
        val params =
            buildJsonObject {
                cursor?.let { put("cursor", it) }
                limit?.let { put("limit", it) }
            }
        return client.send<JsonObject, JsonElement>(
            CodexRequest("experimentalFeature/list", params = params),
        )
    }

    suspend fun readConfig(baseUrl: String, secret: String?): CodexResponse<JsonElement> {
        val client = clientManager.get(baseUrl, secret)
        return client.send<EmptyParams, JsonElement>(CodexRequest("config/read", params = EmptyParams))
    }

    suspend fun writeConfigValue(
        baseUrl: String,
        secret: String?,
        key: String,
        value: JsonElement,
    ): CodexResponse<JsonElement> {
        val client = clientManager.get(baseUrl, secret)
        val params =
            buildJsonObject {
                put("key", key)
                put("value", value)
            }
        return client.send<JsonObject, JsonElement>(
            CodexRequest("config/value/write", params = params),
        )
    }

    suspend fun listSkills(baseUrl: String, secret: String?, cwd: String?): CodexResponse<JsonElement> {
        val client = clientManager.get(baseUrl, secret)
        val normalizedCwd = cwd?.takeIf { it.isNotBlank() }
        val params =
            buildJsonObject {
                normalizedCwd?.let {
                    // `cwds` is the canonical parameter; include legacy `cwd` for older bridges.
                    put("cwds", buildJsonArray { add(JsonPrimitive(it)) })
                    put("cwd", it)
                }
            }
        return client.send<JsonObject, JsonElement>(CodexRequest("skills/list", params = params))
    }

    suspend fun interruptTurn(baseUrl: String, secret: String?, threadId: String, turnId: String): CodexResponse<EmptyResult> {
        val client = clientManager.get(baseUrl, secret)
        val params = TurnInterruptParams(threadId = threadId, turnId = turnId)
        return client.send<TurnInterruptParams, EmptyResult>(
            CodexRequest("turn/interrupt", params = params)
        )
    }

    suspend fun respondToApprovalRequest(
        baseUrl: String,
        secret: String?,
        requestId: Long,
        decision: String, // "accept" | "decline"
    ) {
        val client = clientManager.get(baseUrl, secret)
        val result = buildJsonObject { put("decision", decision) }
        client.sendResponse(requestId, result)
    }

    suspend fun respondToUserInputRequest(
        baseUrl: String,
        secret: String?,
        requestId: Long,
        answers: Map<String, List<String>>,
    ) {
        val client = clientManager.get(baseUrl, secret)
        val result =
            buildJsonObject {
                put(
                    "answers",
                    buildJsonObject {
                        answers.forEach { (questionId, selectedAnswers) ->
                            put(
                                questionId,
                                buildJsonObject {
                                    put(
                                        "answers",
                                        kotlinx.serialization.json.JsonArray(
                                            selectedAnswers.map { kotlinx.serialization.json.JsonPrimitive(it) }
                                        )
                                    )
                                }
                            )
                        }
                    }
                )
            }
        client.sendResponse(requestId, result)
    }
}
