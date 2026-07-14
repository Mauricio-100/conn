package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.compose.ui.platform.LocalContext

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val context = LocalContext.current
    val scale = remember { Animatable(0f) }
    val opacity = remember { Animatable(0f) }

    val permissionsToRequest = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // We proceed regardless of whether permissions were granted or not
        // The app will handle missing permissions when specific features are used
    }

    LaunchedEffect(key1 = true) {
        try {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Animation sequence
        launch {
            scale.animateTo(
                targetValue = 1.2f,
                animationSpec = tween(
                    durationMillis = 1500,
                    easing = FastOutSlowInEasing
                )
            )
            scale.animateTo(
                targetValue = 20f,
                animationSpec = tween(
                    durationMillis = 500,
                    easing = AccelerateInterpolator()
                )
            )
        }
        launch {
            opacity.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800)
            )
            delay(1000)
            opacity.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 700)
            )
        }
        
        delay(2200)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_cat_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 16.dp)
            )
            Text(
                text = "IDDET",
                color = Color(0xFFDC2626), // Netflix Red
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )
            Text(
                text = "VOILA",
                color = Color.White.copy(alpha = opacity.value),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp
            )
        }
    }
}

class AccelerateInterpolator : Easing {
    override fun transform(fraction: Float): Float {
        return fraction * fraction * fraction
    }
}
