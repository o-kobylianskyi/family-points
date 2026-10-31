package com.olehkobylianskyi.familypoints.android.data

data class RewardCategorySummary(
    val id: Long,
    val name: String,
    val active: Boolean,
    val sortOrder: Int
)

data class RewardRequirementSummary(
    val id: Long?,
    val phase: String,
    val requirementType: String,
    val taskDefinitionId: Long?,
    val taskTitle: String?,
    val timeScope: String?,
    val windowValue: Int?,
    val description: String?,
    val required: Boolean,
    val blockingMode: String,
    val blockedCategoryIds: List<Long>,
    val blockedRewardDefinitionIds: List<Long>
)

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
    val categories: List<RewardCategorySummary>,
    val requirements: List<RewardRequirementSummary>,
    val active: Boolean
)

data class RewardRequestSummary(
    val id: Long,
    val requestedByMemberId: Long,
    val requestedByMemberName: String,
    val rewardDefinitionId: Long?,
    val title: String,
    val description: String?,
    val status: String,
    val pointTypeId: Long?,
    val pointTypeCode: String?,
    val priceAmount: Int?,
    val minimumReputation: Int?,
    val durationMinutes: Int?,
    val createdAt: String?,
    val reviewedAt: String?,
    val requirements: List<RewardRequirementSummary>
)

data class RewardPurchaseSummary(
    val id: Long,
    val rewardDefinitionId: Long?,
    val memberId: Long,
    val memberName: String,
    val rewardTitle: String,
    val pointTypeId: Long,
    val pointTypeCode: String,
    val priceAmount: Int,
    val durationMinutes: Int?,
    val status: String,
    val purchasedAt: String?,
    val resolvedAt: String?
)

data class RewardObligationSummary(
    val id: Long,
    val memberId: Long,
    val memberName: String,
    val rewardPurchaseId: Long,
    val rewardTitle: String,
    val title: String,
    val status: String,
    val blockingMode: String,
    val dueAt: String?,
    val createdAt: String?,
    val blockedCategoryIds: List<Long>,
    val blockedRewardDefinitionIds: List<Long>
)

data class RewardRequestCreateRequest(
    val rewardDefinitionId: Long?,
    val title: String?,
    val description: String?
)

data class RewardCategoryCreateRequest(
    val name: String,
    val sortOrder: Int
)

data class RewardRequirementRequest(
    val phase: String,
    val requirementType: String,
    val taskDefinitionId: Long?,
    val timeScope: String?,
    val windowValue: Int?,
    val description: String?,
    val required: Boolean,
    val blockingMode: String,
    val blockedCategoryIds: List<Long>,
    val blockedRewardDefinitionIds: List<Long>
)

data class RewardDefinitionCreateRequest(
    val title: String,
    val description: String?,
    val pointTypeId: Long,
    val priceAmount: Int,
    val minimumReputation: Int?,
    val requiresApproval: Boolean,
    val rewardKind: String,
    val defaultDurationMinutes: Int?,
    val acquisitionMode: String,
    val categoryIds: List<Long>,
    val requirements: List<RewardRequirementRequest>
)

data class RewardRequestReviewRequest(
    val pointTypeId: Long,
    val priceAmount: Int,
    val minimumReputation: Int?,
    val durationMinutes: Int?,
    val requirements: List<RewardRequirementRequest>
)

data class RewardsPageData(
    val rewards: List<RewardDefinitionSummary>,
    val categories: List<RewardCategorySummary>,
    val requests: List<RewardRequestSummary>,
    val purchases: List<RewardPurchaseSummary>,
    val obligations: List<RewardObligationSummary>,
    val pointTypes: List<PointTypeResponse>,
    val tasks: List<TaskDefinitionResponse>
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
