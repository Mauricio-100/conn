package com.example.data


import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field

data class RegisterRequest(val username: String, val password: String? = null)
data class UserResponse(val id: String?, val username: String, val email: String?, val avatar_url: String?, val created_at: String?)
data class TokenResponse(val access_token: String, val token_type: String)

data class ActfileNetwork(
    val id: String,
    val content: String,
    val likes_count: Int,
    val views_count: Int,
    val comments_count: Int? = 0,
    val created_at: String,
    val user_id: String,
    val username: String,
    val avatar_url: String?,
    val is_verified: Boolean,
    val liked: Boolean,
    val category: String? = null
)

data class PublishActfileRequest(val content: String, val category: String? = null)

data class UserProfileNetwork(
    val id: String,
    val username: String,
    val avatar_url: String?,
    val bio: String?,
    val is_verified: Boolean,
    val followers_count: Int,
    val following_count: Int,
    val videos_count: Int,
    val is_online: Boolean
)

data class SearchResult(
    val results: List<SearchResultItem>
)

data class SearchResultItem(
    val id: String,
    val username: String?,
    val avatar_url: String?,
    val bio: String?,
    val is_verified: Boolean?
)

data class ConversationNetwork(
    val id: String,
    val user_id: String,
    val username: String,
    val avatar_url: String?,
    val last_message: String?,
    val last_message_time: String?,
    val unread_count: Int? = 0,
    val is_online: Boolean
)

data class MessageNetwork(
    val id: String,
    val content: String,
    val type: String,
    val sender_id: String,
    val receiver_id: String,
    val read: Boolean,
    val created_at: String,
    val sender_username: String?,
    val sender_avatar: String?
)

data class SendMessageRequest(
    val content: String,
    val receiver_id: String,
    val type: String = "text"
)

data class ActfileCommentCreate(val content: String)

data class ActfileCommentResponse(
    val id: String,
    val content: String,
    val created_at: String,
    val user: CommentUser?
)

data class CommentUser(
    val id: String,
    val username: String,
    val avatar_url: String?
)

data class NotificationNetwork(
    val id: String,
    val type: String,
    val from_user_id: String,
    val from_username: String?,
    val from_avatar: String?,
    val message: String,
    val target_id: String?,
    val read: Boolean,
    val created_at: String
)

data class ActfileCommentListResponse(
    val id: String,
    val content: String,
    val created_at: String,
    val user_id: String,
    val username: String,
    val avatar_url: String?,
    val is_verified: Boolean
)

data class UpdateProfileRequest(
    val bio: String? = null,
    val avatar_url: String? = null,
    val username: String? = null,
    val preferred_category: String? = null
)

interface ApiService {
    @POST("/api/users/profile")
    suspend fun updateProfile(
        @retrofit2.http.Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): UserProfileNetwork

    @FormUrlEncoded
    @POST("/api/token")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String?
    ): TokenResponse

    @POST("/api/users/register")
    suspend fun signup(@Body request: RegisterRequest): UserResponse

    @retrofit2.http.GET("/api/actfile")
    suspend fun getActfiles(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Query("limit") limit: Int = 20
    ): List<ActfileNetwork>

    @POST("/api/actfile")
    suspend fun publishActfile(
        @retrofit2.http.Header("Authorization") token: String?,
        @Body request: PublishActfileRequest
    ): ActfileNetwork

    @retrofit2.http.DELETE("/api/actfile/{id}")
    suspend fun deleteActfile(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): Map<String, Any>

    @POST("/api/actfile/{id}/like")
    suspend fun likeActfile(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): Map<String, Boolean>

    @POST("/api/actfile/{id}/view")
    suspend fun viewActfile(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): Map<String, String>

    @POST("/api/actfile/{id}/comment")
    suspend fun commentActfile(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String,
        @Body request: ActfileCommentCreate
    ): ActfileCommentResponse

    @retrofit2.http.GET("/api/actfile/{id}/comments")
    suspend fun getActfileComments(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): List<ActfileCommentListResponse>

    @retrofit2.http.GET("/api/users/me")
    suspend fun getMyProfile(
        @retrofit2.http.Header("Authorization") token: String?
    ): UserProfileNetwork

    @retrofit2.http.GET("/api/users/{id}")
    suspend fun getUserProfile(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): UserProfileNetwork

    @POST("/api/users/{id}/follow")
    suspend fun followUser(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): Map<String, Boolean>

    @POST("/api/users/{id}/unfollow")
    suspend fun unfollowUser(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): Map<String, Boolean>

    @retrofit2.http.GET("/api/search")
    suspend fun search(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Query("q") query: String,
        @retrofit2.http.Query("type") type: String = "users"
    ): SearchResult

    @retrofit2.http.GET("/api/messages/conversations")
    suspend fun getConversations(
        @retrofit2.http.Header("Authorization") token: String?
    ): List<ConversationNetwork>

    @retrofit2.http.GET("/api/messages/{id}")
    suspend fun getMessages(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") userId: String
    ): List<MessageNetwork>

    @POST("/api/messages/send")
    suspend fun sendMessage(
        @retrofit2.http.Header("Authorization") token: String?,
        @Body request: SendMessageRequest
    ): MessageNetwork

    @retrofit2.http.GET("/api/notifications")
    suspend fun getNotifications(
        @retrofit2.http.Header("Authorization") token: String?
    ): List<NotificationNetwork>
}

object RetrofitClient {
    private const val BASE_URL = "https://hoosthubs-g.onrender.com"

    val apiService: ApiService by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}
