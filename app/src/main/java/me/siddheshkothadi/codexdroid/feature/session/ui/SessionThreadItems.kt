package me.siddheshkothadi.codexdroid.feature.session.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownPadding
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import me.siddheshkothadi.codexdroid.codex.*
import me.siddheshkothadi.codexdroid.ui.theme.CodexTheme

@Composable
fun ThreadItemBubble(item: ThreadItem) {
    val isUser = item is ThreadItem.UserMessage
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val background = if (isUser) CodexTheme.colors.userMessageBackground else Color.Transparent
    val isTranscriptItem = !isUser && item !is ThreadItem.AgentMessage
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
                        isTranscriptItem || item is ThreadItem.AgentMessage -> Modifier.fillMaxWidth()
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
                        CodexMarkdown(
                            markdown = item.text,
                            modifier = Modifier.fillMaxWidth().padding(14.dp)
                        )
                    }
                }
                is ThreadItem.Reasoning -> ReasoningItem(item)
                is ThreadItem.PlanUpdate -> PlanUpdateItem(item)
                is ThreadItem.CommandExecution -> CommandExecutionItem(item)
                is ThreadItem.McpToolCall -> McpToolCallItem(item)
                is ThreadItem.FileChange -> FileChangeItem(item)
                is ThreadItem.ContextCompaction -> ContextCompactionItem(item)
                is ThreadItem.WebSearch -> WebSearchItem(item)
                is ThreadItem.ImageView -> ImageViewItem(item)
                is ThreadItem.TerminalInteraction -> TerminalInteractionItem(item)
                is ThreadItem.EnteredReviewMode -> ReviewModeItem(title = "Entered review mode", body = item.review)
                is ThreadItem.ExitedReviewMode -> ReviewModeItem(title = "Exited review mode", body = item.review)
                is ThreadItem.CollabAgentToolCall ->
                    ReviewModeItem(title = "Collab tool call", body = "${item.tool} (${item.status})")
            }
        }
    }
}

@Composable
fun ReasoningItem(item: ThreadItem.Reasoning) {
    val markdown =
        (item.summary + item.content)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n\n")
    if (markdown.isBlank()) return
    val colors = CodexTheme.colors
    val transition = rememberInfiniteTransition(label = "reasoning_item_glow")
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "reasoning_item_glow_value",
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.bgSecondary,
        shape = RoundedCornerShape(14.dp),
        border =
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = lerp(colors.borderSubtle, colors.borderDefault, glow * 0.85f),
            ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp)) {
            Surface(
                color = colors.bgPrimary.copy(alpha = 0.75f),
                shape = RoundedCornerShape(999.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Reasoning",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textSecondary,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                color = colors.bgPrimary.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                    ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                        CodexMarkdown(markdown = markdown, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
fun CommandExecutionItem(item: ThreadItem.CommandExecution) {
    val colors = CodexTheme.colors
    var expandedOutput by rememberSaveable(item.id) { mutableStateOf(false) }
    val isRunning = item.status == CommandExecutionStatus.inProgress || item.status == CommandExecutionStatus.unknown
    val headerColor =
        when (item.status) {
            CommandExecutionStatus.completed -> colors.accentSuccess
            CommandExecutionStatus.failed, CommandExecutionStatus.declined -> colors.accentError
            CommandExecutionStatus.inProgress -> colors.accentWarning
            CommandExecutionStatus.unknown -> colors.textSecondary
        }
    val actionLines = formatCommandActionLines(item.commandActions)
    val isExploring = actionLines.isNotEmpty()

    TranscriptSurface {
        if (isExploring) {
            if (isRunning) {
                RunningTranscriptLine(
                    text = "Exploring",
                    color = headerColor,
                )
            } else {
                TranscriptLine(
                    text = "• Explored",
                    color = headerColor,
                )
            }
            actionLines.forEachIndexed { index, line ->
                TranscriptLine(
                    text = if (index == 0) "  └ $line" else "    $line",
                    mono = false,
                )
            }
            return@TranscriptSurface
        }

        val commandLines = item.command.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
        val commandFirst = commandLines.firstOrNull().orEmpty()
        val heading = "${if (isRunning) "Running" else "Ran"} $commandFirst".trimEnd()
        if (isRunning) {
            RunningTranscriptLine(
                text = heading,
                color = headerColor,
                mono = true,
            )
        } else {
            TranscriptLine(
                text = "• $heading",
                color = headerColor,
                mono = true,
            )
        }
        commandLines.drop(1).forEach { line ->
            TranscriptLine(text = "  │ $line", mono = true)
        }

        val outputPreview = truncateOutputForCli(item.aggregatedOutput)
        val fullOutputLines = fullOutputLines(item.aggregatedOutput)
        val isTrimmed = fullOutputLines.size > outputPreview.size
        val outputLines = if (expandedOutput) fullOutputLines else outputPreview
        if (outputLines.isEmpty() && !isRunning) {
            TranscriptLine(text = "  └ (no output)", mono = true)
        } else {
            outputLines.forEachIndexed { index, line ->
                TranscriptLine(
                    text = if (index == 0) "  └ $line" else "    $line",
                    mono = true,
                )
            }
        }
        if (!isRunning && isTrimmed) {
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = { expandedOutput = !expandedOutput },
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
            ) {
                Text(
                    text = if (expandedOutput) "Show less output" else "Show full output",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
fun PlanUpdateItem(item: ThreadItem.PlanUpdate) {
    TranscriptSurface {
        TranscriptLine(text = "• Updated Plan", color = CodexTheme.colors.textSecondary)
        item.explanation
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { TranscriptLine(text = "  └ $it") }
        if (item.plan.isEmpty()) {
            TranscriptLine(text = "  └ (no steps provided)")
            return@TranscriptSurface
        }
        item.plan.forEach { entry ->
            val step = entry.step.trim().ifBlank { "(empty step)" }
            val marker = if (entry.status == PlanEntryStatus.completed) "✔ " else "□ "
            val prefix = if (entry.status == PlanEntryStatus.completed) "    " else "  └ "
            TranscriptLine(text = "$prefix$marker$step")
        }
    }
}

@Composable
fun McpToolCallItem(item: ThreadItem.McpToolCall) {
    val colors = CodexTheme.colors
    var expandedResult by rememberSaveable(item.id) { mutableStateOf(false) }
    val running = item.status == McpToolCallStatus.inProgress || item.status == McpToolCallStatus.unknown
    val statusColor =
        when (item.status) {
            McpToolCallStatus.completed -> colors.accentSuccess
            McpToolCallStatus.failed -> colors.accentError
            McpToolCallStatus.inProgress -> colors.accentWarning
            McpToolCallStatus.unknown -> colors.textSecondary
        }
    val args = compactJson(item.arguments).ifBlank { "{}" }
    val invocation = "${item.server}.${item.tool}($args)"

    TranscriptSurface {
        if (running) {
            RunningTranscriptLine(
                text = "Calling $invocation",
                color = statusColor,
                mono = true,
            )
        } else {
            TranscriptLine(
                text = "• Called $invocation",
                color = statusColor,
                mono = true,
            )
        }
        if (running) {
            item.progress.lastOrNull()?.takeIf { it.isNotBlank() }?.let {
                TranscriptLine(text = "  └ $it")
            }
            return@TranscriptSurface
        }
        item.result?.let { res ->
            val full = fullOutputLines(compactJson(res))
            val preview = truncateOutputForCli(compactJson(res))
            val isTrimmed = full.size > preview.size
            val lines = if (expandedResult) full else preview
            lines.forEachIndexed { index, line ->
                TranscriptLine(text = if (index == 0) "  └ $line" else "    $line", mono = true)
            }
            if (isTrimmed) {
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { expandedResult = !expandedResult },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = if (expandedResult) "Show less result" else "Show full result",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                    )
                }
            }
        }
        item.error?.let { err ->
            val msg = compactJson(err).ifBlank { "unknown error" }
            TranscriptLine(text = "  └ Error: $msg", color = colors.accentError, mono = true)
        }
    }
}

@Composable
fun FileChangeItem(item: ThreadItem.FileChange) {
    val colors = CodexTheme.colors
    var expandedOutput by rememberSaveable(item.id) { mutableStateOf(false) }
    val lineColor =
        when (item.status) {
            PatchApplyStatus.completed -> colors.accentSuccess
            PatchApplyStatus.failed, PatchApplyStatus.declined -> colors.accentError
            PatchApplyStatus.inProgress -> colors.accentWarning
            PatchApplyStatus.unknown -> colors.textSecondary
        }
    TranscriptSurface {
        if (item.changes.isEmpty()) {
            TranscriptLine(text = "• Applying patch", color = lineColor)
        } else {
            item.changes.forEach { change ->
                TranscriptLine(text = "• ${formatFileChangeSummary(change)}", color = lineColor)
                truncateDiffPreview(change.diff).forEach { diffLine ->
                    TranscriptLine(text = "    $diffLine", mono = true)
                }
            }
        }
        item.output
            ?.takeIf { it.isNotBlank() }
            ?.let { out ->
                val full = fullOutputLines(out)
                val preview = truncateOutputForCli(out)
                val isTrimmed = full.size > preview.size
                val lines = if (expandedOutput) full else preview
                lines.forEachIndexed { index, line ->
                    TranscriptLine(text = if (index == 0) "  └ $line" else "    $line", mono = true)
                }
                if (isTrimmed) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = { expandedOutput = !expandedOutput },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text = if (expandedOutput) "Show less patch output" else "Show full patch output",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
    }
}

@Composable
fun ContextCompactionItem(item: ThreadItem.ContextCompaction) {
    val colors = CodexTheme.colors
    val isRunning = item.status == ContextCompactionStatus.inProgress || item.status == ContextCompactionStatus.unknown
    val message =
        when {
            isRunning -> "Compacting conversation context to fit token limits..."
            item.status == ContextCompactionStatus.failed -> "Context compaction failed."
            else -> "Context compaction completed."
        }
    val color =
        when (item.status) {
            ContextCompactionStatus.completed -> colors.accentSuccess
            ContextCompactionStatus.failed -> colors.accentError
            ContextCompactionStatus.inProgress, ContextCompactionStatus.unknown -> colors.accentWarning
        }
    TranscriptSurface {
        if (isRunning) {
            RunningTranscriptLine(
                text = message,
                color = color,
            )
        } else {
            TranscriptLine(text = "• $message", color = color)
        }
    }
}

@Composable
private fun WebSearchItem(item: ThreadItem.WebSearch) {
    val running = item.action == null
    val detail = webSearchDetail(item.action, item.query)
    val title = if (running) "Searching the web" else "Searched"
    val lineColor = if (running) CodexTheme.colors.accentWarning else CodexTheme.colors.accentSuccess
    val text = buildString {
        append(title)
        if (detail.isNotBlank()) {
            append(" ")
            append(detail)
        }
    }
    TranscriptSurface {
        if (running) {
            RunningTranscriptLine(text = text, color = lineColor)
        } else {
            TranscriptLine(text = "• $text", color = lineColor)
        }
    }
}

@Composable
private fun ImageViewItem(item: ThreadItem.ImageView) {
    TranscriptSurface {
        TranscriptLine(text = "• Viewed Image")
        item.path.takeIf { it.isNotBlank() }?.let { TranscriptLine(text = "  └ $it", mono = true) }
    }
}

@Composable
private fun TerminalInteractionItem(item: ThreadItem.TerminalInteraction) {
    val commandSuffix = item.command.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
    TranscriptSurface {
        TranscriptLine(text = "↳ Interacted with background terminal$commandSuffix", mono = true)
        if (item.waited || item.stdin.isBlank()) {
            TranscriptLine(text = "  └ (waited)", mono = true)
        } else {
            item.stdin.lines().forEachIndexed { index, line ->
                TranscriptLine(text = if (index == 0) "  └ $line" else "    $line", mono = true)
            }
        }
    }
}

@Composable
private fun ReviewModeItem(title: String, body: String) {
    TranscriptSurface {
        TranscriptLine(text = "• $title")
        body.takeIf { it.isNotBlank() }?.let { TranscriptLine(text = "  └ $it") }
    }
}

@Composable
private fun TranscriptSurface(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CodexTheme.colors.bgSecondary,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            content = content
        )
    }
}

@Composable
private fun ColumnScope.TranscriptLine(
    text: String,
    color: Color = CodexTheme.colors.textSecondary,
    mono: Boolean = false,
) {
    Text(
        text = text,
        style =
            (if (mono) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium).copy(
                fontFamily = if (mono) FontFamily.Monospace else null,
                color = color,
            ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ColumnScope.RunningTranscriptLine(
    text: String,
    color: Color = CodexTheme.colors.textSecondary,
    mono: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RunningTranscriptIndicator()
        Text(
            text = text,
            style =
                (if (mono) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium).copy(
                    fontFamily = if (mono) FontFamily.Monospace else null,
                    color = color,
                ),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RunningTranscriptIndicator() {
    val colors = CodexTheme.colors
    CircularProgressIndicator(
        modifier = Modifier.size(12.dp),
        strokeWidth = 1.6.dp,
        color = colors.textSecondary,
    )
}

private fun compactJson(value: Any?): String {
    val raw = value?.toString().orEmpty().trim()
    if (raw.isBlank()) return ""
    return raw
        .replace("\n", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun truncateOutputForCli(output: String?, limit: Int = 5): List<String> {
    val lines = fullOutputLines(output)
    if (lines.isEmpty()) return emptyList()
    if (lines.size <= limit * 2) return lines
    val omitted = lines.size - (limit * 2)
    return buildList {
        addAll(lines.take(limit))
        add("… +$omitted lines")
        addAll(lines.takeLast(limit))
    }
}

private fun fullOutputLines(output: String?): List<String> {
    return output
        ?.lines()
        ?.map { it.trimEnd() }
        ?.filter { it.isNotBlank() }
        .orEmpty()
}

private fun truncateDiffPreview(diff: String, maxLines: Int = 3): List<String> {
    if (diff.isBlank()) return emptyList()
    return diff.lines().map { it.trimEnd() }.filter { it.isNotBlank() }.take(maxLines)
}

private fun formatFileChangeSummary(change: FileUpdateChange): String {
    val kindType = (change.kind as? kotlinx.serialization.json.JsonObject)?.get("type")?.toString()?.trim('"')
    val kind =
        when (kindType) {
            "add" -> "Added"
            "delete" -> "Deleted"
            else -> "Edited"
        }
    val plus = change.diff.lines().count { it.startsWith("+") && !it.startsWith("+++") }
    val minus = change.diff.lines().count { it.startsWith("-") && !it.startsWith("---") }
    val stats = if (plus == 0 && minus == 0) "" else " (+$plus -$minus)"
    return "$kind ${change.path}$stats"
}

private fun webSearchDetail(action: kotlinx.serialization.json.JsonElement?, query: String): String {
    val obj = action as? kotlinx.serialization.json.JsonObject ?: return query
    val type = obj["type"]?.toString()?.trim('"').orEmpty()
    return when (type) {
        "search" -> {
            val single = obj["query"]?.toString()?.trim('"').orEmpty()
            if (single.isNotBlank()) return single
            val queries =
                (obj["queries"] as? kotlinx.serialization.json.JsonArray)
                    ?.mapNotNull { it.toString().trim('"').takeIf { v -> v.isNotBlank() } }
                    .orEmpty()
            when {
                queries.isEmpty() -> query
                queries.size == 1 -> queries.first()
                else -> "${queries.first()} ..."
            }
        }
        "openPage", "open_page" -> obj["url"]?.toString()?.trim('"').orEmpty()
        "findInPage", "find_in_page" -> {
            val url = obj["url"]?.toString()?.trim('"').orEmpty()
            val pattern = obj["pattern"]?.toString()?.trim('"').orEmpty()
            when {
                pattern.isNotBlank() && url.isNotBlank() -> "'$pattern' in $url"
                pattern.isNotBlank() -> "'$pattern'"
                else -> url
            }
        }
        else -> query
    }
}

private fun formatCommandActionLines(actions: List<kotlinx.serialization.json.JsonElement>): List<String> {
    if (actions.isEmpty()) return emptyList()
    val lines = mutableListOf<String>()
    actions.forEach { element ->
        val obj = element as? kotlinx.serialization.json.JsonObject ?: return@forEach
        when (obj["type"]?.toString()?.trim('"')) {
            "read" -> {
                val name = obj["name"]?.toString()?.trim('"').orEmpty()
                val path = obj["path"]?.toString()?.trim('"').orEmpty()
                val command = obj["command"]?.toString()?.trim('"').orEmpty()
                val value = name.ifBlank { path }.ifBlank { command }
                if (value.isNotBlank()) lines.add("Read $value")
            }
            "listFiles", "list_files" -> {
                val path = obj["path"]?.toString()?.trim('"').orEmpty()
                val command = obj["command"]?.toString()?.trim('"').orEmpty()
                val value = path.ifBlank { command }
                if (value.isNotBlank()) lines.add("List $value")
            }
            "search" -> {
                val query = obj["query"]?.toString()?.trim('"').orEmpty()
                val path = obj["path"]?.toString()?.trim('"').orEmpty()
                val command = obj["command"]?.toString()?.trim('"').orEmpty()
                val value =
                    when {
                        query.isNotBlank() && path.isNotBlank() -> "$query in $path"
                        query.isNotBlank() -> query
                        else -> command
                    }
                if (value.isNotBlank()) lines.add("Search $value")
            }
            "unknown" -> {
                val command = obj["command"]?.toString()?.trim('"').orEmpty()
                if (command.isNotBlank()) lines.add("Run $command")
            }
        }
    }
    if (lines.isEmpty()) return emptyList()
    val merged = mutableListOf<String>()
    lines.forEach { line ->
        if (line.startsWith("Read ") && merged.lastOrNull()?.startsWith("Read ") == true) {
            val previous = merged.removeAt(merged.lastIndex)
            merged.add("$previous, ${line.removePrefix("Read ")}")
        } else {
            merged.add(line)
        }
    }
    return merged
}

@Composable
private fun CodexMarkdown(markdown: String, modifier: Modifier = Modifier) {
    val colors = CodexTheme.colors
    val typography = MaterialTheme.typography
    val baseTextStyle = LocalTextStyle.current.copy(color = colors.textPrimary)
    
    val highlightsBuilder = remember { Highlights.Builder() }
    val markdownComponents =
        remember(highlightsBuilder) {
            codexMarkdownComponents(highlightsBuilder)
        }

    Markdown(
        content = markdown,
        modifier = modifier,
        colors =
            markdownColor(
                text = colors.textPrimary,
                codeText = colors.textPrimary,
                inlineCodeText = colors.textPrimary,
                linkText = colors.textPrimary,
                codeBackground = Color.Transparent,
                inlineCodeBackground = Color.Transparent,
                dividerColor = colors.borderDefault,
            ),
        typography =
            markdownTypography(
                h1 = typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = colors.textPrimary),
                h2 = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = colors.textPrimary),
                h3 = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = colors.textPrimary),
                h4 = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = colors.textPrimary),
                text = baseTextStyle,
                paragraph = baseTextStyle,
                ordered = baseTextStyle,
                bullet = baseTextStyle,
                list = baseTextStyle,
                quote = baseTextStyle.copy(color = colors.textSecondary),
                code = typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = colors.textPrimary),
                inlineCode =
                    baseTextStyle.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                    ),
                link =
                    baseTextStyle.copy(
                        color = colors.textPrimary,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Medium,
                    ),
            ),
        padding = markdownPadding(codeBlock = PaddingValues(0.dp)),
        components = markdownComponents,
    )
}

private fun codexMarkdownComponents(highlightsBuilder: Highlights.Builder): MarkdownComponents {
    return markdownComponents(
        codeFence = {
            MarkdownCodeFence(it.content, it.node) { code, language ->
                CodexCodeBlock(code = code, language = language, highlightsBuilder = highlightsBuilder)
            }
        },
        codeBlock = {
            MarkdownCodeBlock(it.content, it.node) { code, language ->
                CodexCodeBlock(code = code, language = language, highlightsBuilder = highlightsBuilder)
            }
        },
    )
}

@Composable
private fun CodexCodeBlock(code: String, language: String?, highlightsBuilder: Highlights.Builder) {
    val colors = CodexTheme.colors
    val context = LocalContext.current
    val normalizedCode = remember(code) { code.trimEnd('\n', '\r') }
    val normalizedLanguage = remember(language) { normalizeFenceLanguage(language) }
    var wrapLines by rememberSaveable(normalizedCode, normalizedLanguage) { mutableStateOf(false) }
    val horizontalScroll = rememberScrollState()
    val highlightedCode = remember(normalizedCode, normalizedLanguage, highlightsBuilder) {
        buildHighlightedCode(
            code = normalizedCode,
            language = normalizedLanguage,
            highlightsBuilder = highlightsBuilder,
        )
    }
    val textStyle =
        MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            color = colors.textPrimary
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.bgPrimary)
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.bgSecondary)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            normalizedLanguage?.let { lang ->
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = colors.bgPrimary,
                    contentColor = colors.textSecondary
                ) {
                    Text(
                        text = lang,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { wrapLines = !wrapLines }) {
                Text(if (wrapLines) "No wrap" else "Wrap", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = { copyToClipboard(context, normalizedCode) }) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy code block",
                    tint = colors.textSecondary
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
            SelectionContainer {
                Text(
                    text = highlightedCode,
                    modifier =
                        if (wrapLines) {
                            Modifier.fillMaxWidth()
                        } else {
                            Modifier.fillMaxWidth().horizontalScroll(horizontalScroll)
                        },
                    style = textStyle,
                    softWrap = wrapLines
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("code-block", text))
}

private fun buildHighlightedCode(
    code: String,
    language: String?,
    highlightsBuilder: Highlights.Builder,
): AnnotatedString {
    val syntaxLanguage = language?.let { runCatching { SyntaxLanguage.getByName(it) }.getOrNull() }
    val highlightResult =
        runCatching {
            highlightsBuilder
                .code(code)
                .let { builder -> if (syntaxLanguage != null) builder.language(syntaxLanguage) else builder }
                .build()
        }.getOrNull() ?: return AnnotatedString(code)

    return buildAnnotatedString {
        append(highlightResult.getCode())
        highlightResult.getHighlights()
            .filterIsInstance<ColorHighlight>()
            .forEach { token ->
                val start = token.location.start
                val end = token.location.end
                if (start in 0..length && end in 0..length && start < end) {
                    addStyle(SpanStyle(color = Color(token.rgb).copy(alpha = 1f)), start = start, end = end)
                }
            }
        highlightResult.getHighlights()
            .filterIsInstance<BoldHighlight>()
            .forEach { token ->
                val start = token.location.start
                val end = token.location.end
                if (start in 0..length && end in 0..length && start < end) {
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), start = start, end = end)
                }
            }
    }
}

private fun normalizeFenceLanguage(language: String?): String? {
    val raw = language?.trim()?.lowercase().orEmpty()
    if (raw.isBlank()) return null
    return when (language?.lowercase()) {
        "kts" -> "kotlin"
        "kt" -> "kotlin"
        "js" -> "javascript"
        "ts" -> "typescript"
        "py" -> "python"
        "sh", "bash", "zsh" -> "shell"
        "yml" -> "yaml"
        else -> raw
    }
}
