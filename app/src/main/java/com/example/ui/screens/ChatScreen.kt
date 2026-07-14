package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.Message
import com.example.ui.IddetViewModel
import com.example.ui.components.MarkdownActfile
import com.example.ui.components.VoiceMessagePlayer
import com.example.ui.components.VoiceRecorderUI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(userId: String, viewModel: IddetViewModel, navController: NavController) {
    val myUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val partnerConversation = conversations.find { it.user_id == userId }
    val partner = viewModel.searchUsersResult.collectAsStateWithLifecycle().value.find { it.id == userId } // Fallback
    val isLight = MaterialTheme.colorScheme.background.red > 0.5f

    val partnerName = partnerConversation?.username ?: partner?.username ?: "Utilisateur"
    val partnerAvatar = partnerConversation?.avatar_url ?: partner?.avatarUrl
    val isOnline = partnerConversation?.is_online ?: false

    val messages by viewModel.getMessagesWith(userId).collectAsStateWithLifecycle(initialValue = emptyList())
    
    var messageText by remember { mutableStateOf("") }
    var isRecordingMode by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    // Quick replies list
    val quickReplies = listOf(
        "🐾 Salut !",
        "🐱 Meow !",
        "Comment ça va ?",
        "Trop cool ! 👍",
        "À plus tard !"
    )

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    // Poll for new messages every 3 seconds while on this screen
    LaunchedEffect(userId) {
        while (true) {
            viewModel.refreshMessagesWith(userId)
            kotlinx.coroutines.delay(3000)
        }
    }
    
    if (myUser == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("profile/$userId")
                            }
                            .padding(vertical = 4.dp)
                            .testTag("chat_partner_header")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!partnerAvatar.isNullOrBlank()) {
                                AsyncImage(
                                    model = partnerAvatar,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = partnerName.firstOrNull()?.toString()?.uppercase() ?: "?",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = partnerName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isOnline) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("En ligne", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("Hors ligne", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Actions */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // Quick Replies Row (only shown when not recording)
                if (!isRecordingMode) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        itemsIndexed(quickReplies) { _, reply ->
                            SuggestionChip(
                                onClick = {
                                    viewModel.sendMessage(userId, reply)
                                },
                                label = { Text(reply, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (isRecordingMode) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            VoiceRecorderUI(
                                onCancel = { isRecordingMode = false },
                                onSendVoice = { voiceContent ->
                                    viewModel.sendMessage(userId, voiceContent, type = "audio")
                                    isRecordingMode = false
                                },
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input"),
                            placeholder = { Text("Écrire un message...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            maxLines = 5
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (messageText.isBlank()) {
                            IconButton(
                                onClick = { isRecordingMode = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .testTag("chat_record_button")
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Vocal",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (messageText.isNotBlank()) {
                                        viewModel.sendMessage(userId, messageText)
                                        messageText = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .testTag("chat_send_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .offset(x = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Atmospheric Dot Grid Background Pattern (avoid flat colors)
            val dotPatternColor = if (isLight) Color.DarkGray.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.03f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .drawBehind {
                        val spacing = 28.dp.toPx()
                        var x = 0f
                        while (x < size.width) {
                            var y = 0f
                            while (y < size.height) {
                                drawCircle(
                                    color = dotPatternColor,
                                    radius = 1.5f,
                                    center = Offset(x, y)
                                )
                                y += spacing
                            }
                            x += spacing
                        }
                    }
            )
            
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(12.dp),
                                shadowElevation = 1.dp,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            ) {
                                Text(
                                    text = "🔒 Les messages sont chiffrés de bout en bout.",
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
                
                itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
                    val isMine = msg.senderId == myUser!!.id
                    val isAudio = msg.type == "audio" || (msg.content.startsWith("http") && msg.content.contains("voice_messages"))
                    
                    // Show date header if the day changes
                    val showDateHeader = if (index == 0) {
                        true
                    } else {
                        val prevMsg = messages[index - 1]
                        val formatDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                        formatDay.format(Date(msg.createdAt)) != formatDay.format(Date(prevMsg.createdAt))
                    }

                    if (showDateHeader) {
                        DateSeparator(timestamp = msg.createdAt)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                    ) {
                        Column(
                            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                        ) {
                            Surface(
                                color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMine) 16.dp else 2.dp,
                                    bottomEnd = if (isMine) 2.dp else 16.dp
                                ),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .widthIn(max = 290.dp)
                                    .testTag("message_bubble_${msg.id}")
                            ) {
                                Column(modifier = Modifier.padding(2.dp)) {
                                    if (isAudio) {
                                        Box(modifier = Modifier.padding(4.dp)) {
                                            VoiceMessagePlayer(content = msg.content, isMine = isMine)
                                        }
                                    } else {
                                        MarkdownActfile(
                                            content = msg.content,
                                            isMine = isMine,
                                            compactOpenGraph = true,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }

                                    // Bubble footer containing Time + Double Checkmark status
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .padding(end = 8.dp, bottom = 4.dp, start = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatMessageTime(msg.createdAt),
                                            fontSize = 9.sp,
                                            color = if (isMine) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                        if (isMine) {
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Icon(
                                                imageVector = if (msg.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                                                contentDescription = if (msg.isRead) "Lu" else "Envoyé",
                                                tint = if (msg.isRead) Color(0xFF818CF8) else Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(11.dp)
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
    }
}

@Composable
fun DateSeparator(timestamp: Long) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = formatMessageDate(timestamp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

fun formatMessageTime(timestampMillis: Long): String {
    return try {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        format.format(Date(timestampMillis))
    } catch (e: Exception) {
        ""
    }
}

fun formatMessageDate(timestampMillis: Long): String {
    val date = Date(timestampMillis)
    val now = Date()
    val formatDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    if (formatDay.format(date) == formatDay.format(now)) {
        return "Aujourd'hui"
    }
    val yesterday = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
    if (formatDay.format(date) == formatDay.format(yesterday)) {
        return "Hier"
    }
    val outFormat = SimpleDateFormat("d MMMM yyyy", Locale.FRENCH)
    return outFormat.format(date)
}

