package me.siddheshkothadi.codexdroid.codex

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Pure helpers to apply Codex app-server notifications onto a local Thread model.
 *
 * This consolidates the “apply streaming delta / item upsert / turn upsert” logic so it can be used
 * both by UI (foreground) and the global router (background).
 */
object ThreadEventReducer {

    fun applyNotification(thread: Thread, method: String, params: JsonObject): Thread {
        return when (method) {
            "thread/tokenUsage/updated" -> {
                val threadId = readString(params, "threadId", "thread_id")
                if (threadId.isBlank() || threadId != thread.id) thread
                else applyTokenUsageUpdated(thread, params)
            }
            "turn/started" -> {
                val data = CodexJson.decodeFromJsonElement<TurnStartedNotification>(params)
                if (data.threadId != thread.id) thread else applyTurnUpdate(thread, data.turn)
            }
            "turn/completed" -> {
                val data = CodexJson.decodeFromJsonElement<TurnCompletedNotification>(params)
                if (data.threadId != thread.id) thread else applyTurnUpdate(thread, data.turn)
            }
            "turn/plan/updated" -> {
                val data = CodexJson.decodeFromJsonElement<TurnPlanUpdatedNotification>(params)
                applyTurnPlanUpdated(thread, data)
            }
            "codex/event/plan_update" -> {
                val data = CodexJson.decodeFromJsonElement<CodexEventPlanUpdateNotification>(params)
                applyCodexPlanUpdate(thread, data)
            }
            "item/started" -> {
                val data = CodexJson.decodeFromJsonElement<ItemStartedNotification>(params)
                if (data.threadId != thread.id) thread else applyItemUpdate(thread, data.turnId, data.item)
            }
            "item/completed" -> {
                val data = CodexJson.decodeFromJsonElement<ItemCompletedNotification>(params)
                if (data.threadId != thread.id) thread else applyItemUpdate(thread, data.turnId, data.item)
            }
            "item/agentMessage/delta" -> {
                val data = CodexJson.decodeFromJsonElement<AgentMessageDeltaNotification>(params)
                if (data.threadId != thread.id) thread else applyAgentMessageDelta(thread, data)
            }
            "item/reasoning/summaryPartAdded" -> {
                val data = CodexJson.decodeFromJsonElement<ReasoningSummaryPartAddedNotification>(params)
                if (data.threadId != thread.id) thread else applyReasoningSummaryPartAdded(thread, data)
            }
            "item/reasoning/summaryTextDelta" -> {
                val data = CodexJson.decodeFromJsonElement<ReasoningSummaryTextDeltaNotification>(params)
                if (data.threadId != thread.id) thread else applyReasoningSummaryTextDelta(thread, data)
            }
            "item/reasoning/textDelta" -> {
                val data = CodexJson.decodeFromJsonElement<ReasoningTextDeltaNotification>(params)
                if (data.threadId != thread.id) thread else applyReasoningTextDelta(thread, data)
            }
            "item/commandExecution/outputDelta" -> {
                val data = CodexJson.decodeFromJsonElement<CommandExecutionOutputDeltaNotification>(params)
                if (data.threadId != thread.id) thread else applyCommandExecutionOutputDelta(thread, data)
            }
            "item/commandExecution/terminalInteraction" -> {
                val data = CodexJson.decodeFromJsonElement<TerminalInteractionNotification>(params)
                if (data.threadId != thread.id) thread else applyTerminalInteraction(thread, data)
            }
            "item/fileChange/outputDelta" -> {
                val data = CodexJson.decodeFromJsonElement<FileChangeOutputDeltaNotification>(params)
                if (data.threadId != thread.id) thread else applyFileChangeOutputDelta(thread, data)
            }
            "item/mcpToolCall/progress" -> {
                val data = CodexJson.decodeFromJsonElement<McpToolCallProgressNotification>(params)
                if (data.threadId != thread.id) thread else applyMcpToolCallProgress(thread, data)
            }
            else -> thread
        }
    }

    private fun applyTokenUsageUpdated(thread: Thread, params: JsonObject): Thread {
        val tokenUsage =
            (params["tokenUsage"] as? JsonObject)
                ?: (params["token_usage"] as? JsonObject)
                ?: return thread

        val total = (tokenUsage["total"] as? JsonObject) ?: JsonObject(emptyMap())
        val last = (tokenUsage["last"] as? JsonObject) ?: JsonObject(emptyMap())

        val normalized =
            ThreadTokenUsage(
                total =
                    TokenUsageCounts(
                        totalTokens = readLong(total, "totalTokens", "total_tokens"),
                        inputTokens = readLong(total, "inputTokens", "input_tokens"),
                        cachedInputTokens = readLong(total, "cachedInputTokens", "cached_input_tokens"),
                        outputTokens = readLong(total, "outputTokens", "output_tokens"),
                        reasoningOutputTokens = readLong(total, "reasoningOutputTokens", "reasoning_output_tokens"),
                    ),
                last =
                    TokenUsageCounts(
                        totalTokens = readLong(last, "totalTokens", "total_tokens"),
                        inputTokens = readLong(last, "inputTokens", "input_tokens"),
                        cachedInputTokens = readLong(last, "cachedInputTokens", "cached_input_tokens"),
                        outputTokens = readLong(last, "outputTokens", "output_tokens"),
                        reasoningOutputTokens = readLong(last, "reasoningOutputTokens", "reasoning_output_tokens"),
                    ),
                modelContextWindow = readLongOrNull(tokenUsage, "modelContextWindow", "model_context_window"),
            )

        return thread.copy(tokenUsage = normalized)
    }

    private fun readString(obj: JsonObject, vararg keys: String): String {
        for (k in keys) {
            val v = obj[k] as? JsonPrimitive ?: continue
            val s =
                runCatching { v.content }
                    .getOrNull()
                    ?.trim()
                    .orEmpty()
            if (s.isNotBlank()) return s
        }
        return ""
    }

    private fun readLong(obj: JsonObject, vararg keys: String): Long {
        return readLongOrNull(obj, *keys) ?: 0L
    }

    private fun readLongOrNull(obj: JsonObject, vararg keys: String): Long? {
        for (k in keys) {
            val v = obj[k] as? JsonPrimitive ?: continue
            val parsed =
                runCatching { v.content }
                    .getOrNull()
                    ?.trim()
                    ?.toLongOrNull()
            if (parsed != null) return parsed
        }
        return null
    }

    fun ensureTurnExists(thread: Thread, turnId: String): Thread {
        if (thread.turns.any { it.id == turnId }) return thread
        return thread.copy(turns = thread.turns + Turn(id = turnId, status = TurnStatus.unknown, items = emptyList()))
    }

    fun applyTurnUpdate(thread: Thread, turn: Turn): Thread {
        val turns = thread.turns.toMutableList()
        val idx = turns.indexOfFirst { it.id == turn.id }
        if (idx != -1) {
            val items = turn.items.ifEmpty { turns[idx].items }
            turns[idx] = turn.copy(items = items)
        } else {
            turns.add(turn)
        }
        return thread.copy(turns = turns)
    }

    fun applyItemUpdate(thread: Thread, turnId: String, item: ThreadItem): Thread {
        val ensured = ensureTurnExists(thread, turnId)
        return ensured.copy(turns = ensured.turns.map { turn ->
            if (turn.id == turnId) {
                val items = turn.items.toMutableList()
                val idx = items.indexOfFirst { it.id == item.id }
                if (idx != -1) items[idx] = mergeThreadItems(items[idx], item) else items.add(item)
                turn.copy(items = items)
            } else turn
        })
    }

    private fun mergeThreadItems(existing: ThreadItem, incoming: ThreadItem): ThreadItem {
        // Preserve client-accumulated streaming fields when the server sends a partial item (common on item/started).
        return when {
            existing is ThreadItem.CommandExecution && incoming is ThreadItem.CommandExecution -> {
                val mergedOutput = incoming.aggregatedOutput ?: existing.aggregatedOutput
                incoming.copy(aggregatedOutput = mergedOutput)
            }
            existing is ThreadItem.Reasoning && incoming is ThreadItem.Reasoning -> {
                val mergedSummary = if (incoming.summary.isEmpty()) existing.summary else incoming.summary
                val mergedContent = if (incoming.content.isEmpty()) existing.content else incoming.content
                incoming.copy(summary = mergedSummary, content = mergedContent)
            }
            existing is ThreadItem.McpToolCall && incoming is ThreadItem.McpToolCall -> {
                incoming.copy(progress = existing.progress + incoming.progress)
            }
            existing is ThreadItem.FileChange && incoming is ThreadItem.FileChange -> {
                val mergedOutput = incoming.output ?: existing.output
                incoming.copy(output = mergedOutput)
            }
            else -> incoming
        }
    }

    private fun upsertItem(
        thread: Thread,
        turnId: String,
        itemId: String,
        create: () -> ThreadItem,
        update: (ThreadItem) -> ThreadItem
    ): Thread {
        val ensured = ensureTurnExists(thread, turnId)
        return ensured.copy(turns = ensured.turns.map { turn ->
            if (turn.id != turnId) return@map turn
            val items = turn.items.toMutableList()
            val idx = items.indexOfFirst { it.id == itemId }
            if (idx == -1) {
                items.add(update(create()))
            } else {
                items[idx] = update(items[idx])
            }
            turn.copy(items = items)
        })
    }

    private fun ensureSize(list: MutableList<String>, size: Int) {
        while (list.size < size) list.add("")
    }

    private fun applyAgentMessageDelta(thread: Thread, data: AgentMessageDeltaNotification): Thread {
        return upsertItem(thread, data.turnId, data.itemId, create = { ThreadItem.AgentMessage(id = data.itemId) }) { item ->
            val msg = (item as? ThreadItem.AgentMessage) ?: ThreadItem.AgentMessage(id = data.itemId)
            msg.copy(text = msg.text + data.delta)
        }
    }

    private fun applyReasoningSummaryPartAdded(thread: Thread, data: ReasoningSummaryPartAddedNotification): Thread {
        return upsertItem(thread, data.turnId, data.itemId, create = { ThreadItem.Reasoning(id = data.itemId) }) { item ->
            val r = (item as? ThreadItem.Reasoning) ?: ThreadItem.Reasoning(id = data.itemId)
            val summary = r.summary.toMutableList()
            ensureSize(summary, (data.summaryIndex + 1).toInt())
            r.copy(summary = summary)
        }
    }

    private fun applyReasoningSummaryTextDelta(thread: Thread, data: ReasoningSummaryTextDeltaNotification): Thread {
        return upsertItem(thread, data.turnId, data.itemId, create = { ThreadItem.Reasoning(id = data.itemId) }) { item ->
            val r = (item as? ThreadItem.Reasoning) ?: ThreadItem.Reasoning(id = data.itemId)
            val summary = r.summary.toMutableList()
            ensureSize(summary, (data.summaryIndex + 1).toInt())
            summary[data.summaryIndex.toInt()] = summary[data.summaryIndex.toInt()] + data.delta
            r.copy(summary = summary)
        }
    }

    private fun applyReasoningTextDelta(thread: Thread, data: ReasoningTextDeltaNotification): Thread {
        return upsertItem(thread, data.turnId, data.itemId, create = { ThreadItem.Reasoning(id = data.itemId) }) { item ->
            val r = (item as? ThreadItem.Reasoning) ?: ThreadItem.Reasoning(id = data.itemId)
            val content = r.content.toMutableList()
            ensureSize(content, (data.contentIndex + 1).toInt())
            content[data.contentIndex.toInt()] = content[data.contentIndex.toInt()] + data.delta
            r.copy(content = content)
        }
    }

    private fun applyCommandExecutionOutputDelta(thread: Thread, data: CommandExecutionOutputDeltaNotification): Thread {
        return upsertItem(thread, data.turnId, data.itemId, create = { ThreadItem.CommandExecution(id = data.itemId, command = "") }) { item ->
            val ce = (item as? ThreadItem.CommandExecution) ?: ThreadItem.CommandExecution(id = data.itemId, command = "")
            val updated = (ce.aggregatedOutput ?: "") + data.delta
            ce.copy(aggregatedOutput = updated)
        }
    }

    private fun applyTerminalInteraction(thread: Thread, data: TerminalInteractionNotification): Thread {
        return upsertItem(thread, data.turnId, data.itemId, create = { ThreadItem.CommandExecution(id = data.itemId, command = "") }) { item ->
            val ce = (item as? ThreadItem.CommandExecution) ?: ThreadItem.CommandExecution(id = data.itemId, command = "")
            val updated = (ce.aggregatedOutput ?: "") + "\n[stdin requested] ${data.stdin}\n"
            ce.copy(aggregatedOutput = updated)
        }
    }

    private fun applyFileChangeOutputDelta(thread: Thread, data: FileChangeOutputDeltaNotification): Thread {
        return upsertItem(thread, data.turnId, data.itemId, create = { ThreadItem.FileChange(id = data.itemId) }) { item ->
            val fc = (item as? ThreadItem.FileChange) ?: ThreadItem.FileChange(id = data.itemId)
            val updated = (fc.output ?: "") + data.delta
            fc.copy(output = updated)
        }
    }

    private fun applyMcpToolCallProgress(thread: Thread, data: McpToolCallProgressNotification): Thread {
        return upsertItem(thread, data.turnId, data.itemId, create = { ThreadItem.McpToolCall(id = data.itemId) }) { item ->
            val tc = (item as? ThreadItem.McpToolCall) ?: ThreadItem.McpToolCall(id = data.itemId)
            tc.copy(progress = tc.progress + data.message)
        }
    }

    private fun applyTurnPlanUpdated(thread: Thread, data: TurnPlanUpdatedNotification): Thread {
        val targetTurnId = data.turnId
        val ensured = ensureTurnExists(thread, targetTurnId)
        return upsertItem(
            ensured,
            targetTurnId,
            "plan-$targetTurnId",
            create = { ThreadItem.PlanUpdate(id = "plan-$targetTurnId") }
        ) { existing ->
            val prev = existing as? ThreadItem.PlanUpdate ?: ThreadItem.PlanUpdate(id = "plan-$targetTurnId")
            prev.copy(explanation = data.explanation, plan = data.plan)
        }
    }

    private fun applyCodexPlanUpdate(thread: Thread, data: CodexEventPlanUpdateNotification): Thread {
        val msg = data.msg
        val targetTurnId = thread.turns.lastOrNull()?.id ?: "meta"
        val key = "plan-${data.conversationId ?: "conv"}-${data.id ?: "0"}"
        val ensured = ensureTurnExists(thread, targetTurnId)
        return upsertItem(ensured, targetTurnId, key, create = { ThreadItem.PlanUpdate(id = key) }) { existing ->
            val prev = existing as? ThreadItem.PlanUpdate ?: ThreadItem.PlanUpdate(id = key)
            prev.copy(explanation = msg.explanation, plan = msg.plan)
        }
    }
}
