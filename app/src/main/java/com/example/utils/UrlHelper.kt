package com.example.utils

object UrlHelper {
    fun fixCloudinaryUrl(url: String?): String? {
        if (url == null) return null
        
        val baseUrl = if (url.contains("?")) url.substringBefore("?") else url

        if (baseUrl.contains("res.cloudinary.com")) {
            val lastSlash = baseUrl.lastIndexOf('/')
            val lastDot = baseUrl.lastIndexOf('.')
            if (lastDot > lastSlash) {
                return baseUrl.substring(0, lastDot) + ".webp"
            } else {
                return "$baseUrl.webp"
            }
        }
        return baseUrl
    }
}
