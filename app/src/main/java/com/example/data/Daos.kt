package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): User?
    
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserByIdFlow(id: String): Flow<User?>

    @Query("SELECT * FROM users WHERE isGiant = 1 AND id != :currentUserId")
    fun getGiants(currentUserId: String): Flow<List<User>>

    @Query("SELECT * FROM users WHERE username LIKE '%' || :query || '%' AND id != :currentUserId")
    fun searchUsers(query: String, currentUserId: String): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("""
        SELECT users.* FROM users
        INNER JOIN (
            SELECT 
                CASE WHEN senderId = :userId THEN receiverId ELSE senderId END AS partnerId,
                MAX(createdAt) as lastMessageTime
            FROM messages
            WHERE senderId = :userId OR receiverId = :userId
            GROUP BY CASE WHEN senderId = :userId THEN receiverId ELSE senderId END
        ) AS last_msgs ON users.id = last_msgs.partnerId
        ORDER BY last_msgs.lastMessageTime DESC
    """)
    fun getChatPartners(userId: String): Flow<List<User>>
}

@Dao
interface ActfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActfile(actfile: Actfile)

    @Query("""
        SELECT a.id, a.userId, u.username, u.avatarUrl, u.isVerified, a.content, a.tags, a.likesCount, a.viewsCount, a.commentsCount, a.createdAt, a.isLikedByMe AS isLikedByMe, a.category 
        FROM actfiles a 
        INNER JOIN users u ON a.userId = u.id 
        ORDER BY a.createdAt DESC
    """)
    fun getAllActfilesWithUser(): Flow<List<ActfileWithUser>>

    @Query("""
        SELECT a.id, a.userId, u.username, u.avatarUrl, u.isVerified, a.content, a.tags, a.likesCount, a.viewsCount, a.commentsCount, a.createdAt, a.isLikedByMe AS isLikedByMe, a.category 
        FROM actfiles a 
        INNER JOIN users u ON a.userId = u.id 
        WHERE a.userId = :userId
        ORDER BY a.createdAt DESC
    """)
    fun getActfilesByUser(userId: String): Flow<List<ActfileWithUser>>

    @Query("""
        SELECT a.id, a.userId, u.username, u.avatarUrl, u.isVerified, a.content, a.tags, a.likesCount, a.viewsCount, a.commentsCount, a.createdAt, a.isLikedByMe AS isLikedByMe, a.category 
        FROM actfiles a 
        INNER JOIN users u ON a.userId = u.id 
        WHERE a.content LIKE '%' || :query || '%' OR a.tags LIKE '%' || :query || '%'
        ORDER BY a.createdAt DESC
    """)
    fun searchActfiles(query: String): Flow<List<ActfileWithUser>>
    
    @Query("SELECT * FROM actfiles WHERE id = :id LIMIT 1")
    suspend fun getActfileRaw(id: String): Actfile?

    @Query("UPDATE actfiles SET viewsCount = viewsCount + 1 WHERE id = :id")
    suspend fun incrementViewCount(id: String)
    
    @Query("UPDATE actfiles SET likesCount = likesCount + 1 WHERE id = :id")
    suspend fun incrementLikeCount(id: String)
    
    @Query("UPDATE actfiles SET commentsCount = commentsCount + 1 WHERE id = :id")
    suspend fun incrementCommentCount(id: String)

    @Query("UPDATE actfiles SET commentsCount = :count WHERE id = :id")
    suspend fun updateCommentCount(id: String, count: Int)

    @Query("""
        SELECT a.id, a.userId, u.username, u.avatarUrl, u.isVerified, a.content, a.tags, a.likesCount, a.viewsCount, a.commentsCount, a.createdAt, a.isLikedByMe AS isLikedByMe, a.category 
        FROM actfiles a 
        INNER JOIN users u ON a.userId = u.id 
        WHERE a.id = :actfileId
        LIMIT 1
    """)
    fun getActfileById(actfileId: String): Flow<ActfileWithUser?>

    @Query("""
        SELECT a.id, a.userId, u.username, u.avatarUrl, u.isVerified, a.content, a.tags, a.likesCount, a.viewsCount, a.commentsCount, a.createdAt, a.isLikedByMe AS isLikedByMe, a.category 
        FROM actfiles a 
        INNER JOIN users u ON a.userId = u.id 
        WHERE a.isLikedByMe = 1
        ORDER BY a.createdAt DESC
    """)
    fun getLikedActfiles(): Flow<List<ActfileWithUser>>

    @Query("""
        SELECT DISTINCT a.id, a.userId, u.username, u.avatarUrl, u.isVerified, a.content, a.tags, a.likesCount, a.viewsCount, a.commentsCount, a.createdAt, a.isLikedByMe AS isLikedByMe, a.category 
        FROM actfiles a 
        INNER JOIN users u ON a.userId = u.id 
        INNER JOIN actfile_comments c ON a.id = c.actfileId
        WHERE c.userId = :userId
        ORDER BY a.createdAt DESC
    """)
    fun getCommentedActfiles(userId: String): Flow<List<ActfileWithUser>>

    @Query("DELETE FROM actfiles WHERE id = :id")
    suspend fun deleteActfileLocal(id: String)
}

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("SELECT * FROM messages WHERE (senderId = :user1 AND receiverId = :user2) OR (senderId = :user2 AND receiverId = :user1) ORDER BY createdAt ASC")
    fun getMessagesBetween(user1: String, user2: String): Flow<List<Message>>
}

@Dao
interface FollowDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: Follow)

    @Query("DELETE FROM follows WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun deleteFollow(followerId: String, followingId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE followerId = :followerId AND followingId = :followingId)")
    fun isFollowingFlow(followerId: String, followingId: String): Flow<Boolean>

    @Query("SELECT followingId FROM follows WHERE followerId = :followerId")
    fun getFollowingIdsFlow(followerId: String): Flow<List<String>>

    @Query("""
        SELECT a.id, a.userId, u.username, u.avatarUrl, u.isVerified, a.content, a.tags, a.likesCount, a.viewsCount, a.commentsCount, a.createdAt, a.isLikedByMe AS isLikedByMe, a.category 
        FROM actfiles a 
        INNER JOIN users u ON a.userId = u.id 
        WHERE a.userId IN (SELECT followingId FROM follows WHERE followerId = :followerId) OR a.userId = :followerId
        ORDER BY a.createdAt DESC
    """)
    fun getFollowedActfiles(followerId: String): Flow<List<ActfileWithUser>>
}

@Dao
interface CommentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<ActfileComment>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: ActfileComment)

    @Query("SELECT * FROM actfile_comments WHERE actfileId = :actfileId ORDER BY createdAt ASC")
    fun getCommentsForActfile(actfileId: String): Flow<List<ActfileComment>>
    
    @Query("DELETE FROM actfile_comments WHERE actfileId = :actfileId")
    suspend fun deleteCommentsForActfile(actfileId: String)
}

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<Notification>)

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun getNotificationsForUser(userId: String): Flow<List<Notification>>

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: String)
}


