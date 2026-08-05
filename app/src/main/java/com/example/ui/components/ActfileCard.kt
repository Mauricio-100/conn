package com.example.ui.components

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    
    // Automatically register views
    LaunchedEffect(actfile.id) {
        onView(actfile.id)
    }

    val isCommunityPost = !actfile.communityId.isNullOrBlank() || !actfile.channelSlug.isNullOrBlank()
    val avatarSize = if (isDetailView) 56.dp else 40.dp
    val miniAvatarSize = 20.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("actfile_card_${actfile.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isCommunityPost) {
                    // CAS 1 : Publication standard (community_id == null / blank)
                    // Author Avatar
                    Box(
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onUserClick(actfile.userId) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!actfile.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = actfile.avatarUrl,
                                contentDescription = "Avatar de ${actfile.username}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = actfile.username.firstOrNull()?.toString()?.uppercase() ?: "?",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
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
                                style = MaterialTheme.typography.bodyMedium,
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
                            text = relativeTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                } else {
                    // CAS 2 : Publication communautaire (community_id != null / channelSlug is present)
                    // Community Icon / Avatar (using category default icon as fallback)
                    val commIconUrl = remember(actfile.category) {
                        getCategoryDefaultIcon(actfile.category ?: "general")
                    }

                    Box(
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(RoundedCornerShape(8.dp))
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
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onCategoryClick?.invoke(actfile.channelSlug ?: "") }
                            )

                            if (!actfile.channelName.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "#${actfile.channelName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
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
                                        model = actfile.avatarUrl,
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
                                fontWeight = FontWeight.Medium,
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
                    }
                }

                // View count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = if (onDelete != null) 8.dp else 0.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Info,
                        contentDescription = "Vues",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${actfile.viewsCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
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
                Spacer(modifier = Modifier.height(12.dp))
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

            Spacer(modifier = Modifier.height(16.dp))

            // Horizontal bottom action bar & Category badge aligned to the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Interactive Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Like Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onLike(actfile.id) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (actfile.isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (actfile.isLikedByMe) "Ne plus aimer" else "Aimer",
                            tint = if (actfile.isLikedByMe) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${actfile.likesCount}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (actfile.isLikedByMe) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Comment Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onComment(actfile.id) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Forum,
                            contentDescription = "Commenter",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${actfile.commentsCount}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Share Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (onShare != null) {
                                    onShare(actfile.id)
                                } else {
                                    // Trigger default Share Sheet
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
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Partager",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Partager",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Translate Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
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
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isTranslating) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Translate,
                                contentDescription = "Traduire",
                                tint = if (!showOriginal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Category Badge (e.g. #Tech) aligned to the right
                val catInfo = com.example.ui.components.getCategoryById(actfile.category)
                if (catInfo != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
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
    }
}
