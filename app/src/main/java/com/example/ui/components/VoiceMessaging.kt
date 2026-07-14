package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Parses and displays a voice message waveform and allows playback.
 */
@Composable
fun VoiceMessagePlayer(
    content: String,
    modifier: Modifier = Modifier,
    isMine: Boolean = false
) {
    val voiceData = remember(content) { parseVoiceMessage(content) } ?: return
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    // Clean up audio playback on composable disposal
    DisposableEffect(content) {
        onDispose {
            com.example.utils.VoiceSynthPlayer.stop()
        }
    }

    val playIcon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
    val iconColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    val activeWaveColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    val inactiveWaveColor = if (isMine) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    }

    val displayTime = remember(progress) {
        val totalSec = voiceData.durationSeconds
        val currentSec = (progress * totalSec).toInt()
        val remainingSec = totalSec - currentSec
        val minutes = remainingSec / 60
        val seconds = remainingSec % 60
        String.format("%d:%02d", minutes, seconds)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play / Pause Button
        IconButton(
            onClick = {
                if (isPlaying) {
                    com.example.utils.VoiceSynthPlayer.stop()
                    isPlaying = false
                    progress = 0f
                } else {
                    isPlaying = true
                    com.example.utils.VoiceSynthPlayer.play(
                        amplitudes = voiceData.amplitudes,
                        durationSeconds = voiceData.durationSeconds,
                        onProgress = { p -> progress = p },
                        onFinished = {
                            isPlaying = false
                            progress = 0f
                        }
                    )
                }
            },
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = playIcon,
                contentDescription = if (isPlaying) "Pause" else "Lire",
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Waveform Visualizer
        Box(
            modifier = Modifier
                .weight(1f)
                .pointerInput(voiceData.amplitudes) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        progress = ratio
                        if (!isPlaying) {
                            // If paused, just seek
                        }
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                voiceData.amplitudes.forEachIndexed { index, amp ->
                    val isCompleted = (index.toFloat() / voiceData.amplitudes.size) <= progress
                    val barHeight = 36.dp * amp.coerceIn(0.12f, 1.0f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(barHeight)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (isCompleted) activeWaveColor else inactiveWaveColor)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Duration / Countdown text
        Text(
            text = displayTime,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp)
        )
    }
}

/**
 * Compact, highly polished voice recorder bar.
 * Handles recording animation, volume fluctuation simulation, and returns voice markdown on complete.
 */
@Composable
fun VoiceRecorderUI(
    onCancel: () -> Unit,
    onSendVoice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var durationSeconds by remember { mutableStateOf(0) }
    var isRecording by remember { mutableStateOf(true) }
    val recordAmplitudes = remember { mutableStateListOf<Float>() }
    
    // Pulse animation for recording dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Simulate timer and recording sound wave amplitudes
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                durationSeconds++
                // Append a new simulated amplitude level (0.1 to 1.0)
                val nextAmp = (0.2f + 0.8f * (0..100).random().toFloat() / 100f)
                recordAmplitudes.add(nextAmp)
                // Keep the last 15 amplitudes visible
                if (recordAmplitudes.size > 18) {
                    recordAmplitudes.removeAt(0)
                }
            }
        }
    }

    val displayTime = remember(durationSeconds) {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        String.format("%d:%02d", minutes, seconds)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Red Recording Dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color.Red.copy(alpha = dotAlpha))
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Recording Time
        Text(
            text = displayTime,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.width(36.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Live Audio wave simulation
        Row(
            modifier = Modifier
                .weight(1f)
                .height(24.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fill with placeholder or current amplitudes
            val activeAmps = if (recordAmplitudes.isEmpty()) {
                List(15) { 0.15f }
            } else {
                recordAmplitudes
            }
            activeAmps.forEach { amp ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp * amp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Trash / Cancel Button
        IconButton(
            onClick = {
                isRecording = false
                onCancel()
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Annuler",
                tint = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Send Voice Message Button
        IconButton(
            onClick = {
                isRecording = false
                val finalAmps = if (recordAmplitudes.isEmpty()) {
                    List(22) { (0.2f + 0.8f * kotlin.math.sin(it.toFloat() / 4.5f).coerceIn(0.1f, 1f)) }
                } else {
                    recordAmplitudes.toList()
                }
                val ampsString = finalAmps.map { String.format("%.2f", it) }.joinToString(",")
                val finalDuration = if (durationSeconds == 0) 3 else durationSeconds
                val voiceMarkdown = "[Voice Message](voice://duration=$finalDuration&amplitudes=$ampsString)"
                onSendVoice(voiceMarkdown)
            },
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Envoyer vocal",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Utility details
 */
fun isVoiceMessage(content: String): Boolean {
    return content.startsWith("[Voice Message](voice://")
}

data class VoiceMessageData(val durationSeconds: Int, val amplitudes: List<Float>)

fun parseVoiceMessage(content: String): VoiceMessageData? {
    if (!isVoiceMessage(content)) return null
    return try {
        val uri = content.substringAfter("voice://").removeSuffix(")")
        val params = uri.split("&").associate {
            val parts = it.split("=")
            parts[0] to parts.getOrNull(1)
        }
        val duration = params["duration"]?.toIntOrNull() ?: 5
        val ampsString = params["amplitudes"] ?: ""
        val amplitudes = ampsString.split(",").mapNotNull { it.toFloatOrNull() }
        VoiceMessageData(duration, amplitudes)
    } catch (e: Exception) {
        VoiceMessageData(5, List(15) { 0.4f })
    }
}
