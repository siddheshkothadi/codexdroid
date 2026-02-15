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
    val isOther: Boolean = false,
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
        val threadId = readString(params, "threadId", "thread_id").takeIf { it.isNotBlank() }
        val turnId = readString(params, "turnId", "turn_id").takeIf { it.isNotBlank() }
        val itemId = readString(params, "itemId", "item_id").takeIf { it.isNotBlank() }
        val questionsRaw = params["questions"] as? JsonArray
        val questions =
            questionsRaw
                ?.jsonArray
                ?.mapNotNull { entry ->
                    val obj = entry as? JsonObject ?: return@mapNotNull null
                    val id = readString(obj, "id")
                    if (id.isBlank()) return@mapNotNull null
                    val header = readString(obj, "header")
                    val question = readString(obj, "question")
                    val optionsRaw = obj["options"] as? JsonArray
                    val options =
                        optionsRaw
                            ?.jsonArray
                            ?.mapNotNull { opt ->
                                val o = opt as? JsonObject ?: return@mapNotNull null
                                val label = readString(o, "label")
                                val description = readString(o, "description")
                                if (label.isBlank() && description.isBlank()) return@mapNotNull null
                                UserInputOption(label = label, description = description)
                            }
                            .orEmpty()
                    val isOther = readBoolean(obj, "isOther", "is_other")
                    UserInputQuestion(
                        id = id,
                        header = header,
                        question = question,
                        isOther = isOther,
                        options = options,
                    )
                }
                .orEmpty()

        if (questions.isEmpty()) return null

        return UserInputPendingRequest(
            requestId = request.id,
            threadId = threadId,
            turnId = turnId,
            itemId = itemId,
            questions = questions,
        )
    }

    private fun readString(obj: JsonObject, vararg keys: String): String {
        return keys.firstNotNullOfOrNull { key ->
            runCatching { obj[key]?.jsonPrimitive?.content?.trim() }.getOrNull()?.takeIf { it.isNotEmpty() }
        }.orEmpty()
    }

    private fun readBoolean(obj: JsonObject, vararg keys: String): Boolean {
        keys.forEach { key ->
            val value = runCatching { obj[key]?.jsonPrimitive?.content?.trim()?.lowercase() }.getOrNull() ?: return@forEach
            when (value) {
                "true" -> return true
                "false" -> return false
                "1" -> return true
                "0" -> return false
            }
        }
        return false
    }
}
