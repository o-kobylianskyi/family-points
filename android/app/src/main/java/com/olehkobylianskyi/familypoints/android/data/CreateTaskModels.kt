package com.olehkobylianskyi.familypoints.android.data

data class PointTypeResponse(
    val id: Long,
    val code: String,
    val name: String,
    val active: Boolean,
    val sortOrder: Int
)

data class MemberGroupSummary(
    val id: Long,
    val name: String,
    val description: String?,
    val active: Boolean,
    val showInNavigation: Boolean
)

data class TaskActorRef(
    val actorType: String,
    val actorId: Long
)

data class TaskDefinitionCreateRequest(
    val assignmentPolicy: String,
    val assignedMemberId: Long?,
    val targetGroupId: Long?,
    val preferredMemberId: Long?,
    val responsibleMemberId: Long?,
    val parentTaskDefinitionId: Long?,
    val delegationAllowed: Boolean,
    val roleMatchMode: String,
    val requiredGroupRoleIds: List<Long>,
    val administrators: List<TaskActorRef>,
    val observers: List<TaskActorRef>,
    val executors: List<TaskActorRef>,
    val title: String,
    val description: String?,
    val mandatory: Boolean,
    val recurrenceType: String,
    val startDate: String,
    val endDate: String?,
    val recurrenceDayOfWeek: Int?,
    val recurrenceDayOfMonth: Int?,
    val rewardPointTypeId: Long?,
    val rewardAmount: Int?,
    val penaltyPointTypeId: Long?,
    val penaltyAmount: Int?,
    val rewardReputationAmount: Int?,
    val penaltyReputationAmount: Int?,
    val dueTime: String?
)

data class CreateTaskReferenceData(
    val members: List<WorkspaceMemberResponse>,
    val definitions: List<TaskDefinitionResponse>,
    val pointTypes: List<PointTypeResponse>,
    val groups: List<MemberGroupSummary>
)
