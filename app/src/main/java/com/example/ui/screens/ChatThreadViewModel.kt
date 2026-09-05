package com.example.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.IddetRepository
import com.example.data.Message
import com.example.data.RetrofitClient
import com.example.ui.components.ChatMessageUiModel
import com.example.ui.components.ReactionUiGroup
import com.example.utils.ChatSocketClient
import com.example.utils.SocketConnectionState
import com.example.utils.WebSocketEvent
import com.example.utils.WebSocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import java.util.UUID

data class ChatPartnerUiModel(
    val id: String,
    val username: String,
    val avatarUrl: String?,
    val isOnline: Boolean,
    val isVerified: Boolean = false,
    val isIddetPlus: Boolean = false,
    val cardStyle: String? = null,
    val lastSeen: String? = null
)

sealed interface ChatThreadUiState {
    object Loading : ChatThreadUiState
    data class Success(
        val messages: List<ChatMessageUiModel>,
        val partner: ChatPartnerUiModel,
        val connectionState: SocketConnectionState = SocketConnectionState.CONNECTED
    ) : ChatThreadUiState
    object Empty : ChatThreadUiState
    data class Error(val message: String) : ChatThreadUiState
}

class ChatThreadViewModel(
    val partnerUserId: String,
    val repository: IddetRepository,
    private val socketClient: ChatSocketClient = WebSocketManager
) : ViewModel() {

    private val _partnerInfo = MutableStateFlow(
        ChatPartnerUiModel(
            id = partnerUserId,
            username = "Chargement…",
            avatarUrl = null,
            isOnline = false
        )
    )
    val partnerInfo: StateFlow<ChatPartnerUiModel> = _partnerInfo.asStateFlow()

    private val _optimisticMessages = MutableStateFlow<List<ChatMessageUiModel>>(emptyList())
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isPartnerTyping = MutableStateFlow(false)
    val isPartnerTyping: StateFlow<Boolean> = _isPartnerTyping.asStateFlow()

    private var partnerTypingTimeoutJob: Job? = null
    private var myTypingTimeoutJob: Job? = null
    private var lastTypingSentTime = 0L

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    val socketConnectionState: StateFlow<SocketConnectionState> = socketClient.connectionState

    // Live messages from repository Flow
    private val roomMessagesFlow = repository.getMessagesWith(partnerUserId)

    val uiState: StateFlow<ChatThreadUiState> = combine(
        roomMessagesFlow,
        _optimisticMessages,
        _partnerInfo,
        socketConnectionState
    ) { roomList, pendingList, partner, connState ->
        val currentUserId = repository.currentUser.value?.id ?: ""

        // Merge room messages and pending/optimistic messages
        val combined = mutableListOf<ChatMessageUiModel>()

        roomList.filterNot { it.id.startsWith("conv_") }.forEach { m ->
            val isMine = m.senderId == currentUserId
            val reactions = parseReactions(m.reaction)
            combined.add(
                ChatMessageUiModel(
                    id = m.id,
                    senderId = m.senderId,
                    receiverId = m.receiverId,
                    content = m.content,
                    type = m.type,
                    isMine = isMine,
                    isRead = m.isRead,
                    createdAt = m.createdAt,
                    reactions = reactions,
                    isSending = false,
                    isFailed = false
                )
            )
        }

        // Add pending messages if not already present in room messages
        pendingList.forEach { pending ->
            if (combined.none { it.id == pending.id || (it.content == pending.content && it.isMine && Math.abs(it.createdAt - pending.createdAt) < 5000) }) {
                combined.add(pending)
            }
        }

        // Sort by timestamp ASC
        combined.sortBy { it.createdAt }

        // Compute grouping, avatars, and last in group
        val grouped = groupMessages(combined)

        if (grouped.isEmpty()) {
            ChatThreadUiState.Empty
        } else {
            ChatThreadUiState.Success(
                messages = grouped,
                partner = partner,
                connectionState = connState
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatThreadUiState.Loading
    )

    init {
        val cachedConv = repository.conversations.value.find { it.user_id == partnerUserId }
        if (cachedConv != null) {
            _partnerInfo.value = _partnerInfo.value.copy(
                username = cachedConv.username,
                avatarUrl = cachedConv.avatar_url,
                isOnline = cachedConv.is_online,
                isVerified = cachedConv.is_verified
            )
        }
        loadPartnerDetails()
        observeWebSocketEvents()
        connectSocketIfLoggedIn()
        markMessagesRead()
    }

    fun onInputTextChange(newText: String) {
        _inputText.value = newText
        
        // Only send typing indicator if connected and hasn't sent recently
        if (socketConnectionState.value == SocketConnectionState.CONNECTED) {
            val now = System.currentTimeMillis()
            if (now - lastTypingSentTime > 2000) {
                lastTypingSentTime = now
                socketClient.sendTypingStatus(partnerUserId, true)
            }
            
            myTypingTimeoutJob?.let { it.cancel() }
            myTypingTimeoutJob = viewModelScope.launch {
                delay(3000)
                socketClient.sendTypingStatus(partnerUserId, false)
            }
        }
    }

    private fun loadPartnerDetails() {
        viewModelScope.launch {
            // First check local Room cache
            repository.getUserFlow(partnerUserId).collectLatest { user ->
                if (user != null) {
                    _partnerInfo.value = _partnerInfo.value.copy(
                        id = user.id,
                        username = user.username,
                        avatarUrl = user.avatarUrl ?: _partnerInfo.value.avatarUrl,
                        isVerified = user.isVerified
                    )
                }
            }
        }

        // Also fetch network profile for real-time presence & Iddet Plus status
        viewModelScope.launch {
            try {
                val header = repository.userToken?.let { "Bearer $it" }
                val profile = RetrofitClient.apiService.getUserProfile(header, partnerUserId)
                val newAvatar = profile.avatar_url?.ifBlank { null } ?: _partnerInfo.value.avatarUrl
                _partnerInfo.value = ChatPartnerUiModel(
                    id = profile.id ?: partnerUserId,
                    username = profile.username,
                    avatarUrl = newAvatar,
                    isOnline = profile.is_online,
                    isVerified = profile.is_verified,
                    isIddetPlus = profile.is_iddet_plus,
                    cardStyle = profile.card_style,
                    lastSeen = profile.last_seen
                )
            } catch (e: Exception) {
                Log.w("ChatThreadVM", "Could not fetch remote profile: ${e.message}")
            }
        }
    }

    private fun connectSocketIfLoggedIn() {
        viewModelScope.launch {
            val user = repository.currentUser.value
            if (user != null) {
                socketClient.connect(user.id)
            }
        }
    }

    private fun markMessagesRead() {
        viewModelScope.launch {
            repository.markMessagesRead(partnerUserId)
        }
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            socketClient.events.collect { event ->
                when (event) {
                    is WebSocketEvent.NewMessage -> {
                        val currentUserId = repository.currentUser.value?.id ?: ""
                        if (event.senderId == partnerUserId || (event.senderId == currentUserId && event.messageId.isNotBlank())) {
                            // Insert into Room
                            val msg = Message(
                                id = event.messageId,
                                senderId = event.senderId,
                                receiverId = if (event.senderId == currentUserId) partnerUserId else currentUserId,
                                content = event.content,
                                type = event.msgType,
                                isRead = false,
                                createdAt = System.currentTimeMillis()
                            )
                            repository.insertMessageLocal(msg)

                            // Remove matching optimistic message if any
                            _optimisticMessages.value = _optimisticMessages.value.filter {
                                it.id != event.messageId && !(it.content == event.content && it.isMine)
                            }

                            if (event.senderId == partnerUserId) {
                                markMessagesRead()
                            }
                        }
                    }

                    is WebSocketEvent.MessageDeleted -> {
                        repository.deleteMessageLocal(event.messageId)
                        _optimisticMessages.value = _optimisticMessages.value.filter { it.id != event.messageId }
                    }
                    
                    is WebSocketEvent.TypingStatus -> {
                        if (event.senderId == partnerUserId) {
                            _isPartnerTyping.value = event.isTyping
                            partnerTypingTimeoutJob?.let { it.cancel() }
                            if (event.isTyping) {
                                partnerTypingTimeoutJob = viewModelScope.launch {
                                    delay(5000)
                                    _isPartnerTyping.value = false
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    fun sendTextMessage() {
        val text = _inputText.value.trim()
        if (text.isBlank()) return

        _inputText.value = ""
        val currentUserId = repository.currentUser.value?.id ?: ""
        val tempId = UUID.randomUUID().toString()

        val optimistic = ChatMessageUiModel(
            id = tempId,
            senderId = currentUserId,
            receiverId = partnerUserId,
            content = text,
            type = "text",
            isMine = true,
            isRead = false,
            createdAt = System.currentTimeMillis(),
            isSending = true
        )

        _optimisticMessages.value = _optimisticMessages.value + optimistic

        viewModelScope.launch {
            try {
                repository.sendMessage(partnerUserId, text, "text")
                // Success: remove optimistic entry once confirmed or keep until Room emits
                _optimisticMessages.value = _optimisticMessages.value.filter { it.id != tempId }
            } catch (e: Exception) {
                Log.e("ChatThreadVM", "Failed to send message", e)
                // Mark failed
                _optimisticMessages.value = _optimisticMessages.value.map {
                    if (it.id == tempId) it.copy(isSending = false, isFailed = true) else it
                }
            }
        }
    }

    fun sendVoiceMessage(audioB64: String) {
        val currentUserId = repository.currentUser.value?.id ?: ""
        val myUsername = repository.currentUser.value?.username ?: "Moi"
        val tempId = UUID.randomUUID().toString()

        val optimistic = ChatMessageUiModel(
            id = tempId,
            senderId = currentUserId,
            receiverId = partnerUserId,
            content = audioB64,
            type = "voice",
            isMine = true,
            isRead = false,
            createdAt = System.currentTimeMillis(),
            isSending = true
        )
        _optimisticMessages.value = _optimisticMessages.value + optimistic

        viewModelScope.launch(Dispatchers.IO) {
            var sent = false
            try {
                sent = socketClient.sendVoiceMessage(partnerUserId, audioB64, myUsername)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                repository.sendMessage(partnerUserId, audioB64, "voice")
                repository.updateConversationLastMessage(
                    otherUserId = partnerUserId,
                    content = "[Voice Message](voice://duration=5&amplitudes=0.5)",
                    type = "voice",
                    isIncoming = false
                )
                sent = true
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (sent) {
                _optimisticMessages.value = _optimisticMessages.value.filter { it.id != tempId }
            } else {
                _optimisticMessages.value = _optimisticMessages.value.map {
                    if (it.id == tempId) it.copy(isSending = false, isFailed = false) else it
                }
            }
        }
    }

    fun sendMediaMessage(url: String, isVideo: Boolean) {
        val currentUserId = repository.currentUser.value?.id ?: ""
        val type = if (isVideo) "video" else "image"
        val tempId = UUID.randomUUID().toString()

        val optimistic = ChatMessageUiModel(
            id = tempId,
            senderId = currentUserId,
            receiverId = partnerUserId,
            content = url,
            type = type,
            isMine = true,
            isRead = false,
            createdAt = System.currentTimeMillis(),
            isSending = true
        )
        _optimisticMessages.value = _optimisticMessages.value + optimistic

        viewModelScope.launch {
            try {
                repository.sendMessage(partnerUserId, url, type)
                _optimisticMessages.value = _optimisticMessages.value.filter { it.id != tempId }
            } catch (e: Exception) {
                _optimisticMessages.value = _optimisticMessages.value.map {
                    if (it.id == tempId) it.copy(isSending = false, isFailed = true) else it
                }
            }
        }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            try {
                val currentMessage = repository.getMessagesWith(partnerUserId).first().find { it.id == messageId }
                if (currentMessage?.reaction == emoji) {
                    repository.removeMessageReaction(messageId)
                } else {
                    repository.reactToMessage(messageId, emoji)
                }
            } catch (e: Exception) {
                Log.e("ChatThreadVM", "Failed to react", e)
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                repository.deleteMessage(messageId)
                repository.deleteMessageLocal(messageId)
                _optimisticMessages.value = _optimisticMessages.value.filter { it.id != messageId }
            } catch (e: Exception) {
                Log.e("ChatThreadVM", "Failed to delete message", e)
            }
        }
    }

    private fun parseReactions(reactionStr: String?): List<ReactionUiGroup> {
        if (reactionStr.isNullOrBlank()) return emptyList()
        val emojis = reactionStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val counts = emojis.groupingBy { it }.eachCount()
        return counts.map { (emoji, count) ->
            ReactionUiGroup(emoji = emoji, count = count, hasReacted = true)
        }
    }

    private fun groupMessages(messages: List<ChatMessageUiModel>): List<ChatMessageUiModel> {
        if (messages.isEmpty()) return emptyList()

        val result = mutableListOf<ChatMessageUiModel>()
        val timeThresholdMs = 3 * 60 * 1000 // 3 minutes

        for (i in messages.indices) {
            val current = messages[i]
            val prev = messages.getOrNull(i - 1)
            val next = messages.getOrNull(i + 1)

            val isSameSenderPrev = prev != null &&
                    prev.senderId == current.senderId &&
                    (current.createdAt - prev.createdAt < timeThresholdMs)

            val isSameSenderNext = next != null &&
                    next.senderId == current.senderId &&
                    (next.createdAt - current.createdAt < timeThresholdMs)

            val isFirstInGroup = !isSameSenderPrev
            val isLastInGroup = !isSameSenderNext
            val showAvatar = !current.isMine && isLastInGroup

            result.add(
                current.copy(
                    isFirstInGroup = isFirstInGroup,
                    isLastInGroup = isLastInGroup,
                    showAvatar = showAvatar
                )
            )
        }
        return result
    }

    companion object {
        fun provideFactory(
            partnerUserId: String,
            repository: IddetRepository,
            socketClient: ChatSocketClient = WebSocketManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatThreadViewModel(partnerUserId, repository, socketClient) as T
            }
        }
    }
}
