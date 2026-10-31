package com.olehkobylianskyi.familypoints.android.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/** Dates/times kept as ISO strings to avoid Gson java.time module reflection. */
data class CalendarEventDto(
    val kind: String,
    val key: String,
    val date: String,
    val title: String,
    val status: String?,
    val memberId: Long?,
    val taskDefinitionId: Long?,
    val instanceId: Long?,
    val rewardId: Long?,
    val scheduledTime: String?,
    val dueAt: String?,
    val startsAt: String?,
    val endsAt: String?,
    val scheduleOpenNow: Boolean
)

interface CalendarApi {
    @GET("workspaces/{workspaceId}/calendar")
    suspend fun events(
        @Header("Authorization") bearer: String,
        @Path("workspaceId") workspaceId: Long,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("memberId") memberId: Long? = null,
        @Query("type") type: String = "ALL"
    ): List<CalendarEventDto>
}
