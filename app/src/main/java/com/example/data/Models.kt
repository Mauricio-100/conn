package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val username: String,
    val passwordHash: String, // We'll just mock this for now
    val avatarUrl: String? = null,
    val bio: String = "Welcome to my profile!",
    val privacySetting: String = "Public", // "Public" or "Private"
    val isVerified: Boolean = false,
    val followingCount: Int = 0,
    val followersCount: Int = 0,
    val isGiant: Boolean = false,
    val level: Int = 1,
    val xp: Int = 0,
    val badges: String = "", // Comma-separated list of badge names
    val email: String? = null,
    val phoneNumber: String? = null,
    val birthDate: String? = null,
    val zodiacSign: String? = null,
    val preferredCategory: String? = "@(fun)"
)

@Entity(tableName = "actfiles")
data class Actfile(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val content: String,
    val tags: String = "",
    val likesCount: Int = 0,
    val viewsCount: Int = 0,
    val commentsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isLikedByMe: Boolean = false,
    val category: String? = null
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val receiverId: String,
    val content: String,
    val type: String = "text",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "follows")
data class Follow(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val followerId: String,
    val followingId: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class ActfileWithUser(
    val id: String,
    val userId: String,
    val username: String,
    val avatarUrl: String?,
    val isVerified: Boolean,
    val content: String,
    val tags: String,
    val likesCount: Int,
    val viewsCount: Int,
    val commentsCount: Int,
    val createdAt: Long,
    val isLikedByMe: Boolean = false,
    val category: String? = null
)

@Entity(tableName = "actfile_comments")
data class ActfileComment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val actfileId: String,
    val content: String,
    val userId: String,
    val username: String,
    val avatarUrl: String?,
    val isVerified: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val type: String, // "like", "comment", "follow", "message", "mention"
    val fromUserId: String,
    val fromUsername: String,
    val fromAvatar: String? = null,
    val message: String,
    val targetId: String? = null, // e.g., actfileId or messageId
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

