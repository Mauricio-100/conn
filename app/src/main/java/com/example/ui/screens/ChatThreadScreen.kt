package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.ui.IddetViewModel
import com.example.ui.components.*
import com.example.utils.SocketConnectionState
import kotlinx.coroutines.launch

/**
 * High-craftsmanship 1-on-1 Chat Thread screen for iDDET.
 */
@Composable
fun ChatThreadScreen(
    userId: String,
    iddetViewModel: IddetViewModel,
    navController: NavController
) {
    val chatViewModel: ChatThreadViewModel = viewModel(
        key = "chat_$userId",
        factory = ChatThreadViewModel.provideFactory(userId, iddetViewModel.repository)
    )
    ChatThreadScreen(
        userId = userId,
        viewModel = chatViewModel,
        navController = navController
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    userId: String,
    viewModel: ChatThreadViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val uiState by viewModel.uiState.collectAsState()
    val partnerInfo by viewModel.partnerInfo.collectAsState()
    val partnerAvatarUrl = remember(partnerInfo.avatarUrl) {
        partnerInfo.avatarUrl?.let { com.example.utils.UrlHelper.fixCloudinaryUrl(it) } ?: partnerInfo.avatarUrl
    }
    val inputText by viewModel.inputText.collectAsState()
    val isPartnerTyping by viewModel.isPartnerTyping.collectAsState()
    val socketState by viewModel.socketConnectionState.collectAsState()

    val listState = rememberLazyListState()

    // Context menu / Bottom sheet state for long-pressed message
    var selectedMessageForMenu by remember { mutableStateOf<ChatMessageUiModel?>(null) }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    var isVoiceRecording by remember { mutableStateOf(false) }

    // Media Picker for images & videos
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val isVideo = uri.toString().contains("video")
            viewModel.sendMediaMessage(uri.toString(), isVideo)
        }
    }

    // Auto-scroll when messages change or new message is inserted
    val messagesCount = (uiState as? ChatThreadUiState.Success)?.messages?.size ?: 0
    val isNearBottom by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            if (total == 0) true
            else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible >= total - 3
            }
        }
    }

    LaunchedEffect(messagesCount) {
        if (messagesCount > 0 && isNearBottom) {
            listState.animateScrollToItem(messagesCount - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                navController.navigate("profile/$userId")
                            }
                            .testTag("chat_header_profile")
                    ) {
                        // Circular Avatar with Presence Dot
                        Box(
                            modifier = Modifier.size(42.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                val initial = partnerInfo.username.firstOrNull()?.uppercase() ?: "?"
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                if (!partnerAvatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = partnerAvatarUrl,
                                        contentDescription = "Avatar de ${partnerInfo.username}",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }

                            // Active presence dot
                            if (partnerInfo.isOnline) {
                                PresenceDot(
                                    isOnline = true,
                                    size = 12.dp,
                                    borderColor = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 1.dp, y = 1.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = partnerInfo.username,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (partnerInfo.isIddetPlus) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    IddetPlusBadge(size = 15.dp)
                                }

                                if (partnerInfo.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    VerificationBadge(
                                        isVerified = true,
                                        userName = partnerInfo.username,
                                        showExplainingOnClick = false
                                    )
                                }
                            }

                            // Live presence status text
                            Text(
                                text = if (partnerInfo.isOnline) "En ligne" else "Vu récemment",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (partnerInfo.isOnline) {
                                    Color(0xFF22C55E)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isVoiceRecording) {
                    // Voice Recorder UI
                    VoiceRecorderUI(
                        onCancel = { isVoiceRecording = false },
                        onSendVoice = { voiceString ->
                            viewModel.sendVoiceMessage(voiceString)
                            isVoiceRecording = false
                        }
                    )
                } else {
                    // Standard text and media input bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Attachment button (Photos/Videos)
                        IconButton(
                            onClick = {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            modifier = Modifier.testTag("chat_attachment_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Joindre un média",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Voice message button
                        IconButton(
                            onClick = { isVoiceRecording = true },
                            modifier = Modifier.testTag("chat_mic_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Enregistrer un vocal",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Text Field (multi-line extensible)
                        TextField(
                            value = inputText,
                            onValueChange = { viewModel.onInputTextChange(it) },
                            placeholder = {
                                Text(
                                    text = "Message…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            maxLines = 4,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .testTag("chat_input_field")
                        )

                        // Send Button (only active when not empty)
                        val canSend = inputText.trim().isNotBlank()
                        IconButton(
                            onClick = {
                                if (canSend) {
                                    viewModel.sendTextMessage()
                                }
                            },
                            enabled = canSend,
                            modifier = Modifier.testTag("chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Envoyer",
                                tint = if (canSend) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Discrete connection state banner
                AnimatedVisibility(
                    visible = socketState != SocketConnectionState.CONNECTED,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Connexion au serveur en cours…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Messages View
                when (val state = uiState) {
                    is ChatThreadUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is ChatThreadUiState.Empty -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Dites bonjour à ${partnerInfo.username} 👋",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Vos messages et vocaux sont synchronisés en direct.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is ChatThreadUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    is ChatThreadUiState.Success -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("chat_messages_list"),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(
                                items = state.messages,
                                key = { it.id }
                            ) { message ->
                                MessageBubble(
                                    message = message,
                                    partnerAvatar = partnerAvatarUrl,
                                    partnerUsername = partnerInfo.username,
                                    onLongClick = {
                                        selectedMessageForMenu = message
                                    },
                                    onReactionClick = { emoji ->
                                        viewModel.toggleReaction(message.id, emoji)
                                    },
                                    onImageClick = { imageUrl ->
                                        fullScreenImageUrl = imageUrl
                                    },
                                    onVideoClick = { videoUrl ->
                                        // Navigate to video player or open full screen
                                        fullScreenImageUrl = videoUrl
                                    }
                                )
                            }
                            
                            if (isPartnerTyping) {
                                item(key = "typing_indicator") {
                                    com.example.ui.components.TypingIndicatorBubble(
                                        partnerAvatar = partnerAvatarUrl,
                                        partnerUsername = partnerInfo.username
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating "Scroll to bottom / New message" button when user is scrolled up
            if (!isNearBottom && messagesCount > 0) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(messagesCount - 1)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                        .size(40.dp)
                        .testTag("chat_scroll_bottom_button"),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Descendre"
                    )
                }
            }
        }
    }

    // Context Menu Bottom Sheet for Long-Pressed Message
    if (selectedMessageForMenu != null) {
        val targetMessage = selectedMessageForMenu!!

        ModalBottomSheet(
            onDismissRequest = { selectedMessageForMenu = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick emoji reaction bar
                ReactionPicker(
                    onSelectEmoji = { emoji ->
                        viewModel.toggleReaction(targetMessage.id, emoji)
                        selectedMessageForMenu = null
                    },
                    onDismiss = { selectedMessageForMenu = null },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Action: Copy text
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(targetMessage.content))
                            Toast.makeText(context, "Message copié", Toast.LENGTH_SHORT).show()
                            selectedMessageForMenu = null
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Copier le texte",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Action: Delete message (ONLY visible on user's own messages)
                if (targetMessage.isMine) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.deleteMessage(targetMessage.id)
                                selectedMessageForMenu = null
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                            .testTag("chat_menu_delete_message"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Supprimer le message",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Full Screen Image/Video Viewer Dialog
    if (fullScreenImageUrl != null) {
        Dialog(
            onDismissRequest = { fullScreenImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = fullScreenImageUrl,
                    contentDescription = "Image en plein écran",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )

                IconButton(
                    onClick = { fullScreenImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
