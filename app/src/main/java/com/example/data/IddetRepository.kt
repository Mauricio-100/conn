package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.DelicateCoroutinesApi

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
    
    private var currentToken: String? = prefs.getString("auth_token", null)
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
                            id = profile.id,
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
                        id = profile.id,
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
                        category = net.category
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
            val existing = userDao.getUserById(profile.id)
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
                    id = profile.id,
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

    fun logout() {
        _currentUser.value = null
        currentToken = null
        prefs.edit().clear().apply()
    }

    suspend fun publishActfile(content: String, tags: String = "", category: String? = null) {
        val user = _currentUser.value ?: return
        
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                val netActfile = RetrofitClient.apiService.publishActfile(
                    token = header,
                    request = PublishActfileRequest(content = content, category = category)
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
                        category = netActfile.category ?: category
                    )
                )
            } else {
                actfileDao.insertActfile(
                    Actfile(
                        userId = user.id,
                        content = content,
                        tags = tags,
                        category = category
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
                    category = category
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
                            id = res.id,
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
                            isVerified = (conv.username.length > 5)
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
                        type = it.type,
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
                    mapped.filter { !it.isRead && !shownNotifications.contains(it.id) }.forEach { notif ->
                        val route = when (notif.type) {
                            "like", "comment" -> "discussion/${notif.targetId}"
                            "message" -> "chat/${notif.fromUserId}"
                            "follow" -> "profile/${notif.fromUserId}"
                            else -> "notifications"
                        }
                        
                        val title = when (notif.type) {
                            "like" -> "💖 Utilité partagée !"
                            "comment" -> "💬 Nouveau commentaire"
                            "follow" -> "🎉 Nouvel abonné"
                            "message" -> "💬 Message reçu"
                            else -> "🔔 Nouvelle activité"
                        }

                        com.example.utils.NotificationHelper.showSystemNotification(
                            context = context,
                            notificationId = notif.id,
                            title = title,
                            text = notif.message,
                            route = route,
                            avatarUrl = notif.fromAvatar,
                            senderName = notif.fromUsername
                        )
                        
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
}
