package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.unit.sp
import com.example.data.ActfileWithUser
import com.example.ui.IddetViewModel
import com.example.ui.components.ActfileCard
import com.example.ui.components.VerificationBadge
import com.example.ui.components.MarkdownEditor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: IddetViewModel, navController: NavController, onOpenDrawer: () -> Unit) {
    val actfiles by viewModel.actfiles.collectAsStateWithLifecycle()
    val followedActfiles by viewModel.followedActfiles.collectAsStateWithLifecycle()
    val recommendedUsers by viewModel.giants.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val preferredCategory = currentUser?.preferredCategory ?: "@(fun)"
    
    val feedTab by viewModel.feedTab.collectAsStateWithLifecycle()
    val showComposer by viewModel.showComposer.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    
    val activeActfiles = remember(feedTab, actfiles, followedActfiles, preferredCategory, selectedCategoryFilter) {
        val baseList = when (feedTab) {
            0 -> actfiles
            1 -> followedActfiles
            else -> actfiles.shuffled()
        }
        val sortedList = if (feedTab == 0 && preferredCategory.isNotBlank()) {
            baseList.sortedWith(compareByDescending { it.category == preferredCategory })
        } else {
            baseList
        }
        
        if (selectedCategoryFilter != null) {
            sortedList.filter { it.category?.equals(selectedCategoryFilter, ignoreCase = true) == true }
        } else {
            sortedList
        }
    }
    
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val scope = rememberCoroutineScope()
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.iddet_cat_logo_1783909130023),
                            contentDescription = "IDDET Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("IDDET", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onOpenDrawer() }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Ouvrir le menu")
                    }
                },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                if (recommendedUsers.isNotEmpty() && (feedTab == 0 || activeActfiles.isEmpty())) {
                    item {
                        Text(
                            text = if (feedTab == 1) "Follow users to populate your feed!" else "Recommended for you",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recommendedUsers, key = { it.id }) { user ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { navController.navigate("profile/${user.id}") }
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                        .width(100.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.username.firstOrNull()?.toString()?.uppercase() ?: "?",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = user.username,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                items(activeActfiles, key = { it.id }) { actfile ->
                    val isMine = actfile.userId == currentUser?.id
                    ActfileCard(
                        actfile = actfile,
                        onLike = { viewModel.likeActfile(it) },
                        onView = { viewModel.incrementView(it) },
                        onUserClick = {
                            val currentUserId = viewModel.currentUser.value?.id
                            if (it == currentUserId) {
                                navController.navigate("profile")
                            } else {
                                navController.navigate("profile/$it")
                            }
                        },
                        onComment = { actfileId ->
                            navController.navigate("discussion/$actfileId")
                        },
                        onDelete = if (isMine) { { viewModel.deleteActfile(it) } } else null,
                        onMentionClick = { username ->
                            scope.launch {
                                val u = viewModel.getUserByUsername(username)
                                if (u != null) {
                                    navController.navigate("profile/${u.id}")
                                }
                            }
                        },
                        onCategoryClick = { categoryId ->
                            viewModel.setSelectedCategoryFilter(categoryId)
                        }
                    )
                }

                if (activeActfiles.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (feedTab == 1) "No posts in your following feed yet. Find friends to follow!" else "No posts available.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
        
        if (showComposer) {
            ActfileComposer(
                onDismiss = { viewModel.setShowComposer(false) },
                onPublish = { content, tags, category ->
                    viewModel.publishActfile(content, tags, category)
                    viewModel.setShowComposer(false)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActfileComposer(
    onDismiss: () -> Unit,
    onPublish: (String, String, String?) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("@(fun)") }
    var showCategoryPickerByPublish by remember { mutableStateOf(false) }
    val selectedCatInfo = com.example.ui.components.getCategoryById(selectedCategory)
    
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "New Actfile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            MarkdownEditor(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = "Write your markdown actfile here...\n\nHint: Use the toolbar for bold, links, etc."
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Category selector block
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .clickable { showCategoryPickerByPublish = true }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background((selectedCatInfo?.color ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(selectedCatInfo?.emoji ?: "🎭", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Catégorie de la publication", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${selectedCatInfo?.name ?: "Fun"} (${selectedCategory})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = selectedCatInfo?.color ?: MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                    contentDescription = "Changer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tags (comma separated, e.g. code, design)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (content.isNotBlank()) onPublish(content, tags, selectedCategory)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Publish Actfile")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        if (showCategoryPickerByPublish) {
            val categories = com.example.ui.components.APP_CATEGORIES
            AlertDialog(
                onDismissRequest = { showCategoryPickerByPublish = false },
                title = {
                    Text(
                        text = "🏷️ Sélectionner une catégorie",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxHeight(0.6f)) {
                        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(categories) { cat ->
                                val isSelected = cat.id == selectedCategory
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) cat.color.copy(alpha = 0.15f) 
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            selectedCategory = cat.id
                                            showCategoryPickerByPublish = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(cat.color.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(cat.emoji, fontSize = 18.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${cat.name} (${cat.id})",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = cat.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                            contentDescription = "Sélectionné",
                                            tint = cat.color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCategoryPickerByPublish = false }) {
                        Text("Fermer")
                    }
                }
            )
        }
    }
}
