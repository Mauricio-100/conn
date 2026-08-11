package com.example.utils

object UrlHelper {
    fun fixCloudinaryUrl(url: String?): String? {
        if (url == null) return null
        if (url.contains("res.cloudinary.com")) {
            val lastSlash = url.lastIndexOf('/')
            val lastDot = url.lastIndexOf('.')
            if (lastDot > lastSlash) {
                return url.substring(0, lastDot) + ".webp"
            } else {
                return "$url.webp"
            }
        }
        return url
    }
}
