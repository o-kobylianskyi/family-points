package com.olehkobylianskyi.familypoints.android.ui.screens

import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure catalog mapping acceptance, without device, media auth or bitmap rendering. */
class TemplateCategoryIconTest {
    private fun definition(category: String?): JsonObject =
        JsonObject().apply {
            if (category != null) addProperty("category", category)
        }

    @Test
    fun approvedTemplateCategoriesUseRegisteredMediaKeys() {
        val expected = mapOf(
            "hygiene" to "category-self-care",
            "routine" to "category-planning",
            "chores" to "category-chores",
            "school" to "category-learning",
            "reading" to "category-learning",
            "activity" to "category-activity",
            "creativity" to "category-creativity",
            "social" to "category-family-communication",
            "habits" to "category-habits",
            "family" to "category-home",
            "adult" to "category-adult-tasks",
            "time" to "category-screen-time",
            "privilege" to "category-privilege",
            "items" to "category-material-reward"
        )
        expected.forEach { (category, mediaKey) ->
            assertEquals(mediaKey, templateCategoryMediaKey("TASK", definition(category)))
        }
    }

    @Test
    fun missingOrUnknownCategoryDoesNotInventARegisteredCategory() {
        assertEquals("task-default", templateCategoryMediaKey("TASK", definition(null)))
        assertEquals("reward-star", templateCategoryMediaKey("REWARD", definition("unknown")))
        assertEquals("category-learning", templateCategoryMediaKey("TASK", definition("SCHOOL")))
    }
}
