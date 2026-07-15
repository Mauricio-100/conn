package com.example.utils

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log

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

    fun play(url: String, listener: PlaybackListener) {
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
            val player = MediaPlayer().apply {
                setDataSource(url)
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
