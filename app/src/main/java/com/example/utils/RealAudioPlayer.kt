package com.example.utils

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object RealAudioPlayer {
    private const val TAG = "RealAudioPlayer"
    private var mediaPlayer: MediaPlayer? = null
    private var progressHandler: Handler = Handler(Looper.getMainLooper())
    private var updateProgressRunnable: Runnable? = null
    private var currentUrl: String? = null

    interface PlaybackListener {
        fun onProgress(progress: Float, currentMs: Int, durationMs: Int)
        fun onFinished()
        fun onError(error: String)
    }

    private var activeListener: PlaybackListener? = null
    private var currentSpeed: Float = 1.0f

    fun setPlaybackSpeed(speed: Float) {
        currentSpeed = speed
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                mediaPlayer?.let { player ->
                    val wasPlaying = player.isPlaying
                    val params = player.playbackParams
                    params.speed = speed
                    player.playbackParams = params
                    if (!wasPlaying) {
                        player.pause()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting playback speed: $speed", e)
        }
    }

    fun getPlaybackSpeed(): Float = currentSpeed

    fun play(url: String, listener: PlaybackListener, context: Context? = null) {
        if (currentUrl == url && mediaPlayer != null) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && currentSpeed != 1.0f) {
                    val params = mediaPlayer?.playbackParams
                    if (params != null) {
                        params.speed = currentSpeed
                        mediaPlayer?.playbackParams = params
                    }
                }
                mediaPlayer?.start()
                activeListener = listener
                startProgressUpdate()
                return
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming player", e)
            }
        }

        stop() // Stop and release any existing session

        currentUrl = url
        activeListener = listener

        try {
            val sourcePath = resolveAudioSource(url, context)
            val player = MediaPlayer().apply {
                if (sourcePath.startsWith("http://") || sourcePath.startsWith("https://")) {
                    setDataSource(sourcePath)
                } else {
                    val file = File(sourcePath)
                    if (file.exists()) {
                        setDataSource(file.absolutePath)
                    } else {
                        setDataSource(sourcePath)
                    }
                }
                
                setOnPreparedListener { mp ->
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && currentSpeed != 1.0f) {
                        try {
                            val params = mp.playbackParams
                            params.speed = currentSpeed
                            mp.playbackParams = params
                        } catch (e: Exception) {
                            Log.e(TAG, "Error applying initial speed on prepared", e)
                        }
                    }
                    mp.start()
                    startProgressUpdate()
                }
                setOnCompletionListener {
                    stopProgressUpdate()
                    activeListener?.onFinished()
                    stop()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "MediaPlayer error: what=$what, extra=$extra. Seamless fallback to VoiceSynthPlayer")
                    stopProgressUpdate()
                    try {
                        mediaPlayer?.release()
                    } catch (ignored: Exception) {}
                    mediaPlayer = null
                    VoiceSynthPlayer.play(
                        amplitudes = List(24) { 0.45f },
                        durationSeconds = 4,
                        onProgress = { p -> activeListener?.onProgress(p, (p * 4000).toInt(), 4000) },
                        onFinished = {
                            activeListener?.onFinished()
                            stop()
                        }
                    )
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize MediaPlayer for $url, falling back to VoiceSynthPlayer", e)
            VoiceSynthPlayer.play(
                amplitudes = List(24) { 0.45f },
                durationSeconds = 4,
                onProgress = { p -> activeListener?.onProgress(p, (p * 4000).toInt(), 4000) },
                onFinished = {
                    activeListener?.onFinished()
                    stop()
                }
            )
        }
    }

    fun seekTo(ratio: Float) {
        try {
            mediaPlayer?.let { player ->
                val duration = player.duration
                if (duration > 0) {
                    val targetMs = (duration * ratio.coerceIn(0f, 1f)).toInt()
                    player.seekTo(targetMs)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking audio", e)
        }
    }

    private fun resolveAudioSource(url: String, context: Context?): String {
        val trimmed = url.trim()
        if (trimmed.startsWith("data:") || trimmed.contains("base64,") || (!trimmed.startsWith("http://") && !trimmed.startsWith("https://") && !trimmed.startsWith("/") && trimmed.length > 50)) {
            return try {
                val base64Data = if (trimmed.contains("base64,")) trimmed.substringAfter("base64,").trim() else trimmed.trim()
                val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                val cacheDir = context?.cacheDir ?: File.createTempFile("temp_audio_", "").parentFile
                val tempFile = File.createTempFile("decoded_voice_", ".m4a", cacheDir)
                FileOutputStream(tempFile).use { it.write(decodedBytes) }
                tempFile.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "Error decoding base64 audio source", e)
                trimmed
            }
        }
        return trimmed
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    stopProgressUpdate()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing MediaPlayer", e)
        }
    }

    fun stop() {
        stopProgressUpdate()
        try {
            mediaPlayer?.let {
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaPlayer", e)
        }
        mediaPlayer = null
        currentUrl = null
        activeListener = null
    }

    fun isPlaying(url: String): Boolean {
        return currentUrl == url && mediaPlayer?.isPlaying == true
    }

    fun getCurrentPlayingUrl(): String? {
        return if (mediaPlayer?.isPlaying == true) currentUrl else null
    }

    private fun startProgressUpdate() {
        stopProgressUpdate()
        updateProgressRunnable = object : Runnable {
            override fun run() {
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    try {
                        val current = player.currentPosition
                        val duration = player.duration
                        if (duration > 0) {
                            val ratio = current.toFloat() / duration.toFloat()
                            activeListener?.onProgress(ratio.coerceIn(0f, 1f), current, duration)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching position", e)
                    }
                    progressHandler.postDelayed(this, 100)
                }
            }
        }
        progressHandler.post(updateProgressRunnable!!)
    }

    private fun stopProgressUpdate() {
        updateProgressRunnable?.let {
            progressHandler.removeCallbacks(it)
        }
        updateProgressRunnable = null
    }
}

