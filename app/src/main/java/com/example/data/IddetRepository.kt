package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.DelicateCoroutinesApi
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MediaType.Companion.toMediaType

class IddetRepository(
    private val userDao: UserDao,
    private val actfileDao: ActfileDao,
    private val messageDao: MessageDao,
    private val followDao: FollowDao,
    private val commentDao: CommentDao,
    private val notificationDao: NotificationDao,
    private val prefs: android.content.SharedPreferences
) {
    // Current logged in user (in-memory mock)
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val localChannels = mutableMapOf<String, MutableList<Channel>>()
    
    private val _selectedTheme = MutableStateFlow(AppTheme.valueOf(prefs.getString("selected_theme", AppTheme.DEFAULT.name) ?: AppTheme.DEFAULT.name))
    val selectedTheme: StateFlow<AppTheme> = _selectedTheme.asStateFlow()

    private val _targetLanguage = MutableStateFlow(prefs.getString("target_language", "French") ?: "French")
    val targetLanguage: StateFlow<String> = _targetLanguage.asStateFlow()

    fun setSelectedTheme(theme: AppTheme) {
        _selectedTheme.value = theme
        prefs.edit().putString("selected_theme", theme.name).apply()
    }

    fun setTargetLanguage(language: String) {
        _targetLanguage.value = language
        prefs.edit().putString("target_language", language).apply()
    }

    private var currentToken: String? = prefs.getString("auth_token", null)
    val userToken: String? get() = currentToken
    private val viewedActfiles = mutableSetOf<String>()

    init {
        val savedUserId = prefs.getString("user_id", null)
        if (savedUserId != null) {
            kotlinx.coroutines.GlobalScope.launch {
                val user = userDao.getUserById(savedUserId)
                _currentUser.value = user
                try {
                    val token = currentToken
                    if (token != null) {
                        val profile = RetrofitClient.apiService.getMyProfile("Bearer $token")
                        val updatedUser = user?.copy(
                            username = profile.username,
                            avatarUrl = profile.avatar_url,
                            bio = profile.bio ?: user.bio,
                            isVerified = profile.is_verified,
                            followingCount = profile.following_count,
                            followersCount = profile.followers_count
                        ) ?: User(
                            id = profile.id ?: savedUserId ?: "",
                            username = profile.username,
                            passwordHash = "mocked",
                            avatarUrl = profile.avatar_url,
                            bio = profile.bio ?: "Welcome to my profile!",
                            isVerified = profile.is_verified,
                            followingCount = profile.following_count,
                            followersCount = profile.followers_count,
                            isGiant = (profile.username.length > 5)
                        )
                        userDao.insertUser(updatedUser)
                        _currentUser.value = updatedUser
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun refreshProfile() {
        val userId = prefs.getString("user_id", null) ?: return
        try {
            val token = currentToken
            if (token != null) {
                val profile = RetrofitClient.apiService.getMyProfile("Bearer $token")
                val existing = userDao.getUserById(userId)
                val updatedUser = if (existing != null) {
                    existing.copy(
                        username = profile.username,
                        avatarUrl = profile.avatar_url,
                        bio = profile.bio ?: existing.bio,
                        isVerified = profile.is_verified,
                        followingCount = profile.following_count,
                        followersCount = profile.followers_count
                    )
                } else {
                    User(
                        id = profile.id ?: userId,
                        username = profile.username,
                        passwordHash = "mocked",
                        avatarUrl = profile.avatar_url,
                        bio = profile.bio ?: "Welcome to my profile!",
                        isVerified = profile.is_verified,
                        followingCount = profile.following_count,
                        followersCount = profile.followers_count,
                        isGiant = (profile.username.length > 5)
                    )
                }
                userDao.insertUser(updatedUser)
                _currentUser.value = updatedUser
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseIso(date: String?): Long {
        if (date == null) return System.currentTimeMillis()
        return try {
            val cleanDate = date.replace("Z", "+0000")
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", java.util.Locale.US)
            format.parse(cleanDate)?.time ?: System.currentTimeMillis()
        } catch (e1: Exception) {
            try {
                val cleanDate = date.replace("Z", "+0000")
                val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", java.util.Locale.US)
                format.parse(cleanDate)?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    suspend fun refreshActfiles() {
        try {
            val header = currentToken?.let { "Bearer $it" }
            val netActfiles = RetrofitClient.apiService.getActfiles(header)
            
            val actfilesWithComments = coroutineScope {
                val deferreds = netActfiles.map { net ->
                    async {
                        val comments = try {
                            val list = RetrofitClient.apiService.getActfileComments(header, net.id)
                            list.map {
                                ActfileComment(
                                    id = it.id,
                                    actfileId = net.id,
                                    content = it.content,
                                    userId = it.user_id,
                                    username = it.username,
                                    avatarUrl = it.avatar_url,
                                    isVerified = it.is_verified,
                                    createdAt = parseIso(it.created_at)
                                )
                            }
                        } catch(e: Exception) {
                            emptyList<ActfileComment>()
                        }
                        net to comments
                    }
                }
                deferreds.awaitAll()
            }

            for ((net, comments) in actfilesWithComments) {
                val existing = userDao.getUserById(net.user_id)
                val user = if (existing != null) {
                    existing.copy(
                        username = net.username,
                        avatarUrl = net.avatar_url,
                        isVerified = net.is_verified
                    )
                } else {
                    User(
                        id = net.user_id,
                        username = net.username,
                        passwordHash = "mocked",
                        avatarUrl = net.avatar_url,
                        isVerified = net.is_verified
                    )
                }
                userDao.insertUser(user)
                
                if (comments.isNotEmpty()) {
                    commentDao.deleteCommentsForActfile(net.id)
                    commentDao.insertComments(comments)
                }
                
                actfileDao.insertActfile(
                    Actfile(
                        id = net.id,
                        userId = net.user_id,
                        content = net.content,
                        likesCount = net.likes_count,
                        viewsCount = net.views_count,
                        commentsCount = comments.size,
                        createdAt = parseIso(net.created_at),
                        isLikedByMe = net.liked,
                        category = net.category,
                        communityId = net.community_id,
                        channelId = net.channel_id,
                        channelSlug = net.channel_slug,
                        channelName = net.channel_name
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun login(username: String, password: String? = null) {
        try {
            val response = RetrofitClient.apiService.login(username, password)
            currentToken = response.access_token
            prefs.edit().putString("auth_token", currentToken).apply()
            
            val profile = RetrofitClient.apiService.getMyProfile("Bearer $currentToken")
            val existing = userDao.getUserById(profile.id ?: "")
            val user = if (existing != null) {
                existing.copy(
                    username = profile.username,
                    avatarUrl = profile.avatar_url,
                    bio = profile.bio ?: existing.bio,
                    isVerified = profile.is_verified,
                    followingCount = profile.following_count,
                    followersCount = profile.followers_count
                )
            } else {
                User(
                    id = profile.id ?: "",
                    username = profile.username,
                    passwordHash = "mocked",
                    avatarUrl = profile.avatar_url,
                    bio = profile.bio ?: "Welcome to my profile!",
                    isVerified = profile.is_verified,
                    followingCount = profile.following_count,
                    followersCount = profile.followers_count,
                    isGiant = (profile.username.length > 5)
                )
            }
            userDao.insertUser(user)
            _currentUser.value = user
            prefs.edit().putString("user_id", user.id).apply()
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to login: ${e.message}")
        }
    }

    suspend fun signup(username: String, password: String? = null) {
        try {
            RetrofitClient.apiService.signup(RegisterRequest(username, password))
            login(username, password)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to signup: ${e.message}")
        }
    }

    suspend fun updateProfile(
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
        val user = _currentUser.value ?: return
        val updatedUser = user.copy(
            username = username ?: user.username,
            avatarUrl = avatarUrl ?: user.avatarUrl,
            bio = bio,
            privacySetting = privacySetting,
            email = email ?: user.email,
            phoneNumber = phoneNumber ?: user.phoneNumber,
            birthDate = birthDate ?: user.birthDate,
            zodiacSign = zodiacSign ?: user.zodiacSign,
            preferredCategory = preferredCategory ?: user.preferredCategory
        )
        userDao.updateUser(updatedUser)
        _currentUser.value = updatedUser

        // Sync with server if possible
        try {
            val token = currentToken
            if (token != null) {
                RetrofitClient.apiService.updateProfile(
                    token = "Bearer $token",
                    request = UpdateProfileRequest(
                        bio = bio,
                        username = username,
                        avatar_url = avatarUrl,
                        preferred_category = preferredCategory ?: user.preferredCategory
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateProfileWithImage(
        avatarFile: java.io.File?,
        bio: String?,
        phoneNumber: String?
    ): UserProfileNetwork {
        val token = currentToken ?: throw Exception("Not logged in")
        
        val avatarPart = if (avatarFile != null) {
            val mediaType = "image/jpeg".toMediaType()
            val requestFile = avatarFile.asRequestBody(mediaType)
            MultipartBody.Part.createFormData("avatar", avatarFile.name, requestFile)
        } else null
        
        val bioBody = if (bio != null) {
            bio.toRequestBody("text/plain".toMediaType())
        } else null
        
        val phoneBody = if (phoneNumber != null) {
            phoneNumber.toRequestBody("text/plain".toMediaType())
        } else null
        
        val profile = RetrofitClient.apiService.updateProfileMultipart(
            token = "Bearer $token",
            avatar = avatarPart,
            bio = bioBody,
            phoneNumber = phoneBody
        )
        
        val user = _currentUser.value
        if (user != null) {
            val updatedUser = user.copy(
                username = profile.username,
                avatarUrl = profile.avatar_url,
                bio = profile.bio ?: user.bio,
                isVerified = profile.is_verified,
                email = profile.email ?: user.email,
                phoneNumber = profile.phone_number ?: user.phoneNumber,
                zodiacSign = profile.zodiac_sign ?: user.zodiacSign,
                followersCount = profile.followers_count,
                followingCount = profile.following_count
            )
            userDao.insertUser(updatedUser)
            _currentUser.value = updatedUser
        }
        
        return profile
    }

    fun logout() {
        _currentUser.value = null
        currentToken = null
        prefs.edit().clear().apply()
    }

    suspend fun publishActfile(
        content: String,
        tags: String = "",
        category: String? = null,
        communityId: String? = null,
        channelId: String? = null
    ) {
        val user = _currentUser.value ?: return
        
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                val netActfile = RetrofitClient.apiService.publishActfile(
                    token = header,
                    request = PublishActfileRequest(
                        content = content,
                        category = category,
                        community_id = communityId,
                        channel_id = channelId
                    )
                )
                actfileDao.insertActfile(
                    Actfile(
                        id = netActfile.id,
                        userId = netActfile.user_id,
                        content = netActfile.content,
                        tags = tags,
                        likesCount = netActfile.likes_count,
                        viewsCount = netActfile.views_count,
                        createdAt = parseIso(netActfile.created_at),
                        category = netActfile.category ?: category,
                        communityId = netActfile.community_id ?: communityId,
                        channelId = netActfile.channel_id ?: channelId,
                        channelSlug = netActfile.channel_slug,
                        channelName = netActfile.channel_name
                    )
                )
            } else {
                actfileDao.insertActfile(
                    Actfile(
                        userId = user.id,
                        content = content,
                        tags = tags,
                        category = category,
                        communityId = communityId,
                        channelId = channelId
                    )
                )
            }
        } catch(e: Exception) {
            e.printStackTrace()
            // fallback
            actfileDao.insertActfile(
                Actfile(
                    userId = user.id,
                    content = content,
                    tags = tags,
                    category = category,
                    communityId = communityId,
                    channelId = channelId
                )
            )
        }

        // Simple mock leveling up logic
        val newXp = user.xp + 50
        val newLevel = (newXp / 100) + 1
        var newBadges = user.badges
        if (newLevel >= 2 && !newBadges.contains("Creator")) {
            newBadges = if (newBadges.isEmpty()) "Creator" else "$newBadges, Creator"
        }
        val updatedUser = user.copy(xp = newXp, level = newLevel, badges = newBadges)
        userDao.updateUser(updatedUser)
        _currentUser.value = updatedUser
    }

    suspend fun deleteActfile(actfileId: String) {
        actfileDao.deleteActfileLocal(actfileId)
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                RetrofitClient.apiService.deleteActfile(header, actfileId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllActfiles(): Flow<List<ActfileWithUser>> {
        return actfileDao.getAllActfilesWithUser()
    }
    
    fun getUserActfiles(userId: String): Flow<List<ActfileWithUser>> {
        return actfileDao.getActfilesByUser(userId)
    }

    suspend fun incrementView(actfileId: String) {
        if (viewedActfiles.contains(actfileId)) return
        viewedActfiles.add(actfileId)
        actfileDao.incrementViewCount(actfileId)
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                RetrofitClient.apiService.viewActfile(header, actfileId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun likeActfile(actfileId: String) {
        val raw = actfileDao.getActfileRaw(actfileId) ?: return
        val currentLiked = raw.isLikedByMe
        val newLiked = !currentLiked
        val newLikesCount = if (newLiked) raw.likesCount + 1 else kotlin.math.max(0, raw.likesCount - 1)
        
        // Instant visual feedback locally
        val updatedActfile = raw.copy(
            isLikedByMe = newLiked,
            likesCount = newLikesCount
        )
        actfileDao.insertActfile(updatedActfile)
        
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                val res = RetrofitClient.apiService.likeActfile(header, actfileId)
                val serverLikedState = res["liked"] ?: res["success"] ?: newLiked
                if (serverLikedState is Boolean && serverLikedState != newLiked) {
                    val finalActfile = raw.copy(
                        isLikedByMe = serverLikedState,
                        likesCount = if (serverLikedState) {
                            if (!currentLiked) raw.likesCount + 1 else raw.likesCount
                        } else {
                            if (currentLiked) kotlin.math.max(0, raw.likesCount - 1) else raw.likesCount
                        }
                    )
                    actfileDao.insertActfile(finalActfile)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        if (newLiked) {
            val user = _currentUser.value ?: return
            val newXp = user.xp + 10
            val newLevel = (newXp / 100) + 1
            var newBadges = user.badges
            if (newLevel >= 5 && !newBadges.contains("Active")) {
                newBadges = if (newBadges.isEmpty()) "Active" else "$newBadges, Active"
            }
            val updatedUser = user.copy(xp = newXp, level = newLevel, badges = newBadges)
            userDao.updateUser(updatedUser)
            _currentUser.value = updatedUser
        }
    }

    suspend fun commentActfile(actfileId: String, content: String) {
        actfileDao.incrementCommentCount(actfileId)
        val myUser = _currentUser.value
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                val res = RetrofitClient.apiService.commentActfile(header, actfileId, ActfileCommentCreate(content))
                commentDao.insertComment(
                    ActfileComment(
                        id = res.id,
                        actfileId = actfileId,
                        content = res.content,
                        userId = res.user?.id ?: myUser?.id ?: "unknown",
                        username = res.user?.username ?: myUser?.username ?: "Me",
                        avatarUrl = res.user?.avatar_url ?: myUser?.avatarUrl,
                        isVerified = myUser?.isVerified ?: false,
                        createdAt = parseIso(res.created_at)
                    )
                )
            } else {
                commentDao.insertComment(
                    ActfileComment(
                        actfileId = actfileId,
                        content = content,
                        userId = myUser?.id ?: "local_user",
                        username = myUser?.username ?: "Me",
                        avatarUrl = myUser?.avatarUrl,
                        isVerified = myUser?.isVerified ?: false
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to local insertion
            commentDao.insertComment(
                ActfileComment(
                    actfileId = actfileId,
                    content = content,
                    userId = myUser?.id ?: "local_user",
                    username = myUser?.username ?: "Me",
                    avatarUrl = myUser?.avatarUrl,
                    isVerified = myUser?.isVerified ?: false
                )
            )
        }
    }

    fun getComments(actfileId: String): Flow<List<ActfileComment>> {
        kotlinx.coroutines.GlobalScope.launch {
            try {
                val header = currentToken?.let { "Bearer $it" }
                val commentsList = RetrofitClient.apiService.getActfileComments(header, actfileId)
                val mapped = commentsList.map {
                    ActfileComment(
                        id = it.id,
                        actfileId = actfileId,
                        content = it.content,
                        userId = it.user_id,
                        username = it.username,
                        avatarUrl = it.avatar_url,
                        isVerified = it.is_verified,
                        createdAt = parseIso(it.created_at)
                    )
                }
                commentDao.deleteCommentsForActfile(actfileId)
                commentDao.insertComments(mapped)
                actfileDao.updateCommentCount(actfileId, mapped.size)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return commentDao.getCommentsForActfile(actfileId)
    }

    fun getActfile(actfileId: String): Flow<ActfileWithUser?> {
        return actfileDao.getActfileById(actfileId)
    }

    fun searchUsers(query: String): Flow<List<User>> = kotlinx.coroutines.flow.flow {
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                val res = RetrofitClient.apiService.search(header, query, "users")
                emit(res.results.map {
                    User(
                        id = it.id,
                        username = it.username ?: "Unknown",
                        passwordHash = "mocked",
                        avatarUrl = it.avatar_url,
                        bio = it.bio ?: "",
                        isVerified = it.is_verified ?: false
                    )
                })
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    fun searchActfiles(query: String): Flow<List<ActfileWithUser>> {
        return actfileDao.searchActfiles(query)
    }
    
    fun getGiants(): Flow<List<User>> {
        val userId = _currentUser.value?.id ?: ""
        return userDao.getGiants(userId)
    }
    
    fun getUserFlow(userId: String): Flow<User?> {
        kotlinx.coroutines.GlobalScope.launch {
            try {
                val header = currentToken?.let { "Bearer $it" }
                if (header != null) {
                    val res = RetrofitClient.apiService.getUserProfile(header, userId)
                    val existing = userDao.getUserById(userId)
                    val user = if (existing != null) {
                        existing.copy(
                            username = res.username,
                            avatarUrl = res.avatar_url,
                            bio = res.bio ?: existing.bio,
                            isVerified = res.is_verified,
                            followersCount = res.followers_count,
                            followingCount = res.following_count
                        )
                    } else {
                        User(
                            id = res.id ?: userId,
                            username = res.username,
                            passwordHash = "mocked",
                            avatarUrl = res.avatar_url,
                            bio = res.bio ?: "",
                            isVerified = res.is_verified,
                            followersCount = res.followers_count,
                            followingCount = res.following_count
                        )
                    }
                    userDao.insertUser(user)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return userDao.getUserByIdFlow(userId)
    }

    private val _conversations = MutableStateFlow<List<ConversationNetwork>>(emptyList())
    val conversations: StateFlow<List<ConversationNetwork>> = _conversations.asStateFlow()

    fun updateConversationLastMessage(otherUserId: String, content: String, type: String = "text", isIncoming: Boolean = false, senderUsername: String? = null) {
        val current = _conversations.value.toMutableList()
        val index = current.indexOfFirst { it.user_id == otherUserId }
        val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).format(java.util.Date())
        val displayContent = if (type.startsWith("audio")) {
            if (content.startsWith("http") || content.startsWith("/") || content.startsWith("[Voice Message]")) {
                content
            } else {
                "[Voice Message](voice://duration=5&amplitudes=0.5)"
            }
        } else {
            content
        }
        
        if (index != -1) {
            val existing = current.removeAt(index)
            val updated = existing.copy(
                last_message = displayContent,
                last_message_time = nowIso,
                unread_count = if (isIncoming) (existing.unread_count ?: 0) + 1 else existing.unread_count
            )
            current.add(0, updated)
        } else {
            val newConv = ConversationNetwork(
                id = "temp_conv_${otherUserId}",
                user_id = otherUserId,
                username = senderUsername ?: "Utilisateur",
                avatar_url = null,
                last_message = displayContent,
                last_message_time = nowIso,
                unread_count = if (isIncoming) 1 else 0,
                is_online = true,
                is_verified = false
            )
            current.add(0, newConv)
        }
        _conversations.value = current
    }

    suspend fun getCategories(): List<String> {
        return try {
            RetrofitClient.apiService.getCategories().categories
        } catch (e: Exception) {
            e.printStackTrace()
            listOf("Fun", "Amour", "Motivation", "Tech", "Sport", "Musique", "Actu", "Business", "Spiritualité", "Autres")
        }
    }

    suspend fun getMyPreferredCategories(): List<String> {
        val token = currentToken ?: return emptyList()
        return try {
            RetrofitClient.apiService.getMyPreferredCategories("Bearer $token").preferred_categories
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun updatePreferredCategories(categories: List<String>): List<String> {
        val token = currentToken ?: return emptyList()
        return try {
            val response = RetrofitClient.apiService.updatePreferredCategories(
                token = "Bearer $token",
                body = UpdateCategoriesRequest(categories = categories)
            )
            response.preferred_categories
        } catch (e: Exception) {
            e.printStackTrace()
            categories
        }
    }

    suspend fun refreshConversations() {
        val userId = _currentUser.value?.id ?: return
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                val res = RetrofitClient.apiService.getConversations(header)
                _conversations.value = res
                res.forEach { conv ->
                    val existing = userDao.getUserById(conv.user_id)
                    val partnerUser = if (existing != null) {
                        existing.copy(
                            username = conv.username,
                            avatarUrl = conv.avatar_url
                        )
                    } else {
                        User(
                            id = conv.user_id,
                            username = conv.username,
                            passwordHash = "mocked",
                            avatarUrl = conv.avatar_url,
                            isVerified = conv.is_verified
                        )
                    }
                    userDao.insertUser(partnerUser)
                    
                    val lastMsgContent = conv.last_message ?: ""
                    val lastMsgTime = parseIso(conv.last_message_time)
                    messageDao.insertMessage(
                        Message(
                            id = "conv_${conv.id}",
                            senderId = conv.user_id,
                            receiverId = userId,
                            content = lastMsgContent,
                            createdAt = lastMsgTime
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun refreshMessagesWith(otherUserId: String) {
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                val res = RetrofitClient.apiService.getMessages(header, otherUserId)
                val msgs = res.map {
                    Message(
                        id = it.id,
                        senderId = it.sender_id,
                        receiverId = it.receiver_id,
                        content = it.content,
                        type = if (com.example.utils.AudioMessageHelper.isAudioContent(it.content, it.type)) "audio" else it.type,
                        isRead = it.read,
                        createdAt = parseIso(it.created_at)
                    )
                }
                msgs.forEach { messageDao.insertMessage(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getChatPartners(): Flow<List<User>> {
        val userId = _currentUser.value?.id ?: ""
        kotlinx.coroutines.GlobalScope.launch {
            refreshConversations()
        }
        return userDao.getChatPartners(userId)
    }

    fun getMessagesWith(otherUserId: String): Flow<List<Message>> {
        val myId = _currentUser.value?.id ?: ""
        kotlinx.coroutines.GlobalScope.launch {
            refreshMessagesWith(otherUserId)
        }
        return messageDao.getMessagesBetween(myId, otherUserId)
    }

    suspend fun sendMessage(receiverId: String, content: String, type: String = "text") {
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                val res = RetrofitClient.apiService.sendMessage(header, SendMessageRequest(content = content, receiver_id = receiverId, type = type))
                messageDao.insertMessage(Message(
                    id = res.id,
                    senderId = res.sender_id,
                    receiverId = res.receiver_id,
                    content = res.content,
                    type = res.type,
                    isRead = res.read,
                    createdAt = parseIso(res.created_at)
                ))
            } else {
                val myId = _currentUser.value?.id ?: return
                messageDao.insertMessage(Message(senderId = myId, receiverId = receiverId, content = content, type = type))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val myId = _currentUser.value?.id ?: return
            messageDao.insertMessage(Message(senderId = myId, receiverId = receiverId, content = content, type = type))
        }
    }

    suspend fun sendAudioMessageMultipart(receiverId: String, file: java.io.File): MessageNetwork? {
        return try {
            val header = currentToken?.let { "Bearer $it" } ?: return null
            val requestFile = file.asRequestBody("audio/m4a".toMediaType())
            val body = okhttp3.MultipartBody.Part.createFormData("file", file.name, requestFile)
            val receiverPart = receiverId.toRequestBody("text/plain".toMediaType())
            
            val res = RetrofitClient.apiService.sendAudioMessage(header, receiverPart, body)
            messageDao.insertMessage(Message(
                id = res.id,
                senderId = res.sender_id,
                receiverId = res.receiver_id,
                content = res.content,
                type = res.type,
                isRead = res.read,
                createdAt = parseIso(res.created_at)
            ))
            res
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun insertMessageLocal(message: Message) {
        messageDao.insertMessage(message)
    }

    suspend fun deleteMessageLocal(id: String) {
        messageDao.deleteMessage(id)
    }

    suspend fun verifyCurrentUser() {
        val user = _currentUser.value ?: return
        val updatedUser = user.copy(isVerified = true)
        userDao.updateUser(updatedUser)
        _currentUser.value = updatedUser
    }

    fun getFollowedActfiles(): Flow<List<ActfileWithUser>> {
        val myId = _currentUser.value?.id ?: ""
        return followDao.getFollowedActfiles(myId)
    }

    fun isFollowing(otherUserId: String): Flow<Boolean> {
        val myId = _currentUser.value?.id ?: ""
        return followDao.isFollowingFlow(myId, otherUserId)
    }

    suspend fun followUser(otherUserId: String) {
        val myId = _currentUser.value?.id ?: return
        if (myId == otherUserId) return
        
        // 1. Insert local follow record
        followDao.insertFollow(Follow(followerId = myId, followingId = otherUserId))

        // 2. Update follower/following counts locally for current user
        val me = userDao.getUserById(myId)
        if (me != null) {
            val updatedMe = me.copy(followingCount = me.followingCount + 1)
            userDao.insertUser(updatedMe)
            _currentUser.value = updatedMe
        }

        // 3. Update follower/following counts locally for other user
        val other = userDao.getUserById(otherUserId)
        if (other != null) {
            val updatedOther = other.copy(followersCount = other.followersCount + 1)
            userDao.insertUser(updatedOther)
        }

        // 4. Try network call
        try {
            val token = currentToken
            if (token != null) {
                RetrofitClient.apiService.followUser("Bearer $token", otherUserId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun unfollowUser(otherUserId: String) {
        val myId = _currentUser.value?.id ?: return
        
        // 1. Delete local follow record
        followDao.deleteFollow(myId, otherUserId)

        // 2. Update follower/following counts locally for current user
        val me = userDao.getUserById(myId)
        if (me != null) {
            val updatedMe = me.copy(followingCount = (me.followingCount - 1).coerceAtLeast(0))
            userDao.insertUser(updatedMe)
            _currentUser.value = updatedMe
        }

        // 3. Update follower/following counts locally for other user
        val other = userDao.getUserById(otherUserId)
        if (other != null) {
            val updatedOther = other.copy(followersCount = (other.followersCount - 1).coerceAtLeast(0))
            userDao.insertUser(updatedOther)
        }

        // 4. Try network call
        try {
            val token = currentToken
            if (token != null) {
                RetrofitClient.apiService.unfollowUser("Bearer $token", otherUserId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLikedActfiles(): Flow<List<ActfileWithUser>> {
        return actfileDao.getLikedActfiles()
    }

    fun getCommentedActfiles(userId: String): Flow<List<ActfileWithUser>> {
        return actfileDao.getCommentedActfiles(userId)
    }

    fun getNotifications(): Flow<List<Notification>> {
        val userId = _currentUser.value?.id ?: ""
        kotlinx.coroutines.GlobalScope.launch {
            refreshNotifications()
        }
        return notificationDao.getNotificationsForUser(userId)
    }

    suspend fun refreshNotifications() {
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                val res = RetrofitClient.apiService.getNotifications(header)
                val mapped = res.map {
                    Notification(
                        id = it.id,
                        userId = _currentUser.value?.id ?: "",
                        type = it.type,
                        fromUserId = it.from_user_id,
                        fromUsername = it.from_username ?: "Unknown",
                        fromAvatar = it.from_avatar,
                        message = it.message,
                        targetId = it.target_id,
                        isRead = it.read,
                        createdAt = parseIso(it.created_at)
                    )
                }
                notificationDao.insertNotifications(mapped)

                // Real Android system notifications integration
                val context = AppDatabase.appContext
                if (context != null) {
                    val shownNotifications = prefs.getStringSet("shown_notification_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
                    var updated = false
                    val newUnread = mapped.filter { !it.isRead && !shownNotifications.contains(it.id) }
                    
                    // Only show up to 5 most recent system notifications to prevent system limit error
                    newUnread.takeLast(5).forEach { notif ->
                        val route = when (notif.type) {
                            "like", "comment" -> "discussion/${notif.targetId}"
                            "message" -> "chat/${notif.fromUserId}"
                            "follow" -> "profile/${notif.fromUserId}"
                            else -> "notifications"
                        }
                        
                        val title = when (notif.type) {
                            "like" -> "💖 Utilité partagée !"
                            "comment" -> "💬 Nouvelle interaction"
                            "follow" -> "🎉 Nouveau membre dans votre réseau"
                            "message" -> "📩 Message privé reçu"
                            else -> "🔔 Notification S-3 CMO"
                        }

                        val descriptiveText = when (notif.type) {
                            "like" -> "${notif.fromUsername} a aimé votre fichier d'acte."
                            "comment" -> "${notif.fromUsername} a commenté : ${notif.message}"
                            "follow" -> "${notif.fromUsername} s'est abonné à votre profil."
                            "message" -> "Nouveau message de ${notif.fromUsername}."
                            else -> notif.message
                        }

                        com.example.utils.NotificationHelper.showSystemNotification(
                            context = context,
                            notificationId = notif.id,
                            title = title,
                            text = descriptiveText,
                            route = route,
                            avatarUrl = notif.fromAvatar,
                            senderName = notif.fromUsername
                        )
                    }

                    newUnread.forEach { notif ->
                        shownNotifications.add(notif.id)
                        updated = true
                    }

                    if (updated) {
                        prefs.edit().putStringSet("shown_notification_ids", shownNotifications).apply()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markNotificationAsRead(id: String) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() {
        val userId = _currentUser.value?.id ?: return
        notificationDao.markAllAsRead(userId)
    }

    suspend fun getUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username)
    }

    suspend fun searchCommunities(query: String?, category: String?, sort: String): List<Community> {
        val header = currentToken?.let { "Bearer $it" }
        return try {
            RetrofitClient.apiService.searchCommunities(header, query, category, sort)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getCommunity(slug: String): Community? {
        val header = currentToken?.let { "Bearer $it" }
        return try {
            RetrofitClient.apiService.getCommunity(header, slug)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun joinCommunity(slug: String): Boolean {
        val token = currentToken ?: return false
        return try {
            RetrofitClient.apiService.joinCommunity("Bearer $token", slug)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getCommunityChannels(slug: String): List<Channel> {
        val header = currentToken?.let { "Bearer $it" }
        val netList = try {
            RetrofitClient.apiService.getCommunityChannels(header, slug)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
        val localList = localChannels[slug] ?: emptyList()
        return (netList + localList).distinctBy { it.id }
    }

    suspend fun createChannel(slug: String, name: String, description: String?): Channel? {
        val token = currentToken ?: return null
        
        var chanSlug = name.lowercase().trim().replace(Regex("[^a-z0-9_]"), "_").replace(Regex("_+"), "_").trim('_')
        if (chanSlug.isBlank()) chanSlug = "chan_" + (100..999).random()

        try {
            val body = mapOf(
                "name" to name,
                "slug" to chanSlug,
                "description" to (description ?: "")
            )
            val channel = RetrofitClient.apiService.createChannel("Bearer $token", slug, body)
            return channel
        } catch (e: Exception) {
            e.printStackTrace()
            val community = getCommunity(slug)
            val newChannel = Channel(
                id = "local_chan_" + java.util.UUID.randomUUID().toString(),
                communityId = community?.id ?: slug,
                slug = chanSlug,
                name = name,
                description = description,
                isDefault = false,
                createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
            )
            val list = localChannels.getOrPut(slug) { mutableListOf() }
            list.add(newChannel)
            return newChannel
        }
    }

    fun getCommunityPostsFlow(slug: String): Flow<List<ActfileWithUser>> = kotlinx.coroutines.flow.flow {
        try {
            val header = currentToken?.let { "Bearer $it" }
            val netActfiles = RetrofitClient.apiService.getCommunityPosts(header, slug)
            val mapped = netActfiles.filter { it.channel_id.isNullOrBlank() }.map { net ->
                val existing = userDao.getUserById(net.user_id)
                val user = if (existing != null) {
                    existing.copy(
                        username = net.username,
                        avatarUrl = net.avatar_url,
                        isVerified = net.is_verified
                    )
                } else {
                    User(
                        id = net.user_id,
                        username = net.username,
                        passwordHash = "mocked",
                        avatarUrl = net.avatar_url,
                        isVerified = net.is_verified
                    )
                }
                userDao.insertUser(user)
                
                ActfileWithUser(
                    id = net.id,
                    userId = net.user_id,
                    username = net.username,
                    avatarUrl = net.avatar_url,
                    isVerified = net.is_verified,
                    content = net.content,
                    tags = "",
                    likesCount = net.likes_count,
                    viewsCount = net.views_count,
                    commentsCount = net.comments_count ?: 0,
                    createdAt = parseIso(net.created_at),
                    isLikedByMe = net.liked,
                    category = net.category,
                    communityId = net.community_id,
                    channelId = net.channel_id,
                    channelSlug = net.channel_slug,
                    channelName = net.channel_name
                )
            }
            emit(mapped)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    suspend fun getMyCommunities(): List<Community> {
        val token = currentToken ?: return emptyList()
        return try {
            RetrofitClient.apiService.getMyCommunities("Bearer $token")
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun createCommunity(name: String, category: String, description: String, isPrivate: Boolean): Community? {
        val token = currentToken ?: return null
        
        // Generate a robust, server-compliant slug
        var slug = name.lowercase().trim()
        val replacements = mapOf(
            'à' to 'a', 'â' to 'a', 'ä' to 'a',
            'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
            'î' to 'i', 'ï' to 'i',
            'ô' to 'o', 'ö' to 'o',
            'û' to 'u', 'ü' to 'u',
            'ç' to 'c'
        )
        val sb = java.lang.StringBuilder()
        for (char in slug) {
            sb.append(replacements[char] ?: char)
        }
        slug = sb.toString()
        slug = slug.replace(Regex("[^a-z0-9_]"), "_")
        slug = slug.replace(Regex("_+"), "_")
        slug = slug.trim('_')
        
        if (slug.length < 3) {
            val randomSuffix = (100..999).random()
            slug = "com_${slug}_$randomSuffix".replace(Regex("_+"), "_").trim('_')
            if (slug.length < 3) {
                slug = "com_$randomSuffix"
            }
        }
        if (slug.length > 50) {
            slug = slug.substring(0, 50).trim('_')
        }

        return try {
            val body = mapOf(
                "slug" to slug,
                "name" to name,
                "category" to category,
                "description" to description,
                "is_private" to isPrivate
            )
            RetrofitClient.apiService.createCommunity("Bearer $token", body)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateCommunity(
        slug: String,
        name: String?,
        description: String?,
        category: String?,
        isPrivate: Boolean?,
        iconUrl: String? = null
    ): Community? {
        val token = currentToken ?: return null
        return try {
            val body = mutableMapOf<String, Any>()
            if (name != null) body["name"] = name
            if (description != null) body["description"] = description
            if (category != null) body["category"] = category
            if (isPrivate != null) body["is_private"] = isPrivate
            if (iconUrl != null) {
                body["icon_url"] = iconUrl
                body["iconUrl"] = iconUrl
            }

            RetrofitClient.apiService.updateCommunity("Bearer $token", slug, body)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateCommunityIcon(slug: String, iconFile: java.io.File): Boolean {
        val token = currentToken ?: return false
        return try {
            val mediaType = "image/jpeg".toMediaType()
            val requestFile = iconFile.asRequestBody(mediaType)
            val iconPart = MultipartBody.Part.createFormData("icon", iconFile.name, requestFile)
            RetrofitClient.apiService.updateCommunityIcon("Bearer $token", slug, iconPart)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
