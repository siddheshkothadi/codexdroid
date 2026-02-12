package me.siddheshkothadi.codexdroid.codex.requests

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import me.siddheshkothadi.codexdroid.codex.ServerRequest

sealed interface PendingRequest {
    val requestId: Long
    val method: String
    val params: JsonElement?
}

data class ApprovalPendingRequest(
    override val requestId: Long,
    override val method: String,
    override val params: JsonElement? = null,
) : PendingRequest

data class UserInputPendingRequest(
    override val requestId: Long,
    val threadId: String? = null,
    val turnId: String? = null,
    val itemId: String? = null,
    val questions: List<UserInputQuestion> = emptyList(),
) : PendingRequest {
    override val method: String = "item/tool/requestUserInput"
    override val params: JsonElement? = null
}

data class UnknownPendingRequest(
    override val requestId: Long,
    override val method: String,
    override val params: JsonElement? = null,
) : PendingRequest

data class UserInputQuestion(
    val id: String,
    val header: String = "",
    val question: String = "",
    val options: List<UserInputOption> = emptyList(),
)

data class UserInputOption(
    val label: String,
    val description: String,
)

object PendingRequestParser {

    fun parse(request: ServerRequest): PendingRequest {
        return when {
            request.method.contains("requestApproval") -> {
                ApprovalPendingRequest(
                    requestId = request.id,
                    method = request.method,
                    params = request.params,
                )
            }
            request.method == "item/tool/requestUserInput" -> {
                parseUserInput(request) ?: UnknownPendingRequest(
                    requestId = request.id,
                    method = request.method,
                    params = request.params,
                )
            }
            else -> {
                UnknownPendingRequest(
                    requestId = request.id,
                    method = request.method,
                    params = request.params,
                )
            }
        }
    }

    private fun parseUserInput(request: ServerRequest): UserInputPendingRequest? {
        val params = request.params as? JsonObject ?: return null
        val threadId = params["threadId"]?.jsonPrimitive?.content
        val turnId = params["turnId"]?.jsonPrimitive?.content
        val itemId = params["itemId"]?.jsonPrimitive?.content
        val questionsRaw = params["questions"] as? JsonArray
        val questions =
            questionsRaw
                ?.jsonArray
                ?.mapNotNull { entry ->
                    val obj = entry as? JsonObject ?: return@mapNotNull null
                    val id = obj["id"]?.jsonPrimitive?.content?.trim().orEmpty()
                    if (id.isBlank()) return@mapNotNull null
                    val header = obj["header"]?.jsonPrimitive?.content?.trim().orEmpty()
                    val question = obj["question"]?.jsonPrimitive?.content?.trim().orEmpty()
                    val optionsRaw = obj["options"] as? JsonArray
                    val options =
                        optionsRaw
                            ?.jsonArray
                            ?.mapNotNull { opt ->
                                val o = opt as? JsonObject ?: return@mapNotNull null
                                val label = o["label"]?.jsonPrimitive?.content?.trim().orEmpty()
                                val description = o["description"]?.jsonPrimitive?.content?.trim().orEmpty()
                                if (label.isBlank() && description.isBlank()) return@mapNotNull null
                                UserInputOption(label = label, description = description)
                            }
                            .orEmpty()
                    UserInputQuestion(id = id, header = header, question = question, options = options)
                }
                .orEmpty()
                .filter { it.options.isNotEmpty() }

        if (questions.isEmpty()) return null

        return UserInputPendingRequest(
            requestId = request.id,
            threadId = threadId,
            turnId = turnId,
            itemId = itemId,
            questions = questions,
        )
    }
}
