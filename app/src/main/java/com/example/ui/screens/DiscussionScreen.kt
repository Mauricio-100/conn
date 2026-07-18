package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.ActfileComment
import com.example.data.ActfileWithUser
import com.example.ui.IddetViewModel
import com.example.ui.components.MarkdownActfile
import com.example.ui.components.VerificationBadge
import com.example.ui.components.VoiceRecorderUI
import com.example.ui.components.ActfileCard
import com.example.ui.components.getRelativeTimeString
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionScreen(
    viewModel: IddetViewModel,
    navController: NavController,
    actfileId: String
) {
    val actfile by viewModel.getActfile(actfileId).collectAsStateWithLifecycle(initialValue = null)
    val comments by viewModel.getComments(actfileId).collectAsStateWithLifecycle(initialValue = emptyList())
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    
    var replyText by remember { mutableStateOf("") }
    var isPreviewMode by remember { mutableStateOf(false) }

    var translatedContent by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var translationError by remember { mutableStateOf<String?>(null) }
    var currentTranslatedTo by remember { mutableStateOf<String?>(null) }
    val targetLanguage by viewModel.targetLanguage.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var smartReplies by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(actfile?.id) {
        val currentActfile = actfile
        if (currentActfile != null && com.example.utils.LocalAiManager.state.value == com.example.utils.AiModelState.READY) {
            smartReplies = com.example.utils.LocalAiManager.suggestReplies(currentActfile.content)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discussion", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Post content at the top
                actfile?.let { post ->
                    item {
                        ActfileCard(
                            actfile = post,
                            onLike = { viewModel.likeActfile(post.id) },
                            onView = { viewModel.incrementView(post.id) },
                            onUserClick = { userId ->
                                val myId = currentUser?.id
                                if (userId == myId) {
                                    navController.navigate("profile")
                                } else {
                                    navController.navigate("profile/$userId")
                                }
                            },
                            onComment = { /* already on discussion screen */ },
                            onDelete = if (post.userId == currentUser?.id) { { viewModel.deleteActfile(post.id); navController.popBackStack() } } else null,
                            onCategoryClick = { channelSlug ->
                                navController.navigate("community/$channelSlug")
                            },
                            onMentionClick = { mention ->
                                if (mention.startsWith("@")) {
                                    coroutineScope.launch {
                                        val u = viewModel.getUserByUsername(mention.removePrefix("@"))
                                        if (u != null) {
                                            navController.navigate("profile/${u.id}")
                                        }
                                    }
                                } else if (mention.startsWith("#")) {
                                    navController.navigate("search?query=${java.net.URLEncoder.encode(mention, "UTF-8")}")
                                }
                            },
                            isDetailView = true
                        )
                    }
                }
                
                // Section Title: Discussions (Count)
                item {
                    Text(
                        text = "Discussions (${comments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                // Comments list
                if (comments.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Aucune discussion pour le moment. Lancez la conversation !",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    items(comments, key = { it.id }) { comment ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Commenter Header: Avatar + Username + VerificationBadge + Timestamp relative
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        val myId = viewModel.currentUser.value?.id
                                        if (comment.userId == myId) {
                                            navController.navigate("profile")
                                        } else {
                                            navController.navigate("profile/${comment.userId}")
                                        }
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!comment.avatarUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = comment.avatarUrl,
                                                contentDescription = "Avatar de ${comment.username}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = comment.username.firstOrNull()?.toString()?.uppercase() ?: "?",
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = comment.username,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            VerificationBadge(
                                                userName = comment.username,
                                                isVerified = comment.isVerified,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Text(
                                            text = getRelativeTimeString(comment.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // Comment Body (Supporting Markdown & clickable elements)
                                val scope = rememberCoroutineScope()
                                MarkdownActfile(
                                    content = comment.content,
                                    onMentionClick = { username ->
                                        scope.launch {
                                            val u = viewModel.getUserByUsername(username)
                                            if (u != null) {
                                                navController.navigate("profile/${u.id}")
                                            }
                                        }
                                    },
                                    onLinkClick = { url ->
                                        val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                                        navController.navigate("browser/$encodedUrl")
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Sticky reply composer at the bottom with Markdown toggle, preview and voice recorder!
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp
            ) {
                var isRecordingMode by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (isRecordingMode) {
                        VoiceRecorderUI(
                            onCancel = { isRecordingMode = false },
                            onSendVoice = { voiceContent ->
                                viewModel.commentActfile(actfileId, voiceContent)
                                isRecordingMode = false
                            }
                        )
                    } else {
                        if (smartReplies.isNotEmpty() && replyText.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                smartReplies.forEach { reply ->
                                    SuggestionChip(
                                        onClick = { replyText = reply },
                                        label = { Text(reply, style = MaterialTheme.typography.labelSmall) },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                            }
                        }
                        if (isPreviewMode) {
                            // Compact Markdown Preview Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Aperçu de votre réponse",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(
                                            onClick = { isPreviewMode = false },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Éditer",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (replyText.isBlank()) {
                                        Text(
                                            text = "Rien à prévisualiser...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    } else {
                                        MarkdownActfile(
                                            content = replyText,
                                            onLinkClick = { url ->
                                                val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                                                navController.navigate("browser/$encodedUrl")
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Compact input row (Reddit style)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Toggle preview mode icon button
                            IconButton(
                                onClick = { isPreviewMode = !isPreviewMode },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                                    contentDescription = "Aperçu",
                                    tint = if (isPreviewMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Rounded text input field
                            OutlinedTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(max = 100.dp),
                                placeholder = { Text("Votre réponse...") },
                                shape = RoundedCornerShape(20.dp),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                singleLine = false,
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            // If text is blank: Voice recording icon button. Else: Send icon button!
                            if (replyText.isBlank()) {
                                IconButton(
                                    onClick = { isRecordingMode = true },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Message vocal",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        viewModel.commentActfile(actfileId, replyText)
                                        replyText = ""
                                        isPreviewMode = false
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Envoyer",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
