package me.siddheshkothadi.codexdroid.feature.session.ui

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray

object SessionControlsParser {
    fun parseModels(result: JsonElement): List<ModelOptionUi> {
        val root = result as? JsonObject
        val items =
            when {
                root != null -> {
                    val data = root["data"] ?: root["models"] ?: root["items"]
                    (data as? JsonArray)?.jsonArray
                }
                result is JsonArray -> result.jsonArray
                else -> null
            } ?: return emptyList()

        return items.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val id = readString(obj, "id").ifBlank { readString(obj, "model") }
            if (id.isBlank()) return@mapNotNull null
            val model = readString(obj, "model").ifBlank { id }
            val displayName =
                readString(obj, "displayName", "display_name").ifBlank { model }
            val description = readString(obj, "description")
            val isDefault = readBoolean(obj, "isDefault", "is_default")

            val supportedEfforts = parseReasoningEfforts(obj)

            val defaultEffort =
                readDefaultEffort(obj)
                    .takeIf { it.isNotBlank() }

            ModelOptionUi(
                id = id,
                model = model,
                displayName = displayName,
                description = description,
                supportedReasoningEfforts = supportedEfforts,
                defaultReasoningEffort = defaultEffort,
                isDefault = isDefault,
            )
        }.distinctBy { it.id }
    }

    fun parseSkills(result: JsonElement): List<SkillOptionUi> {
        val skillObjects = mutableListOf<JsonObject>()

        fun collectSkillObjects(array: JsonArray) {
            array.forEach { element ->
                val obj = element as? JsonObject ?: return@forEach
                val nestedSkills = obj["skills"] as? JsonArray
                if (nestedSkills != null) {
                    nestedSkills.forEach { nested ->
                        val nestedObj = nested as? JsonObject ?: return@forEach
                        skillObjects.add(nestedObj)
                    }
                    return@forEach
                }
                skillObjects.add(obj)
            }
        }

        when (result) {
            is JsonArray -> collectSkillObjects(result)
            is JsonObject -> {
                listOf(result["skills"], result["items"], result["data"]).forEach { candidate ->
                    val arr = candidate as? JsonArray ?: return@forEach
                    collectSkillObjects(arr)
                }
            }
            else -> Unit
        }

        return skillObjects
            .mapNotNull { obj ->
                val name = readString(obj, "name")
                if (name.isBlank()) return@mapNotNull null
                SkillOptionUi(
                    name = name,
                    path = readString(obj, "path"),
                    description = readString(obj, "description").takeIf { it.isNotBlank() },
                )
            }
            .distinctBy { canonicalSkillKey(it) }
    }

    fun parseConfigPreferences(result: JsonElement): Pair<String?, String?> {
        val root = result as? JsonObject ?: return null to null
        val config =
            readObject(root, "config", "effectiveConfig", "effective_config", "data")
                ?: root

        val model =
            readString(config, "model")
                .takeIf { it.isNotBlank() }
        val effort =
            readString(config, "model_reasoning_effort", "modelReasoningEffort")
                .takeIf { it.isNotBlank() }
                ?: readObject(config, "model_reasoning_effort", "modelReasoningEffort")
                    ?.let { effortObj ->
                        readString(effortObj, "effort", "reasoningEffort", "reasoning_effort")
                            .takeIf { it.isNotBlank() }
                    }

        return model to effort
    }

    fun resolveModelId(models: List<ModelOptionUi>, candidate: String?): String? {
        val target = candidate?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return models.firstOrNull { it.id == target || it.model == target }?.id
    }

    private fun parseReasoningEfforts(model: JsonObject): List<ReasoningEffortUi> {
        val arrays =
            listOf(
                model["supportedReasoningEfforts"],
                model["supported_reasoning_efforts"],
                model["reasoningEffort"],
                model["reasoning_effort"],
            ).mapNotNull { it as? JsonArray }
        if (arrays.isEmpty()) return emptyList()

        return arrays
            .flatMap { arr ->
                arr.mapNotNull { entry ->
                    when (entry) {
                        is JsonObject -> {
                            val effort =
                                readString(entry, "reasoningEffort", "reasoning_effort", "effort")
                            if (effort.isBlank()) return@mapNotNull null
                            ReasoningEffortUi(
                                reasoningEffort = effort,
                                description = readString(entry, "description"),
                            )
                        }
                        is JsonPrimitive -> {
                            val effort = readPrimitiveString(entry)
                            if (effort.isBlank()) return@mapNotNull null
                            ReasoningEffortUi(reasoningEffort = effort)
                        }
                        else -> null
                    }
                }
            }
            .distinctBy { it.reasoningEffort.lowercase() }
    }

    private fun readDefaultEffort(model: JsonObject): String {
        val scalar =
            readString(
                model,
                "defaultReasoningEffort",
                "default_reasoning_effort",
                "defaultEffort",
                "default_effort",
            )
        if (scalar.isNotBlank()) return scalar

        val nested =
            (model["defaultReasoningEffort"] as? JsonObject)
                ?: (model["default_reasoning_effort"] as? JsonObject)
                ?: (model["defaultEffort"] as? JsonObject)
                ?: (model["default_effort"] as? JsonObject)
                ?: return ""
        return readString(nested, "reasoningEffort", "reasoning_effort", "effort")
    }

    private fun canonicalSkillKey(skill: SkillOptionUi): String {
        val normalizedName = skill.name.trim().lowercase()
        val normalizedPath = skill.path.trim().lowercase()
        return if (normalizedPath.isBlank()) normalizedName else "$normalizedName|$normalizedPath"
    }

    private fun readPrimitiveString(value: JsonElement?): String {
        val primitive = value as? JsonPrimitive ?: return ""
        return runCatching { primitive.content }.getOrNull()?.trim().orEmpty()
    }

    private fun readString(obj: JsonObject, vararg keys: String): String {
        for (k in keys) {
            val v = obj[k] as? JsonPrimitive ?: continue
            val s = runCatching { v.content }.getOrNull()?.trim().orEmpty()
            if (s.isNotBlank()) return s
        }
        return ""
    }

    private fun readObject(obj: JsonObject, vararg keys: String): JsonObject? {
        for (k in keys) {
            val nested = obj[k] as? JsonObject ?: continue
            return nested
        }
        return null
    }

    private fun readBoolean(obj: JsonObject, vararg keys: String): Boolean {
        for (k in keys) {
            val v = obj[k] as? JsonPrimitive ?: continue
            val s = runCatching { v.content }.getOrNull()?.trim()?.lowercase()
            if (s == "true") return true
            if (s == "false") return false
            val n = s?.toIntOrNull()
            if (n != null) return n != 0
        }
        return false
    }
}
