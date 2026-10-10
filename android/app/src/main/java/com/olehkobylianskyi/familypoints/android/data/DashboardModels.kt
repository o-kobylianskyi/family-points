package com.olehkobylianskyi.familypoints.android.data

data class PointBalanceResponse(
    val memberId: Long,
    val balance: Long
)

data class WorkspaceMemberResponse(
    val id: Long,
    val name: String,
    val memberType: String,
    val workspaceRoleId: Long,
    val workspaceRoleCode: String,
    val workspaceRoleName: String,
    val workspaceId: Long
)

data class TaskInstanceResponse(
    val id: Long,
    val taskDefinitionId: Long,
    val memberId: Long,
    val originalAssignedMemberId: Long?,
    val claimedAt: String?,
    val delegationAllowed: Boolean,
    val title: String,
    val description: String?,
    val mandatory: Boolean,
    val dueTime: String?,
    val rewardPointTypeId: Long?,
    val rewardPointTypeCode: String?,
    val rewardAmount: Int?,
    val penaltyPointTypeId: Long?,
    val penaltyPointTypeCode: String?,
    val penaltyAmount: Int?,
    val rewardReputationAmount: Int?,
    val penaltyReputationAmount: Int?,
    val scheduledDate: String,
    val status: String,
    val startedAt: String?,
    val completedAt: String?
)

data class TaskDefinitionResponse(
    val id: Long,
    val assignedMemberId: Long?,
    val targetGroupId: Long?,
    val preferredMemberId: Long?,
    val responsibleMemberId: Long?,
    val createdByMemberId: Long?,
    val createdByMemberName: String?,
    val parentTaskDefinitionId: Long?,
    val title: String,
    val description: String?,
    val mandatory: Boolean,
    val active: Boolean,
    val delegationAllowed: Boolean,
    val recurrenceType: String?,
    val assignmentPolicy: String?,
    val roleMatchMode: String?,
    val startDate: String?,
    val endDate: String?,
    val recurrenceDayOfWeek: Int?,
    val recurrenceDayOfMonth: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val dueTime: String?,
    val rewardPointTypeId: Long?,
    val rewardPointTypeCode: String?,
    val rewardAmount: Int?,
    val penaltyPointTypeId: Long?,
    val penaltyPointTypeCode: String?,
    val penaltyAmount: Int?,
    val rewardReputationAmount: Int?,
    val penaltyReputationAmount: Int?
)

data class TaskParticipantResponse(
    val id: Long,
    val role: String,
    val actorType: String,
    val actorId: Long,
    val actorName: String
)

data class TasksPageData(
    val members: List<WorkspaceMemberResponse>,
    val tasks: List<TaskInstanceResponse>,
    val openTasks: List<TaskDefinitionResponse>,
    val definitions: List<TaskDefinitionResponse>
)

data class DashboardData(
    val balance: Long,
    val tasks: List<TaskInstanceResponse>,
    val openTasks: List<TaskDefinitionResponse>
) {
    val activeTasks: List<TaskInstanceResponse>
        get() = tasks.filterNot {
            it.status == "COMPLETED" || it.status == "EXCUSED"
        }
}


data class TaskAuditEventResponse(
    val id: Long,
    val taskDefinitionId: Long,
    val taskInstanceId: Long?,
    val eventType: String,
    val performedByMemberId: Long?,
    val performedByName: String?,
    val occurredAt: String,
    val fromActorType: String?,
    val fromActorId: Long?,
    val fromActorName: String?,
    val toActorType: String?,
    val toActorId: Long?,
    val toActorName: String?,
    val details: String?
)

data class TaskDetailsData(
    val task: TaskDefinitionResponse,
    val visibleDefinitions: List<TaskDefinitionResponse>,
    val instance: TaskInstanceResponse?,
    val members: List<WorkspaceMemberResponse>,
    val participants: List<TaskParticipantResponse>,
    val history: List<TaskAuditEventResponse>,
    val subtasks: List<TaskDefinitionResponse>
)

data class TaskDelegationRequest(
    val toMemberId: Long,
    val reason: String? = null
)
