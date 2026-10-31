package com.olehkobylianskyi.familypoints.android.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

data class WorkspaceEpochResponse(val workspaceId: Long, val dataEpoch: Long)

interface WorkspaceEpochApi {
    @GET("workspaces/{workspaceId}/data-epoch")
    suspend fun getEpoch(
        @Header("Authorization") auth: String,
        @Path("workspaceId") workspaceId: Long
    ): WorkspaceEpochResponse
}
