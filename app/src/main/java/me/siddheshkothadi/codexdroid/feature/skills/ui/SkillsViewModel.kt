package me.siddheshkothadi.codexdroid.feature.skills.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import me.siddheshkothadi.codexdroid.domain.model.Connection
import me.siddheshkothadi.codexdroid.domain.usecase.GetConnectionsUseCase
import me.siddheshkothadi.codexdroid.domain.usecase.ListSkillsUseCase

data class SkillsItemUi(
    val name: String,
    val description: String? = null,
    val path: String = "",
)

data class SkillsUiState(
    val isLoading: Boolean = true,
    val activeConnectionName: String? = null,
    val workspaceCwd: String? = null,
    val skills: List<SkillsItemUi> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class SkillsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getConnectionsUseCase: GetConnectionsUseCase,
    private val listSkillsUseCase: ListSkillsUseCase,
) : ViewModel() {
    private val workspaceCwd = savedStateHandle.get<String>("cwd")?.trim()?.takeIf { it.isNotBlank() }
    private var activeConnection: Connection? = null
    private val _uiState =
        MutableStateFlow(
            SkillsUiState(
                isLoading = true,
                workspaceCwd = workspaceCwd,
            )
        )
    val uiState: StateFlow<SkillsUiState> = _uiState.asStateFlow()

    init {
        observeActiveConnection()
    }

    fun refresh() {
        val connection = activeConnection ?: return
        viewModelScope.launch { loadSkills(connection) }
    }

    private fun observeActiveConnection() {
        viewModelScope.launch {
            getConnectionsUseCase().collectLatest { connections ->
                val connection = connections.firstOrNull()
                activeConnection = connection
                if (connection == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            activeConnectionName = null,
                            skills = emptyList(),
                            error = "No active connection.",
                        )
                    }
                    return@collectLatest
                }
                _uiState.update {
                    it.copy(
                        activeConnectionName = connection.name,
                    )
                }
                loadSkills(connection)
            }
        }
    }

    private suspend fun loadSkills(connection: Connection) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        val response =
            runCatching {
                listSkillsUseCase(
                    baseUrl = connection.baseUrl,
                    secret = connection.secret,
                    cwd = workspaceCwd,
                )
            }.getOrNull()
        val skills = response?.result?.let { SkillsParser.parseSkills(it) }.orEmpty()
        val error =
            when {
                response?.error != null -> response.error?.message
                response == null -> "Failed to load skills."
                else -> null
            }
        _uiState.update {
            it.copy(
                isLoading = false,
                skills = skills,
                error = error,
                activeConnectionName = connection.name,
            )
        }
    }
}

private object SkillsParser {
    fun parseSkills(result: JsonElement): List<SkillsItemUi> {
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
                SkillsItemUi(
                    name = name,
                    path = readString(obj, "path"),
                    description = readString(obj, "description").takeIf { it.isNotBlank() },
                )
            }
            .distinctBy { skill ->
                val normalizedName = skill.name.trim().lowercase()
                val normalizedPath = skill.path.trim().lowercase()
                if (normalizedPath.isBlank()) normalizedName else "$normalizedName|$normalizedPath"
            }
    }

    private fun readString(obj: JsonObject, vararg keys: String): String {
        for (k in keys) {
            val v = obj[k] as? JsonPrimitive ?: continue
            val s = runCatching { v.content }.getOrNull()?.trim().orEmpty()
            if (s.isNotBlank()) return s
        }
        return ""
    }
}
