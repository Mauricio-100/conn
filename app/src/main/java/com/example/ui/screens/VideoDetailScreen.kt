package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.VideoFeedItemNetwork
import com.example.ui.IddetViewModel
import com.example.ui.components.ActfileVideoPlayer
import com.example.ui.components.ShareEntityType
import com.example.ui.components.SharePayload
import com.example.ui.components.UniversalShareModal
import com.example.ui.components.VerificationBadge
import com.example.utils.UrlHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(
    videoId: String,
    viewModel: IddetViewModel,
    navController: NavController
) {
    val videoFeed by viewModel.realVideoFeed.collectAsStateWithLifecycle()
    val video = remember(videoFeed, videoId) {
        videoFeed.find { it.id == videoId } ?: VideoFeedItemNetwork(
            id = videoId,
            video_url = "",
            description = "Clip Vidéo",
            likes = 0,
            views = 0,
            created_at = "",
            liked = false
        )
    }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val authorUserId = video.user_id
    val isFollowingAuthor by viewModel.isFollowing(authorUserId).collectAsStateWithLifecycle(initialValue = false)
    val isSelf = currentUser?.id == authorUserId

    var isLiked by remember(video.id) { mutableStateOf(video.liked) }
    var likesCount by remember(video.id) { mutableStateOf(video.likes) }
    var showShareModal by remember { mutableStateOf(false) }

    LaunchedEffect(video.id) {
        viewModel.recordVideoView(video.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Vidéo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showShareModal = true }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Partager",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Video Player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .background(Color.Black)
            ) {
                if (video.video_url.isNotBlank()) {
                    ActfileVideoPlayer(
                        videoUrl = video.video_url,
                        modifier = Modifier.fillMaxSize(),
                        title = video.description ?: "",
                        autoPlay = true,
                        lazyLoad = false
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Creator & Video Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Mini Profile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (video.user_id.isNotBlank()) {
                                    navController.navigate("profile/${video.user_id}")
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!video.avatar_url.isNullOrBlank()) {
                                AsyncImage(
                                    model = UrlHelper.fixCloudinaryUrl(video.avatar_url),
                                    contentDescription = video.username,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = video.username.take(1).uppercase().ifEmpty { "U" },
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "@${video.username.ifEmpty { "créateur" }}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (video.is_verified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    VerificationBadge(userName = video.username, isVerified = true, modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(
                                text = "${video.views} vues",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!isSelf && video.user_id.isNotBlank()) {
                        Button(
                            onClick = {
                                if (isFollowingAuthor) viewModel.unfollowUser(authorUserId) else viewModel.followUser(authorUserId)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowingAuthor) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                                contentColor = if (isFollowingAuthor) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isFollowingAuthor) "Abonné" else "Suivre",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                if (!video.description.isNullOrBlank()) {
                    Text(
                        text = video.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Interaction Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            isLiked = !isLiked
                            likesCount = if (isLiked) likesCount + 1 else maxOf(0, likesCount - 1)
                            viewModel.likeVideo(video.id)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("$likesCount J'aime")
                    }

                    FilledTonalButton(
                        onClick = { showShareModal = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Partager")
                    }
                }
            }
        }
    }

    if (showShareModal) {
        UniversalShareModal(
            payload = SharePayload(
                type = ShareEntityType.VIDEO,
                idOrSlug = video.id,
                title = video.description?.take(40) ?: "Vidéo IDDET",
                subtitle = "par @${video.username.ifEmpty { "créateur" }}",
                imageUrl = video.thumbnail_url
            ),
            onDismiss = { showShareModal = false }
        )
    }
}
