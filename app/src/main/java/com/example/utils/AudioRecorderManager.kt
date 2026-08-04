package com.example.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioRecorderManager(private val context: Context) {
    private val TAG = "AudioRecorderManager"
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var isRecording = false

    fun startRecording(): File? {
        stopRecording() // Clean up before starting a new one

        val tempFile = try {
            File.createTempFile("voice_record_", ".m4a", context.cacheDir)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to create temp file", e)
            return null
        }

        currentFile = tempFile

        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(tempFile.absolutePath)
                prepare()
                start()
            }
            isRecording = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaRecorder", e)
            tempFile.delete()
            currentFile = null
            mediaRecorder = null
            isRecording = false
            return null
        }

        return currentFile
    }

    /**
     * Get real peak amplitude normalized from 0.0 to 1.0
     */
    fun getMaxAmplitudeNormalized(): Float {
        if (!isRecording) return 0f
        return try {
            val amp = mediaRecorder?.maxAmplitude ?: 0
            (amp / 32767f).coerceIn(0.05f, 1.0f)
        } catch (e: Exception) {
            0.1f
        }
    }

    fun stopRecording(): File? {
        if (!isRecording && currentFile == null) return null
        isRecording = false
        try {
            mediaRecorder?.let {
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder (it might have been too short)", e)
        }
        mediaRecorder = null
        val result = currentFile
        currentFile = null
        return result
    }

    fun cancelRecording() {
        isRecording = false
        try {
            mediaRecorder?.let {
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            // ignore
        }
        mediaRecorder = null
        currentFile?.delete()
        currentFile = null
    }

    fun isCurrentlyRecording(): Boolean = isRecording
}

