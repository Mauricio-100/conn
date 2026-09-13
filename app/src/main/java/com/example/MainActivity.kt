package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.IddetRepository
import com.example.ui.IddetViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.AppTheme
import android.os.Build
import coil.ImageLoader
import coil.decode.ImageDecoderDecoder
import coil.decode.GifDecoder
import coil.decode.SvgDecoder
import coil.Coil

class MainActivity : ComponentActivity() {
    private var crashError by mutableStateOf<String?>(null)

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent == null) return
        val route = intent.getStringExtra("route")
        if (route != null) {
            com.example.utils.NotificationRouter.pendingRoute.value = route
        } else {
            val uri = intent.data
            if (uri != null) {
                // Check if URI points to a community (iddet://community/slug, https://hoosthubs-g.onrender.com/c/slug, etc.)
                val communitySlug = com.example.utils.CommunitySlugHelper.extractSlugFromUrl(uri.toString())
                if (!communitySlug.isNullOrBlank()) {
                    com.example.utils.NotificationRouter.pendingRoute.value = "community/$communitySlug"
                    return
                }

                val pathSegments = uri.pathSegments
                if (pathSegments != null && pathSegments.size >= 3 && pathSegments[0] == "s" && pathSegments[1] == "actfile") {
                    val actfileId = pathSegments[2]
                    com.example.utils.NotificationRouter.pendingRoute.value = "discussion/$actfileId"
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val imageLoader = ImageLoader.Builder(applicationContext)
            .crossfade(true)
            .respectCacheHeaders(false)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
            }
            .build()
        Coil.setImageLoader(imageLoader)
        
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val stackTrace = android.util.Log.getStackTraceString(e)
            android.util.Log.e("CRASH_TAG", "App crashed", e)
            runOnUiThread {
                crashError = e.javaClass.simpleName + ": " + e.message + "\n\n" + stackTrace
            }
        }

        enableEdgeToEdge()
        com.example.utils.LocalAiManager.initOnce(applicationContext)
        com.example.utils.HideItProManager.initialize(applicationContext)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 101)
            }
        }
        
        handleIntent(intent)
        
        try {
            // Schedule WorkManager for background notifications (WhatsApp-like background polling & persistent ringtone alerts)
            try {
                val constraints = androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
                
                val periodicWork = androidx.work.PeriodicWorkRequestBuilder<com.example.worker.NotificationWorker>(
                    15, java.util.concurrent.TimeUnit.MINUTES
                ).setConstraints(constraints).build()

                androidx.work.WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                    com.example.worker.NotificationWorker.WORK_NAME,
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    periodicWork
                )

                val immediateWork = androidx.work.OneTimeWorkRequestBuilder<com.example.worker.NotificationWorker>()
                    .setConstraints(constraints)
                    .build()
                androidx.work.WorkManager.getInstance(applicationContext).enqueue(immediateWork)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to schedule NotificationWorker", e)
            }
            
            try {
                val serviceIntent = android.content.Intent(this, com.example.worker.MessageSyncService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to start MessageSyncService", e)
            }

            val db = AppDatabase.getDatabase(this)
            val prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val repository = IddetRepository(
                db.userDao(),
                db.actfileDao(),
                db.messageDao(),
                db.followDao(),
                db.commentDao(),
                db.notificationDao(),
                db.savedAccountDao(),
                db.channelMessageDao(),
                prefs
            )
            
            val factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return IddetViewModel(repository) as T
                }
            }

            setContent {
                val viewModel: IddetViewModel = viewModel(factory = factory)
                val appTheme by viewModel.selectedTheme.collectAsState()
                
                MyApplicationTheme(appTheme = appTheme) {
                    if (crashError != null) {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp, 64.dp)) {
                            Text("APP CRASHED", color = Color.Red)
                            Text(crashError ?: "", color = Color.Black)
                        }
                    } else {
                        var showSplash by remember { mutableStateOf(true) }
                        
                        if (showSplash) {
                            SplashScreen(onTimeout = { showSplash = false })
                        } else {
                            val currentUser by viewModel.currentUser.collectAsState()
                            
                            if (currentUser == null) {
                                AuthScreen(viewModel)
                            } else {
                                MainScreen(viewModel)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            crashError = android.util.Log.getStackTraceString(e)
            setContent {
                MyApplicationTheme(appTheme = AppTheme.DEFAULT) {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp, 64.dp)) {
                        Text("APP INITIALIZATION FAILED", color = Color.Red)
                        Text(crashError ?: "", color = Color.Black)
                    }
                }
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        try {
            Coil.imageLoader(this).memoryCache?.trimMemory(level)
        } catch (e: Exception) {
            // Ignore cache trim errors gracefully
        }
    }
}
