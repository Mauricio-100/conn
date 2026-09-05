package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ActfileWithUser
import com.example.data.getCategoryDefaultIcon
import com.example.ui.components.VerificationBadge
import com.example.ui.components.CopyableUserId
import com.example.ui.components.MarkdownContent
import com.example.ui.components.TagChip
import kotlinx.coroutines.launch

// Helper to calculate relative time
fun getRelativeTimeString(timeMs: Long): String {
    return try {
        android.text.format.DateUtils.getRelativeTimeSpanString(
            timeMs,
            System.currentTimeMillis(),
            android.text.format.DateUtils.MINUTE_IN_MILLIS
        ).toString()
    } catch (e: Exception) {
        val diffSec = (System.currentTimeMillis() - timeMs) / 1000
        when {
            diffSec < 60 -> "à l'instant"
            diffSec < 3600 -> "il y a ${diffSec / 60}m"
            diffSec < 86400 -> "il y a ${diffSec / 3600}h"
            else -> "il y a ${diffSec / 86400}j"
        }
    }
}

@Composable
fun ActfileCard(
    actfile: ActfileWithUser,
    onLike: (String) -> Unit,
    onView: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onComment: (String) -> Unit,
    targetLanguageName: String = "French",
    isAiReady: Boolean = false,
    onLinkClick: ((String) -> Unit)? = null,
    onDelete: ((String) -> Unit)? = null,
    onMentionClick: ((String) -> Unit)? = null,
    onCategoryClick: ((String) -> Unit)? = null,
    onShare: ((String) -> Unit)? = null,
    isDetailView: Boolean = false
) {
    val context = LocalContext.current
    var translatedContent by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var showOriginal by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val relativeTime = remember(actfile.createdAt) { getRelativeTimeString(actfile.createdAt) }
    
    // Animated like scale
    val likeScale by animateFloatAsState(
        targetValue = if (actfile.isLikedByMe) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "like_scale"
    )
    val likeColor by animateColorAsState(
        targetValue = if (actfile.isLikedByMe) Color(0xFFFF2D55) else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "like_color"
    )

    // Automatically register views
    LaunchedEffect(actfile.id) {
        onView(actfile.id)
    }

    val isCommunityPost = !actfile.communityId.isNullOrBlank() || !actfile.channelSlug.isNullOrBlank()
    val avatarSize = if (isDetailView) 52.dp else 42.dp
    val miniAvatarSize = 20.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onView(actfile.id) }
            .testTag("actfile_card_${actfile.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isCommunityPost) {
                    // Standard Author Avatar
                    Box(
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onUserClick(actfile.userId) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!actfile.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = actfile.avatarUrl?.let { com.example.utils.UrlHelper.fixCloudinaryUrl(it) },
                                contentDescription = "Avatar de ${actfile.username}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = actfile.username.firstOrNull()?.toString()?.uppercase() ?: "?",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Username + VerificationBadge + Timestamp relative
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onUserClick(actfile.userId) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = actfile.username,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            VerificationBadge(
                                userName = actfile.username,
                                isVerified = actfile.isVerified,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Text(
                            text = "@${actfile.username} • $relativeTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        CopyableUserId(
                            id = actfile.userId,
                            isBot = actfile.username.contains("bot", ignoreCase = true)
                        )
                    }

                } else {
                    // CAS 2 : Publication communautaire
                    val commIconUrl = remember(actfile.category) {
                        getCategoryDefaultIcon(actfile.category ?: "general")
                    }

                    Box(
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { onCategoryClick?.invoke(actfile.channelSlug ?: "") },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = commIconUrl,
                            contentDescription = "Communauté ${actfile.channelSlug}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Community Details + Author secondary line
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Line 1: c/slug in bold + optional channel chip
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "c/${actfile.channelSlug ?: "communaute"}",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onCategoryClick?.invoke(actfile.channelSlug ?: "") }
                            )

                            if (!actfile.channelName.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "#${actfile.channelName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Line 2: mini-avatar + Username + VerificationBadge + Timestamp
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUserClick(actfile.userId) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(miniAvatarSize)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!actfile.avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = actfile.avatarUrl?.let { com.example.utils.UrlHelper.fixCloudinaryUrl(it) },
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = actfile.username.firstOrNull()?.toString()?.uppercase() ?: "?",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = actfile.username,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            VerificationBadge(
                                userName = actfile.username,
                                isVerified = actfile.isVerified,
                                modifier = Modifier.size(12.dp)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = "• $relativeTime",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))
                        CopyableUserId(
                            id = actfile.userId,
                            isBot = actfile.username.contains("bot", ignoreCase = true)
                        )
                    }
                }

                // View count pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.padding(end = if (onDelete != null) 4.dp else 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = "Vues",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${com.example.utils.FormatUtils.formatCount(actfile.viewsCount)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Delete button for post owner
                if (onDelete != null) {
                    IconButton(
                        onClick = { onDelete(actfile.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Supprimer la publication",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body content & Markdown
            MarkdownContent(
                content = (if (showOriginal) actfile.content else translatedContent) ?: actfile.content,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onComment(actfile.id) }
                    .testTag("actfile_body_${actfile.id}"),
                isDetailView = isDetailView,
                onReadMoreClick = { onComment(actfile.id) },
                onMentionClick = onMentionClick,
                onLinkClick = onLinkClick
            )

            // Dynamic tags rendering above action bar
            val tagsList = remember(actfile.content, actfile.tags) {
                val foundTags = Regex("#([a-zA-Z0-9_À-ÿ]+)")
                    .findAll(actfile.content)
                    .map { it.groupValues[1] }
                    .toList()
                val modelTags = (actfile.tags ?: "")
                    .split(",")
                    .map { it.trim().removePrefix("#") }
                    .filter { it.isNotBlank() }
                (foundTags + modelTags).distinct()
            }

            if (tagsList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tagsList.forEach { tag ->
                        TagChip(
                            tagName = tag,
                            onClick = { onMentionClick?.invoke("#$tag") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Horizontal bottom action bar & Category badge aligned to the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Interactive Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Like Button with responsive glow
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (actfile.isLikedByMe) Color(0xFFFF2D55).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onLike(actfile.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .scale(likeScale),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (actfile.isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (actfile.isLikedByMe) "Ne plus aimer" else "Aimer",
                                tint = likeColor,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${com.example.utils.FormatUtils.formatCount(actfile.likesCount)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = likeColor
                            )
                        }
                    }

                    // Comment Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onComment(actfile.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Forum,
                                contentDescription = "Commenter",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${com.example.utils.FormatUtils.formatCount(actfile.commentsCount)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Share Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (onShare != null) {
                                    onShare(actfile.id)
                                } else {
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(
                                            android.content.Intent.EXTRA_TEXT,
                                            "Découvrez cette publication sur bit :\n" +
                                            "👉 https://bit.gopu.inc/s/actfile/${actfile.id}"
                                        )
                                        type = "text/plain"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, "Partager")
                                    context.startActivity(shareIntent)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Partager",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    // Translate Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (!showOriginal) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (showOriginal) {
                                    if (translatedContent == null) {
                                        isTranslating = true
                                        coroutineScope.launch {
                                            try {
                                                translatedContent = com.example.utils.TranslationHelper.translateText(actfile.content, targetLanguageName)
                                                showOriginal = false
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(context, "Erreur", android.widget.Toast.LENGTH_SHORT).show()
                                            } finally {
                                                isTranslating = false
                                            }
                                        }
                                    } else {
                                        showOriginal = false
                                    }
                                } else {
                                    showOriginal = true
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isTranslating) {
                                CircularProgressIndicator(modifier = Modifier.size(15.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Translate,
                                    contentDescription = "Traduire",
                                    tint = if (!showOriginal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }

                // Category Badge (e.g. #Tech) aligned to the right
                val catInfo = com.example.ui.components.getCategoryById(actfile.category)
                if (catInfo != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = catInfo.color.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = catInfo.color.copy(alpha = 0.25f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${catInfo.emoji} #${catInfo.name}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = catInfo.color
                            )
                        }
                    }
                }
            }
        }
        if (!isDetailView) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }
    }
}

