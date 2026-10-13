package com.olehkobylianskyi.familypoints.android.data

/** Read-only recurring projection; instanceId is null until an action materializes it. */
data class TaskScheduleOccurrenceResponse(
    val occurrenceKey: String,
    @com.google.gson.annotations.SerializedName("taskDefinitionId") val definitionId: Long,
    val instanceId: Long?,
    val memberId: Long,
    @com.google.gson.annotations.SerializedName("scheduledDate") val date: String,
    @com.google.gson.annotations.SerializedName("scheduledTime") val time: String?,
    val title: String,
    val status: String,
    val rewardAmount: Int?,
    val rewardReputationAmount: Int?
) {
    val slotKey: String get() = occurrenceKey.substringAfterLast(':', "default")
}
