package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActfileWithUser
import com.example.data.ActfileComment
import com.example.data.IddetRepository
import com.example.data.User
import com.example.data.Message
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class IddetViewModel(private val repository: IddetRepository) : ViewModel() {

    val currentUser: StateFlow<User?> = repository.currentUser
    
    val selectedTheme: StateFlow<com.example.ui.theme.AppTheme> = repository.selectedTheme

    fun setSelectedTheme(theme: com.example.ui.theme.AppTheme) {
        repository.setSelectedTheme(theme)
    }

    val targetLanguage: StateFlow<String> = repository.targetLanguage

    val aiState = com.example.utils.LocalAiManager.state

    fun setTargetLanguage(language: String) {
        repository.setTargetLanguage(language)
    }

    private val _showComposer = MutableStateFlow(false)
    val showComposer: StateFlow<Boolean> = _showComposer.asStateFlow()

    fun setShowComposer(show: Boolean) {
        _showComposer.value = show
    }

    private val _feedTab = MutableStateFlow(0)
    val feedTab: StateFlow<Int> = _feedTab.asStateFlow()

    fun setFeedTab(tab: Int) {
        _feedTab.value = tab
    }

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    fun setSelectedCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    private val _allCategories = MutableStateFlow<List<String>>(emptyList())
    val allCategories: StateFlow<List<String>> = _allCategories.asStateFlow()

    private val _preferredCategories = MutableStateFlow<List<String>>(emptyList())
    val preferredCategories: StateFlow<List<String>> = _preferredCategories.asStateFlow()

    fun loadCategories() {
        viewModelScope.launch {
            if (_allCategories.value.isEmpty()) {
                _allCategories.value = repository.getCategories()
            }
            _preferredCategories.value = repository.getMyPreferredCategories()
        }
    }

    fun savePreferredCategories(categoriesList: List<String>, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val updated = repository.updatePreferredCategories(categoriesList)
                _preferredCategories.value = updated
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Une erreur est survenue")
            }
        }
    }
    
    val actfiles = repository.getAllActfiles().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val followedActfiles: StateFlow<List<ActfileWithUser>> = currentUser.flatMapLatest {
        repository.getFollowedActfiles()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun refreshActfiles() {
        viewModelScope.launch {
            repository.refreshActfiles()
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            repository.refreshProfile()
        }
    }

    init {
        viewModelScope.launch {
            repository.refreshActfiles()
        }
        viewModelScope.launch {
            while (true) {
                try {
                    repository.refreshNotifications()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                kotlinx.coroutines.delay(15000)
            }
        }
        viewModelScope.launch {
            com.example.utils.WebSocketManager.events.collect { event ->
                when (event) {
                    is com.example.utils.WebSocketEvent.NewMessage -> {
                        val parsedTime = try {
                            val cleanTimestamp = event.timestamp.substringBefore(".")
                            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).parse(cleanTimestamp)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                        val incomingMsg = Message(
                            id = event.messageId,
                            senderId = event.senderId,
                            receiverId = currentUser.value?.id ?: "",
                            content = event.content,
                            type = event.msgType,
                            isRead = false,
                            createdAt = parsedTime
                        )
                        repository.insertMessageLocal(incomingMsg)
                        repository.updateConversationLastMessage(
                            otherUserId = event.senderId,
                            content = event.content,
                            type = event.msgType,
                            isIncoming = true,
                            senderUsername = event.senderUsername
                        )
                    }
                    is com.example.utils.WebSocketEvent.MessageSent -> {
                        val myId = currentUser.value?.id ?: ""
                        val targetReceiverId = lastSendingReceiverId ?: ""
                        
                        try {
                            val currentMessages = repository.getMessagesWith(targetReceiverId).first()
                            currentMessages.forEach { msg ->
                                if (msg.type == "audio_sending" || msg.type == "audio_error") {
                                    repository.deleteMessageLocal(msg.id)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        
                        val confirmedMsg = Message(
                            id = event.messageId,
                            senderId = myId,
                            receiverId = targetReceiverId,
                            content = event.content,
                            type = event.msgType,
                            isRead = false,
                            createdAt = System.currentTimeMillis()
                        )
                        repository.insertMessageLocal(confirmedMsg)
                        repository.updateConversationLastMessage(
                            otherUserId = targetReceiverId,
                            content = event.content,
                            type = event.msgType,
                            isIncoming = false
                        )
                    }
                    is com.example.utils.WebSocketEvent.Error -> {
                        val targetReceiverId = lastSendingReceiverId ?: ""
                        try {
                            val currentMessages = repository.getMessagesWith(targetReceiverId).first()
                            currentMessages.forEach { msg ->
                                if (msg.type == "audio_sending") {
                                    repository.insertMessageLocal(msg.copy(type = "audio_error"))
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        repository.updateConversationLastMessage(
                            otherUserId = targetReceiverId,
                            content = "Échec de l'envoi",
                            type = "audio_error",
                            isIncoming = false
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    // Properly scoped flows that update when currentUser changes, without causing infinite loops
    val giants: StateFlow<List<User>> = currentUser.flatMapLatest {
        repository.getGiants()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val conversations: StateFlow<List<com.example.data.ConversationNetwork>> = repository.conversations

    private val _searchQuery = MutableStateFlow("")
    val searchUsersResult: StateFlow<List<User>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) flowOf(emptyList()) else repository.searchUsers(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchActfilesResult: StateFlow<List<ActfileWithUser>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) flowOf(emptyList()) else repository.searchActfiles(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    fun clearAuthError() {
        _authError.value = null
    }

    fun login(username: String, password: String? = null) {
        viewModelScope.launch {
            try {
                repository.login(username, password)
                _authError.value = null
            } catch (e: Exception) {
                _authError.value = e.message
            }
        }
    }

    fun signup(username: String, password: String? = null) {
        viewModelScope.launch {
            try {
                repository.signup(username, password)
                _authError.value = null
            } catch (e: Exception) {
                _authError.value = e.message
            }
        }
    }

    val notifications = repository.getNotifications().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val likedActfiles: StateFlow<List<ActfileWithUser>> = currentUser.flatMapLatest {
        repository.getLikedActfiles()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val commentedActfiles: StateFlow<List<ActfileWithUser>> = currentUser.flatMapLatest { user ->
        repository.getCommentedActfiles(user?.id ?: "")
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun updateProfile(
        username: String? = null,
        avatarUrl: String? = null,
        bio: String,
        privacySetting: String,
        email: String? = null,
        phoneNumber: String? = null,
        birthDate: String? = null,
        zodiacSign: String? = null,
        preferredCategory: String? = null
    ) {
        viewModelScope.launch {
            repository.updateProfile(username, avatarUrl, bio, privacySetting, email, phoneNumber, birthDate, zodiacSign, preferredCategory)
        }
    }

    fun updateProfileWithImage(
        avatarFile: java.io.File?,
        bio: String?,
        phoneNumber: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.updateProfileWithImage(avatarFile, bio, phoneNumber)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Erreur de mise à jour du profil")
            }
        }
    }

    suspend fun getUserByUsername(username: String): User? {
        return repository.getUserByUsername(username)
    }

    fun logout() {
        repository.logout()
    }

    fun publishActfile(content: String, tags: String = "", category: String? = null) {
        viewModelScope.launch {
            repository.publishActfile(content, tags, category)
        }
    }

    fun deleteActfile(id: String) {
        viewModelScope.launch {
            repository.deleteActfile(id)
        }
    }
    
    fun incrementView(id: String) {
        viewModelScope.launch {
            repository.incrementView(id)
        }
    }
    
    fun likeActfile(id: String) {
        viewModelScope.launch {
            repository.likeActfile(id)
        }
    }
    
    fun commentActfile(id: String, content: String) {
        viewModelScope.launch {
            repository.commentActfile(id, content)
        }
    }
    
    fun getUserActfiles(userId: String): Flow<List<ActfileWithUser>> {
        return repository.getUserActfiles(userId)
    }
    
    fun getUserProfile(userId: String): Flow<User?> {
        return repository.getUserFlow(userId)
    }

    fun getMessagesWith(otherUserId: String): Flow<List<Message>> {
        return repository.getMessagesWith(otherUserId)
    }

    fun refreshMessagesWith(otherUserId: String) {
        viewModelScope.launch {
            repository.refreshMessagesWith(otherUserId)
        }
    }

    fun refreshConversations() {
        viewModelScope.launch {
            repository.refreshConversations()
        }
    }

    private var lastSendingReceiverId: String? = null

    fun sendVoiceMessage(receiverId: String, file: java.io.File) {
        lastSendingReceiverId = receiverId
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val myId = currentUser.value?.id ?: return@launch
            val username = currentUser.value?.username ?: "Utilisateur"
            
            // Insert temporary local sending message
            val tempId = "temp_voice_" + java.util.UUID.randomUUID().toString()
            val tempMsg = Message(
                id = tempId,
                senderId = myId,
                receiverId = receiverId,
                content = file.absolutePath, // Local path for immediate visual feedback / play
                type = "audio_sending",
                createdAt = System.currentTimeMillis()
            )
            repository.insertMessageLocal(tempMsg)
            repository.updateConversationLastMessage(
                otherUserId = receiverId,
                content = "Envoi d'un message vocal...",
                type = "audio_sending",
                isIncoming = false
            )
            
            try {
                val bytes = file.readBytes()
                val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                
                val success = com.example.utils.WebSocketManager.sendVoiceMessage(receiverId, b64, username)
                if (!success) {
                    repository.insertMessageLocal(tempMsg.copy(type = "audio_error"))
                    repository.updateConversationLastMessage(
                        otherUserId = receiverId,
                        content = "Échec de l'envoi",
                        type = "audio_error",
                        isIncoming = false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                repository.insertMessageLocal(tempMsg.copy(type = "audio_error"))
                repository.updateConversationLastMessage(
                    otherUserId = receiverId,
                    content = "Échec de l'envoi",
                    type = "audio_error",
                    isIncoming = false
                )
            } finally {
                try {
                    file.delete()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    fun sendMessage(receiverId: String, content: String, type: String = "text") {
        viewModelScope.launch {
            repository.sendMessage(receiverId, content, type)
        }
    }

    fun verifyCurrentUser() {
        viewModelScope.launch {
            repository.verifyCurrentUser()
        }
    }

    fun isFollowing(otherUserId: String): Flow<Boolean> {
        return repository.isFollowing(otherUserId)
    }

    fun followUser(otherUserId: String) {
        viewModelScope.launch {
            repository.followUser(otherUserId)
        }
    }

    fun unfollowUser(otherUserId: String) {
        viewModelScope.launch {
            repository.unfollowUser(otherUserId)
        }
    }

    fun getComments(actfileId: String): Flow<List<ActfileComment>> {
        return repository.getComments(actfileId)
    }

    fun getActfile(actfileId: String): Flow<ActfileWithUser?> {
        return repository.getActfile(actfileId)
    }
}
