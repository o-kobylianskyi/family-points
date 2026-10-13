package com.olehkobylianskyi.familypoints.android

import com.google.gson.Gson
import com.olehkobylianskyi.familypoints.android.data.TaskScheduleOccurrenceResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskScheduleOccurrenceJsonTest {
    @Test fun mapsBackendScheduledOccurrenceFields() {
        val json = """{
          "occurrenceKey":"39:2026-10-13:time_2",
          "taskDefinitionId":39,
          "instanceId":null,
          "memberId":7,
          "scheduledDate":"2026-10-13",
          "scheduledTime":"16:00:00",
          "title":"Clean room",
          "status":"PLANNED",
          "rewardAmount":1,
          "rewardReputationAmount":1
        }"""
        val item = Gson().fromJson(json, TaskScheduleOccurrenceResponse::class.java)
        assertEquals(39L, item.definitionId)
        assertEquals("2026-10-13", item.date)
        assertEquals("16:00:00", item.time)
        assertEquals("time_2", item.slotKey)
        assertEquals(null, item.instanceId)
    }
}
