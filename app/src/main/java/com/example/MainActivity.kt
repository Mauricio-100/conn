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

class MainActivity : ComponentActivity() {
    private var crashError by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        val route = intent.getStringExtra("route")
        if (route != null) {
            com.example.utils.NotificationRouter.pendingRoute.value = route
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val stackTrace = android.util.Log.getStackTraceString(e)
            android.util.Log.e("CRASH_TAG", "App crashed", e)
            runOnUiThread {
                crashError = e.javaClass.simpleName + ": " + e.message + "\n\n" + stackTrace
            }
        }

        enableEdgeToEdge()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 101)
            }
        }
        
        val route = intent?.getStringExtra("route")
        if (route != null) {
            com.example.utils.NotificationRouter.pendingRoute.value = route
        }
        
        try {
            val db = AppDatabase.getDatabase(this)
            val prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val repository = IddetRepository(db.userDao(), db.actfileDao(), db.messageDao(), db.followDao(), db.commentDao(), db.notificationDao(), prefs)
            
            val factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return IddetViewModel(repository) as T
                }
            }

            setContent {
                MyApplicationTheme {
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
                            val viewModel: IddetViewModel = viewModel(factory = factory)
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
                MyApplicationTheme {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp, 64.dp)) {
                        Text("APP INITIALIZATION FAILED", color = Color.Red)
                        Text(crashError ?: "", color = Color.Black)
                    }
                }
            }
        }
    }
}
