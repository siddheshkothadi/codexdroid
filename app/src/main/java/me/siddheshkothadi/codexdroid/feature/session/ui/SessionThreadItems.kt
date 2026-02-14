package me.siddheshkothadi.codexdroid.feature.session.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halilibo.richtext.commonmark.CommonMarkdownParseOptions
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.markdown.AstBlockNodeComposer
import com.halilibo.richtext.markdown.node.AstBlockNodeType
import com.halilibo.richtext.markdown.node.AstFencedCodeBlock
import com.halilibo.richtext.markdown.node.AstIndentedCodeBlock
import com.halilibo.richtext.markdown.node.AstNode
import com.halilibo.richtext.ui.BlockQuoteGutter
import com.halilibo.richtext.ui.CodeBlockStyle
import com.halilibo.richtext.ui.ListStyle
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.TableStyle
import com.halilibo.richtext.ui.string.RichTextStringStyle
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
    RichText(
        modifier = modifier,
        style = rememberCodexMarkdownStyle(),
    ) {
        Markdown(
            content = markdown,
            markdownParseOptions = CommonMarkdownParseOptions.Default.copy(autolink = true),
            astBlockNodeComposer = rememberCodexAstBlockNodeComposer(),
        )
    }
}

@Composable
private fun rememberCodexAstBlockNodeComposer(): AstBlockNodeComposer {
    return remember {
        object : AstBlockNodeComposer {
            override fun predicate(astBlockNodeType: AstBlockNodeType): Boolean {
                return astBlockNodeType is AstFencedCodeBlock || astBlockNodeType is AstIndentedCodeBlock
            }

            @Composable
            override fun com.halilibo.richtext.ui.RichTextScope.Compose(
                astNode: AstNode,
                visitChildren: @Composable (AstNode) -> Unit
            ) {
                when (val nodeType = astNode.type) {
                    is AstFencedCodeBlock ->
                        MarkdownCodeBlock(
                            code = nodeType.literal,
                            language = parseFenceLanguage(nodeType.info),
                        )
                    is AstIndentedCodeBlock ->
                        MarkdownCodeBlock(
                            code = nodeType.literal,
                            language = null,
                        )
                    else -> visitChildren(astNode)
                }
            }
        }
    }
}

@Composable
private fun MarkdownCodeBlock(code: String, language: String?) {
    val colors = CodexTheme.colors
    val context = LocalContext.current
    val normalizedCode = remember(code) { code.trimEnd('\n', '\r') }
    var wrapLines by rememberSaveable(normalizedCode, language) { mutableStateOf(false) }
    val horizontalScroll = rememberScrollState()
    val highlightedCode = remember(normalizedCode, language, colors) {
        buildHighlightedCode(
            code = normalizedCode,
            language = language,
            colors = colors,
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
            language?.let { lang ->
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

private fun parseFenceLanguage(info: String): String? {
    val raw =
        info
            .trim()
            .lineSequence()
            .firstOrNull()
            .orEmpty()
            .trim()
    if (raw.isBlank()) return null
    return raw.split(Regex("\\s+")).firstOrNull()?.takeIf { it.isNotBlank() }?.lowercase()
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("code-block", text))
}

private fun buildHighlightedCode(
    code: String,
    language: String?,
    colors: me.siddheshkothadi.codexdroid.ui.theme.CodexColors,
): AnnotatedString {
    val normalizedLanguage = normalizeLanguage(language)
    val styleKeyword = SpanStyle(color = colors.accentPrimary, fontWeight = FontWeight.SemiBold)
    val styleString = SpanStyle(color = colors.accentSuccess)
    val styleNumber = SpanStyle(color = colors.accentWarning)
    val styleComment = SpanStyle(color = colors.textTertiary)
    val styleType = SpanStyle(color = colors.accentInfo)
    val stylePunctuation = SpanStyle(color = colors.textSecondary)
    val keywords = languageKeywords(normalizedLanguage)

    return buildAnnotatedString {
        var i = 0
        var inBlockComment = false
        while (i < code.length) {
            if (inBlockComment) {
                val end = code.indexOf("*/", startIndex = i)
                val to = if (end == -1) code.length else end + 2
                append(code.substring(i, to))
                addStyle(styleComment, i, to)
                i = to
                inBlockComment = end == -1
                continue
            }

            val current = code[i]
            val next = code.getOrNull(i + 1)

            if (isLineCommentStart(normalizedLanguage, current, next)) {
                val end = code.indexOf('\n', startIndex = i).let { if (it == -1) code.length else it }
                append(code.substring(i, end))
                addStyle(styleComment, i, end)
                i = end
                continue
            }

            if (current == '/' && next == '*') {
                val end = code.indexOf("*/", startIndex = i + 2)
                val to = if (end == -1) code.length else end + 2
                append(code.substring(i, to))
                addStyle(styleComment, i, to)
                i = to
                inBlockComment = end == -1
                continue
            }

            if (current == '"' || current == '\'' || (current == '`' && supportsBacktickStrings(normalizedLanguage))) {
                val quote = current
                var j = i + 1
                var escaped = false
                while (j < code.length) {
                    val c = code[j]
                    if (escaped) {
                        escaped = false
                    } else if (c == '\\') {
                        escaped = true
                    } else if (c == quote) {
                        j += 1
                        break
                    }
                    j += 1
                }
                val end = j.coerceAtMost(code.length)
                append(code.substring(i, end))
                addStyle(styleString, i, end)
                i = end
                continue
            }

            if (current.isDigit()) {
                var j = i + 1
                while (j < code.length && (code[j].isDigit() || code[j] == '.' || code[j] == '_')) j++
                append(code.substring(i, j))
                addStyle(styleNumber, i, j)
                i = j
                continue
            }

            if (current.isLetter() || current == '_') {
                var j = i + 1
                while (j < code.length && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                val token = code.substring(i, j)
                append(token)
                when {
                    token in keywords -> addStyle(styleKeyword, i, j)
                    token.firstOrNull()?.isUpperCase() == true -> addStyle(styleType, i, j)
                }
                i = j
                continue
            }

            append(current)
            if (current in setOf('{', '}', '(', ')', '[', ']', ';', ',', ':')) {
                addStyle(stylePunctuation, i, i + 1)
            }
            i += 1
        }
    }
}

private fun normalizeLanguage(language: String?): String {
    return when (language?.lowercase()) {
        "kts" -> "kotlin"
        "kt" -> "kotlin"
        "js" -> "javascript"
        "ts" -> "typescript"
        "py" -> "python"
        "sh", "bash", "zsh" -> "shell"
        "yml" -> "yaml"
        else -> language?.lowercase().orEmpty()
    }
}

private fun supportsBacktickStrings(language: String): Boolean {
    return language == "javascript" || language == "typescript" || language == "kotlin"
}

private fun isLineCommentStart(language: String, current: Char, next: Char?): Boolean {
    return when {
        current == '/' && next == '/' -> true
        current == '#' && (language == "python" || language == "shell" || language == "yaml") -> true
        else -> false
    }
}

private fun languageKeywords(language: String): Set<String> {
    return when (language) {
        "kotlin" -> setOf(
            "package", "import", "class", "interface", "object", "fun", "val", "var", "if", "else",
            "when", "for", "while", "do", "return", "try", "catch", "finally", "throw", "null",
            "true", "false", "is", "in", "as", "this", "super", "override", "private", "public",
            "internal", "protected", "suspend", "data", "sealed", "enum", "companion"
        )
        "typescript", "javascript" -> setOf(
            "function", "const", "let", "var", "if", "else", "switch", "case", "for", "while",
            "do", "return", "try", "catch", "finally", "throw", "class", "extends", "implements",
            "new", "import", "export", "from", "as", "true", "false", "null", "undefined", "async",
            "await", "type", "interface"
        )
        "python" -> setOf(
            "def", "class", "if", "elif", "else", "for", "while", "return", "try", "except", "finally",
            "raise", "import", "from", "as", "with", "lambda", "True", "False", "None", "pass", "break",
            "continue", "yield", "async", "await"
        )
        "shell" -> setOf(
            "if", "then", "else", "fi", "for", "in", "do", "done", "case", "esac", "while", "function",
            "return", "local", "export"
        )
        "java" -> setOf(
            "package", "import", "class", "interface", "enum", "public", "private", "protected", "static",
            "final", "void", "new", "if", "else", "switch", "case", "for", "while", "return", "try",
            "catch", "finally", "throw", "true", "false", "null"
        )
        "json" -> setOf("true", "false", "null")
        else -> setOf(
            "if", "else", "for", "while", "return", "class", "function", "const", "let", "var", "true",
            "false", "null"
        )
    }
}

@Composable
private fun rememberCodexMarkdownStyle(): RichTextStyle {
    val colors = CodexTheme.colors
    val typography = MaterialTheme.typography
    return remember(colors, typography) {
        RichTextStyle(
            paragraphSpacing = 6.sp,
            headingStyle = { level, _ ->
                when (level) {
                    1 -> typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    2 -> typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    3 -> typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    else -> typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                }
            },
            listStyle = ListStyle(itemSpacing = 3.sp),
            blockQuoteGutter =
                BlockQuoteGutter.BarGutter(
                    color = { colors.borderDefault },
                    startMargin = 4.sp,
                    barWidth = 3.sp,
                    endMargin = 8.sp,
                ),
            codeBlockStyle =
                CodeBlockStyle(
                    textStyle = typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = colors.textPrimary),
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, colors.borderDefault, RoundedCornerShape(8.dp)),
                    padding = 12.sp,
                    wordWrap = false,
                ),
            tableStyle =
                TableStyle(
                    headerTextStyle = typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = colors.textPrimary),
                    cellPadding = 8.sp,
                    borderColor = colors.borderDefault,
                    borderStrokeWidth = 1f,
                ),
            stringStyle =
                RichTextStringStyle(
                    codeStyle =
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            background = Color.Transparent,
                        ),
                    linkStyle =
                        TextLinkStyles(
                            style =
                                SpanStyle(
                                    color = colors.accentPrimary,
                                    textDecoration = TextDecoration.Underline,
                                ),
                            pressedStyle =
                                SpanStyle(
                                    color = colors.accentPrimary.copy(alpha = 0.75f),
                                    textDecoration = TextDecoration.Underline,
                                ),
                        ),
                ),
        )
    }
}

