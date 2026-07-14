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
