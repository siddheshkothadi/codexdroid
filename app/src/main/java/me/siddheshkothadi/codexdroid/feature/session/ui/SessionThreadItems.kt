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
import androidx.compose.foundation.isSystemInDarkTheme
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
    val isFullWidthItem = !isUser && item is ThreadItem.AgentMessage
    val isCardLikeItem =
        item is ThreadItem.CommandExecution ||
            item is ThreadItem.Reasoning ||
            item is ThreadItem.PlanUpdate ||
            item is ThreadItem.FileChange ||
            item is ThreadItem.ContextCompaction ||
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
                        CodexMarkdown(markdown = it, modifier = Modifier.fillMaxWidth())
                    }
                }
                item.content.forEach {
                    ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                        CodexMarkdown(markdown = it, modifier = Modifier.fillMaxWidth())
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
                    val trimmedOutput = trimCommandRunOutput(out)
                    if (trimmedOutput.isNotBlank()) {
                        if (item.command.isNotBlank()) Spacer(Modifier.height(8.dp))
                        Text("Output", style = MaterialTheme.typography.labelSmall, color = CodexTheme.colors.textSecondary)
                        Text(
                            trimmedOutput,
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
                        CodexMarkdown(markdown = markdown, modifier = Modifier.fillMaxWidth())
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
fun ContextCompactionItem(item: ThreadItem.ContextCompaction) {
    val colors = CodexTheme.colors
    val isRunning = item.status == ContextCompactionStatus.inProgress || item.status == ContextCompactionStatus.unknown
    val dotColor =
        when (item.status) {
            ContextCompactionStatus.completed -> colors.accentSuccess
            ContextCompactionStatus.failed -> colors.accentError
            ContextCompactionStatus.inProgress, ContextCompactionStatus.unknown -> colors.accentWarning
        }

    val shimmer =
        rememberInfiniteTransition(label = "context_compaction_glow")
            .animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 900, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "context_compaction_alpha",
            )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.bgSecondary)
                .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .background(dotColor, CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Context compaction",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text =
                    if (isRunning) {
                        "Compacting conversation context to fit token limits…"
                    } else if (item.status == ContextCompactionStatus.failed) {
                        "Context compaction failed."
                    } else {
                        "Context compaction completed."
                    },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary.copy(alpha = if (isRunning) shimmer.value else 1f),
            )
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
                        CodexMarkdown(markdown = body, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun CodexMarkdown(markdown: String, modifier: Modifier = Modifier) {
    val colors = CodexTheme.colors
    val typography = MaterialTheme.typography
    val baseTextStyle = LocalTextStyle.current.copy(color = colors.textPrimary)
    val markdownLinkColor = lerp(colors.accentUi, colors.accentPrimary, 0.45f)
    val highlightsBuilder =
        remember(isSystemInDarkTheme()) {
            Highlights.Builder()
        }
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
                linkText = markdownLinkColor,
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
                        color = markdownLinkColor,
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

private fun trimCommandRunOutput(output: String, topLines: Int = 5, bottomLines: Int = 5): String {
    if (output.isBlank()) return ""
    val lines = output.lines()
    val keepCount = topLines + bottomLines
    if (lines.size <= keepCount) return output

    val omitted = lines.size - keepCount
    return buildString {
        append(lines.take(topLines).joinToString("\n"))
        append('\n')
        append("... ($omitted lines omitted) ...")
        append('\n')
        append(lines.takeLast(bottomLines).joinToString("\n"))
    }
}

