package com.olehkobylianskyi.familypoints.android.network

import okhttp3.ResponseBody
import retrofit2.http.Streaming
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Query
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.MultipartBody
import retrofit2.http.Path

/** References are workspace-authorized, and image bytes require the same bearer token. */
data class MediaRefDto(
    val source: String?,
    val mediaId: String?,
    val catalogKey: String?,
    val version: Int,
    val path: String?
)
data class MediaCatalogItemDto(
    val key: String,
    val category: String,
    val labelKey: String,
    val version: Int
)
data class MediaManifestDto(
    val cover: MediaRefDto?,
    val icon: MediaRefDto?,
    val coverChoices: List<MediaCatalogItemDto>
)
data class MediaTargetDto(val type: String, val targetId: Long, val slot: String)
data class MediaReferenceBatchDto(val targets: List<MediaTargetDto>)
data class MediaReferenceDto(val target: MediaTargetDto, val image: MediaRefDto?)

data class MediaBindRequest(val assetId: String?, val catalogKey: String?, val expectedVersion: Int)
data class MediaUploadResponse(val assetId: String)

interface MediaApi {
    @GET("workspaces/{workspaceId}/media/bindings/MEMBER/{memberId}/AVATAR")
    suspend fun memberAvatar(
        @Header("Authorization") bearer: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("memberId") memberId: Long
    ): MediaRefDto

    @PUT("workspaces/{workspaceId}/media/bindings/MEMBER/{memberId}/AVATAR")
    suspend fun bindMemberAvatar(
        @Header("Authorization") bearer: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("memberId") memberId: Long,
        @Body body: MediaBindRequest
    ): MediaRefDto

    @DELETE("workspaces/{workspaceId}/media/bindings/MEMBER/{memberId}/AVATAR")
    suspend fun clearMemberAvatar(
        @Header("Authorization") bearer: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("memberId") memberId: Long,
        @Query("expectedVersion") expectedVersion: Int
    ): MediaRefDto

    @Multipart
    @POST("workspaces/{workspaceId}/media/uploads")
    suspend fun uploadMemberImage(
        @Header("Authorization") bearer: String,
        @Path("workspaceId") workspaceId: Long,
        @Part file: MultipartBody.Part
    ): MediaUploadResponse

    @Streaming
    @GET("workspaces/{workspaceId}/media/catalog/{key}/preview")
    suspend fun catalogPreview(
        @Header("Authorization") bearer: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("key") key: String
    ): ResponseBody

    @Streaming
    @GET("workspaces/{workspaceId}/media/{assetId}/variants/{variant}")
    suspend fun imageVariant(
        @Header("Authorization") bearer: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("assetId") assetId: String,
        @Path("variant") variant: String = "THUMB"
    ): ResponseBody

    @GET("workspaces/{workspaceId}/media/manifest")
    suspend fun manifest(
        @Header("Authorization") bearer: String,
        @Path("workspaceId") workspaceId: Long
    ): MediaManifestDto

    @POST("workspaces/{workspaceId}/media/references")
    suspend fun references(
        @Header("Authorization") bearer: String,
        @Path("workspaceId") workspaceId: Long,
        @Body batch: MediaReferenceBatchDto
    ): List<MediaReferenceDto>

    @GET("workspaces/{workspaceId}/media/catalog")
    suspend fun catalog(
        @Header("Authorization") bearer: String,
        @Path("workspaceId") workspaceId: Long
    ): List<MediaCatalogItemDto>
}
