package com.olehkobylianskyi.familypoints.android.network

import okhttp3.ResponseBody
import retrofit2.http.Streaming
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
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

interface MediaApi {
    @Streaming
    @GET("workspaces/{workspaceId}/media/{assetId}/variants/{variant}")
    suspend fun imageVariant(
        @Header("Authorization") bearer: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("assetId") assetId: String,
        @Path("variant") variant: String = "THUMBNAIL"
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
