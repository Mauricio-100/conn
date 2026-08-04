package com.example.data

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class UploadRepository {
    private val TAG = "UploadRepository"

    suspend fun uploadAudioFile(token: String?, file: File): String? {
        return try {
            val requestFile = file.asRequestBody("audio/m4a".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val authHeader = token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }
            
            val response = RetrofitClient.apiService.uploadAudio(authHeader, body)
            val url = response.secure_url ?: response.url
            Log.d(TAG, "Audio file uploaded successfully: $url")
            url
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload audio via Retrofit, falling back", e)
            null
        }
    }
}
