import com.example.data.Community
import com.example.data.Channel
import retrofit2.http.*

interface CommunityApi {
    @POST("/api/communities")
    suspend fun createCommunity(
        @Header("Authorization") token: String,
        @Body request: Map<String, Any>
    ): Community

    @GET("/api/communities")
    suspend fun searchCommunities(
        @Header("Authorization") token: String?,
        @Query("q") query: String?,
        @Query("category") category: String?,
        @Query("sort") sort: String = "popular",
        @Query("limit") limit: Int = 20
    ): List<Community>

    @GET("/api/communities/{slug}")
    suspend fun getCommunity(
        @Header("Authorization") token: String?,
        @Path("slug") slug: String
    ): Community

    @POST("/api/communities/{slug}/join")
    suspend fun joinCommunity(
        @Header("Authorization") token: String,
        @Path("slug") slug: String
    ): Map<String, Any>

    @GET("/api/communities/{slug}/channels")
    suspend fun getCommunityChannels(
        @Header("Authorization") token: String?,
        @Path("slug") slug: String
    ): List<Channel>
    
    @GET("/api/users/me/communities")
    suspend fun getMyCommunities(
        @Header("Authorization") token: String
    ): List<Community>
}
