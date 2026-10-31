package com.olehkobylianskyi.familypoints.android.network

import com.olehkobylianskyi.familypoints.android.data.*
import retrofit2.http.*

interface RoleCatalogApi {
    @GET("workspaces/{id}/role-sets")
    suspend fun sets(@Header("Authorization") token: String, @Path("id") id: Long): List<RoleSetItem>
    @GET("workspaces/{id}/role-definitions")
    suspend fun roles(@Header("Authorization") token: String, @Path("id") id: Long): List<RoleDefinitionItem>
    @POST("workspaces/{id}/role-sets")
    suspend fun createSet(@Header("Authorization") token: String, @Path("id") id: Long, @Body body: RoleSetSave): RoleSetItem
    @PUT("workspaces/{id}/role-sets/{setId}")
    suspend fun updateSet(@Header("Authorization") token: String, @Path("id") id: Long, @Path("setId") setId: Long, @Body body: RoleSetSave): RoleSetItem
    @DELETE("workspaces/{id}/role-sets/{setId}")
    suspend fun deleteSet(@Header("Authorization") token: String, @Path("id") id: Long, @Path("setId") setId: Long): retrofit2.Response<Unit>
    @POST("workspaces/{id}/role-definitions")
    suspend fun createRole(@Header("Authorization") token: String, @Path("id") id: Long, @Body body: RoleDefinitionSave): RoleDefinitionItem
    @PUT("workspaces/{id}/role-definitions/{roleId}")
    suspend fun updateRole(@Header("Authorization") token: String, @Path("id") id: Long, @Path("roleId") roleId: Long, @Body body: RoleDefinitionSave): RoleDefinitionItem
    @DELETE("workspaces/{id}/role-definitions/{roleId}")
    suspend fun deleteRole(@Header("Authorization") token: String, @Path("id") id: Long, @Path("roleId") roleId: Long): retrofit2.Response<Unit>
}
