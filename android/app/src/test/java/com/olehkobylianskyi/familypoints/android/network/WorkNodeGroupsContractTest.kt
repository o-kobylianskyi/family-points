package com.olehkobylianskyi.familypoints.android.network

import com.google.gson.Gson
import com.olehkobylianskyi.familypoints.android.data.WorkNodeGroupsRepository
import org.junit.Assert.*
import org.junit.Test

class WorkNodeGroupsContractTest {
    private val gson = Gson()

    @Test fun groupReplyUsesSeparateTaskFolderIdentity() {
        val group = gson.fromJson(
            """{"id":42,"parentGroupId":7,"title":"Chores",
              "description":null,"sortOrder":0,"active":true}""",
            WorkNodeGroupDto::class.java)
        assertEquals(42L, group.id)
        assertEquals(7L, group.parentGroupId)
        assertTrue(group.active)
        assertNull(group.description)
    }

    @Test fun mutationKeepsNullParentAndDoesNotInferCatalogCategory() {
        val request = WorkNodeGroupMutation("Chores", null, null, 0)
        WorkNodeGroupsRepository.validate(request)
        assertEquals(
            """{"title":"Chores","parentGroupId":null,"sortOrder":0}""",
            gson.toJson(request))
    }

    @Test fun malformedFoldersAreRejectedLocallyBeforePost() {
        for (name in listOf("", " name ", "x".repeat(151))) {
            assertThrows(IllegalArgumentException::class.java) {
                WorkNodeGroupsRepository.validate(WorkNodeGroupMutation(name, null, null, 0))
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorkNodeGroupsRepository.validate(WorkNodeGroupMutation("Valid", null, -1, 0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorkNodeGroupsRepository.validate(WorkNodeGroupMutation("Valid", "x".repeat(2001), null, 0))
        }
    }
}
