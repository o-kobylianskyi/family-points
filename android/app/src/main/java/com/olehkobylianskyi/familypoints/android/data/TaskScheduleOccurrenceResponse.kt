package com.olehkobylianskyi.familypoints.android.data

/** Read-only recurring projection; instanceId is null until an action materializes it. */
data class TaskScheduleOccurrenceResponse(
    val occurrenceKey: String,
    val definitionId: Long,
    val instanceId: Long?,
    val memberId: Long,
    val date: String,
    val time: String?,
    val title: String,
    val status: String,
    val rewardAmount: Int?,
    val rewardReputationAmount: Int?
) {
    val slotKey: String get() = occurrenceKey.substringAfterLast(':', "default")
}
