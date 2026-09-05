package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.User
import com.example.ui.IddetViewModel
import com.example.ui.components.ActfileCard
import com.example.ui.components.VerificationBadge
import com.example.ui.components.CopyableUserId
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.example.ui.components.PersistentSearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: IddetViewModel, navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("search_prefs", Context.MODE_PRIVATE) }
    
    var searchHistory by remember { 
        mutableStateOf(prefs.getStringSet("history", emptySet())?.toList() ?: emptyList()) 
    }
    
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    val giants by viewModel.giants.collectAsStateWithLifecycle()
    val actfiles by viewModel.actfiles.collectAsStateWithLifecycle() // For actfile suggestions
    val searchUsersResult by viewModel.searchUsersResult.collectAsStateWithLifecycle()
    val searchActfilesResult by viewModel.searchActfilesResult.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableStateOf(0) } // 0: Users, 1: Actfiles

    // Function to add query to history
    val addToHistory = { q: String ->
        if (q.isNotBlank()) {
            val newHistory = (listOf(q) + searchHistory).distinct().take(10)
            searchHistory = newHistory
            prefs.edit().putStringSet("history", newHistory.toSet()).apply()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("S-3 Advanced Search", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PersistentSearchBar(
                viewModel = viewModel,
                onSearch = { addToHistory(it) }
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Users") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Actfiles") }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                if (query.isBlank()) {
                    // SEARCH HISTORY
                    if (searchHistory.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Searches",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { 
                                    searchHistory = emptyList()
                                    prefs.edit().remove("history").apply()
                                }) {
                                    Text("Clear All")
                                }
                            }
                        }
                        
                        items(searchHistory) { historyItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        viewModel.updateSearchQuery(historyItem)
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = historyItem,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    val newHistory = searchHistory.filter { it != historyItem }
                                    searchHistory = newHistory
                                    prefs.edit().putStringSet("history", newHistory.toSet()).apply()
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                    
                    // SUGGESTIONS
                    if (selectedTab == 0) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Suggested Accounts",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(giants, key = { it.id }) { user ->
                            val isFollowing by viewModel.isFollowing(user.id).collectAsStateWithLifecycle(initialValue = false)
                            UserCard(
                                user = user,
                                isFollowing = isFollowing,
                                onFollowClick = {
                                    if (isFollowing) viewModel.unfollowUser(user.id) else viewModel.followUser(user.id)
                                },
                                onClick = { navController.navigate("profile/${user.id}") }
                            )
                        }
                    } else {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Suggested Actfiles",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(actfiles.take(10), key = { actfile -> "sug_${actfile.id}" }) { actfile ->
                            val isMine = actfile.userId == currentUser?.id
                            val targetLanguage by viewModel.targetLanguage.collectAsStateWithLifecycle()
                            val aiState by viewModel.aiState.collectAsStateWithLifecycle()
                            ActfileCard(
                                actfile = actfile,
                                onLike = { viewModel.likeActfile(actfile.id) },
                                onView = { viewModel.incrementView(actfile.id) },
                                targetLanguageName = targetLanguage,
                                isAiReady = aiState == com.example.utils.AiModelState.READY,
                                onUserClick = { navController.navigate("profile/${actfile.userId}") },
                                onComment = { navController.navigate("discussion/${actfile.id}") },
                                onDelete = if (isMine) { { viewModel.deleteActfile(actfile.id) } } else null,
                                onLinkClick = { url ->
                                    val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                                    navController.navigate("browser/$encodedUrl")
                                },
                                onMentionClick = { username ->
                                    scope.launch {
                                        val u = viewModel.getUserByUsername(username)
                                        if (u != null) {
                                            navController.navigate("profile/${u.id}")
                                        }
                                    }
                                }
                            )
                        }
                    }
                } else {
                    // SEARCH RESULTS
                    if (selectedTab == 0) {
                        items(searchUsersResult, key = { it.id }) { user ->
                            val isFollowing by viewModel.isFollowing(user.id).collectAsStateWithLifecycle(initialValue = false)
                            UserCard(
                                user = user,
                                isFollowing = isFollowing,
                                onFollowClick = {
                                    if (isFollowing) viewModel.unfollowUser(user.id) else viewModel.followUser(user.id)
                                },
                                onClick = { navController.navigate("profile/${user.id}") }
                            )
                        }
                        if (searchUsersResult.isEmpty()) {
                            item {
                                Text(
                                    text = "No users found.",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    } else {
                        items(searchActfilesResult, key = { it.id }) { actfile ->
                            val isMine = actfile.userId == currentUser?.id
                            val targetLanguage by viewModel.targetLanguage.collectAsStateWithLifecycle()
                            val aiState by viewModel.aiState.collectAsStateWithLifecycle()
                            ActfileCard(
                                actfile = actfile,
                                onLike = { viewModel.likeActfile(actfile.id) },
                                onView = { viewModel.incrementView(actfile.id) },
                                targetLanguageName = targetLanguage,
                                isAiReady = aiState == com.example.utils.AiModelState.READY,
                                onUserClick = { navController.navigate("profile/${actfile.userId}") },
                                onComment = { navController.navigate("discussion/${actfile.id}") },
                                onDelete = if (isMine) { { viewModel.deleteActfile(actfile.id) } } else null,
                                onLinkClick = { url ->
                                    val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                                    navController.navigate("browser/$encodedUrl")
                                },
                                onMentionClick = { username ->
                                    scope.launch {
                                        val u = viewModel.getUserByUsername(username)
                                        if (u != null) {
                                            navController.navigate("profile/${u.id}")
                                        }
                                    }
                                }
                            )
                        }
                        if (searchActfilesResult.isEmpty()) {
                            item {
                                Text(
                                    text = "No actfiles found.",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserCard(user: User, isFollowing: Boolean, onFollowClick: () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (!user.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = user.avatarUrl?.let { com.example.utils.UrlHelper.fixCloudinaryUrl(it) },
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = user.username.firstOrNull()?.toString()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.username,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                VerificationBadge(userName = user.username, isVerified = user.isVerified)
            }
            Spacer(modifier = Modifier.height(2.dp))
            CopyableUserId(
                id = user.id,
                isBot = user.username.contains("bot", ignoreCase = true),
                fontSize = 10.sp,
                iconSize = 10.dp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${com.example.utils.FormatUtils.formatCount(user.followersCount)} followers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            if (user.badges.isNotBlank()) {
                Text(
                    text = "🏅 ${user.badges}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        Button(
            onClick = onFollowClick,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(if (isFollowing) "Unfollow" else "Follow")
        }
    }
}
