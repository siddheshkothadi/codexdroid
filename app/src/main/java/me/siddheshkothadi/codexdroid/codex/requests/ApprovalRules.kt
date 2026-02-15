package me.siddheshkothadi.codexdroid.codex.requests

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val COMMAND_KEYS =
    setOf(
        "argv",
        "args",
        "command",
        "cmd",
        "exec",
        "shellCommand",
        "script",
        "proposedExecPolicyAmendment",
        "proposed_exec_policy_amendment",
    )

object ApprovalRules {
    fun extractCommandTokens(params: JsonElement?): List<String>? {
        val tokens = extractTokens(params) ?: return null
        val normalized = normalizeCommandTokens(tokens)
        return normalized.takeIf { it.isNotEmpty() }
    }

    fun normalizeCommandTokens(tokens: List<String>): List<String> {
        return tokens.map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun matchesCommandPrefix(command: List<String>, allowlist: List<List<String>>): Boolean {
        val normalized = normalizeCommandTokens(command)
        if (normalized.isEmpty()) return false
        return allowlist.any { prefix ->
            if (prefix.isEmpty() || prefix.size > normalized.size) return@any false
            prefix.indices.all { idx -> prefix[idx] == normalized[idx] }
        }
    }

    private fun extractTokens(value: JsonElement?): List<String>? {
        if (value == null) return null
        return when (value) {
            is JsonArray -> {
                val list = value.jsonArray.mapNotNull { token ->
                    (token as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
                }
                list.takeIf { it.isNotEmpty() }
            }

            is JsonPrimitive -> {
                val text = value.contentOrNull?.trim().orEmpty()
                if (text.isBlank()) null else splitCommandLine(text)
            }

            is JsonObject -> {
                val obj = value.jsonObject
                COMMAND_KEYS.firstNotNullOfOrNull { key ->
                    extractTokens(obj[key])
                } ?: obj.entries.firstNotNullOfOrNull { (key, nested) ->
                    val normalized = key.lowercase()
                    if (normalized.contains("execpolicy") || normalized.contains("exec_policy")) {
                        extractTokens(nested)
                    } else {
                        null
                    }
                }
            }
        }
    }

    private fun splitCommandLine(input: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var escaped = false

        input.forEach { ch ->
            if (escaped) {
                current.append(ch)
                escaped = false
                return@forEach
            }

            if (ch == '\\') {
                escaped = true
                return@forEach
            }

            if (quote != null) {
                if (ch == quote) quote = null else current.append(ch)
                return@forEach
            }

            if (ch == '"' || ch == '\'') {
                quote = ch
                return@forEach
            }

            if (ch.isWhitespace()) {
                if (current.isNotEmpty()) {
                    tokens.add(current.toString())
                    current.clear()
                }
                return@forEach
            }

            current.append(ch)
        }

        if (current.isNotEmpty()) {
            tokens.add(current.toString())
        }
        return tokens
    }

    private val JsonPrimitive.contentOrNull: String?
        get() = runCatching { jsonPrimitive.content }.getOrNull()
}
