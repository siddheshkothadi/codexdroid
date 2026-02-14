package me.siddheshkothadi.codexdroid.feature.session.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import me.siddheshkothadi.codexdroid.codex.*
import me.siddheshkothadi.codexdroid.ui.theme.CodexTheme
@Composable
fun ThreadItemBubble(item: ThreadItem) {
    val isUser = item is ThreadItem.UserMessage
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val background = if (isUser) CodexTheme.colors.userMessageBackground else Color.Transparent
    val isFullWidthItem = !isUser && item is ThreadItem.AgentMessage
    val isCardLikeItem =
        item is ThreadItem.CommandExecution ||
            item is ThreadItem.Reasoning ||
            item is ThreadItem.PlanUpdate ||
            item is ThreadItem.FileChange ||
            item is ThreadItem.McpToolCall ||
            item is ThreadItem.WebSearch ||
            item is ThreadItem.ImageView ||
            item is ThreadItem.EnteredReviewMode ||
            item is ThreadItem.ExitedReviewMode ||
            item is ThreadItem.CollabAgentToolCall
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(background)
                .padding(
                    if (isUser) 14.dp else 0.dp
                )
                .then(
                    when {
                        isUser -> Modifier.widthIn(max = 280.dp)
                        isFullWidthItem || isCardLikeItem -> Modifier.fillMaxWidth()
                        else -> Modifier.widthIn(max = 340.dp)
                    }
                )
        ) {
            when (item) {
                is ThreadItem.UserMessage -> {
                    SelectionContainer {
                        Text(
                            text = item.content.joinToString { it.text.orEmpty() },
                            style = MaterialTheme.typography.bodyLarge.copy(color = CodexTheme.colors.userMessageText),
                        )
                    }
                }
                is ThreadItem.AgentMessage -> {
                    SelectionContainer {
                        RichText(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Markdown(item.text)
                        }
                    }
                }
                is ThreadItem.Reasoning -> ReasoningItem(item)
                is ThreadItem.PlanUpdate -> PlanUpdateItem(item)
                is ThreadItem.CommandExecution -> CommandExecutionItem(item)
                is ThreadItem.McpToolCall -> McpToolCallItem(item)
                is ThreadItem.FileChange -> FileChangeItem(item)
                is ThreadItem.WebSearch -> InfoItem(id = item.id, title = "Web search", body = item.query)
                is ThreadItem.ImageView -> InfoItem(id = item.id, title = "Image", body = item.path)
                is ThreadItem.EnteredReviewMode -> InfoItem(id = item.id, title = "Review started", body = item.review)
                is ThreadItem.ExitedReviewMode -> InfoItem(id = item.id, title = "Review finished", body = item.review)
                is ThreadItem.CollabAgentToolCall ->
                    InfoItem(id = item.id, title = "Collab tool call", body = "${item.tool} (${item.status})")
            }
        }
    }
}

@Composable
fun ReasoningItem(item: ThreadItem.Reasoning) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    Column {
        val toggle = { expanded = !expanded }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CodexTheme.colors.bgSecondary)
                .clickable(onClick = toggle)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Reasoning",
                style = MaterialTheme.typography.bodyLarge,
                color = CodexTheme.colors.textSecondary,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = CodexTheme.colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        if (expanded) {
            Column(modifier = Modifier.padding(12.dp)) {
                item.summary.forEach {
                    ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                        RichText(modifier = Modifier.fillMaxWidth()) {
                            Markdown(it)
                        }
                    }
                }
                item.content.forEach {
                    ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                        RichText(modifier = Modifier.fillMaxWidth()) {
                            Markdown(it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommandExecutionItem(item: ThreadItem.CommandExecution) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    val colors = CodexTheme.colors
    Column {
        val toggle = { expanded = !expanded }
        val isRunning = item.status == CommandExecutionStatus.inProgress || item.status == CommandExecutionStatus.unknown
        val title = if (isRunning) "Running" else "Ran"
        val dotColor =
            when (item.status) {
                CommandExecutionStatus.inProgress -> colors.accentWarning
                CommandExecutionStatus.completed -> colors.accentSuccess
                CommandExecutionStatus.failed, CommandExecutionStatus.declined -> colors.accentError
                CommandExecutionStatus.unknown -> colors.borderDefault
            }

        val commandOneLine = item.command.lineSequence().firstOrNull().orEmpty().trim()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CodexTheme.colors.bgSecondary)
                .clickable(onClick = toggle)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = CodexTheme.colors.textSecondary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = CodexTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    commandOneLine,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CodexTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (expanded) {
            Column(Modifier.fillMaxWidth().background(CodexTheme.colors.bgPrimary).padding(10.dp)) {
                if (item.command.isNotBlank()) {
                    Text("Command", style = MaterialTheme.typography.labelSmall, color = CodexTheme.colors.textSecondary)
                    Text(
                        item.command,
                        style = MaterialTheme.typography.bodySmall,
                        color = CodexTheme.colors.textSecondary
                    )
                }

                item.aggregatedOutput?.let { out ->
                    if (out.isNotBlank()) {
                        if (item.command.isNotBlank()) Spacer(Modifier.height(8.dp))
                        Text("Output", style = MaterialTheme.typography.labelSmall, color = CodexTheme.colors.textSecondary)
                        Text(
                            out,
                            style = MaterialTheme.typography.bodySmall,
                            color = CodexTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlanUpdateItem(item: ThreadItem.PlanUpdate) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    Column {
        val toggle = { expanded = !expanded }
        val total = item.plan.size
        val done = item.plan.count { it.status == PlanEntryStatus.completed }
        val summary =
            when {
                total == 0 -> ""
                done == 0 -> "$total"
                else -> "$done/$total"
            }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CodexTheme.colors.bgSecondary)
                .clickable(onClick = toggle)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "To-dos",
                style = MaterialTheme.typography.bodyLarge,
                color = CodexTheme.colors.textSecondary,
            )
            if (summary.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = CodexTheme.colors.textSecondary,
                )
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = CodexTheme.colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        if (expanded) {
            val markdown =
                buildString {
                    val explanation = item.explanation?.trim().orEmpty()
                    if (explanation.isNotBlank()) {
                        appendLine(explanation)
                        appendLine()
                    }
                    item.plan.forEach { entry ->
                        val step = entry.step.trim()
                        if (step.isBlank()) return@forEach
                        val line =
                            when (entry.status) {
                                PlanEntryStatus.completed -> "- [x] $step"
                                PlanEntryStatus.inProgress -> "- [ ] (in progress) $step"
                                PlanEntryStatus.failed -> "- [ ] (failed) $step"
                                PlanEntryStatus.cancelled -> "- [ ] (cancelled) $step"
                                PlanEntryStatus.pending, PlanEntryStatus.unknown -> "- [ ] $step"
                            }
                        appendLine(line)
                    }
                }

            Column(modifier = Modifier.padding(12.dp)) {
                if (markdown.isNotBlank()) {
                    ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                        RichText(modifier = Modifier.fillMaxWidth()) {
                            Markdown(markdown)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun McpToolCallItem(item: ThreadItem.McpToolCall) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    val colors = CodexTheme.colors
    Column {
        val toggle = { expanded = !expanded }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CodexTheme.colors.bgSecondary)
                .clickable(onClick = toggle)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${item.server} :: ${item.tool}",
                style = MaterialTheme.typography.labelSmall,
                color = CodexTheme.colors.textSecondary,
                modifier = Modifier.weight(1f)
            )
            Text(
                item.status.name,
                style = MaterialTheme.typography.labelSmall,
                color = when (item.status) {
                    McpToolCallStatus.completed -> colors.accentSuccess
                    McpToolCallStatus.failed -> colors.accentError
                    else -> colors.accentWarning
                }
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = CodexTheme.colors.textSecondary
            )
        }

        if (expanded) {
            Column(Modifier.fillMaxWidth().background(CodexTheme.colors.bgPrimary).padding(10.dp)) {
                if (item.progress.isNotEmpty()) {
                    Text("Progress", style = MaterialTheme.typography.labelSmall, color = CodexTheme.colors.textSecondary)
                    item.progress.takeLast(8).forEach { msg ->
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = CodexTheme.colors.textPrimary)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Text("Arguments", style = MaterialTheme.typography.labelSmall, color = CodexTheme.colors.textSecondary)
                Text(item.arguments.toString(), style = MaterialTheme.typography.bodySmall)

                item.result?.let { res ->
                    Spacer(Modifier.height(8.dp))
                    Text("Result", style = MaterialTheme.typography.labelSmall, color = CodexTheme.colors.textSecondary)
                    Text(res.toString(), style = MaterialTheme.typography.bodySmall)
                }

                item.error?.let { err ->
                    Spacer(Modifier.height(8.dp))
                    Text("Error", style = MaterialTheme.typography.labelSmall, color = CodexTheme.colors.accentError)
                    Text(err.toString(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun FileChangeItem(item: ThreadItem.FileChange) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    val colors = CodexTheme.colors
    Column {
        val toggle = { expanded = !expanded }
        val dotColor =
            when (item.status) {
                PatchApplyStatus.inProgress -> colors.accentWarning
                PatchApplyStatus.completed -> colors.accentSuccess
                PatchApplyStatus.failed, PatchApplyStatus.declined -> colors.accentError
                PatchApplyStatus.unknown -> colors.borderDefault
            }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CodexTheme.colors.bgSecondary)
                .clickable(onClick = toggle)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "File changes (${item.changes.size})",
                style = MaterialTheme.typography.bodyLarge,
                color = CodexTheme.colors.textSecondary,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = CodexTheme.colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        if (expanded) {
            Column(Modifier.fillMaxWidth().background(CodexTheme.colors.bgPrimary).padding(10.dp)) {
                item.changes.forEach { change ->
                    if (change.path.isNotBlank()) {
                        Text(change.path, style = MaterialTheme.typography.bodySmall, color = CodexTheme.colors.textSecondary)
                    }
                    if (change.diff.isNotBlank()) {
                        Text(change.diff, style = MaterialTheme.typography.bodySmall, color = CodexTheme.colors.textSecondary)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                item.output?.let { out ->
                    if (out.isNotBlank()) {
                        Text("Output", style = MaterialTheme.typography.labelSmall, color = CodexTheme.colors.textSecondary)
                        Text(out, style = MaterialTheme.typography.bodySmall, color = CodexTheme.colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoItem(id: String, title: String, body: String) {
    var expanded by rememberSaveable(id) { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        val toggle = { expanded = !expanded }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CodexTheme.colors.bgSecondary)
                    .clickable(onClick = toggle)
                    .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CodexTheme.colors.textSecondary,
                )
                if (!expanded && body.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        body,
                        style = MaterialTheme.typography.bodySmall,
                        color = CodexTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = CodexTheme.colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        if (expanded) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (body.isNotBlank()) {
                    ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                        RichText(modifier = Modifier.fillMaxWidth()) {
                            Markdown(body)
                        }
                    }
                }
            }
        }
    }
}


