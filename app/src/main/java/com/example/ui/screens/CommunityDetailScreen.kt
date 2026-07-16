package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.Channel
import com.example.data.Community
import com.example.data.getCategoryDefaultIcon
import com.example.data.getCategoryDefaultBanner
import com.example.ui.IddetViewModel
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDetailScreen(
    slug: String,
    viewModel: IddetViewModel,
    navController: NavController
) {
    val coroutineScope = rememberCoroutineScope()
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Fetch community details
    val community by produceState<Community?>(initialValue = null, slug, refreshTrigger) {
        viewModel.getCommunityFlow(slug).collect { value = it }
    }
    
    // Fetch community channels
    val channels by produceState<List<Channel>>(initialValue = emptyList(), slug, refreshTrigger) {
        viewModel.getCommunityChannelsFlow(slug).collect { value = it }
    }
    
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var snackbarHostState = remember { SnackbarHostState() }
    
    var showEditDialog by remember { mutableStateOf(false) }
    val currentUserState by viewModel.currentUser.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(community?.name ?: "Chargement...", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    val isAdmin = community?.myRole == "admin" || community?.creatorId == currentUserState?.id
                    if (isAdmin) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Paramètres")
                        }
                    }
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        if (community == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val com = community!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header Banner & Profile Section
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        // Banner background with gradient overlay
                        val bannerModel = if (!com.bannerUrl.isNullOrBlank()) com.bannerUrl else getCategoryDefaultBanner(com.category)
                        AsyncImage(
                            model = bannerModel,
                            contentDescription = "Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Icon overlapping banner
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .size(72.dp)
                                .align(Alignment.BottomStart)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            val iconModel = if (!com.iconUrl.isNullOrBlank()) com.iconUrl else getCategoryDefaultIcon(com.category)
                            AsyncImage(
                                model = iconModel,
                                contentDescription = "Icon",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                // Community Metadata Details
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = com.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    if (com.isPrivate) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Privée",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = "c/${com.slug}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Button(
                                onClick = {
                                    viewModel.toggleCommunityJoin(com.slug) { success ->
                                        if (success) {
                                            refreshTrigger++
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    if (com.isMember) "Vous avez quitté c/${com.slug}" else "Vous avez rejoint c/${com.slug}"
                                                )
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (com.isMember) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                    contentColor = if (com.isMember) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.testTag("detail_join_button")
                            ) {
                                Text(if (com.isMember) "Quitter" else "Rejoindre", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!com.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = com.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Stats Card row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CommunityStatCard(
                                value = "${com.membersCount}",
                                label = "Membres",
                                icon = Icons.Default.Group,
                                modifier = Modifier.weight(1f)
                            )
                            CommunityStatCard(
                                value = "${com.postsCount}",
                                label = "Publications",
                                icon = Icons.Outlined.ChatBubbleOutline,
                                modifier = Modifier.weight(1f)
                            )
                            CommunityStatCard(
                                value = com.category,
                                label = "Catégorie",
                                icon = Icons.Default.Label,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Channel divider text
                item {
                    Text(
                        text = "CANAUX ET DISCUSSIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                // Channels list
                if (channels.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Tag,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Aucun canal disponible",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(channels, key = { it.id }) { channel ->
                        ChannelListItem(
                            channel = channel,
                            isLocked = com.isPrivate && !com.isMember,
                            onClick = {
                                if (com.isPrivate && !com.isMember) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Rejoignez d'abord cette communauté privée pour accéder à ce canal.")
                                    }
                                } else {
                                    selectedChannel = channel
                                }
                            }
                        )
                    }
                }
            }
        }

        // Active channel discussion Dialog (modal layout mimicking a chat workspace room)
        selectedChannel?.let { activeChannel ->
            ChannelChatRoomDialog(
                channel = activeChannel,
                community = community,
                viewModel = viewModel,
                onDismiss = { selectedChannel = null }
            )
        }

        if (showEditDialog && community != null) {
            val categories = listOf("Fun", "Amour", "Motivation", "Tech", "Sport", "Musique", "Actu", "Business", "Spiritualité", "Autres")
            EditCommunityDialog(
                community = community!!,
                categories = categories,
                onDismiss = { showEditDialog = false },
                onSave = { name, desc, cat, priv, presetIconUrl ->
                    viewModel.updateCommunity(
                        slug = community!!.slug,
                        name = name,
                        description = desc,
                        category = cat,
                        isPrivate = priv,
                        iconUrl = presetIconUrl,
                        onResult = { updated ->
                            if (updated != null) {
                                refreshTrigger++
                                showEditDialog = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Communauté mise à jour avec succès !")
                                }
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Erreur lors de la mise à jour de la communauté.")
                                }
                            }
                        }
                    )
                },
                onUploadIcon = { file ->
                    viewModel.updateCommunityIcon(community!!.slug, file) { success ->
                        if (success) {
                            refreshTrigger++
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Photo de profil de la communauté mise à jour !")
                            }
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Erreur lors du téléchargement de la photo.")
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun CommunityStatCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ChannelListItem(
    channel: Channel,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("channel_item_${channel.slug}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Tag,
                contentDescription = null,
                tint = if (isLocked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )
                if (!channel.description.isNullOrBlank()) {
                    Text(
                        text = channel.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!isLocked) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelChatRoomDialog(
    channel: Channel,
    community: Community?,
    viewModel: IddetViewModel,
    onDismiss: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var refreshCount by remember { mutableStateOf(0) }
    
    // Query list of all actfiles and filter by community mention or tag to show relevant posts
    val posts by produceState<List<com.example.data.ActfileWithUser>>(initialValue = emptyList(), refreshCount) {
        viewModel.actfiles.collect { list ->
            // Filter posts that contain this community's slug or channel name as mentions/text, or general category matches
            val filtered = list.filter { post ->
                val slugMatch = community?.slug?.let { post.content.contains("@c/$it") } ?: false
                val chanMatch = post.content.contains("@#${channel.slug}") || post.content.contains("#${channel.name}")
                val directMatch = post.channelId == channel.id || post.communityId == community?.id
                slugMatch || chanMatch || directMatch
            }
            value = filtered.sortedBy { it.createdAt }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header of Channel
                TopAppBar(
                    title = {
                        Column {
                            Text("# ${channel.name}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            if (!channel.description.isNullOrBlank()) {
                                Text(channel.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    },
                    actions = {
                        IconButton(onClick = { refreshCount++ }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                // Messages / Feed Area
                if (posts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Bienvenue dans #${channel.name} !",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Commencez la discussion en envoyant le premier message.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(posts, key = { it.id }) { post ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                // User Avatar
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!post.avatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = post.avatarUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = post.username.firstOrNull()?.toString()?.uppercase() ?: "?",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                // Message Bubble
                                Card(
                                    shape = RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = post.username,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = android.text.format.DateUtils.getRelativeTimeSpanString(post.createdAt).toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = post.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Chat Input box with auto tagging to make sure messages route to this channel/community
                Surface(
                    tonalElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Écrire dans #${channel.name}...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("channel_message_input"),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    coroutineScope.launch {
                                        // Auto-append tags so Markdown and list filters pick it up correctly
                                        val taggedContent = "$messageText\n\n@c/${community?.slug} @#${channel.slug}"
                                        viewModel.publishActfile(content = taggedContent, category = community?.category)
                                        messageText = ""
                                        refreshCount++
                                    }
                                }
                            },
                            enabled = messageText.isNotBlank(),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("send_channel_message_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Envoyer",
                                tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCommunityDialog(
    community: Community,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean, String?) -> Unit,
    onUploadIcon: (java.io.File) -> Unit
) {
    var name by remember { mutableStateOf(community.name) }
    var description by remember { mutableStateOf(community.description ?: "") }
    var selectedCategory by remember { mutableStateOf(community.category) }
    var isPrivate by remember { mutableStateOf(community.isPrivate) }
    var customIconUrl by remember { mutableStateOf(community.iconUrl ?: "") }
    
    var categoryExpanded by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current

    // Activity result launcher for picking gallery photo profile
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val file = uriToTempFile(context, it)
            if (file != null) {
                onUploadIcon(file)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Modifier la communauté",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Community Profile Picture (Icon) Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Photo de profil de la communauté",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            val displayUrl = if (customIconUrl.isNotBlank()) customIconUrl else getCategoryDefaultIcon(selectedCategory)
                            AsyncImage(
                                model = displayUrl,
                                contentDescription = "Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Importer de la galerie", style = MaterialTheme.typography.labelMedium)
                            }
                            
                            Text(
                                text = "Ou sélectionnez un avatar thématique ci-dessous.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                             )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Preset themed community avatars
                    Text(
                        text = "Avatars prédéfinis :",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    val presetIcons = listOf(
                        "Gaming" to "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=200&q=80",
                        "Tech" to "https://images.unsplash.com/photo-1542831371-29b0f74f9713?w=200&q=80",
                        "Art" to "https://images.unsplash.com/photo-1452421820245-17cd229f72e7?w=200&q=80",
                        "Music" to "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=200&q=80",
                        "Sport" to "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=200&q=80",
                        "Cooking" to "https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=200&q=80",
                        "Books" to "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=200&q=80",
                        "Business" to "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=200&q=80",
                        "Love" to "https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=200&q=80"
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetIcons) { (label, url) ->
                            Card(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clickable { customIconUrl = url },
                                shape = CircleShape,
                                border = if (customIconUrl == url) {
                                    androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                                } else null
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = label,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                // Community Name
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Nom de la communauté",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Description
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 4
                    )
                }

                // Category dropdown picker
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Catégorie",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { categoryExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedCategory, color = MaterialTheme.colorScheme.onSurface)
                                Icon(
                                    imageVector = if (categoryExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Privacy Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Communauté privée",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Seuls les membres approuvés peuvent voir les canaux et publier.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it }
                     )
                 }
 
                 Spacer(modifier = Modifier.height(12.dp))
 
                 // Actions
                 Row(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.End,
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     TextButton(onClick = onDismiss) {
                         Text("Annuler", fontWeight = FontWeight.Bold)
                     }
                     Spacer(modifier = Modifier.width(12.dp))
                     Button(
                         onClick = {
                             onSave(name, description, selectedCategory, isPrivate, customIconUrl.ifBlank { null })
                         },
                         shape = RoundedCornerShape(12.dp)
                     ) {
                         Text("Enregistrer", fontWeight = FontWeight.Bold)
                     }
                 }
             }
         }
     }
 }
 
 fun uriToTempFile(context: android.content.Context, uri: android.net.Uri): java.io.File? {
     return try {
         val inputStream = context.contentResolver.openInputStream(uri) ?: return null
         val tempFile = java.io.File.createTempFile("community_icon_", ".jpg", context.cacheDir)
         tempFile.deleteOnExit()
         val outputStream = java.io.FileOutputStream(tempFile)
         inputStream.use { input ->
             outputStream.use { output ->
                 input.copyTo(output)
             }
         }
         tempFile
     } catch (e: Exception) {
         e.printStackTrace()
         null
     }
 }
