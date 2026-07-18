package com.example.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownContent(
    content: String,
    modifier: Modifier = Modifier,
    isDetailView: Boolean = false,
    onReadMoreClick: (() -> Unit)? = null,
    onMentionClick: ((String) -> Unit)? = null,
    onLinkClick: ((String) -> Unit)? = null
) {
    // Render the complete rich markdown in all cases as requested
    MarkdownActfile(
        content = content,
        modifier = modifier.testTag("full_markdown_content"),
        onMentionClick = onMentionClick,
        onLinkClick = onLinkClick,
        onReadMoreClick = onReadMoreClick
    )
}
