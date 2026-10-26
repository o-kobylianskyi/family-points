package com.olehkobylianskyi.familypoints.android.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

data class PrivateTemplateUpdateRequest(val title: String, val definitionJson: String, val kind: String)
data class TemplateSelectionRequest(val selectedCodes: List<String>)
data class CatalogTemplateDto(val code: String, val kind: String, val title: Map<String, String>?, val description: Map<String, String>?, val definition: com.google.gson.JsonObject?)
data class TemplateCatalogDto(val version: Int, val templates: List<CatalogTemplateDto>?, val packs: List<com.google.gson.JsonObject>?)
data class PrivateTemplateDto(val id: Long, val kind: String?, val title: String?, val definitionJson: String?, val catalogCode: String?, val catalogVersion: Int?)
data class TemplateImportPreviewDto(val alreadyImported: List<String>?, val toImport: List<String>?)

interface TemplateApi {
    @GET("workspaces/{workspaceId}/templates/{templateId}/creation-draft")
    suspend fun creationDraft(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("templateId") templateId: Long
    ): com.google.gson.JsonObject

    @PUT("workspaces/{workspaceId}/templates/{templateId}")
    suspend fun updatePrivateTemplate(@Header("Authorization") auth: String, @Path("workspaceId") workspaceId: Long, @Path("templateId") templateId: Long, @Body body: PrivateTemplateUpdateRequest): retrofit2.Response<okhttp3.ResponseBody>

    @DELETE("workspaces/{workspaceId}/templates/{templateId}")
    suspend fun deletePrivateTemplate(@Header("Authorization") auth: String, @Path("workspaceId") workspaceId: Long, @Path("templateId") templateId: Long): retrofit2.Response<Void>

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
    suspend fun importSelected(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Body request: TemplateSelectionRequest
    ): retrofit2.Response<Void>
}
