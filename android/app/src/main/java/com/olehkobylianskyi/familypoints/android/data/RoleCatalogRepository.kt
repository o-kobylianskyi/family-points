package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class RoleCatalogRepository(private val store: TokenStore) {
    private fun token() = "Bearer " + (store.getAccessToken() ?: error("Missing access token"))
    suspend fun load(id: Long): Pair<List<RoleSetItem>, List<RoleDefinitionItem>> = coroutineScope {
        val t = token()
        val sets = async { ApiClient.roleCatalogApi.sets(t, id) }
        val roles = async { ApiClient.roleCatalogApi.roles(t, id) }
        sets.await() to roles.await()
    }
    suspend fun saveSet(id: Long, setId: Long?, body: RoleSetSave) {
        if (setId == null) ApiClient.roleCatalogApi.createSet(token(), id, body)
        else ApiClient.roleCatalogApi.updateSet(token(), id, setId, body)
    }
    suspend fun deleteSet(id: Long, setId: Long) { ApiClient.roleCatalogApi.deleteSet(token(), id, setId) }
    suspend fun saveRole(id: Long, roleId: Long?, body: RoleDefinitionSave) {
        if (roleId == null) ApiClient.roleCatalogApi.createRole(token(), id, body)
        else ApiClient.roleCatalogApi.updateRole(token(), id, roleId, body)
    }
    suspend fun deleteRole(id: Long, roleId: Long) { ApiClient.roleCatalogApi.deleteRole(token(), id, roleId) }
}
