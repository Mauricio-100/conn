package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.FriendActivityFilter
import com.example.data.FriendLocation
import com.example.ui.IddetViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class MapTileTheme(val label: String, val icon: String, val url: String, val attribution: String) {
    SNAP_DARK(
        "Snap Dark",
        "🌙",
        "https://{s}.basemaps.cartocdn.com/rastertiles/voyager_labels_under/{z}/{x}/{y}{r}.png",
        "&copy; CARTO &copy; OpenStreetMap"
    ),
    STREETS(
        "Rues Claires",
        "🗺️",
        "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
        "&copy; OpenStreetMap contributors"
    ),
    SATELLITE(
        "Hybride",
        "🛰️",
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
        "&copy; Esri, Maxar, Earthstar Geographics"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsMapScreen(
    viewModel: IddetViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val friends by viewModel.friendsLocations.collectAsStateWithLifecycle()
    val isGhostMode by viewModel.isGhostMode.collectAsStateWithLifecycle()
    val currentUserVibe by viewModel.currentUserVibe.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val realLocation by viewModel.realLocation.collectAsStateWithLifecycle()
    val lastWavedFriend by viewModel.lastWavedFriend.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf(FriendActivityFilter.ALL) }
    var selectedFriend by remember { mutableStateOf<FriendLocation?>(null) }
    var currentMapTheme by remember { mutableStateOf(MapTileTheme.SNAP_DARK) }
    var showVibeEditorDialog by remember { mutableStateOf(false) }
    var showWaveCelebration by remember { mutableStateOf(false) }
    var wavedFriendName by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    // Check location permission state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            hasLocationPermission = true
            viewModel.initLocationTracking(context)
            Toast.makeText(context, "📍 GPS réel activé !", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Autorisation localisation requise pour la position GPS en direct", Toast.LENGTH_LONG).show()
        }
    }

    // Auto initialize location tracking on mount
    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            viewModel.initLocationTracking(context)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Filtered friends list
    val filteredFriends = remember(friends, selectedFilter) {
        when (selectedFilter) {
            FriendActivityFilter.ALL -> friends
            FriendActivityFilter.NEARBY -> friends.filter { it.distanceKm <= 2.0 }
            FriendActivityFilter.AVAILABLE -> friends.filter { it.activityTag.contains("Dispo", ignoreCase = true) || it.statusEmoji == "⚡" }
            FriendActivityFilter.STUDY_DEV -> friends.filter { it.activityTag.contains("Dev", ignoreCase = true) || it.activityTag.contains("Docs", ignoreCase = true) }
            FriendActivityFilter.GAMING -> friends.filter { it.activityTag.contains("Game", ignoreCase = true) || it.activityTag.contains("Musique", ignoreCase = true) }
        }
    }

    // Push updated coordinates or theme to Leaflet map
    LaunchedEffect(realLocation, filteredFriends, isGhostMode, currentMapTheme, isMapLoaded) {
        if (isMapLoaded && webViewRef != null) {
            val friendsJson = JSONArray().apply {
                filteredFriends.forEach { f ->
                    put(JSONObject().apply {
                        put("id", f.id)
                        put("username", f.username)
                        put("displayName", f.displayName)
                        put("avatarUrl", f.avatarUrl ?: "")
                        put("lat", f.latitude)
                        put("lng", f.longitude)
                        put("statusEmoji", f.statusEmoji)
                        put("statusMessage", f.statusMessage)
                        put("activityTag", f.activityTag)
                        put("isOnline", f.isOnline)
                        put("battery", f.batteryPercent)
                        put("distanceKm", f.distanceKm)
                        put("isFavorite", f.isFavorite)
                    })
                }
            }.toString()

            val userAvatar = currentUser?.avatarUrl ?: ""
            val username = currentUser?.username ?: "Moi"
            val jsCall = """
                if (window.updateMapData) {
                    window.updateMapData(
                        ${realLocation.latitude},
                        ${realLocation.longitude},
                        ${realLocation.accuracy},
                        $isGhostMode,
                        '${currentUserVibe.emoji}',
                        '$username',
                        '$userAvatar',
                        $friendsJson,
                        '${currentMapTheme.url}'
                    );
                }
            """.trimIndent()
            webViewRef?.evaluateJavascript(jsCall, null)
        }
    }

    LaunchedEffect(lastWavedFriend) {
        if (lastWavedFriend != null) {
            wavedFriendName = lastWavedFriend!!
            showWaveCelebration = true
            delay(2800)
            showWaveCelebration = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Snap Map des Potes",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (realLocation.isRealGpsAcquired) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (realLocation.isRealGpsAcquired) Color(0xFF10B981) else Color(0xFFF59E0B))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (realLocation.isRealGpsAcquired) "GPS Réel Fixé" else "Recherche GPS...",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (realLocation.isRealGpsAcquired) Color(0xFF059669) else Color(0xFFD97706),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (isGhostMode) "👻 Mode Fantôme activé (Tu es masqué)" else "📍 ${realLocation.address}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isGhostMode) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    // Map Theme Selector Button
                    IconButton(
                        onClick = {
                            val nextTheme = when (currentMapTheme) {
                                MapTileTheme.SNAP_DARK -> MapTileTheme.STREETS
                                MapTileTheme.STREETS -> MapTileTheme.SATELLITE
                                MapTileTheme.SATELLITE -> MapTileTheme.SNAP_DARK
                            }
                            currentMapTheme = nextTheme
                            Toast.makeText(context, "Carte : ${nextTheme.label}", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(currentMapTheme.icon, fontSize = 16.sp)
                            }
                        }
                    }

                    // Quick Status Vibe Button
                    IconButton(
                        onClick = { showVibeEditorDialog = true },
                        modifier = Modifier.testTag("status_vibe_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(currentUserVibe.emoji, fontSize = 16.sp)
                            }
                        }
                    }

                    // Ghost Mode Button
                    IconButton(
                        onClick = {
                            val newMode = !isGhostMode
                            viewModel.setGhostMode(newMode)
                            Toast.makeText(
                                context,
                                if (newMode) "👻 Mode Fantôme : Position masquée aux amis !" else "✨ Tu es de nouveau visible sur la carte !",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.testTag("ghost_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (isGhostMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Mode Fantôme",
                            tint = if (isGhostMode) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F172A))
        ) {
            // Real Interactive Leaflet / OpenStreetMap / CartoDB Map via hardware-accelerated AndroidView WebView
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            builtInZoomControls = false
                            displayZoomControls = false
                            allowFileAccess = true
                            setGeolocationEnabled(true)
                        }
                        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isMapLoaded = true
                            }

                            override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                                return true
                            }
                        }
                        webChromeClient = WebChromeClient()

                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onFriendClicked(friendId: String) {
                                scope.launch {
                                    val f = friends.find { it.id == friendId }
                                    if (f != null) {
                                        selectedFriend = f
                                    }
                                }
                            }

                            @JavascriptInterface
                            fun onMapClicked() {
                                scope.launch {
                                    selectedFriend = null
                                }
                            }
                        }, "AndroidBridge")

                        loadDataWithBaseURL(
                            "https://iddet.local/",
                            generateLeafletSnapMapHtml(
                                userLat = realLocation.latitude,
                                userLng = realLocation.longitude,
                                tileUrl = currentMapTheme.url,
                                tileAttribution = currentMapTheme.attribution
                            ),
                            "text/html",
                            "UTF-8",
                            null
                        )
                        webViewRef = this
                    }
                },
                update = { webView ->
                    webViewRef = webView
                }
            )

            // Permission Request Prompt if not granted
            if (!hasLocationPermission) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Localisation requise",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Active le GPS pour te voir en direct sur la carte avec tes amis.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Activer", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Top Filter Chips (Snap Categories)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = if (!hasLocationPermission) 80.dp else 12.dp)
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(FriendActivityFilter.values()) { filter ->
                        val isSelected = filter == selectedFilter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    "${filter.emoji} ${filter.label}",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = Color(0xFF0F172A).copy(alpha = 0.85f),
                                labelColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Color.Transparent else Color(0xFF334155)
                            )
                        )
                    }
                }
            }

            // Right-Side Map Action Buttons (Snapchat style)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Center on Real User GPS
                FloatingActionButton(
                    onClick = {
                        webViewRef?.evaluateJavascript(
                            "if(window.centerOnUser){ window.centerOnUser(${realLocation.latitude}, ${realLocation.longitude}); }",
                            null
                        )
                        Toast.makeText(context, "Centré sur ma position GPS 📍", Toast.LENGTH_SHORT).show()
                    },
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color(0xFF38BDF8),
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Ma position", modifier = Modifier.size(22.dp))
                }

                // Zoom In (+)
                FloatingActionButton(
                    onClick = {
                        webViewRef?.evaluateJavascript("if(window.map){ window.map.zoomIn(); }", null)
                    },
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom +", modifier = Modifier.size(22.dp))
                }

                // Zoom Out (-)
                FloatingActionButton(
                    onClick = {
                        webViewRef?.evaluateJavascript("if(window.map){ window.map.zoomOut(); }", null)
                    },
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom -", modifier = Modifier.size(22.dp))
                }

                // Real-time GPS Pulse Refresh
                FloatingActionButton(
                    onClick = {
                        viewModel.refreshFriendsRadar()
                        viewModel.initLocationTracking(context)
                        Toast.makeText(context, "📡 Actualisation de la carte en temps réel...", Toast.LENGTH_SHORT).show()
                    },
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color(0xFF10B981),
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualiser", modifier = Modifier.size(22.dp))
                }
            }

            // Bottom Friends Carousel / Selected Friend Bottom Sheet Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                AnimatedContent(
                    targetState = selectedFriend,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) + slideInVertically { it / 2 } togetherWith
                                fadeOut(animationSpec = tween(180)) + slideOutVertically { it / 2 }
                    },
                    label = "SelectedFriendCard"
                ) { friend ->
                    if (friend != null) {
                        SelectedFriendDetailCard(
                            friend = friend,
                            onClose = {
                                selectedFriend = null
                            },
                            onWave = {
                                viewModel.sendWaveToFriend(friend)
                            },
                            onChat = {
                                navController.navigate("chat/${friend.username}")
                            },
                            onViewProfile = {
                                navController.navigate("profile/${friend.username}")
                            },
                            onToggleFavorite = {
                                viewModel.toggleFavoriteFriend(friend.id)
                            }
                        )
                    } else {
                        // Quick Carousel of buddies
                        FriendsQuickCarousel(
                            friends = filteredFriends,
                            onSelectFriend = { f ->
                                selectedFriend = f
                                webViewRef?.evaluateJavascript(
                                    "if(window.centerOnFriend){ window.centerOnFriend(${f.latitude}, ${f.longitude}); }",
                                    null
                                )
                            },
                            onWaveFriend = { f ->
                                viewModel.sendWaveToFriend(f)
                            }
                        )
                    }
                }
            }

            // Wave celebration overlay
            if (showWaveCelebration) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("👋✨", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Coucou envoyé !",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Tu as fait une vague à $wavedFriendName sur la Snap Map.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal to customize user vibe / status
    if (showVibeEditorDialog) {
        VibeStatusEditorDialog(
            currentVibe = currentUserVibe,
            onDismiss = { showVibeEditorDialog = false },
            onSave = { emoji, text, type ->
                viewModel.updateUserVibe(emoji, text, type)
                showVibeEditorDialog = false
                Toast.makeText(context, "Statut du jour mis à jour ! $emoji", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// Generate high-performance Leaflet HTML for real Snap Map
private fun generateLeafletSnapMapHtml(
    userLat: Double,
    userLng: Double,
    tileUrl: String,
    tileAttribution: String
): String {
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body, #map { width: 100%; height: 100%; background: #0b132b; overflow: hidden; }
        
        /* Snap Map marker styles */
        .snap-user-marker {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            transform: translate(-50%, -50%);
        }
        .snap-user-bubble {
            background: linear-gradient(135deg, #0284c7, #38bdf8);
            color: #ffffff;
            font-size: 11px;
            font-weight: 800;
            padding: 3px 8px;
            border-radius: 12px;
            box-shadow: 0 4px 12px rgba(2, 132, 199, 0.5);
            white-space: nowrap;
            margin-bottom: 4px;
            border: 1.5px solid #ffffff;
        }
        .snap-user-avatar {
            width: 44px;
            height: 44px;
            border-radius: 50%;
            border: 3px solid #38bdf8;
            box-shadow: 0 0 20px rgba(56, 189, 248, 0.8);
            background: #0284c7;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 18px;
            color: #fff;
            overflow: hidden;
            animation: pulse-ring 2.5s infinite;
        }
        .snap-user-avatar img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }

        .snap-friend-marker {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            transform: translate(-50%, -50%);
            transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
        }
        .snap-friend-marker:active {
            transform: translate(-50%, -50%) scale(1.15);
        }
        .snap-friend-bubble {
            background: rgba(15, 23, 42, 0.95);
            color: #fff;
            font-size: 11px;
            font-weight: bold;
            padding: 2px 7px;
            border-radius: 10px;
            border: 1px solid #38bdf8;
            box-shadow: 0 4px 10px rgba(0,0,0,0.5);
            white-space: nowrap;
            margin-bottom: 3px;
        }
        .snap-friend-avatar {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            border: 2.5px solid #10b981;
            box-shadow: 0 4px 12px rgba(0,0,0,0.6);
            background: #1e293b;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #fff;
            font-size: 16px;
            overflow: hidden;
            position: relative;
        }
        .snap-friend-avatar img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }
        .snap-friend-badge {
            position: absolute;
            bottom: -2px;
            right: -2px;
            width: 12px;
            height: 12px;
            border-radius: 50%;
            background: #10b981;
            border: 2px solid #0f172a;
        }

        @keyframes pulse-ring {
            0% { box-shadow: 0 0 0 0 rgba(56, 189, 248, 0.7); }
            70% { box-shadow: 0 0 0 16px rgba(56, 189, 248, 0); }
            100% { box-shadow: 0 0 0 0 rgba(56, 189, 248, 0); }
        }

        .leaflet-control-attribution {
            background: rgba(15, 23, 42, 0.7) !important;
            color: #94a3b8 !important;
            font-size: 9px !important;
        }
        .leaflet-control-attribution a {
            color: #38bdf8 !important;
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <script>
        var map = L.map('map', {
            center: [$userLat, $userLng],
            zoom: 14,
            zoomControl: false,
            attributionControl: true
        });

        var currentTileLayer = L.tileLayer('$tileUrl', {
            maxZoom: 19,
            attribution: '$tileAttribution'
        }).addTo(map);

        var userMarker = null;
        var accuracyCircle = null;
        var friendMarkers = {};

        map.on('click', function() {
            if (window.AndroidBridge) {
                window.AndroidBridge.onMapClicked();
            }
        });

        window.centerOnUser = function(lat, lng) {
            map.flyTo([lat, lng], 15, { animate: true, duration: 1.2 });
        };

        window.centerOnFriend = function(lat, lng) {
            map.flyTo([lat, lng], 16, { animate: true, duration: 1.0 });
        };

        window.updateMapData = function(userLat, userLng, accuracy, isGhostMode, vibeEmoji, username, userAvatar, friends, tileUrl) {
            // Update tile layer if changed
            if (currentTileLayer._url !== tileUrl) {
                map.removeLayer(currentTileLayer);
                currentTileLayer = L.tileLayer(tileUrl, { maxZoom: 19 }).addTo(map);
            }

            // Update user position
            if (!isGhostMode) {
                var userIconHtml = '<div class="snap-user-marker">' +
                    '<div class="snap-user-bubble">' + vibeEmoji + ' ' + username + ' (Moi)</div>' +
                    '<div class="snap-user-avatar">' +
                    (userAvatar ? '<img src="' + userAvatar + '" />' : '<span>' + username.charAt(0).toUpperCase() + '</span>') +
                    '</div></div>';

                var userIcon = L.divIcon({
                    html: userIconHtml,
                    className: '',
                    iconSize: [44, 70],
                    iconAnchor: [22, 60]
                });

                if (!userMarker) {
                    userMarker = L.marker([userLat, userLng], { icon: userIcon }).addTo(map);
                } else {
                    userMarker.setLatLng([userLat, userLng]);
                    userMarker.setIcon(userIcon);
                }

                if (!accuracyCircle) {
                    accuracyCircle = L.circle([userLat, userLng], {
                        radius: Math.max(accuracy, 30),
                        color: '#38bdf8',
                        fillColor: '#38bdf8',
                        fillOpacity: 0.15,
                        weight: 1.5
                    }).addTo(map);
                } else {
                    accuracyCircle.setLatLng([userLat, userLng]);
                    accuracyCircle.setRadius(Math.max(accuracy, 30));
                }
            } else {
                if (userMarker) { map.removeLayer(userMarker); userMarker = null; }
                if (accuracyCircle) { map.removeLayer(accuracyCircle); accuracyCircle = null; }
            }

            // Update friends markers
            var newFriendIds = {};
            friends.forEach(function(friend) {
                newFriendIds[friend.id] = true;
                var friendIconHtml = '<div class="snap-friend-marker" onclick="onFriendClick(\'' + friend.id + '\')">' +
                    '<div class="snap-friend-bubble">' + friend.statusEmoji + ' ' + friend.username + '</div>' +
                    '<div class="snap-friend-avatar" style="border-color:' + (friend.isOnline ? '#10b981' : '#94a3b8') + '">' +
                    (friend.avatarUrl ? '<img src="' + friend.avatarUrl + '" />' : '<span>' + friend.displayName.charAt(0) + '</span>') +
                    (friend.isOnline ? '<div class="snap-friend-badge"></div>' : '') +
                    '</div></div>';

                var friendIcon = L.divIcon({
                    html: friendIconHtml,
                    className: '',
                    iconSize: [40, 65],
                    iconAnchor: [20, 55]
                });

                if (!friendMarkers[friend.id]) {
                    var m = L.marker([friend.lat, friend.lng], { icon: friendIcon }).addTo(map);
                    m.on('click', function(e) {
                        L.DomEvent.stopPropagation(e);
                        onFriendClick(friend.id);
                    });
                    friendMarkers[friend.id] = m;
                } else {
                    friendMarkers[friend.id].setLatLng([friend.lat, friend.lng]);
                    friendMarkers[friend.id].setIcon(friendIcon);
                }
            });

            // Remove old friends
            for (var fId in friendMarkers) {
                if (!newFriendIds[fId]) {
                    map.removeLayer(friendMarkers[fId]);
                    delete friendMarkers[fId];
                }
            }
        };

        function onFriendClick(friendId) {
            if (window.AndroidBridge) {
                window.AndroidBridge.onFriendClicked(friendId);
            }
        }
    </script>
</body>
</html>
    """.trimIndent()
}

@Composable
fun SelectedFriendDetailCard(
    friend: FriendLocation,
    onClose: () -> Unit,
    onWave: () -> Unit,
    onChat: () -> Unit,
    onViewProfile: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header: Avatar, Name, Distance, Close & Favorite
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(2.dp, if (friend.isOnline) Color(0xFF10B981) else Color(0xFF94A3B8), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!friend.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = friend.avatarUrl,
                            contentDescription = friend.displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = friend.displayName.take(1),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = friend.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(friend.statusEmoji, fontSize = 16.sp)
                    }
                    Text(
                        text = "@${friend.username} • 📍 ${friend.distanceKm} km (${friend.district})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Favorite button
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (friend.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favori",
                        tint = if (friend.isFavorite) Color(0xFFFBBF24) else Color(0xFF94A3B8)
                    )
                }

                // Close button
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status message bubble
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E293B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = friend.statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats badges: Battery, Streak, Online status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🔋", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${friend.batteryPercent}%", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🔥", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Série ${friend.streakDays}j", fontSize = 12.sp, color = Color(0xFFF97316), fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (friend.isOnline) Color(0xFF10B981) else Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            friend.lastSeenFormatted,
                            fontSize = 12.sp,
                            color = if (friend.isOnline) Color(0xFF10B981) else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Wave 👋, Chat 💬, Profile 👤
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onWave,
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7), contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("👋 Coucou", fontWeight = FontWeight.Black)
                }

                Button(
                    onClick = onChat,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155), contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("💬 Chat", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onViewProfile,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Text("👤 Profil", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FriendsQuickCarousel(
    friends: List<FriendLocation>,
    onSelectFriend: (FriendLocation) -> Unit,
    onWaveFriend: (FriendLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(friends) { friend ->
            Surface(
                onClick = { onSelectFriend(friend) },
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                shadowElevation = 8.dp,
                modifier = Modifier.width(170.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .border(1.5.dp, if (friend.isOnline) Color(0xFF10B981) else Color(0xFF94A3B8), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!friend.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = friend.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = friend.displayName.take(1),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = friend.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "📍 ${friend.distanceKm} km",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(friend.statusEmoji, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    friend.activityTag,
                                    fontSize = 9.sp,
                                    color = Color(0xFFE2E8F0),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = { onWaveFriend(friend) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("👋", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VibeStatusEditorDialog(
    currentVibe: com.example.data.UserVibe,
    onDismiss: () -> Unit,
    onSave: (emoji: String, text: String, type: String) -> Unit
) {
    var selectedEmoji by remember { mutableStateOf(currentVibe.emoji) }
    var statusText by remember { mutableStateOf(currentVibe.text) }
    var selectedType by remember { mutableStateOf(currentVibe.activityType) }

    val presetEmojis = listOf("🚀", "☕", "💻", "🎮", "📚", "🎧", "🔥", "✨", "🍕", "😴")
    val presetTypes = listOf("Dispo", "Dev", "Chill", "Musique", "Gaming", "Focus")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("✨ Ma Vibe sur la Snap Map", fontWeight = FontWeight.Black)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Choisis ton emoji et ton statut d'activité visible par tes amis à proximité.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Emoji picker row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetEmojis) { emoji ->
                        val isSelected = emoji == selectedEmoji
                        Surface(
                            onClick = { selectedEmoji = emoji },
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }

                // Activity tags
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(presetTypes) { type ->
                        val isSelected = type == selectedType
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedType = type
                                if (statusText.isBlank() || statusText.startsWith("En mode")) {
                                    statusText = "En mode $type • Venez parler !"
                                }
                            },
                            label = { Text(type, fontSize = 12.sp) }
                        )
                    }
                }

                // Text field
                OutlinedTextField(
                    value = statusText,
                    onValueChange = { statusText = it },
                    label = { Text("Message de statut") },
                    placeholder = { Text("ex. En train de tester le nouveau code...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedEmoji, statusText, selectedType) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

