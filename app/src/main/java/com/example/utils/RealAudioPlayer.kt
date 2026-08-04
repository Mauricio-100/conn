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

    fun play(url: String, listener: PlaybackListener, context: Context? = null) {
        if (currentUrl == url && mediaPlayer != null) {
            try {
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
                    mp.start()
                    startProgressUpdate()
                }
                setOnCompletionListener {
                    stopProgressUpdate()
                    activeListener?.onFinished()
                    stop()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    activeListener?.onError("Erreur de lecture audio")
                    stop()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio $url", e)
            listener.onError(e.message ?: "Impossible d'initialiser le lecteur audio.")
            stop()
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
        if (trimmed.startsWith("data:audio") || (!trimmed.startsWith("http://") && !trimmed.startsWith("https://") && !trimmed.startsWith("/") && trimmed.length > 100)) {
            return try {
                val base64Data = if (trimmed.contains("base64,")) trimmed.substringAfter("base64,") else trimmed
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

