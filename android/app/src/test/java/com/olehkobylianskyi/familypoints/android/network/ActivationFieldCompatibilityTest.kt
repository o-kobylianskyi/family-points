package com.olehkobylianskyi.familypoints.android.network

import com.google.gson.JsonNull
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

/** Tests Gson copy semantics underlying the request-aware Compose V48 editor. */
class ActivationFieldCompatibilityTest {
    @Test fun unchangedNullPriceAndDurationSurviveUnrelatedEdit() {
        val original = JsonParser.parseString(
            """{"title":"Screen time","price":null,"defaultDurationMinutes":30,"unrecognized":{"keep":true}}"""
        ).asJsonObject
        val changed = original.deepCopy()
        changed.addProperty("title", "Family time")
        assertEquals(JsonNull.INSTANCE, changed.get("price"))
        assertEquals(30, changed.get("defaultDurationMinutes").asInt)
        assertEquals(true, changed.getAsJsonObject("unrecognized").get("keep").asBoolean)
        assertEquals(JsonNull.INSTANCE, original.get("price"))
    }

    @Test fun explicitlyEmptyNumericFieldCanRemainNullRatherThanFreeZero() {
        val original = JsonParser.parseString("""{"price":17,"defaultDurationMinutes":30}""").asJsonObject
        val updated = original.deepCopy()
        updated.add("price", JsonNull.INSTANCE)
        assertTrue(updated.get("price").isJsonNull)
        assertFalse(updated.get("price").isJsonPrimitive)
        assertEquals(30, updated.get("defaultDurationMinutes").asInt)
    }
}
