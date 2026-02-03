package me.siddheshkothadi.codexdroid.codex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

/**
 * Global JSON configuration for Codex communication.
 */
val CodexJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    coerceInputValues = true
    isLenient = true
    // Codex app-server uses a "type" discriminator for tagged unions (ThreadItem, etc.).
    classDiscriminator = "type"
}

@Serializable
sealed interface CodexMessage

@Serializable
data class CodexRequest<P>(
    val method: String,
    val id: Long? = null,
    val params: P? = null
) : CodexMessage

@Serializable
data class CodexResponse<R>(
    val id: Long,
    val result: R? = null,
    val error: CodexError? = null
) : CodexMessage

@Serializable
data class CodexNotification<P>(
    val method: String,
    val params: P
) : CodexMessage, CodexEvent

@Serializable
data class CodexError(
    val code: Int? = null,
    val message: String = ""
)

interface CodexEvent

@Serializable
data class ServerRequest(
    val method: String,
    val id: Long,
    val params: JsonElement? = null
) : CodexMessage, CodexEvent

// ---------- Thread Models ----------

@Serializable
data class Thread(
    val id: String,
    val preview: String = "",
    val modelProvider: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val path: String = "",
    val cwd: String = "",
    val source: SessionSource = SessionSource.unknown,
    val gitInfo: GitInfo? = null,
    val tokenUsage: ThreadTokenUsage? = null,
    // Client-only preferences (not guaranteed to exist on server Thread schema).
    val clientModel: String? = null,
    val clientEffort: String? = null,
    val turns: List<Turn> = emptyList()
)

@Serializable
data class ThreadTokenUsage(
    val total: TokenUsageCounts = TokenUsageCounts(),
    val last: TokenUsageCounts = TokenUsageCounts(),
    val modelContextWindow: Long? = null,
)

@Serializable
data class TokenUsageCounts(
    val totalTokens: Long = 0,
    val inputTokens: Long = 0,
    val cachedInputTokens: Long = 0,
    val outputTokens: Long = 0,
    val reasoningOutputTokens: Long = 0,
)

@Serializable
enum class SessionSource {
    cli, vscode, appServer, exec, unknown
}

@Serializable
data class GitInfo(
    val sha: String? = null,
    val branch: String? = null,
    val originUrl: String? = null
)

@Serializable
data class ThreadListParams(
    val cursor: String? = null,
    val limit: Int? = null,
    val sortKey: String? = null
)

@Serializable
data class ThreadListResult(
    val data: List<Thread>,
    val nextCursor: String? = null
)

@Serializable
data class ThreadReadParams(
    val threadId: String,
    val includeTurns: Boolean = true
)

@Serializable
data class ThreadReadResult(
    val thread: Thread
)

@Serializable
data class ThreadStartParams(
    val cwd: String? = null,
    val source: SessionSource? = null
)

@Serializable
data class ThreadResumeParams(
    val threadId: String
)

@Serializable
data class ThreadStartResult(
    val thread: Thread
)

@Serializable
data class ThreadResumeResult(
    val thread: Thread
)

// ---------- Turn Models ----------

@Serializable
data class Turn(
    val id: String,
    val status: TurnStatus = TurnStatus.inProgress,
    val items: List<ThreadItem> = emptyList(),
    val error: TurnError? = null
)

@Serializable(with = TurnStatus.Serializer::class)
enum class TurnStatus {
    completed, interrupted, failed, inProgress, unknown;

    object Serializer : KSerializer<TurnStatus> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("TurnStatus", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): TurnStatus {
            val value = decoder.decodeString()
            val normalized = value.trim()
            return when (normalized) {
                "completed" -> completed
                "interrupted" -> interrupted
                "failed" -> failed
                "inProgress" -> inProgress
                "in_progress", "in-progress", "in progress" -> inProgress
                else -> entries.firstOrNull { it.name == normalized } ?: unknown
            }
        }

        override fun serialize(encoder: Encoder, value: TurnStatus) {
            encoder.encodeString(value.name)
        }
    }
}

@Serializable
data class TurnError(
    val message: String
)

@Serializable
data class TurnStartParams(
    val threadId: String,
    val input: List<UserInput>,
    val cwd: String? = null,
    val model: String? = null,
    val effort: String? = null,
)

@Serializable
data class TurnStartResult(
    val turn: Turn
)

@Serializable
data class TurnInterruptParams(
    val threadId: String,
    val turnId: String
)

@Serializable
object EmptyResult

@Serializable
object EmptyParams

@Serializable
data class UserInput(
    val type: String = "text",
    val text: String? = null
)

// ---------- Thread Items ----------

@Serializable
sealed interface ThreadItem {
    val id: String

    @Serializable
    @SerialName("userMessage")
    data class UserMessage(override val id: String, val content: List<UserInput>) : ThreadItem

    @Serializable
    @SerialName("agentMessage")
    data class AgentMessage(
        override val id: String,
        val text: String = ""
    ) : ThreadItem

    @Serializable
    @SerialName("reasoning")
    data class Reasoning(
        override val id: String,
        val summary: List<String> = emptyList(),
        val content: List<String> = emptyList()
    ) : ThreadItem

    @Serializable
    @SerialName("commandExecution")
    data class CommandExecution(
        override val id: String,
        val command: String,
        val status: CommandExecutionStatus = CommandExecutionStatus.inProgress,
        val aggregatedOutput: String? = null,
        // Additional fields exist on the wire (cwd, exitCode, durationMs, etc.). We keep the model small and resilient.
    ) : ThreadItem

    @Serializable
    @SerialName("fileChange")
    data class FileChange(
        override val id: String,
        val changes: List<FileUpdateChange> = emptyList(),
        val status: PatchApplyStatus = PatchApplyStatus.inProgress,
        // Local-only: streamed output from item/fileChange/outputDelta (not part of the server ThreadItem schema).
        val output: String? = null,
    ) : ThreadItem

    @Serializable
    @SerialName("mcpToolCall")
    data class McpToolCall(
        override val id: String,
        val server: String = "",
        val tool: String = "",
        val status: McpToolCallStatus = McpToolCallStatus.inProgress,
        val arguments: JsonElement = JsonNull,
        val result: JsonElement? = null,
        val error: JsonElement? = null,
        val durationMs: Long? = null,
        // Local-only: progress messages from item/mcpToolCall/progress (not part of the server ThreadItem schema).
        val progress: List<String> = emptyList(),
    ) : ThreadItem

    @Serializable
    @SerialName("collabAgentToolCall")
    data class CollabAgentToolCall(
        override val id: String,
        val tool: String = "",
        val status: String = "",
        val senderThreadId: String = "",
        val receiverThreadIds: List<String> = emptyList(),
        val prompt: String? = null,
    ) : ThreadItem

    @Serializable
    @SerialName("webSearch")
    data class WebSearch(override val id: String, val query: String = "") : ThreadItem

    @Serializable
    @SerialName("imageView")
    data class ImageView(override val id: String, val path: String = "") : ThreadItem

    @Serializable
    @SerialName("enteredReviewMode")
    data class EnteredReviewMode(override val id: String, val review: String = "") : ThreadItem

    @Serializable
    @SerialName("exitedReviewMode")
    data class ExitedReviewMode(override val id: String, val review: String = "") : ThreadItem

    // Local-only: a UI block to render Codex "plan"/todo updates (from turn/plan/updated or codex/event/plan_update).
    @Serializable
    @SerialName("planUpdate")
    data class PlanUpdate(
        override val id: String,
        val explanation: String? = null,
        val plan: List<PlanEntry> = emptyList(),
    ) : ThreadItem
}

@Serializable
data class PlanEntry(
    val step: String,
    val status: PlanEntryStatus = PlanEntryStatus.pending,
)

@Serializable(with = PlanEntryStatus.Serializer::class)
enum class PlanEntryStatus {
    pending, inProgress, completed, failed, cancelled, unknown;

    object Serializer : KSerializer<PlanEntryStatus> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("PlanEntryStatus", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): PlanEntryStatus {
            val value = decoder.decodeString().trim()
            return when (value) {
                "pending" -> pending
                "inProgress", "in_progress", "in-progress", "in progress" -> inProgress
                "completed" -> completed
                "failed" -> failed
                "cancelled", "canceled" -> cancelled
                else -> entries.firstOrNull { it.name == value } ?: unknown
            }
        }

        override fun serialize(encoder: Encoder, value: PlanEntryStatus) {
            encoder.encodeString(value.name)
        }
    }
}

@Serializable(with = CommandExecutionStatus.Serializer::class)
enum class CommandExecutionStatus {
    inProgress, completed, failed, declined, unknown;

    object Serializer : KSerializer<CommandExecutionStatus> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("CommandExecutionStatus", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): CommandExecutionStatus {
            val value = decoder.decodeString()
            return entries.firstOrNull { it.name == value } ?: unknown
        }

        override fun serialize(encoder: Encoder, value: CommandExecutionStatus) {
            encoder.encodeString(value.name)
        }
    }
}

@Serializable(with = PatchApplyStatus.Serializer::class)
enum class PatchApplyStatus {
    inProgress, completed, failed, declined, unknown;

    object Serializer : KSerializer<PatchApplyStatus> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("PatchApplyStatus", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): PatchApplyStatus {
            val value = decoder.decodeString()
            return entries.firstOrNull { it.name == value } ?: unknown
        }

        override fun serialize(encoder: Encoder, value: PatchApplyStatus) {
            encoder.encodeString(value.name)
        }
    }
}

@Serializable(with = McpToolCallStatus.Serializer::class)
enum class McpToolCallStatus {
    inProgress, completed, failed, unknown;

    object Serializer : KSerializer<McpToolCallStatus> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("McpToolCallStatus", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): McpToolCallStatus {
            val value = decoder.decodeString()
            return entries.firstOrNull { it.name == value } ?: unknown
        }

        override fun serialize(encoder: Encoder, value: McpToolCallStatus) {
            encoder.encodeString(value.name)
        }
    }
}

@Serializable
data class FileUpdateChange(
    val path: String = "",
    val kind: JsonElement? = null,
    val diff: String = "",
)

// ---------- Notifications ----------

@Serializable data class TurnStartedNotification(val threadId: String, val turn: Turn)
@Serializable data class TurnCompletedNotification(val threadId: String, val turn: Turn)
@Serializable data class ItemStartedNotification(val threadId: String, val turnId: String, val item: ThreadItem)
@Serializable data class ItemCompletedNotification(val threadId: String, val turnId: String, val item: ThreadItem)
@Serializable data class AgentMessageDeltaNotification(val threadId: String, val turnId: String, val itemId: String, val delta: String)
@Serializable data class CommandExecutionOutputDeltaNotification(val threadId: String, val turnId: String, val itemId: String, val delta: String)
@Serializable data class FileChangeOutputDeltaNotification(val threadId: String, val turnId: String, val itemId: String, val delta: String)
@Serializable data class McpToolCallProgressNotification(val threadId: String, val turnId: String, val itemId: String, val message: String)
@Serializable data class ReasoningTextDeltaNotification(val threadId: String, val turnId: String, val itemId: String, val contentIndex: Long, val delta: String)
@Serializable data class ReasoningSummaryTextDeltaNotification(val threadId: String, val turnId: String, val itemId: String, val summaryIndex: Long, val delta: String)
@Serializable data class ReasoningSummaryPartAddedNotification(val threadId: String, val turnId: String, val itemId: String, val summaryIndex: Long)
@Serializable data class TerminalInteractionNotification(val threadId: String, val turnId: String, val itemId: String, val processId: String, val stdin: String)

@Serializable
data class TurnPlanUpdatedNotification(
    val turnId: String,
    val explanation: String? = null,
    val plan: List<PlanEntry> = emptyList(),
)

@Serializable
data class CodexEventPlanUpdateNotification(
    val id: String? = null,
    val msg: CodexEventPlanUpdateMsg,
    val conversationId: String? = null,
)

@Serializable
data class CodexEventPlanUpdateMsg(
    val type: String = "plan_update",
    val explanation: String? = null,
    val plan: List<PlanEntry> = emptyList(),
)
