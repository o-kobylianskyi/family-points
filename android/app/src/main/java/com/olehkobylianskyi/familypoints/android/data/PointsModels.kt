package com.olehkobylianskyi.familypoints.android.data

data class PointTransactionResponse(
    val id: Long,
    val memberId: Long,
    val pointTypeCode: String?,
    val pointTypeName: String?,
    val amount: Int,
    val type: String,
    val sourceType: String,
    val sourceId: Long?,
    val description: String?,
    val createdAt: String?
)

data class PointOperationRequest(
    val amount: Int,
    val description: String?
)

data class PointsPageData(
    val members: List<WorkspaceMemberResponse>,
    val balance: Long,
    val history: List<PointTransactionResponse>
)
