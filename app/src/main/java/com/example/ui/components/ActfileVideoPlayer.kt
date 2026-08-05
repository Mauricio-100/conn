package com.example.ui.components

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun ActfileVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    title: String? = null
) {
    val context = LocalContext.current
    val videoInfo = remember(videoUrl) { VideoUrlHelper.parseVideoInfo(videoUrl) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .testTag("actfile_video_player"),
        color = Color.Black,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header platform indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141414))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (videoInfo.type) {
                            VideoUrlHelper.VideoType.YOUTUBE -> Icons.Default.PlayCircle
                            VideoUrlHelper.VideoType.TIKTOK -> Icons.Default.MusicNote
                            VideoUrlHelper.VideoType.INSTAGRAM -> Icons.Default.CameraAlt
                            VideoUrlHelper.VideoType.DIRECT_FILE -> Icons.Default.Movie
                            else -> Icons.Default.Videocam
                        },
                        contentDescription = "Platform",
                        tint = when (videoInfo.type) {
                            VideoUrlHelper.VideoType.YOUTUBE -> Color(0xFFFF0000)
                            VideoUrlHelper.VideoType.TIKTOK -> Color(0xFF00F2FE)
                            VideoUrlHelper.VideoType.INSTAGRAM -> Color(0xFFE1306C)
                            VideoUrlHelper.VideoType.DIRECT_FILE -> Color(0xFF4CAF50)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (videoInfo.type) {
                            VideoUrlHelper.VideoType.YOUTUBE -> "YouTube Video"
                            VideoUrlHelper.VideoType.TIKTOK -> "TikTok Video"
                            VideoUrlHelper.VideoType.INSTAGRAM -> "Instagram Reel"
                            VideoUrlHelper.VideoType.DIRECT_FILE -> if (videoUrl.lowercase().contains("github")) "GitHub Video" else "Vidéo Actfile"
                            else -> "Vidéo Web"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Video Player view depending on type
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 340.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                when (videoInfo.type) {
                    VideoUrlHelper.VideoType.DIRECT_FILE -> {
                        DirectVideoView(videoUrl = videoInfo.originalUrl)
                    }
                    else -> {
                        EmbeddedWebVideoView(videoInfo = videoInfo)
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectVideoView(videoUrl: String) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var mediaPlayerInstance by remember { mutableStateOf<MediaPlayer?>(null) }
    var isError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setVideoURI(Uri.parse(videoUrl))
                    
                    val controller = MediaController(ctx)
                    controller.setAnchorView(this)
                    setMediaController(controller)

                    setOnPreparedListener { mp ->
                        mediaPlayerInstance = mp
                        mp.isLooping = true
                        mp.setVolume(1.0f, 1.0f) // Audio turned ON loud & clear
                        start()
                        isPlaying = true
                    }

                    setOnErrorListener { _, _, _ ->
                        isError = true
                        true
                    }
                }
            },
            update = { view ->
                if (!view.isPlaying && isPlaying) {
                    view.start()
                }
            }
        )

        // Overlay audio mute toggle button
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            IconButton(
                onClick = {
                    isMuted = !isMuted
                    mediaPlayerInstance?.let { mp ->
                        if (isMuted) {
                            mp.setVolume(0f, 0f)
                        } else {
                            mp.setVolume(1.0f, 1.0f)
                        }
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                    contentDescription = "Toggle Mute",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (isError) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = Color.Yellow,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Impossible de lire le flux vidéo.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EmbeddedWebVideoView(videoInfo: VideoUrlHelper.VideoInfo) {
    val htmlContent = remember(videoInfo) {
        when (videoInfo.type) {
            VideoUrlHelper.VideoType.YOUTUBE -> {
                val embed = videoInfo.embedUrl ?: videoInfo.originalUrl
                """
                <!DOCTYPE html>
                <html>
                <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                  * { margin:0; padding:0; box-sizing:border-box; }
                  body, html { width:100%; height:100%; background-color:#000000; overflow:hidden; display:flex; justify-content:center; align-items:center; }
                  iframe { width:100%; height:100%; border:none; }
                </style>
                </head>
                <body>
                  <iframe src="$embed" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>
                </body>
                </html>
                """.trimIndent()
            }
            VideoUrlHelper.VideoType.TIKTOK -> {
                val embed = videoInfo.embedUrl ?: videoInfo.originalUrl
                """
                <!DOCTYPE html>
                <html>
                <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                  * { margin:0; padding:0; }
                  body, html { width:100%; height:100%; background:#000; display:flex; justify-content:center; align-items:center; }
                  iframe { width:100%; height:100%; border:none; }
                </style>
                </head>
                <body>
                  <iframe src="$embed" allowfullscreen allow="autoplay; encrypted-media"></iframe>
                </body>
                </html>
                """.trimIndent()
            }
            VideoUrlHelper.VideoType.INSTAGRAM -> {
                val embed = videoInfo.embedUrl ?: videoInfo.originalUrl
                """
                <!DOCTYPE html>
                <html>
                <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                  * { margin:0; padding:0; }
                  body, html { width:100%; height:100%; background:#000; display:flex; justify-content:center; align-items:center; }
                  iframe { width:100%; height:100%; border:none; }
                </style>
                </head>
                <body>
                  <iframe src="$embed" allowfullscreen allow="autoplay; encrypted-media"></iframe>
                </body>
                </html>
                """.trimIndent()
            }
            else -> {
                val targetUrl = videoInfo.originalUrl
                """
                <!DOCTYPE html>
                <html>
                <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                  * { margin:0; padding:0; }
                  body, html { width:100%; height:100%; background:#000; display:flex; justify-content:center; align-items:center; }
                  video { width:100%; max-height:100%; object-fit:contain; }
                  iframe { width:100%; height:100%; border:none; }
                </style>
                </head>
                <body>
                  <video src="$targetUrl" controls playsinline autoplay style="width:100%; height:100%;"></video>
                </body>
                </html>
                """.trimIndent()
            }
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
        }
    )
}
