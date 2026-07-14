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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.User
import com.example.ui.IddetViewModel
import com.example.ui.components.ActfileCard
import com.example.ui.components.VerificationBadge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: IddetViewModel, navController: NavController) {
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    val giants by viewModel.giants.collectAsStateWithLifecycle()
    val searchUsersResult by viewModel.searchUsersResult.collectAsStateWithLifecycle()
    val searchActfilesResult by viewModel.searchActfilesResult.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableStateOf(0) } // 0: Users, 1: Actfiles

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
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { 
                        query = it 
                        viewModel.updateSearchQuery(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search actfiles and users...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

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
                if (selectedTab == 0) {
                    if (query.isBlank()) {
                        item {
                            Text(
                                text = "Giants to follow",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
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
                    }
                } else {
                    if (query.isBlank()) {
                        item {
                            Text(
                                text = "Type to search actfiles by content or tags...",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        items(searchActfilesResult, key = { it.id }) { actfile ->
                            val isMine = actfile.userId == currentUser?.id
                            ActfileCard(
                                actfile = actfile,
                                onLike = { viewModel.likeActfile(it) },
                                onView = { viewModel.incrementView(it) },
                                onUserClick = { navController.navigate("profile/$it") },
                                onComment = { navController.navigate("discussion/$it") },
                                onDelete = if (isMine) { { viewModel.deleteActfile(it) } } else null,
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
                    model = user.avatarUrl,
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
                if (user.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    VerificationBadge(userName = user.username)
                }
            }
            Text(
                text = "${user.followersCount} followers",
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
