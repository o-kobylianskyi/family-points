package com.olehkobylianskyi.familypoints.android.data

data class RewardDefinitionSummary(
    val id: Long,
    val title: String,
    val description: String?,
    val pointTypeId: Long,
    val pointTypeCode: String,
    val pointTypeName: String,
    val priceAmount: Int,
    val minimumReputation: Int?,
    val requiresApproval: Boolean,
    val rewardKind: String,
    val defaultDurationMinutes: Int?,
    val acquisitionMode: String,
    val active: Boolean
)

data class TaskRewardRequestCreateRequest(
    val pointTypeId: Long?,
    val pointAmount: Int?,
    val reputationAmount: Int?,
    val rewardDefinitionId: Long?,
    val durationMinutes: Int?,
    val customRewardTitle: String?,
    val comment: String?
)

data class TaskRewardRequestResponse(
    val id: Long,
    val taskInstanceId: Long,
    val taskTitle: String,
    val requestedByMemberId: Long,
    val requestedByMemberName: String,
    val status: String,
    val requestedPointTypeId: Long?,
    val requestedPointTypeCode: String?,
    val requestedPointAmount: Int?,
    val requestedReputationAmount: Int?,
    val requestedDurationMinutes: Int?,
    val requestedRewardDefinitionId: Long?,
    val requestedRewardTitle: String?,
    val requestedCustomRewardTitle: String?,
    val requestedComment: String?,
    val approvedPointTypeId: Long?,
    val approvedPointTypeCode: String?,
    val approvedPointAmount: Int?,
    val approvedReputationAmount: Int?,
    val approvedDurationMinutes: Int?,
    val approvedRewardDefinitionId: Long?,
    val approvedRewardTitle: String?,
    val approvedCustomRewardTitle: String?,
    val reviewerComment: String?,
    val createdAt: String?,
    val reviewedAt: String?,
    val fulfilledAt: String?
)

data class RewardNegotiationData(
    val taskInstance: TaskInstanceResponse,
    val pointTypes: List<PointTypeResponse>,
    val rewards: List<RewardDefinitionSummary>,
    val requests: List<TaskRewardRequestResponse>
)
