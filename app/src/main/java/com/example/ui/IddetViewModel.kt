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
    private val _isFeedLoading = MutableStateFlow(true)
    val isFeedLoading: StateFlow<Boolean> = _isFeedLoading.asStateFlow()

    // Trending Topics powered by Google News & Search for Tech, AI & Markdown
    private val _trendingTopics = MutableStateFlow<List<com.example.data.TrendingTopic>>(emptyList())
    val trendingTopics: StateFlow<List<com.example.data.TrendingTopic>> = _trendingTopics.asStateFlow()

    private val _selectedTrendingCategory = MutableStateFlow(com.example.data.TrendingCategory.ALL)
    val selectedTrendingCategory: StateFlow<com.example.data.TrendingCategory> = _selectedTrendingCategory.asStateFlow()

    private val _isTrendingLoading = MutableStateFlow(false)
    val isTrendingLoading: StateFlow<Boolean> = _isTrendingLoading.asStateFlow()

    private val _composerInitialContent = MutableStateFlow<String?>(null)
    val composerInitialContent: StateFlow<String?> = _composerInitialContent.asStateFlow()

    fun setComposerInitialContent(content: String?) {
        _composerInitialContent.value = content
    }

    private val _realtimeStoryViews = MutableStateFlow<Map<String, Int>>(emptyMap())
    val realtimeStoryViews: StateFlow<Map<String, Int>> = _realtimeStoryViews.asStateFlow()

    fun trackStoryView(storyId: String, initialViews: Int) {
        val currentViews = _realtimeStoryViews.value[storyId] ?: initialViews
        // Simulate real-time increment on view
        val increment = (1..3).random()
        _realtimeStoryViews.value = _realtimeStoryViews.value + (storyId to (currentViews + increment))
    }

    // Existing selectTrendingCategory
    fun selectTrendingCategory(category: com.example.data.TrendingCategory) {
        _selectedTrendingCategory.value = category
        loadTrendingTopics(category)
    }

    fun refreshTrendingTopics() {
        loadTrendingTopics(_selectedTrendingCategory.value)
    }

    fun loadTrendingTopics(category: com.example.data.TrendingCategory = _selectedTrendingCategory.value) {
        viewModelScope.launch {
            _isTrendingLoading.value = true
            try {
                val topics = com.example.data.TrendingTopicsService.fetchTrendingTopics(category)
                _trendingTopics.value = topics
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isTrendingLoading.value = false
            }
        }
    }

    // Friends Live Radar / Real-Time Map & Real GPS Location
    private var realLocationProvider: com.example.data.RealLocationProvider? = null
    private val _realLocation = MutableStateFlow(com.example.data.UserRealLocation())
    val realLocation: StateFlow<com.example.data.UserRealLocation> = _realLocation.asStateFlow()

    val friendsLocations = com.example.data.FriendsLocationService.friends
    val isGhostMode = com.example.data.FriendsLocationService.isGhostMode
    val currentUserVibe = com.example.data.FriendsLocationService.currentUserVibe

    fun initLocationTracking(context: android.content.Context) {
        if (realLocationProvider == null) {
            val provider = com.example.data.RealLocationProvider(context.applicationContext)
            realLocationProvider = provider
            viewModelScope.launch {
                provider.locationFlow.collect { loc ->
                    _realLocation.value = loc
                }
            }
            provider.startLocationUpdates()
        } else {
            realLocationProvider?.startLocationUpdates()
        }
    }

    fun stopLocationTracking() {
        realLocationProvider?.stopLocationUpdates()
    }

    private val _lastWavedFriend = MutableStateFlow<String?>(null)
    val lastWavedFriend: StateFlow<String?> = _lastWavedFriend.asStateFlow()

    fun setGhostMode(enabled: Boolean) {
        com.example.data.FriendsLocationService.setGhostMode(enabled)
    }

    fun updateUserVibe(emoji: String, text: String, activityType: String) {
        com.example.data.FriendsLocationService.updateVibe(emoji, text, activityType)
    }

    fun toggleFavoriteFriend(friendId: String) {
        com.example.data.FriendsLocationService.toggleFavorite(friendId)
    }

    fun sendWaveToFriend(friend: com.example.data.FriendLocation) {
        viewModelScope.launch {
            _lastWavedFriend.value = friend.displayName
            // Trigger a simulated friendly response or notification
            kotlinx.coroutines.delay(3000)
            if (_lastWavedFriend.value == friend.displayName) {
                _lastWavedFriend.value = null
            }
        }
    }

    fun refreshFriendsRadar() {
        com.example.data.FriendsLocationService.simulateRadarPing()
    }

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

    private val _stories = MutableStateFlow<List<com.example.data.Story>>(emptyList())
    val stories: StateFlow<List<com.example.data.Story>> = _stories.asStateFlow()

    private val _isStoriesLoading = MutableStateFlow(false)
    val isStoriesLoading: StateFlow<Boolean> = _isStoriesLoading.asStateFlow()

    private val _isUploadingStory = MutableStateFlow(false)
    val isUploadingStory: StateFlow<Boolean> = _isUploadingStory.asStateFlow()

    fun refreshActfiles() {
        viewModelScope.launch {
            _isFeedLoading.value = true
            try {
                repository.refreshActfiles()
            } finally {
                _isFeedLoading.value = false
            }
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
            loadStories()
            loadTrendingTopics()
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
                        val detectedType = if (com.example.utils.AudioMessageHelper.isAudioContent(event.content, event.msgType)) "audio" else event.msgType
                        val incomingMsg = Message(
                            id = event.messageId,
                            senderId = event.senderId,
                            receiverId = currentUser.value?.id ?: "",
                            content = event.content,
                            type = detectedType,
                            isRead = false,
                            createdAt = parsedTime
                        )
                        repository.insertMessageLocal(incomingMsg)
                        repository.updateConversationLastMessage(
                            otherUserId = event.senderId,
                            content = event.content,
                            type = detectedType,
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
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchInContent = MutableStateFlow(true)
    val searchInContent: StateFlow<Boolean> = _searchInContent.asStateFlow()

    private val _searchInTags = MutableStateFlow(true)
    val searchInTags: StateFlow<Boolean> = _searchInTags.asStateFlow()

    private val _searchInUsers = MutableStateFlow(true)
    val searchInUsers: StateFlow<Boolean> = _searchInUsers.asStateFlow()

    private val _searchSortOrder = MutableStateFlow("Recent") // Recent, Popular
    val searchSortOrder: StateFlow<String> = _searchSortOrder.asStateFlow()

    fun setSearchOptions(content: Boolean, tags: Boolean, users: Boolean) {
        _searchInContent.value = content
        _searchInTags.value = tags
        _searchInUsers.value = users
    }

    fun setSearchSortOrder(order: String) {
        _searchSortOrder.value = order
    }

    val searchUsersResult: StateFlow<List<User>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) flowOf(emptyList()) else repository.searchUsers(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchActfilesResult: StateFlow<List<ActfileWithUser>> = combine(
        _searchQuery, _searchInContent, _searchInTags, _searchInUsers, _searchSortOrder
    ) { query, inContent, inTags, inUsers, sortOrder ->
        if (query.isBlank()) return@combine emptyList<ActfileWithUser>()
        
        repository.searchActfiles(query).first().filter { actfile ->
            val matchContent = inContent && actfile.content.contains(query, ignoreCase = true)
            val matchTags = inTags && actfile.tags?.contains(query, ignoreCase = true) == true
            val matchUsers = inUsers && actfile.username.contains(query, ignoreCase = true)
            matchContent || matchTags || matchUsers
        }.let { list ->
            if (sortOrder == "Popular") {
                list.sortedByDescending { it.likesCount + it.viewsCount }
            } else {
                list.sortedByDescending { it.createdAt }
            }
        }
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

    fun signupFull(
        username: String, 
        password: String?, 
        avatarUrl: String?,
        bio: String,
        email: String?,
        phoneNumber: String?,
        birthDate: String?,
        zodiacSign: String?
    ) {
        viewModelScope.launch {
            try {
                repository.signup(username, password)
                repository.updateProfile(
                    username = null,
                    avatarUrl = avatarUrl,
                    bio = bio,
                    privacySetting = "public",
                    email = email,
                    phoneNumber = phoneNumber,
                    birthDate = birthDate,
                    zodiacSign = zodiacSign
                )
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

    fun publishActfile(
        content: String,
        tags: String = "",
        category: String? = null,
        communityId: String? = null,
        channelId: String? = null
    ) {
        viewModelScope.launch {
            repository.publishActfile(content, tags, category, communityId, channelId)
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
                // Encode the audio file to base64
                val bytes = file.readBytes()
                val audioB64 = "base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                
                // Transmit the audio data directly via REST API as base64 content
                val sentMsg = repository.sendMessage(receiverId, audioB64, "audio")
                
                // Remove temporary sending placeholder and let the server response populate it
                repository.deleteMessageLocal(tempId)
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

    // Communities and Channels Support
    fun searchCommunitiesFlow(query: String?, category: String?, sort: String = "popular"): Flow<List<com.example.data.Community>> {
        return flow {
            emit(repository.searchCommunities(query, category, sort))
        }
    }

    fun getCommunityFlow(slug: String): Flow<com.example.data.Community?> {
        return flow {
            emit(repository.getCommunity(slug))
        }
    }

    fun getCommunityChannelsFlow(slug: String): Flow<List<com.example.data.Channel>> {
        return flow {
            emit(repository.getCommunityChannels(slug))
        }
    }

    fun getCommunityPostsFlow(slug: String): Flow<List<com.example.data.ActfileWithUser>> {
        return repository.getCommunityPostsFlow(slug)
    }

    fun createChannel(slug: String, name: String, description: String?, onResult: (com.example.data.Channel?) -> Unit) {
        viewModelScope.launch {
            val result = repository.createChannel(slug, name, description)
            onResult(result)
        }
    }

    fun getMyCommunitiesFlow(): Flow<List<com.example.data.Community>> {
        return flow {
            emit(repository.getMyCommunities())
        }
    }

    fun toggleCommunityJoin(slug: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.joinCommunity(slug)
            onComplete(success)
        }
    }

    fun createCommunity(
        name: String,
        category: String,
        description: String,
        isPrivate: Boolean,
        onResult: (com.example.data.Community?) -> Unit
    ) {
        viewModelScope.launch {
            val com = repository.createCommunity(name, category, description, isPrivate)
            onResult(com)
        }
    }

    fun updateCommunity(
        slug: String,
        name: String?,
        description: String?,
        category: String?,
        isPrivate: Boolean?,
        iconUrl: String? = null,
        onResult: (com.example.data.Community?) -> Unit
    ) {
        viewModelScope.launch {
            val com = repository.updateCommunity(slug, name, description, category, isPrivate, iconUrl)
            onResult(com)
        }
    }

    fun updateCommunityIcon(
        slug: String,
        iconFile: java.io.File,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.updateCommunityIcon(slug, iconFile)
            onResult(success)
        }
    }

    fun loadStories() {
        viewModelScope.launch {
            _isStoriesLoading.value = true
            try {
                val list = repository.getStories()
                _stories.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isStoriesLoading.value = false
            }
        }
    }

    fun createStory(
        context: android.content.Context,
        uri: android.net.Uri,
        effect: String?,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isUploadingStory.value = true
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val extension = when {
                    mimeType.contains("png") -> "png"
                    mimeType.contains("video") || mimeType.contains("mp4") -> "mp4"
                    mimeType.contains("audio") || mimeType.contains("m4a") -> "m4a"
                    mimeType.contains("webm") -> "webm"
                    else -> "jpg"
                }
                val tempFile = java.io.File.createTempFile("story_upload_", ".$extension", context.cacheDir)
                contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val success = repository.createStory(tempFile, mimeType, effect)
                try { tempFile.delete() } catch (_: Exception) {}

                if (success) {
                    loadStories()
                    onComplete(true, null)
                } else {
                    onComplete(false, "Impossible d'envoyer la story")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, e.message ?: "Erreur inconnue")
            } finally {
                _isUploadingStory.value = false
            }
        }
    }

    fun sendStoryReaction(
        receiverId: String,
        reaction: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.sendMessage(receiverId, reaction, "text")
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendStoryReply(
        receiverId: String,
        replyText: String,
        storyMediaUrl: String?,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val formattedMessage = if (!storyMediaUrl.isNullOrBlank()) {
                    "📷 Réponse à votre story: $replyText"
                } else {
                    replyText
                }
                repository.sendMessage(receiverId, formattedMessage, "text")
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteMessage(id: String) {
        viewModelScope.launch {
            repository.deleteMessage(id)
        }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        viewModelScope.launch {
            repository.reactToMessage(messageId, emoji)
        }
    }

    fun removeMessageReaction(messageId: String) {
        viewModelScope.launch {
            repository.removeMessageReaction(messageId)
        }
    }

    fun deleteStory(id: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.deleteStory(id)
            if (success) {
                _stories.value = _stories.value.filter { it.id != id }
            }
            onComplete(success)
        }
    }

    // ── NIVEAUX & RANGS ──────────────────────────────────────────
    private val _myLevel = MutableStateFlow<com.example.data.UserLevelResponse?>(null)
    val myLevel: StateFlow<com.example.data.UserLevelResponse?> = _myLevel.asStateFlow()

    private val _levelsTable = MutableStateFlow<List<com.example.data.LevelInfo>>(emptyList())
    val levelsTable: StateFlow<List<com.example.data.LevelInfo>> = _levelsTable.asStateFlow()

    fun refreshMyLevel() {
        viewModelScope.launch {
            val res = repository.getMyLevel()
            if (res != null) {
                _myLevel.value = res
            }
        }
    }

    fun loadLevelsTable() {
        viewModelScope.launch {
            _levelsTable.value = repository.getLevelsTable()
        }
    }

    fun getUserLevelFlow(userId: String): Flow<com.example.data.UserLevelResponse?> = flow {
        emit(repository.getUserLevel(userId))
    }
}
