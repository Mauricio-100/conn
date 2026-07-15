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
                setOutputFile(tempFile.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaRecorder", e)
            tempFile.delete()
            currentFile = null
            mediaRecorder = null
            return null
        }

        return currentFile
    }

    fun stopRecording(): File? {
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
}
