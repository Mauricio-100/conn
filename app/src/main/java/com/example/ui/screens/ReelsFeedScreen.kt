package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.ActfileWithUser
import com.example.ui.IddetViewModel
import com.example.ui.components.ActfileVideoPlayer
import com.example.ui.components.VerificationBadge
import com.example.utils.FormatUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReelsFeedScreen(
    viewModel: IddetViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allActfiles by viewModel.actfiles.collectAsStateWithLifecycle()

    // Filter media actfiles (with video or image)
    val mediaPosts = remember(allActfiles) {
        allActfiles.filter { actfile ->
            val content = actfile.content
            content.contains(".mp4", ignoreCase = true) ||
            content.contains(".webm", ignoreCase = true) ||
            content.contains("http", ignoreCase = true) ||
            content.contains("![", ignoreCase = true)
        }.ifEmpty { allActfiles }
    }

    val pagerState = rememberPagerState(pageCount = { mediaPosts.size })

    Scaffold(
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (mediaPosts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.VideoLibrary,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Aucun clip vidéo pour le moment",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            } else {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val post = mediaPosts[page]
                    val isPageActive = pagerState.currentPage == page

                    ReelItemView(
                        actfileWithUser = post,
                        isActive = isPageActive,
                        onLike = { viewModel.likeActfile(post.id) },
                        onComment = { navController.navigate("discussion/${post.id}") },
                        onShare = {
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    "Regarde ce clip sur IDDET : ${post.content.take(50)}\nhttps://hoosthubs-g.onrender.com/s/actfile/${post.id}"
                                )
                                type = "text/plain"
                            }
                            val shareIntent = android.content.Intent.createChooser(sendIntent, "Partager ce clip")
                            context.startActivity(shareIntent)
                        },
                        onUserClick = { navController.navigate("profile/${post.userId}") },
                        onMusicClick = { navController.navigate("music") }
                    )
                }
            }

            // Top overlay bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Clips IDDET",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        viewModel.setShowComposer(true)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Créer un clip", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ReelItemView(
    actfileWithUser: ActfileWithUser,
    isActive: Boolean,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onUserClick: () -> Unit,
    onMusicClick: () -> Unit
) {
    val actfile = actfileWithUser

    var isLiked by remember(actfile.isLikedByMe) { mutableStateOf(actfile.isLikedByMe) }
    var likesCount by remember(actfile.likesCount) { mutableIntStateOf(actfile.likesCount) }
    var isPlaying by remember { mutableStateOf(true) }
    var showDoubleTapHeart by remember { mutableStateOf(false) }

    // Helper to extract media
    val content = actfile.content
    val isVideo = remember(content) {
        content.contains(".mp4", ignoreCase = true) || content.contains(".webm", ignoreCase = true)
    }

    val mediaUrl = remember(content) {
        val regex = Regex("""(https?://[^\s)]+\.(?:mp4|webm|jpg|jpeg|png|webp|gif))|!\[.*?\]\((https?://[^\s)]+)\)""", RegexOption.IGNORE_CASE)
        val match = regex.find(content)
        match?.groupValues?.getOrNull(1)?.ifBlank { null }
            ?: match?.groupValues?.getOrNull(2)?.ifBlank { null }
            ?: "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=800&auto=format&fit=crop&q=80"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "music_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { isPlaying = !isPlaying },
                    onDoubleTap = {
                        if (!isLiked) {
                            isLiked = true
                            likesCount += 1
                            onLike()
                        }
                        showDoubleTapHeart = true
                    }
                )
            }
    ) {
        // Media content (Video or Image)
        if (isVideo && isActive) {
            ActfileVideoPlayer(
                videoUrl = mediaUrl,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = mediaUrl,
                contentDescription = actfile.content.take(30),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Dark gradient overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Floating Double Tap Heart Animation
        if (showDoubleTapHeart) {
            LaunchedEffect(Unit) {
                delay(800)
                showDoubleTapHeart = false
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF2D55),
                    modifier = Modifier
                        .size(120.dp)
                        .scale(1.2f)
                )
            }
        }

        // Pause indicator overlay
        if (!isPlaying) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Lecture",
                        tint = Color.White,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }
        }

        // Right Action Bar (TikTok Style)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // User Avatar with '+' follow badge
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = actfile.avatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
                    contentDescription = actfile.username,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .clickable { onUserClick() },
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Suivre", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }

            // Like button with counter
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        isLiked = !isLiked
                        likesCount += if (isLiked) 1 else -1
                        onLike()
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "J'aime",
                        tint = if (isLiked) Color(0xFFFF2D55) else Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Text(
                    text = FormatUtils.formatCount(likesCount),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Comment button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onComment,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Commentaires",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = FormatUtils.formatCount(actfile.commentsCount),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Share button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Partager",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(
                    text = "Partager",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Rotating Vinyl Sound Disc
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .rotate(if (isPlaying) rotationAngle else 0f)
                    .clickable { onMusicClick() },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=200",
                    contentDescription = "Piste sonore",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                )
            }
        }

        // Bottom Left Info Overlay (Author, Caption, Sound Title)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Author row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onUserClick() }
            ) {
                Text(
                    text = "@${actfile.username}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                VerificationBadge(isVerified = actfile.isVerified, userName = actfile.username)
            }

            // Caption / Title
            Text(
                text = actfile.content.take(120),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Music track pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { onMusicClick() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Son original • IDDET Sound Beats",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}
