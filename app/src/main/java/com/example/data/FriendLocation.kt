package com.example.data

data class FriendLocation(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val district: String = "Centre-Ville",
    val statusMessage: String,
    val statusEmoji: String = "✨",
    val batteryPercent: Int = 85,
    val isOnline: Boolean = true,
    val lastSeenFormatted: String = "À l'instant",
    val distanceKm: Double = 0.8,
    val activityTag: String = "Dev & Tech",
    val mutualFriendsCount: Int = 4,
    val streakDays: Int = 12,
    val isFavorite: Boolean = false
)

enum class FriendActivityFilter(val label: String, val emoji: String) {
    ALL("Tous les potes", "👥"),
    NEARBY("À proximité (< 2km)", "📍"),
    AVAILABLE("Dispos pour chiller", "⚡"),
    STUDY_DEV("Dev & Focus", "💻"),
    GAMING("En jeu / Fun", "🎮")
}

data class UserVibe(
    val emoji: String,
    val text: String,
    val activityType: String,
    val timestamp: Long = System.currentTimeMillis()
)
