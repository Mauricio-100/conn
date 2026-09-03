package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ReactionUiGroup(
    val emoji: String,
    val count: Int,
    val hasReacted: Boolean = false
)

data class ChatMessageUiModel(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val type: String, // "text", "image", "video", "voice", "audio_sending", "audio_error"
    val isMine: Boolean,
    val isRead: Boolean,
    val createdAt: Long,
    val reactions: List<ReactionUiGroup> = emptyList(),
    val isSending: Boolean = false,
    val isFailed: Boolean = false,
    val showAvatar: Boolean = false,
    val isLastInGroup: Boolean = true,
    val isFirstInGroup: Boolean = true
)

/**
 * Modern chat bubble with asymmetric corners, rich multimedia support,
 * reactions chips, and message status ticks.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessageUiModel,
    partnerAvatar: String?,
    partnerUsername: String,
    onLongClick: () -> Unit,
    onReactionClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMine = message.isMine
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val bubbleShape = if (isMine) {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = if (message.isFirstInGroup) 18.dp else 6.dp,
            bottomStart = 18.dp,
            bottomEnd = if (message.isLastInGroup) 4.dp else 6.dp
        )
    } else {
        RoundedCornerShape(
            topStart = if (message.isFirstInGroup) 18.dp else 6.dp,
            topEnd = 18.dp,
            bottomStart = if (message.isLastInGroup) 4.dp else 6.dp,
            bottomEnd = 18.dp
        )
    }

    val bubbleColor = if (isMine) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isMine) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val timeString = remember(message.createdAt) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.createdAt))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = if (message.isFirstInGroup) 6.dp else 2.dp,
                bottom = if (message.isLastInGroup) 6.dp else 2.dp,
                start = 12.dp,
                end = 12.dp
            )
            .testTag("message_row_${message.id}"),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Partner Avatar (displayed only on receiver's side, on the last message of consecutive series)
        if (!isMine) {
            if (message.showAvatar) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!partnerAvatar.isNullOrBlank()) {
                        AsyncImage(
                            model = partnerAvatar,
                            contentDescription = "Avatar de $partnerUsername",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = partnerUsername.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(32.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Bubble + Reactions + Meta container
        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            // Main Bubble
            Surface(
                shape = bubbleShape,
                color = bubbleColor,
                tonalElevation = if (isMine) 0.dp else 1.dp,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .clip(bubbleShape)
                    .combinedClickable(
                        onClick = {
                            when (message.type) {
                                "image" -> onImageClick(message.content)
                                "video" -> onVideoClick(message.content)
                            }
                        },
                        onLongClick = onLongClick
                    )
                    .testTag("message_bubble_${message.id}")
            ) {
                Box(modifier = Modifier.padding(10.dp)) {
                    when {
                        // Voice message
                        message.type == "voice" || message.type == "audio" || isVoiceMessage(message.content) -> {
                            VoiceMessagePlayer(
                                content = message.content,
                                isMine = isMine,
                                modifier = Modifier.widthIn(min = 200.dp)
                            )
                        }

                        // Image message
                        message.type == "image" || isImageUrl(message.content) -> {
                            Column {
                                AsyncImage(
                                    model = message.content,
                                    contentDescription = "Image envoyée",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        // Video message
                        message.type == "video" || isVideoUrl(message.content) -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.8f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleFilled,
                                    contentDescription = "Lire la vidéo",
                                    tint = Color.White,
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }

                        // Sending/Failed audio placeholder
                        message.type == "audio_sending" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = contentColor
                                )
                                Text(
                                    text = "Envoi du vocal en cours…",
                                    color = contentColor,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        message.type == "audio_error" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Échec de l'envoi vocal",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // Standard text message
                        else -> {
                            ClickableUrlText(
                                text = message.content,
                                contentColor = contentColor,
                                onUrlClick = { url ->
                                    try {
                                        uriHandler.openUri(url)
                                    } catch (e: Exception) {
                                        // fallback
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Grouped reactions below bubble
            if (message.reactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.testTag("bubble_reactions_${message.id}")
                ) {
                    message.reactions.forEach { reactionGroup ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (reactionGroup.hasReacted) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            border = BorderStroke(
                                1.dp,
                                if (reactionGroup.hasReacted) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                }
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onReactionClick(reactionGroup.emoji) }
                                .testTag("reaction_group_${message.id}_${reactionGroup.emoji}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(text = reactionGroup.emoji, fontSize = 12.sp)
                                if (reactionGroup.count > 1) {
                                    Text(
                                        text = reactionGroup.count.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (reactionGroup.hasReacted) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Timestamp and delivery status (only on the last message of a series)
            if (message.isLastInGroup) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )

                    // Delivery status for sender's messages: NO read timestamp, strictly "envoyé" vs "lu"
                    if (isMine) {
                        when {
                            message.isSending -> {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Envoi en cours…",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            message.isFailed -> {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Échec de l'envoi",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            message.isRead -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Lu",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = "Envoyé",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clickable text helper detecting URLs with underline/accent and handling navigation.
 */
@Composable
private fun ClickableUrlText(
    text: String,
    contentColor: Color,
    onUrlClick: (String) -> Unit
) {
    val urlPattern = Pattern.compile("(https?://[\\w-]+(\\.[\\w-]+)+(/[\\w-.,/?%&=]*)?)")
    val matcher = remember(text) { urlPattern.matcher(text) }

    val annotatedString = remember(text, contentColor) {
        buildAnnotatedString {
            append(text)
            var lastIndex = 0
            val localMatcher = urlPattern.matcher(text)
            while (localMatcher.find()) {
                val start = localMatcher.start()
                val end = localMatcher.end()
                val url = localMatcher.group()

                addStyle(
                    style = SpanStyle(
                        color = contentColor,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.SemiBold
                    ),
                    start = start,
                    end = end
                )
                addStringAnnotation(
                    tag = "URL",
                    annotation = url,
                    start = start,
                    end = end
                )
            }
        }
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = contentColor,
            lineHeight = 22.sp
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onUrlClick(annotation.item)
                }
        }
    )
}

private fun isImageUrl(content: String): Boolean {
    val lower = content.lowercase().trim()
    return lower.startsWith("http") && (
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                    lower.endsWith(".png") || lower.endsWith(".webp") ||
                    lower.endsWith(".gif")
            )
}

private fun isVideoUrl(content: String): Boolean {
    val lower = content.lowercase().trim()
    return lower.startsWith("http") && (
            lower.endsWith(".mp4") || lower.endsWith(".webm") ||
                    lower.endsWith(".mov") || lower.endsWith(".mkv")
            )
}
