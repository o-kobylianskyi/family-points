package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.*
import com.olehkobylianskyi.familypoints.android.storage.TokenStore

class TemplateRepository(private val tokens: TokenStore) {
    private fun auth(): String = "Bearer " + (tokens.getAccessToken() ?: error("Missing access token"))
    suspend fun update(workspaceId: Long, id: Long, title: String, definitionJson: String) {
        val response = ApiClient.templateApi.updatePrivateTemplate(auth(), workspaceId, id, PrivateTemplateUpdateRequest(title, definitionJson))
        if (!response.isSuccessful) throw retrofit2.HttpException(response)
    }
    suspend fun delete(workspaceId: Long, id: Long) {
        val response = ApiClient.templateApi.deletePrivateTemplate(auth(), workspaceId, id)
        if (!response.isSuccessful) throw retrofit2.HttpException(response)
    }
    suspend fun catalog() = ApiClient.templateApi.catalog(auth())
    suspend fun privateTemplates(workspaceId: Long) = ApiClient.templateApi.privateTemplates(auth(), workspaceId)
    suspend fun preview(workspaceId: Long, codes: List<String>) =
        ApiClient.templateApi.preview(auth(), workspaceId, TemplateSelectionRequest(codes))
    suspend fun importSelected(workspaceId: Long, codes: List<String>) {
        val response = ApiClient.templateApi.importSelected(auth(), workspaceId, TemplateSelectionRequest(codes))
        if (!response.isSuccessful) throw retrofit2.HttpException(response)
    }
}
