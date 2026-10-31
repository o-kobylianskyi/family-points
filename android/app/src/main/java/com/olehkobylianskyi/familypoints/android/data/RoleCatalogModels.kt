package com.olehkobylianskyi.familypoints.android.data

data class RoleSetItem(
    val id: Long, val name: String, val description: String?,
    val systemCode: String?, val systemDefault: Boolean
)
data class RoleDefinitionItem(
    val id: Long, val name: String, val description: String?,
    val systemCode: String?, val systemDefault: Boolean,
    val roleSetId: Long?, val visibility: String?
)
data class RoleSetSave(val name: String, val description: String?)
data class RoleDefinitionSave(
    val name: String, val description: String?, val roleSetId: Long?,
    val visibility: String = "SHARED",
    val ownerContextType: String = "WORKSPACE",
    val ownerContextId: Long? = null
)
