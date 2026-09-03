package com.example.data


import retrofit2.Response
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.Json
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part
import okhttp3.MultipartBody
import okhttp3.RequestBody

data class RegisterRequest(val username: String, val password: String? = null)
data class UserResponse(val id: String?, val username: String, val email: String?, val avatar_url: String?, val created_at: String?)
data class IddetPlusStatusResponse(
    val is_iddet_plus: Boolean,
    val expires_at: String?,
    val credits: Int,
    val card_style: String,
    val monthly_price_usd: Double,
    val monthly_credits: Int,
    val history: List<IddetPlusHistoryItem>
)

data class IddetPlusHistoryItem(
    val status: String,
    val started_at: String?,
    val expires_at: String?,
    val price_amount: Double?,
    val price_currency: String?,
    val payment_ref: String?
)

data class UserCardResponse(
    val user_id: String,
    val username: String,
    val avatar_url: String?,
    val bio: String?,
    val is_verified: Boolean,
    val card_style: String,
    val is_iddet_plus: Boolean,
    val credits: Int? = null,
    val level: LevelInfo
)

data class LevelsTableResponse(val levels: List<LevelInfo>)
data class LevelInfo(
    val score: Int,
    val level_index: Int,
    val level_name: String,
    val next_level_name: String?,
    val next_level_score: Int?,
    val points_to_next: Int,
    val progress: Double,
    val is_max_level: Boolean
)



data class CardStyleUpdateRequest(val style: String)
data class CardStyleUpdateResponse(val card_style: String)

data class CreditsResponse(
    val credits: Int,
    val transactions: List<CreditTransaction>
)

data class CreditTransaction(
    val amount: Int,
    val reason: String,
    val reference: String?,
    val balance_after: Int,
    val created_at: String
)

data class IddetPlusCheckoutRequest(val currency: String = "USD")
data class IddetPlusCheckoutResponse(
    val reference: String,
    val checkout_url: String,
    val amount: Double,
    val currency: String
)
data class TokenResponse(val access_token: String, val token_type: String)

data class CategoriesResponse(val categories: List<String>)
data class PreferredCategoriesResponse(val preferred_categories: List<String>)
data class UpdateCategoriesRequest(val categories: List<String>)
data class UpdateCategoriesResponse(val status: String, val preferred_categories: List<String>)

data class ActfileNetwork(
    val id: String = "",
    val content: String = "",
    val likes_count: Int = 0,
    val views_count: Int = 0,
    val comments_count: Int? = 0,
    val created_at: String = "",
    val user_id: String = "",
    val username: String = "",
    val avatar_url: String? = null,
    val is_verified: Boolean = false,
    val liked: Boolean = false,
    val category: String? = null,
    val community_id: String? = null,
    val channel_id: String? = null,
    val channel_slug: String? = null,
    val channel_name: String? = null
)

data class PublishActfileRequest(
    @field:Json(name = "content") val content: String,
    @field:Json(name = "category") val category: String = "Autres",
    @field:Json(name = "community_slug") val community_slug: String? = null,
    @field:Json(name = "channel_slug") val channel_slug: String? = null
)

data class UserProfileNetwork(
    val id: String? = null,
    val username: String,
    val avatar_url: String?,
    val bio: String?,
    val is_verified: Boolean = false,
    val followers_count: Int = 0,
    val following_count: Int = 0,
    val videos_count: Int = 0,
    val is_online: Boolean = false,
    val email: String? = null,
    val phone_number: String? = null,
    val zodiac_sign: String? = null,
    val created_at: String? = null,
    val last_seen: String? = null,
    val is_iddet_plus: Boolean = false,
    val card_style: String? = null
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

data class MessageReactionGroup(
    val emoji: String,
    val count: Int = 1,
    val users: List<String> = emptyList(),
    @Json(name = "has_reacted") val has_reacted: Boolean = false
)

data class ConversationNetwork(
    val id: String,
    val user_id: String,
    val username: String,
    val avatar_url: String?,
    val last_message: String?,
    val last_message_time: String?,
    val unread_count: Int? = 0,
    val is_online: Boolean,
    val is_verified: Boolean = false,
    @Json(name = "is_iddet_plus") val is_iddet_plus: Boolean = false,
    @Json(name = "card_style") val card_style: String? = null
)

data class MessageNetwork(
    val id: String,
    val content: String,
    val type: String,
    val sender_id: String,
    val receiver_id: String,
    val read: Boolean,
    val created_at: String,
    val sender_username: String? = null,
    val sender_avatar: String? = null,
    val reaction: String? = null,
    val reactions: List<MessageReactionGroup>? = null
)

data class SendMessageRequest(
    val content: String,
    val receiver_id: String,
    val type: String = "text"
)

data class UploadResponse(
    val url: String,
    val public_id: String? = null,
    val secure_url: String? = null
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
    val preferred_category: String? = null,
    val phone_number: String? = null,
    val privacy_setting: String? = null,
    val email: String? = null,
    val birth_date: String? = null,
    val zodiac_sign: String? = null
)

data class MessageReactionRequest(
    val emoji: String
)

data class UserLevelResponse(
    val user_id: String? = null,
    val score: Int = 0,
    val level_index: Int = 0,
    val level_name: String = "Débutant",
    val next_level_name: String? = "Bronze",
    val next_level_score: Int? = 100,
    val points_to_next: Int = 100,
    val progress: Float = 0f,
    val is_max_level: Boolean = false
)



data class StoryResponse(
    val id: String,
    val media_url: String,
    val media_type: String = "image",
    val effect: String? = null,
    val created_at: String = "",
    val user: StoryUserResponse,
    val views: Int? = null,
    val view_count: Int? = null,
    val reactions: Map<String, Int>? = null
)

data class StoryUserResponse(
    val id: String,
    val username: String,
    val avatar_url: String? = null,
    val is_verified: Boolean? = false
)

data class CreateStoryResponse(
    val status: String? = null,
    val story_id: String? = null,
    val media_url: String? = null,
    val media_type: String? = null,
    val effect_applied: String? = null
)

interface ApiService {

    @retrofit2.http.GET("/api/users/me/iddet-plus")
    suspend fun getMyIddetPlusStatus(
        @retrofit2.http.Header("Authorization") token: String
    ): IddetPlusStatusResponse

    @retrofit2.http.GET("/api/users/me/card")
    suspend fun getMyCard(
        @retrofit2.http.Header("Authorization") token: String
    ): UserCardResponse

    @retrofit2.http.GET("/api/users/{user_id}/card")
    suspend fun getUserCard(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("user_id") userId: String
    ): UserCardResponse

    @retrofit2.http.PUT("/api/users/me/card/style")
    suspend fun updateCardStyle(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Body request: CardStyleUpdateRequest
    ): CardStyleUpdateResponse

    @retrofit2.http.GET("/api/credits/me")
    suspend fun getMyCredits(
        @retrofit2.http.Header("Authorization") token: String
    ): CreditsResponse

    @retrofit2.http.POST("/api/iddet-plus/checkout")
    suspend fun createIddetPlusCheckout(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Body request: IddetPlusCheckoutRequest = IddetPlusCheckoutRequest()
    ): IddetPlusCheckoutResponse
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
        @retrofit2.http.Query("limit") limit: Int? = 900,
        @retrofit2.http.Query("cursor") cursor: String? = null,
        @retrofit2.http.Query("user_id") userId: String? = null
    ): List<ActfileNetwork>

    @retrofit2.http.GET("/api/actfiles")
    suspend fun getActfilesPlural(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Query("limit") limit: Int? = 900,
        @retrofit2.http.Query("cursor") cursor: String? = null,
        @retrofit2.http.Query("user_id") userId: String? = null
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

    @Multipart
    @PUT("/api/users/me")
    suspend fun updateProfileMultipart(
        @retrofit2.http.Header("Authorization") token: String,
        @Part avatar: MultipartBody.Part? = null,
        @Part("bio") bio: RequestBody? = null,
        @Part("phone_number") phoneNumber: RequestBody? = null
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

    @POST("/api/messages/read/{userId}")
    suspend fun markMessagesRead(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("userId") userId: String
    ): Map<String, Any>

    @retrofit2.http.DELETE("/api/messages/{id}")
    suspend fun deleteMessage(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): Map<String, Any>

    @retrofit2.http.DELETE("/api/messages/conversation/{userId}")
    suspend fun deleteConversation(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("userId") userId: String
    ): Map<String, Any>

    @POST("/api/messages/{id}/react")
    suspend fun reactToMessage(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String,
        @Body body: MessageReactionRequest
    ): Map<String, Any>

    @retrofit2.http.GET("/api/messages/{id}/reactions")
    suspend fun getMessageReactions(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): List<MessageReactionGroup>

    @retrofit2.http.DELETE("/api/messages/{id}/react")
    suspend fun removeMessageReaction(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): Map<String, Any>

    @Multipart
    @POST("/api/upload")
    suspend fun uploadAudio(
        @retrofit2.http.Header("Authorization") token: String?,
        @Part file: okhttp3.MultipartBody.Part
    ): UploadResponse

    @retrofit2.http.GET("/api/notifications")
    suspend fun getNotifications(
        @retrofit2.http.Header("Authorization") token: String?
    ): List<NotificationNetwork>

    @retrofit2.http.GET("/api/categories")
    suspend fun getCategories(): CategoriesResponse

    @retrofit2.http.GET("/api/users/me/categories")
    suspend fun getMyPreferredCategories(
        @retrofit2.http.Header("Authorization") token: String
    ): PreferredCategoriesResponse

    @retrofit2.http.PUT("/api/users/me/categories")
    suspend fun updatePreferredCategories(
        @retrofit2.http.Header("Authorization") token: String,
        @Body body: UpdateCategoriesRequest
    ): UpdateCategoriesResponse

    @POST("/api/communities")
    suspend fun createCommunity(
        @retrofit2.http.Header("Authorization") token: String,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @retrofit2.http.GET("/api/communities")
    suspend fun searchCommunities(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Query("q") query: String?,
        @retrofit2.http.Query("category") category: String?,
        @retrofit2.http.Query("sort") sort: String = "popular",
        @retrofit2.http.Query("limit") limit: Int = 20
    ): List<Community>

    @retrofit2.http.GET("/api/communities/{slug}")
    suspend fun getCommunity(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("slug") slug: String
    ): Community

    @POST("/api/communities/{slug}/join")
    suspend fun joinCommunity(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String
    ): Map<String, @JvmSuppressWildcards Any>

    @retrofit2.http.GET("/api/communities/{slug}/posts")
    suspend fun getCommunityPosts(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("slug") slug: String,
        @retrofit2.http.Query("limit") limit: Int = 20,
        @retrofit2.http.Query("cursor") cursor: String? = null
    ): List<ActfileNetwork>

    @retrofit2.http.GET("/api/communities/{slug}/channels")
    suspend fun getCommunityChannels(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("slug") slug: String
    ): List<Channel>

    @retrofit2.http.POST("/api/communities/{slug}/channels")
    suspend fun createChannel(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Channel
    
    @retrofit2.http.GET("/api/users/me/communities")
    suspend fun getMyCommunities(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Query("role") role: String? = null
    ): List<Community>

    @retrofit2.http.PUT("/api/communities/{slug}")
    suspend fun updateCommunity(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Community

    @DELETE("/api/communities/{slug}")
    suspend fun deleteCommunity(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String
    ): Map<String, @JvmSuppressWildcards Any>

    @PUT("/api/communities/{slug}/members/{user_id}/role")
    suspend fun updateMemberRole(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @retrofit2.http.Path("user_id") userId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Map<String, @JvmSuppressWildcards Any>

    @POST("/api/communities/{slug}/ban")
    suspend fun banCommunityMember(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Map<String, @JvmSuppressWildcards Any>

    @Multipart
    @retrofit2.http.PUT("/api/communities/{slug}/icon")
    suspend fun updateCommunityIcon(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @retrofit2.http.Part icon: okhttp3.MultipartBody.Part
    ): Map<String, @JvmSuppressWildcards Any>

    @retrofit2.http.GET("/api/communities/{slug}/bots")
    suspend fun getCommunityBots(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String
    ): Response<ResponseBody>

    @retrofit2.http.POST("/api/communities/{slug}/bots")
    suspend fun createCommunityBot(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @retrofit2.http.PUT("/api/communities/{slug}/bots/{bot_id}")
    suspend fun updateCommunityBot(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @retrofit2.http.Path("bot_id") botId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @DELETE("/api/communities/{slug}/bots/{bot_id}")
    suspend fun deleteCommunityBot(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @retrofit2.http.Path("bot_id") botId: String
    ): Response<ResponseBody>

    @retrofit2.http.POST("/api/communities/{slug}/bots/{bot_id}/actions")
    suspend fun performBotModAction(
        @retrofit2.http.Header("X-Bot-Token") botToken: String?,
        @retrofit2.http.Path("slug") slug: String,
        @retrofit2.http.Path("bot_id") botId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @retrofit2.http.GET("/api/communities/{slug}/mod-actions")
    suspend fun getCommunityModActions(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @retrofit2.http.Query("limit") limit: Int = 50
    ): Response<ResponseBody>

    @retrofit2.http.GET("/api/stories")
    suspend fun getStories(
        @retrofit2.http.Header("Authorization") token: String?
    ): List<StoryResponse>

    @Multipart
    @POST("/api/stories")
    suspend fun createStory(
        @retrofit2.http.Header("Authorization") token: String?,
        @Part file: okhttp3.MultipartBody.Part,
        @Part("effect") effect: okhttp3.RequestBody? = null
    ): CreateStoryResponse

    @retrofit2.http.DELETE("/api/stories/{id}")
    suspend fun deleteStory(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): Map<String, Any>

    @retrofit2.http.GET("/api/users/me/level")
    suspend fun getMyLevel(
        @retrofit2.http.Header("Authorization") token: String?
    ): UserLevelResponse

    @retrofit2.http.GET("/api/users/{user_id}/level")
    suspend fun getUserLevel(
        @retrofit2.http.Path("user_id") userId: String
    ): UserLevelResponse

    @retrofit2.http.GET("/api/levels")
    suspend fun getLevelsTable(): LevelsTableResponse
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
