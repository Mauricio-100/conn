package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.components.markdown.MarkdownRenderer

@Composable
fun MarkdownContent(
    content: String,
    modifier: Modifier = Modifier,
    isDetailView: Boolean = false,
    onReadMoreClick: (() -> Unit)? = null,
    onMentionClick: ((String) -> Unit)? = null,
    onLinkClick: ((String) -> Unit)? = null
) {
    MarkdownRenderer(
        content = content,
        modifier = modifier.testTag("full_markdown_content"),
        onMentionClick = onMentionClick,
        onLinkClick = onLinkClick,
        onReadMoreClick = onReadMoreClick,
        truncateChars = if (isDetailView) null else 250
    )
}

