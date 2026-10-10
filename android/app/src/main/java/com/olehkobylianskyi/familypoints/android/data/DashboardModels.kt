package com.olehkobylianskyi.familypoints.android.data

data class PointBalanceResponse(
    val memberId: Long,
    val balance: Long
)

data class TaskInstanceResponse(
    val id: Long,
    val taskDefinitionId: Long,
    val memberId: Long,
    val title: String,
    val description: String?,
    val mandatory: Boolean,
    val dueTime: String?,
    val scheduledDate: String,
    val status: String
)

data class TaskDefinitionResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val mandatory: Boolean,
    val active: Boolean
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
