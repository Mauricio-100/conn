package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.Story
import com.example.data.StoryUser
import com.example.ui.IddetViewModel
import com.example.utils.UrlHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val StoryEffects = listOf(
    StoryEffect("none", "Normal", "✨", "Aucun effet"),
    StoryEffect("cyberpunk", "Cyberpunk", "🌆", "Néon & contrastes futuristes"),
    StoryEffect("sketch", "Croquis", "✏️", "Dessin au fusain & esquisse"),
    StoryEffect("vintage", "Vintage", "🎞️", "Sépia & grain analogique"),
    StoryEffect("cartoon", "Cartoon", "🎨", "Style BD & aplats colorés"),
    StoryEffect("vignette", "Vignette", "🌑", "Ombrage cinématographique"),
    StoryEffect("retro_bw", "N&B Rétro", "🖤", "Noir et blanc dramatique")
)

data class StoryEffect(
    val id: String,
    val label: String,
    val emoji: String,
    val description: String
)

@Composable
fun StoriesBar(
    viewModel: IddetViewModel,
    stories: List<Story>,
    modifier: Modifier = Modifier,
    onStoryClick: (Story) -> Unit,
    onCreateClick: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Create Story Button / My Story
        item {
            CreateStoryItem(
                userAvatar = currentUser?.avatarUrl,
                username = currentUser?.username ?: "Moi",
                onClick = onCreateClick
            )
        }

        // Active Stories grouped by User
        items(stories, key = { it.id }) { story ->
            StoryCardItem(
                story = story,
                onClick = { onStoryClick(story) }
            )
        }
    }
}

@Composable
fun CreateStoryItem(
    userAvatar: String?,
    username: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(70.dp)
            .height(105.dp)
            .testTag("create_story_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Upper half avatar preview or primary gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.62f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!userAvatar.isNullOrBlank()) {
                    AsyncImage(
                        model = UrlHelper.fixCloudinaryUrl(userAvatar),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Circular Add Button in Center Offset
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.Center)
                    .offset(y = 18.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Créer une story",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(4.dp)
                )
            }

            // Bottom Label
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Créer une\nstory",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
fun StoryCardItem(
    story: Story,
    onClick: () -> Unit
) {
    val storyBorderGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF8B5CF6), // Purple
            Color(0xFFEC4899), // Pink
            Color(0xFFF59E0B)  // Amber
        )
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(70.dp)
            .height(105.dp)
            .testTag("story_card_${story.id}"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background media
            if (story.mediaType == "video") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else {
                AsyncImage(
                    model = UrlHelper.fixCloudinaryUrl(story.mediaUrl),
                    contentDescription = "Story de ${story.user.username}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Subtle dark overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )

            // User Avatar ring at top
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(34.dp)
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(storyBorderGradient)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (!story.user.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = UrlHelper.fixCloudinaryUrl(story.user.avatarUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = story.user.username.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Effect badge if present
            if (!story.effect.isNullOrBlank() && story.effect != "none") {
                val effectInfo = StoryEffects.find { it.id == story.effect }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = effectInfo?.emoji ?: "✨",
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // User username at bottom
            Text(
                text = story.user.username,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun StoryViewerDialog(
    stories: List<Story>,
    initialIndex: Int = 0,
    viewModel: IddetViewModel,
    onDismiss: () -> Unit
) {
    if (stories.isEmpty()) {
        onDismiss()
        return
    }

    var currentIndex by remember { mutableStateOf(initialIndex.coerceIn(0, stories.size - 1)) }
    val currentStory = stories[currentIndex]
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()

    var isPaused by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var showReplyInput by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val isMyStory = currentUser?.id == currentStory.user.id || currentUser?.username == currentStory.user.username

    // Progress timer for current story (5 seconds per story)
    val storyDurationMs = 5000L
    val progress = remember { Animatable(0f) }

    LaunchedEffect(currentIndex, isPaused) {
        if (!isPaused) {
            val startProgress = progress.value
            val remainingMs = ((1f - startProgress) * storyDurationMs).toLong().coerceAtLeast(100L)
            val result = progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = remainingMs.toInt(),
                    easing = LinearEasing
                )
            )
            if (result.endReason == AnimationEndReason.Finished) {
                if (currentIndex < stories.size - 1) {
                    progress.snapTo(0f)
                    currentIndex++
                } else {
                    onDismiss()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth * 0.35f) {
                                // Tap Left: Previous story
                                if (currentIndex > 0) {
                                    coroutineScope.launch {
                                        progress.snapTo(0f)
                                        currentIndex--
                                    }
                                } else {
                                    coroutineScope.launch { progress.snapTo(0f) }
                                }
                            } else {
                                // Tap Right: Next story
                                if (currentIndex < stories.size - 1) {
                                    coroutineScope.launch {
                                        progress.snapTo(0f)
                                        currentIndex++
                                    }
                                } else {
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
        ) {
            // Media Content (Image or Video)
            if (currentStory.mediaType == "video") {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ActfileVideoPlayer(
                        videoUrl = UrlHelper.fixCloudinaryUrl(currentStory.mediaUrl) ?: currentStory.mediaUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                AsyncImage(
                    model = UrlHelper.fixCloudinaryUrl(currentStory.mediaUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
            )

            // Header & Segmented Progress Bars
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Segmented Progress Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { index, _ ->
                        val segmentProgress = when {
                            index < currentIndex -> 1f
                            index == currentIndex -> progress.value
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { segmentProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f),
                        )
                    }
                }

                // User Info & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!currentStory.user.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = UrlHelper.fixCloudinaryUrl(currentStory.user.avatarUrl),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = currentStory.user.username.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentStory.user.username,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (currentStory.user.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Vérifié",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            if (!currentStory.user.profession.isNullOrBlank()) {
                                Text(
                                    text = currentStory.user.profession!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            if (!currentStory.effect.isNullOrBlank() && currentStory.effect != "none") {
                                val effect = StoryEffects.find { it.id == currentStory.effect }
                                Text(
                                    text = "${effect?.emoji ?: "✨"} ${effect?.label ?: currentStory.effect}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isMyStory) {
                            IconButton(
                                onClick = {
                                    isPaused = true
                                    showDeleteConfirmDialog = true
                                },
                                modifier = Modifier.testTag("delete_story_button")
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "Supprimer la story",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_story_button")
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            val realtimeViews by viewModel.realtimeStoryViews.collectAsState()
            val currentViews = realtimeViews[currentStory.id] ?: currentStory.views

            LaunchedEffect(currentStory.id) {
                viewModel.trackStoryView(currentStory.id, currentStory.views)
            }

            // Bottom Gradient & Interactions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 60.dp, start = 16.dp, end = 16.dp, top = 16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Views and Reactions Stats
                    if (currentViews > 0 || currentStory.reactions.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$currentViews", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                            
                            if (currentStory.reactions.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(12.dp))
                                val topReactions = currentStory.reactions.entries.sortedByDescending { it.value }.take(3)
                                topReactions.forEach { (emoji, count) ->
                                    Text("$emoji $count", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                            }
                        }
                    }

                    // Quick Emoji Reactions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("❤️", "🔥", "😂", "👏", "😮", "🚀").forEach { emoji ->
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable {
                                        viewModel.sendStoryReaction(currentStory.user.id, emoji) {
                                            Toast.makeText(context, "Réaction $emoji envoyée !", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                contentColor = Color.White
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    // Reply Text Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = {
                                Text(
                                    "Répondre à ${currentStory.user.username}...",
                                    color = Color.White.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("story_reply_input"),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White.copy(alpha = 0.7f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedContainerColor = Color.White.copy(alpha = 0.15f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.1f)
                            ),
                            singleLine = true
                        )

                        if (replyText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val textToSend = replyText
                                    replyText = ""
                                    viewModel.sendStoryReply(
                                        currentStory.user.id,
                                        textToSend,
                                        UrlHelper.fixCloudinaryUrl(currentStory.mediaUrl) ?: currentStory.mediaUrl
                                    ) {
                                        Toast.makeText(context, "Message envoyé !", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .testTag("send_story_reply_btn")
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Envoyer",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteConfirmDialog = false
                    isPaused = false
                },
                title = { Text("Supprimer cette story ?") },
                text = { Text("Votre story sera définitivement supprimée.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.deleteStory(currentStory.id) { success ->
                                if (success) {
                                    Toast.makeText(context, "Story supprimée", Toast.LENGTH_SHORT).show()
                                }
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Supprimer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteConfirmDialog = false
                        isPaused = false
                    }) {
                        Text("Annuler")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryCreatorDialog(
    viewModel: IddetViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedEffect by remember { mutableStateOf<String>("none") }
    val isUploading by viewModel.isUploadingStory.collectAsState()

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isUploading) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nouvelle Story",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { if (!isUploading) onDismiss() },
                    enabled = !isUploading
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                }
            }

            // Media Selection & Live Preview Box
            Card(
                onClick = {
                    if (!isUploading) {
                        mediaPickerLauncher.launch("*/*")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .testTag("story_media_picker_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (selectedMediaUri != null) {
                        AsyncImage(
                            model = selectedMediaUri,
                            contentDescription = "Aperçu de la story",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top change button
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Changer", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Choisir une photo ou vidéo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Formats acceptés : JPG, PNG, MP4",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Effects Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "EFFETS VISUELS & FILTRES IA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(StoryEffects) { effect ->
                        val isSelected = selectedEffect == effect.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedEffect = effect.id },
                            label = {
                                Text(
                                    text = "${effect.emoji} ${effect.label}",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Uploading state indicator
            if (isUploading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "Application de l'effet et publication...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Publish Button
            Button(
                onClick = {
                    val uri = selectedMediaUri
                    if (uri != null) {
                        viewModel.createStory(
                            context = context,
                            uri = uri,
                            effect = if (selectedEffect == "none") null else selectedEffect
                        ) { success, errorMsg ->
                            if (success) {
                                Toast.makeText(context, "Story publiée avec succès ! 🎉", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, errorMsg ?: "Échec de publication", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Veuillez d'abord sélectionner une photo ou vidéo", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = selectedMediaUri != null && !isUploading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("publish_story_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.RocketLaunch, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Partager en Story",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
