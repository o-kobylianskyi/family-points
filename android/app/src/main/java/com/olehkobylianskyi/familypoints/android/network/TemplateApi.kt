package com.olehkobylianskyi.familypoints.android.network

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

data class TemplateSelectionRequest(val selectedCodes: List<String>)
data class CatalogTemplateDto(val code: String, val kind: String, val title: Map<String, String>?)
data class TemplateCatalogDto(val version: Int, val templates: List<CatalogTemplateDto>?, val packs: List<JsonObject>?)
data class PrivateTemplateDto(val id: Long, val kind: String?, val title: String?, val definitionJson: String?)
data class TemplateImportPreviewDto(val alreadyImported: List<String>?, val toImport: List<String>?)

interface TemplateApi {
    @GET("template-catalog")
    suspend fun catalog(@Header("Authorization") authorization: String): TemplateCatalogDto

    @GET("workspaces/{workspaceId}/templates")
    suspend fun privateTemplates(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long
    ): List<PrivateTemplateDto>

    @POST("workspaces/{workspaceId}/templates/imports/preview")
    suspend fun preview(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Body request: TemplateSelectionRequest
    ): TemplateImportPreviewDto

    @POST("workspaces/{workspaceId}/templates/imports")
    suspend fun import(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Body request: TemplateSelectionRequest
    ): JsonObject
}
