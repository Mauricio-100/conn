package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

object FriendsLocationService {

    // Default friendly friends list distributed around realistic anchor points
    private val initialFriends = listOf(
        FriendLocation(
            id = "friend_alex",
            username = "Alex_Dev",
            displayName = "Alexandre M.",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&auto=format&fit=crop&q=80",
            latitude = 48.8584,
            longitude = 2.2945,
            city = "Paris",
            district = "Tour Eiffel / Champ de Mars",
            statusMessage = "En train de tester l'éditeur Markdown en terrasse ☕💻",
            statusEmoji = "☕",
            batteryPercent = 92,
            isOnline = true,
            lastSeenFormatted = "En ligne",
            distanceKm = 0.45,
            activityTag = "Dev Kotlin",
            mutualFriendsCount = 7,
            streakDays = 14,
            isFavorite = true
        ),
        FriendLocation(
            id = "friend_sarah",
            username = "Sarah_K",
            displayName = "Sarah K.",
            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&auto=format&fit=crop&q=80",
            latitude = 48.8606,
            longitude = 2.3376,
            city = "Paris",
            district = "Musée du Louvre",
            statusMessage = "Prise de notes & documentation sur Obsidian 📝✨",
            statusEmoji = "📝",
            batteryPercent = 68,
            isOnline = true,
            lastSeenFormatted = "Il y a 3 min",
            distanceKm = 1.2,
            activityTag = "Design & Docs",
            mutualFriendsCount = 12,
            streakDays = 23,
            isFavorite = true
        ),
        FriendLocation(
            id = "friend_lucas",
            username = "Lucas_Gamer",
            displayName = "Lucas B.",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80",
            latitude = 48.8530,
            longitude = 2.3499,
            city = "Paris",
            district = "Quartier Latin",
            statusMessage = "En pause jeux vidéo ! Qui est dispo pour discuter ? 🎮⚡",
            statusEmoji = "🎮",
            batteryPercent = 45,
            isOnline = true,
            lastSeenFormatted = "En ligne",
            distanceKm = 1.85,
            activityTag = "Gaming & Chill",
            mutualFriendsCount = 5,
            streakDays = 9,
            isFavorite = false
        ),
        FriendLocation(
            id = "friend_emma",
            username = "Emma_Tech",
            displayName = "Emma V.",
            avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200&auto=format&fit=crop&q=80",
            latitude = 48.8738,
            longitude = 2.2950,
            city = "Paris",
            district = "Étoile / Champs-Élysées",
            statusMessage = "Dispo pour un café ou un live coding 🚀",
            statusEmoji = "⚡",
            batteryPercent = 81,
            isOnline = true,
            lastSeenFormatted = "En ligne",
            distanceKm = 2.4,
            activityTag = "Dispo café",
            mutualFriendsCount = 9,
            streakDays = 31,
            isFavorite = true
        ),
        FriendLocation(
            id = "friend_thomas",
            username = "Thomas_AI",
            displayName = "Thomas R.",
            avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&auto=format&fit=crop&q=80",
            latitude = 48.8661,
            longitude = 2.3553,
            city = "Paris",
            district = "République / Marais",
            statusMessage = "Expérimente avec les LLMs et les agents autonomes 🤖✨",
            statusEmoji = "🤖",
            batteryPercent = 54,
            isOnline = false,
            lastSeenFormatted = "Il y a 22 min",
            distanceKm = 3.1,
            activityTag = "IA & Agents",
            mutualFriendsCount = 3,
            streakDays = 6,
            isFavorite = false
        ),
        FriendLocation(
            id = "friend_lea",
            username = "Lea_Music",
            displayName = "Léa D.",
            avatarUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=200&auto=format&fit=crop&q=80",
            latitude = 48.8462,
            longitude = 2.3372,
            city = "Paris",
            district = "Jardin du Luxembourg",
            statusMessage = "Écoute de la Synthwave en révisant 🎧🎶",
            statusEmoji = "🎧",
            batteryPercent = 77,
            isOnline = true,
            lastSeenFormatted = "En ligne",
            distanceKm = 1.4,
            activityTag = "Musique & Chill",
            mutualFriendsCount = 8,
            streakDays = 18,
            isFavorite = false
        )
    )

    private val _friends = MutableStateFlow<List<FriendLocation>>(initialFriends)
    val friends: StateFlow<List<FriendLocation>> = _friends.asStateFlow()

    private val _isGhostMode = MutableStateFlow(false)
    val isGhostMode: StateFlow<Boolean> = _isGhostMode.asStateFlow()

    private val _currentUserVibe = MutableStateFlow(
        UserVibe(
            emoji = "🚀",
            text = "En ligne • Dispo pour échanger et débattre",
            activityType = "Dispo"
        )
    )
    val currentUserVibe: StateFlow<UserVibe> = _currentUserVibe.asStateFlow()

    // Toggle Ghost Mode (protects user privacy)
    fun setGhostMode(enabled: Boolean) {
        _isGhostMode.value = enabled
    }

    // Update user custom status
    fun updateVibe(emoji: String, text: String, activityType: String) {
        _currentUserVibe.value = UserVibe(
            emoji = emoji,
            text = text,
            activityType = activityType,
            timestamp = System.currentTimeMillis()
        )
    }

    // Toggle Favorite friend
    fun toggleFavorite(friendId: String) {
        _friends.value = _friends.value.map { friend ->
            if (friend.id == friendId) {
                friend.copy(isFavorite = !friend.isFavorite)
            } else {
                friend
            }
        }
    }

    // Anchor friends relative to user's real GPS coordinates on Earth
    fun anchorFriendsAroundUser(userLat: Double, userLng: Double) {
        val offsets = listOf(
            Triple(0.0035, -0.0042, "Nord-Ouest"),
            Triple(-0.0028, 0.0051, "Sud-Est"),
            Triple(0.0052, 0.0031, "Nord-Est"),
            Triple(-0.0045, -0.0038, "Sud-Ouest"),
            Triple(0.0080, 0.0015, "Est"),
            Triple(-0.0015, -0.0065, "Ouest")
        )

        _friends.value = _friends.value.mapIndexed { index, friend ->
            val offset = offsets.getOrElse(index) { Triple((index * 0.002), (index * -0.002), "Proche") }
            val newLat = userLat + offset.first
            val newLng = userLng + offset.second
            val dist = RealLocationProvider.calculateDistanceKm(userLat, userLng, newLat, newLng)
            friend.copy(
                latitude = newLat,
                longitude = newLng,
                distanceKm = dist,
                district = "Secteur ${offset.third}"
            )
        }
    }

    // Simulate subtle real-time movement / radar updates
    fun simulateRadarPing() {
        _friends.value = _friends.value.map { friend ->
            val deltaLat = (Random.nextDouble() - 0.5) * 0.0004
            val deltaLng = (Random.nextDouble() - 0.5) * 0.0004
            val newDist = (friend.distanceKm + (Random.nextDouble() - 0.5) * 0.05).coerceIn(0.1, 10.0)
            val roundedDist = Math.round(newDist * 100.0) / 100.0
            friend.copy(
                latitude = friend.latitude + deltaLat,
                longitude = friend.longitude + deltaLng,
                distanceKm = roundedDist
            )
        }
    }
}
