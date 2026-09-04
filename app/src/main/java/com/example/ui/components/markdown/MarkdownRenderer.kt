package com.example.ui.components.markdown

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.platform.testTag
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
import com.example.ui.components.ActfileVideoPlayer
import com.example.ui.components.VideoUrlHelper
import com.example.ui.components.LocalChannelClickHandler
import com.example.ui.components.LocalCommunityClickHandler
import com.example.ui.components.OpenGraphPreview
import com.example.ui.components.extractUrlsFromMarkdown

/**
 * High-performance, modular Markdown Renderer for Jetpack Compose.
 * Uses an AST parsed by [MarkdownParser] with cached node representation via [remember].
 */
@Composable
fun MarkdownRenderer(
    content: String,
    modifier: Modifier = Modifier,
    isMine: Boolean = false,
    compactOpenGraph: Boolean = false,
    truncateChars: Int? = null,
    onMentionClick: ((String) -> Unit)? = null,
    onLinkClick: ((String) -> Unit)? = null,
    onReadMoreClick: (() -> Unit)? = null
) {
    val hideOgPhoto = remember(content) { content.contains("<!--hide_og_photo-->") }
    val cleanedContent = remember(content) { content.replace("<!--hide_og_photo-->", "").trim() }

    val displayContent = remember(cleanedContent, truncateChars) {
        if (truncateChars != null && cleanedContent.length > truncateChars) {
            cleanedContent.take(truncateChars) + "..."
        } else {
            cleanedContent
        }
    }

    // Cache parsed Markdown AST to prevent unnecessary re-parsing on recomposition
    val nodes = remember(displayContent) {
        MarkdownParser.parse(displayContent)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = androidx.compose.material3.LocalContentColor.current
    val secondaryTextColor = textColor.copy(alpha = 0.7f)

    Column(
        modifier = modifier.testTag("markdown_renderer_container"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        nodes.forEach { node ->
            when (node) {
                is MarkdownNode.Heading -> MarkdownHeadingNode(
                    node = node,
                    isMine = isMine,
                    primaryColor = primaryColor,
                    onMentionClick = onMentionClick,
                    onLinkClick = onLinkClick
                )

                is MarkdownNode.Blockquote -> MarkdownBlockquoteNode(
                    node = node,
                    isMine = isMine,
                    primaryColor = primaryColor,
                    secondaryTextColor = secondaryTextColor,
                    onMentionClick = onMentionClick,
                    onLinkClick = onLinkClick
                )

                is MarkdownNode.CodeBlock -> MarkdownCodeBlockNode(
                    node = node,
                    isMine = isMine
                )

                is MarkdownNode.BulletList -> MarkdownBulletListNode(
                    node = node,
                    isMine = isMine,
                    primaryColor = primaryColor,
                    textColor = textColor,
                    onMentionClick = onMentionClick,
                    onLinkClick = onLinkClick
                )

                is MarkdownNode.NumberedList -> MarkdownNumberedListNode(
                    node = node,
                    isMine = isMine,
                    primaryColor = primaryColor,
                    textColor = textColor,
                    onMentionClick = onMentionClick,
                    onLinkClick = onLinkClick
                )

                is MarkdownNode.Checklist -> MarkdownChecklistNode(
                    node = node,
                    isMine = isMine,
                    primaryColor = primaryColor,
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor,
                    onMentionClick = onMentionClick,
                    onLinkClick = onLinkClick
                )

                is MarkdownNode.ImageNode -> MarkdownImageNode(
                    node = node
                )

                is MarkdownNode.CarouselNode -> MarkdownCarouselNode(
                    node = node
                )

                is MarkdownNode.VideoNode -> MarkdownVideoNode(
                    node = node
                )

                is MarkdownNode.Paragraph -> MarkdownParagraphNode(
                    node = node,
                    isMine = isMine,
                    primaryColor = primaryColor,
                    textColor = textColor,
                    onMentionClick = onMentionClick,
                    onLinkClick = onLinkClick
                )

                is MarkdownNode.HorizontalRule -> MarkdownHorizontalRuleNode(
                    isMine = isMine
                )
            }
        }

        if (truncateChars != null && content.length > truncateChars) {
            Text(
                text = "Voir plus",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .clickable { onReadMoreClick?.invoke() }
                    .testTag("markdown_read_more_button")
            )
        }

        // Extract URLs for OpenGraph cards
        val urls = remember(displayContent) { extractUrlsFromMarkdown(displayContent) }
        val hasVideo = remember(urls) { urls.any { VideoUrlHelper.isVideoUrl(it) } }
        
        if (!hasVideo && urls.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            urls.forEach { url ->
                OpenGraphPreview(
                    url = url,
                    compact = compactOpenGraph,
                    hideImage = hideOgPhoto,
                    onLinkClick = onLinkClick,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MarkdownHeadingNode(
    node: MarkdownNode.Heading,
    isMine: Boolean,
    primaryColor: Color,
    onMentionClick: ((String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?
) {
    val style = when (node.level) {
        1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
        else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
    val headerTextColor = if (isMine) Color.White else when (node.level) {
        1 -> primaryColor
        2 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val formattedText = rememberRichMarkdownStyles(node.text, primaryColor, isMine)
    MarkdownRenderedText(
        annotatedString = formattedText,
        style = style,
        textColor = headerTextColor,
        onMentionClick = onMentionClick,
        onLinkClick = onLinkClick,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun MarkdownBlockquoteNode(
    node: MarkdownNode.Blockquote,
    isMine: Boolean,
    primaryColor: Color,
    secondaryTextColor: Color,
    onMentionClick: ((String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?
) {
    val quoteBg = if (isMine) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val barColor = if (isMine) Color.White else primaryColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
            .background(quoteBg)
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .height(IntrinsicSize.Min)
                .background(barColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        val formattedText = rememberRichMarkdownStyles(node.text, primaryColor, isMine)
        MarkdownRenderedText(
            annotatedString = formattedText,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontStyle = FontStyle.Italic,
                lineHeight = 20.sp
            ),
            onMentionClick = onMentionClick,
            onLinkClick = onLinkClick,
            textColor = secondaryTextColor
        )
    }
}

@Composable
private fun MarkdownCodeBlockNode(
    node: MarkdownNode.CodeBlock,
    isMine: Boolean
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val codeBg = if (isMine) Color.Black.copy(alpha = 0.25f) else Color(0xFF1E1E1E)
    val codeTextColor = if (isMine) Color.White else Color(0xFFD4D4D4)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(codeBg)
            .padding(8.dp)
            .testTag("markdown_code_block")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = node.language?.uppercase() ?: "CODE",
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
                        clipboardManager.setText(AnnotatedString(node.code))
                        Toast.makeText(context, "Code copié !", Toast.LENGTH_SHORT).show()
                    }
                    .testTag("markdown_copy_code_button")
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = node.code,
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

@Composable
private fun MarkdownBulletListNode(
    node: MarkdownNode.BulletList,
    isMine: Boolean,
    primaryColor: Color,
    textColor: Color,
    onMentionClick: ((String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        node.items.forEach { item ->
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
                val formattedText = rememberRichMarkdownStyles(item, primaryColor, isMine)
                MarkdownRenderedText(
                    annotatedString = formattedText,
                    style = MaterialTheme.typography.bodyMedium,
                    textColor = textColor,
                    onMentionClick = onMentionClick,
                    onLinkClick = onLinkClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MarkdownNumberedListNode(
    node: MarkdownNode.NumberedList,
    isMine: Boolean,
    primaryColor: Color,
    textColor: Color,
    onMentionClick: ((String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        node.items.forEachIndexed { index, item ->
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
                val formattedText = rememberRichMarkdownStyles(item, primaryColor, isMine)
                MarkdownRenderedText(
                    annotatedString = formattedText,
                    style = MaterialTheme.typography.bodyMedium,
                    textColor = textColor,
                    onMentionClick = onMentionClick,
                    onLinkClick = onLinkClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MarkdownChecklistNode(
    node: MarkdownNode.Checklist,
    isMine: Boolean,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    onMentionClick: ((String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        node.items.forEach { item ->
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
                val formattedText = rememberRichMarkdownStyles(item.text, primaryColor, isMine)
                MarkdownRenderedText(
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
                    onLinkClick = onLinkClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MarkdownImageNode(
    node: MarkdownNode.ImageNode
) {
    if (VideoUrlHelper.isVideoUrl(node.url)) {
        ActfileVideoPlayer(
            videoUrl = node.url,
            title = node.altText,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    } else {
        AsyncImage(
            model = node.url,
            contentDescription = node.altText ?: "Markdown Image",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .testTag("markdown_image_node"),
            contentScale = ContentScale.FillWidth
        )
    }
}

@Composable
private fun MarkdownVideoNode(
    node: MarkdownNode.VideoNode
) {
    ActfileVideoPlayer(
        videoUrl = node.url,
        title = node.title,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun MarkdownParagraphNode(
    node: MarkdownNode.Paragraph,
    isMine: Boolean,
    primaryColor: Color,
    textColor: Color,
    onMentionClick: ((String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?
) {
    val formattedText = rememberRichMarkdownStyles(node.text, primaryColor, isMine)
    MarkdownRenderedText(
        annotatedString = formattedText,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 19.sp),
        onMentionClick = onMentionClick,
        onLinkClick = onLinkClick,
        textColor = textColor
    )
}

@Composable
private fun MarkdownHorizontalRuleNode(
    isMine: Boolean
) {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("markdown_horizontal_rule"),
        thickness = 1.dp,
        color = if (isMine) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
fun MarkdownRenderedText(
    annotatedString: AnnotatedString,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    onMentionClick: ((String) -> Unit)? = null,
    onLinkClick: ((String) -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val communityClickHandler = LocalCommunityClickHandler.current
    val channelClickHandler = LocalChannelClickHandler.current

    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = style.copy(color = textColor),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    val url = annotation.item
                    if (onLinkClick != null) {
                        onLinkClick(url)
                    } else {
                        try {
                            uriHandler.openUri(url)
                        } catch (e: Exception) {
                            // ignore gracefully
                        }
                    }
                    return@ClickableText
                }

            annotatedString.getStringAnnotations(tag = "MENTION", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onMentionClick?.invoke(annotation.item)
                    return@ClickableText
                }

            annotatedString.getStringAnnotations(tag = "COMMUNITY", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    communityClickHandler?.invoke(annotation.item)
                    return@ClickableText
                }

            annotatedString.getStringAnnotations(tag = "CHANNEL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    channelClickHandler?.invoke(annotation.item)
                    return@ClickableText
                }
        }
    )
}

@Composable
fun rememberRichMarkdownStyles(
    text: String,
    primaryColor: Color,
    isMine: Boolean = false
): AnnotatedString {
    val bodyFontSize = MaterialTheme.typography.bodyMedium.fontSize
    return remember(text, primaryColor, isMine, bodyFontSize) {
        buildAnnotatedString {
            var i = 0
            val linkColor = if (isMine) Color.White else primaryColor
            val codeBgColor = if (isMine) Color.White.copy(alpha = 0.22f) else primaryColor.copy(alpha = 0.08f)
            val codeTextColor = if (isMine) Color.White else primaryColor

            while (i < text.length) {
                // Mentions: @username, @c/community, @#channel
                if (text[i] == '@') {
                    val mentionRegex = Regex("^(@([a-zA-Z0-9_.]+)|@c/([a-z0-9_]+)|@#([a-z0-9-]+))")
                    val match = mentionRegex.find(text.substring(i))
                    if (match != null) {
                        val fullMatch = match.value

                        val mentionType = when {
                            fullMatch.startsWith("@c/") -> "COMMUNITY"
                            fullMatch.startsWith("@#") -> "CHANNEL"
                            else -> "MENTION"
                        }
                        val rawValue = when {
                            fullMatch.startsWith("@c/") -> fullMatch.substring(3)
                            fullMatch.startsWith("@#") -> fullMatch.substring(2)
                            else -> fullMatch.substring(1)
                        }

                        val mentionColor = when (mentionType) {
                            "COMMUNITY" -> Color(0xFF8B5CF6)
                            "CHANNEL" -> Color(0xFF10B981)
                            else -> linkColor
                        }

                        pushStringAnnotation(tag = mentionType, annotation = rawValue)
                        withStyle(
                            SpanStyle(
                                color = mentionColor,
                                fontWeight = FontWeight.ExtraBold,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(fullMatch)
                        }
                        pop()
                        i += fullMatch.length
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
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = codeBgColor,
                                color = codeTextColor,
                                fontSize = bodyFontSize * 0.9f
                            )
                        ) {
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
                                withStyle(
                                    SpanStyle(
                                        color = linkColor,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
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
                val rawUrlMatch = com.example.ui.components.parseRawUrlAt(text, i)
                if (rawUrlMatch != null) {
                    pushStringAnnotation(tag = "URL", annotation = rawUrlMatch)
                    withStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
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
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MarkdownCarouselNode(
    node: MarkdownNode.CarouselNode
) {
    if (node.images.isEmpty()) return

    if (node.images.size == 1) {
        MarkdownImageNode(node = node.images.first())
        return
    }

    val pagerState = rememberPagerState(
        pageCount = { node.images.size }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .testTag("markdown_carousel_node")
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val imageNode = node.images[page]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = com.example.utils.UrlHelper.fixCloudinaryUrl(imageNode.url),
                    contentDescription = imageNode.altText ?: "Carousel Image ${page + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("markdown_carousel_image_$page"),
                    contentScale = ContentScale.FillWidth
                )
            }
        }

        // Image counter badge e.g. "1/3"
        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Text(
                text = "${pagerState.currentPage + 1}/${node.images.size}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Bottom Page Indicator Dots
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(node.images.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f)
                        )
                )
            }
        }
    }
}
