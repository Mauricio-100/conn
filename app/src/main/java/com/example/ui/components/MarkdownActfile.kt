package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import android.widget.Toast

// Markdown Block types
sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class Blockquote(val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String?, val code: String) : MarkdownBlock()
    data class BulletList(val items: List<String>) : MarkdownBlock()
    data class NumberedList(val items: List<String>) : MarkdownBlock()
    data class Checklist(val items: List<ChecklistItem>) : MarkdownBlock()
    data class ImageBlock(val url: String, val altText: String?) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

data class ChecklistItem(val isChecked: Boolean, val text: String)

@Composable
fun MarkdownActfile(
    content: String,
    modifier: Modifier = Modifier,
    isMine: Boolean = false,
    compactOpenGraph: Boolean = false,
    onMentionClick: ((String) -> Unit)? = null
) {
    if (isVoiceMessage(content)) {
        VoiceMessagePlayer(
            content = content,
            modifier = modifier,
            isMine = isMine
        )
        return
    }

    val blocks = rememberParsedMarkdown(content)
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    val headerTextColor = if (isMine) Color.White else when (block.level) {
                        1 -> primaryColor
                        2 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    val formattedText = parseRichMarkdownStyles(block.text, primaryColor, isMine)
                    MarkdownText(
                        annotatedString = formattedText,
                        style = style,
                        textColor = headerTextColor,
                        onMentionClick = onMentionClick,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                
                is MarkdownBlock.Blockquote -> {
                    val quoteBg = if (isMine) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    val barColor = if (isMine) Color.White else primaryColor
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                            .background(quoteBg)
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    ) {
                        // Left border bar
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .height(IntrinsicSize.Min)
                                .background(barColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        val formattedText = parseRichMarkdownStyles(block.text, primaryColor, isMine)
                        MarkdownText(
                            annotatedString = formattedText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                lineHeight = 20.sp
                            ),
                            onMentionClick = onMentionClick,
                            textColor = secondaryTextColor
                        )
                    }
                }
                
                is MarkdownBlock.CodeBlock -> {
                    val codeBg = if (isMine) Color.Black.copy(alpha = 0.25f) else Color(0xFF1E1E1E)
                    val codeTextColor = if (isMine) Color.White else Color(0xFFD4D4D4)
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(codeBg)
                            .padding(8.dp)
                    ) {
                        // Title bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = block.language?.uppercase() ?: "CODE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = if (isMine) Color.White.copy(alpha = 0.6f) else Color.Gray
                            )
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy code",
                                tint = if (isMine) Color.White.copy(alpha = 0.7f) else Color.LightGray,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(block.code))
                                        Toast.makeText(context, "Code copié !", Toast.LENGTH_SHORT).show()
                                    }
                            )
                        }
                        
                        // Scrollable code content
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = block.code,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 16.sp
                                ),
                                color = codeTextColor,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
                
                is MarkdownBlock.BulletList -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        block.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = " • ",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isMine) Color.White else primaryColor,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                val formattedText = parseRichMarkdownStyles(item, primaryColor, isMine)
                                MarkdownText(
                                    annotatedString = formattedText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textColor = textColor,
                                    onMentionClick = onMentionClick,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                
                is MarkdownBlock.NumberedList -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        block.items.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = " ${index + 1}. ",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isMine) Color.White else primaryColor,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                val formattedText = parseRichMarkdownStyles(item, primaryColor, isMine)
                                MarkdownText(
                                    annotatedString = formattedText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textColor = textColor,
                                    onMentionClick = onMentionClick,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                
                is MarkdownBlock.Checklist -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        block.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = if (item.isChecked) "Completed" else "Incomplete",
                                    tint = if (isMine) Color.White else if (item.isChecked) primaryColor else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(end = 4.dp)
                                )
                                val formattedText = parseRichMarkdownStyles(item.text, primaryColor, isMine)
                                MarkdownText(
                                    annotatedString = formattedText,
                                    style = MaterialTheme.typography.bodyMedium.let {
                                        if (item.isChecked) {
                                            it.copy(textDecoration = TextDecoration.LineThrough)
                                        } else {
                                            it
                                        }
                                    },
                                    textColor = if (item.isChecked) secondaryTextColor else textColor,
                                    onMentionClick = onMentionClick,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                
                is MarkdownBlock.ImageBlock -> {
                    AsyncImage(
                        model = block.url,
                        contentDescription = block.altText ?: "Image Markdown",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        contentScale = ContentScale.FillWidth
                    )
                }
                
                is MarkdownBlock.Paragraph -> {
                    val formattedText = parseRichMarkdownStyles(block.text, primaryColor, isMine)
                    MarkdownText(
                        annotatedString = formattedText,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 19.sp),
                        onMentionClick = onMentionClick,
                        textColor = textColor
                    )
                }
            }
        }

        // OpenGraph URL Preview Integration!
        // We scan for links in the raw content, filter duplicates, and render a gorgeous preview card for each!
        val urls = remember(content) { extractUrlsFromMarkdown(content) }
        if (urls.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            urls.forEach { url ->
                OpenGraphPreview(
                    url = url,
                    compact = compactOpenGraph,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun rememberParsedMarkdown(content: String): List<MarkdownBlock> {
    return androidx.compose.runtime.remember(content) {
        parseMarkdownBlocks(content)
    }
}

@Composable
fun MarkdownText(
    annotatedString: AnnotatedString,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    onMentionClick: ((String) -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    
    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = style.copy(color = textColor),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        uriHandler.openUri(annotation.item)
                    } catch (e: Exception) {
                        // ignore gracefully
                    }
                }
            
            annotatedString.getStringAnnotations(tag = "MENTION", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onMentionClick?.invoke(annotation.item)
                }
        }
    )
}

// Inline formatting parser
@Composable
fun parseRichMarkdownStyles(
    text: String,
    primaryColor: Color,
    isMine: Boolean = false
): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val linkColor = if (isMine) Color.White else primaryColor
        val codeBgColor = if (isMine) Color.White.copy(alpha = 0.22f) else primaryColor.copy(alpha = 0.08f)
        val codeTextColor = if (isMine) Color.White else primaryColor
        
        while (i < text.length) {
            // Mention @username
            if (text[i] == '@') {
                var end = i + 1
                while (end < text.length && (text[end].isLetterOrDigit() || text[end] == '_' || text[end] == '.')) {
                    end++
                }
                val len = end - i
                if (len > 1) {
                    val username = text.substring(i + 1, end)
                    pushStringAnnotation(tag = "MENTION", annotation = username)
                    withStyle(SpanStyle(
                        color = linkColor,
                        fontWeight = FontWeight.ExtraBold,
                        textDecoration = TextDecoration.Underline
                    )) {
                        append("@$username")
                    }
                    pop()
                    i = end
                    continue
                }
            }

            // Bold **
            if (i < text.length - 1 && text[i] == '*' && text[i + 1] == '*') {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }
            // Bold __
            if (i < text.length - 1 && text[i] == '_' && text[i + 1] == '_') {
                val end = text.indexOf("__", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }
            // Italic *
            if (text[i] == '*') {
                val end = text.indexOf("*", i + 1)
                if (end != -1 && end > i + 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }
            // Italic _
            if (text[i] == '_') {
                val end = text.indexOf("_", i + 1)
                if (end != -1 && end > i + 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }
            // Strikethrough ~~
            if (i < text.length - 1 && text[i] == '~' && text[i + 1] == '~') {
                val end = text.indexOf("~~", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }
            // Inline code `
            if (text[i] == '`') {
                val end = text.indexOf("`", i + 1)
                if (end != -1 && end > i + 1) {
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBgColor,
                        color = codeTextColor,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize * 0.9f
                    )) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }
            // Link [text](url)
            if (text[i] == '[') {
                val closingBracket = text.indexOf(']', i + 1)
                if (closingBracket != -1) {
                    if (closingBracket < text.length - 1 && text[closingBracket + 1] == '(') {
                        val closingParen = text.indexOf(')', closingBracket + 2)
                        if (closingParen != -1) {
                            val linkText = text.substring(i + 1, closingBracket)
                            val url = text.substring(closingBracket + 2, closingParen)
                            
                            pushStringAnnotation(tag = "URL", annotation = url)
                            withStyle(SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Bold
                            )) {
                                append(linkText)
                            }
                            pop()
                            i = closingParen + 1
                            continue
                        }
                    }
                }
            }
            
            // Raw HTTP URL check
            val rawUrlMatch = parseRawUrlAt(text, i)
            if (rawUrlMatch != null) {
                pushStringAnnotation(tag = "URL", annotation = rawUrlMatch)
                withStyle(SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Bold
                )) {
                    append(rawUrlMatch)
                }
                pop()
                i += rawUrlMatch.length
                continue
            }
            
            append(text[i].toString())
            i++
        }
    }
}

// Helper to check if there's a raw URL at current index
fun parseRawUrlAt(text: String, index: Int): String? {
    if (index + 7 > text.length) return null
    val substring = text.substring(index)
    if (substring.startsWith("http://") || substring.startsWith("https://")) {
        val endOfUrl = substring.indexOfFirst { it.isWhitespace() || it == ')' || it == ']' || it == '}' || it == '*' || it == '~' }
        val len = if (endOfUrl == -1) substring.length else endOfUrl
        return substring.substring(0, len)
    }
    return null
}

// Block-by-block Markdown parser
fun parseMarkdownBlocks(content: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = content.lines()
    var i = 0
    
    while (i < lines.size) {
        val line = lines[i]
        
        // 1. Code blocks
        if (line.trimStart().startsWith("```")) {
            val lang = line.trimStart().removePrefix("```").trim().ifEmpty { null }
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n")))
            i++
            continue
        }
        
        // 2. Blockquotes
        if (line.trimStart().startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            quoteLines.add(line.trimStart().removePrefix(">").trim())
            i++
            while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                quoteLines.add(lines[i].trimStart().removePrefix(">").trim())
                i++
            }
            blocks.add(MarkdownBlock.Blockquote(quoteLines.joinToString("\n")))
            continue
        }
        
        // 3. Headings
        if (line.trimStart().startsWith("#")) {
            val trimmed = line.trimStart()
            var level = 0
            while (level < trimmed.length && trimmed[level] == '#') {
                level++
            }
            if (level in 1..6 && level < trimmed.length && trimmed[level].isWhitespace()) {
                val headerText = trimmed.substring(level).trim()
                blocks.add(MarkdownBlock.Heading(level, headerText))
                i++
                continue
            }
        }
        
        // 4. Checklist items
        if (line.trimStart().startsWith("- [ ]") || line.trimStart().startsWith("- [x]") ||
            line.trimStart().startsWith("* [ ]") || line.trimStart().startsWith("* [x]")) {
            val checklistItems = mutableListOf<ChecklistItem>()
            var currentLine = line.trimStart()
            while (i < lines.size && (currentLine.startsWith("- [ ]") || currentLine.startsWith("- [x]") ||
                    currentLine.startsWith("* [ ]") || currentLine.startsWith("* [x]"))) {
                val isChecked = currentLine.contains("[x]") || currentLine.contains("[X]")
                val text = currentLine.substring(currentLine.indexOf(']') + 1).trim()
                checklistItems.add(ChecklistItem(isChecked, text))
                i++
                if (i < lines.size) currentLine = lines[i].trimStart()
            }
            blocks.add(MarkdownBlock.Checklist(checklistItems))
            continue
        }
        
        // 5. Bullet lists
        if (line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") || line.trimStart().startsWith("• ")) {
            val bulletItems = mutableListOf<String>()
            var currentLine = line.trimStart()
            while (i < lines.size && (currentLine.startsWith("- ") || currentLine.startsWith("* ") || currentLine.startsWith("• "))) {
                if (!currentLine.startsWith("- [") && !currentLine.startsWith("* [")) {
                    val text = currentLine.substring(2).trim()
                    bulletItems.add(text)
                }
                i++
                if (i < lines.size) currentLine = lines[i].trimStart()
            }
            blocks.add(MarkdownBlock.BulletList(bulletItems))
            continue
        }
        
        // 6. Numbered lists
        val numberRegex = Regex("^\\d+\\.\\s+(.*)")
        if (numberRegex.matches(line.trimStart())) {
            val numberedItems = mutableListOf<String>()
            var currentLine = line.trimStart()
            while (i < lines.size && numberRegex.matches(currentLine)) {
                val match = numberRegex.matchEntire(currentLine)
                val text = match?.groupValues?.get(1) ?: ""
                numberedItems.add(text)
                i++
                if (i < lines.size) currentLine = lines[i].trimStart()
            }
            blocks.add(MarkdownBlock.NumberedList(numberedItems))
            continue
        }
        
        // 7. Image Block ([IMG](url) or ![alt](url))
        val imgRegex = Regex("(?i)^\\[img\\]\\((.*?)\\)$")
        val altImgRegex = Regex("^!\\[(.*?)\\]\\((.*?)\\)$")
        val trimLine = line.trim()
        if (imgRegex.matches(trimLine)) {
            val url = imgRegex.matchEntire(trimLine)!!.groupValues[1]
            blocks.add(MarkdownBlock.ImageBlock(url, null))
            i++
            continue
        } else if (altImgRegex.matches(trimLine)) {
            val match = altImgRegex.matchEntire(trimLine)!!
            val alt = match.groupValues[1]
            val url = match.groupValues[2]
            blocks.add(MarkdownBlock.ImageBlock(url, alt))
            i++
            continue
        }
        
        // 8. Normal Paragraph (splits inline [IMG] or ![alt] visual attachments gracefully)
        if (line.trim().isNotEmpty()) {
            val mixed = parseMixedParagraph(line)
            blocks.addAll(mixed)
        }
        i++
    }
    
    return blocks
}

// Parses visual image splits inside paragraphs
fun parseMixedParagraph(text: String): List<MarkdownBlock> {
    val regex = Regex("(?i)(?:\\[img\\]|!\\[(.*?)\\])\\((.*?)\\)")
    val blocks = mutableListOf<MarkdownBlock>()
    var lastIndex = 0
    val matches = regex.findAll(text)
    
    for (match in matches) {
        val textBefore = text.substring(lastIndex, match.range.first)
        if (textBefore.trim().isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(textBefore))
        }
        val altText = match.groupValues[1].ifEmpty { null }
        val url = match.groupValues[2]
        blocks.add(MarkdownBlock.ImageBlock(url, altText))
        lastIndex = match.range.last + 1
    }
    
    if (lastIndex < text.length) {
        val remaining = text.substring(lastIndex)
        if (remaining.trim().isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(remaining))
        }
    }
    
    return blocks
}

// Helper to extract clean urls from markdown text for OpenGraph rendering
fun extractUrlsFromMarkdown(content: String): List<String> {
    val urls = mutableListOf<String>()
    
    // 1. Extract markdown links: [text](url)
    val markdownLinkRegex = Regex("\\[.*?\\]\\((https?://[^\n\\s)]+)\\)")
    markdownLinkRegex.findAll(content).forEach { match ->
        val url = match.groupValues[1]
        if (!urls.contains(url)) urls.add(url)
    }
    
    // 2. Extract raw URLs: https://... or http://...
    val rawUrlRegex = Regex("(?<!\\()(https?://[^\\s\\n\\])]+)")
    rawUrlRegex.findAll(content).forEach { match ->
        val url = match.groupValues[1]
        // Clean trailing punctuation
        var cleanUrl = url
        while (cleanUrl.endsWith(".") || cleanUrl.endsWith(",") || cleanUrl.endsWith("?") || cleanUrl.endsWith("!")) {
            cleanUrl = cleanUrl.dropLast(1)
        }
        if (!urls.contains(cleanUrl)) urls.add(cleanUrl)
    }
    
    return urls
}
