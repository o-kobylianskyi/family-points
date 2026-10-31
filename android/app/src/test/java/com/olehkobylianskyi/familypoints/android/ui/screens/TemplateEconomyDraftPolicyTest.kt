package com.olehkobylianskyi.familypoints.android.ui.screens

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TemplateEconomyDraftPolicyTest {
    private fun draft(kind: String, amount: String, extra: String = "") =
        JsonParser.parseString(
            """{"templateId":12,"kind":"$kind","requiresConfirmation":true,
              "economySuggestion":{"reviewRequired":true,"pricingState":"OPTIONAL_SUGGESTION",
              "${if (kind == "TASK") "suggestedRewardPoints" else "suggestedPricePoints"}":$amount}
              ${if (extra.isBlank()) "" else ",$extra"}}"""
        ).asJsonObject

    @Test fun taskNumericSuggestionRequiresAdultReview() {
        assertEquals(5, applicableEconomyDraftAmount(draft("TASK", "5"), true))
        assertNull(applicableEconomyDraftAmount(draft("TASK", "5"), false))
        assertNull(applicableEconomyDraftAmount(draft("TASK", "null"), true))
        assertNull(applicableEconomyDraftAmount(draft("TASK", "\"5\""), true))
    }

    @Test fun rewardNullNeverMeansFree() {
        assertNull(applicableEconomyDraftAmount(draft("REWARD", "null"), true))
        assertEquals(0, applicableEconomyDraftAmount(draft("REWARD", "0"), true))
        assertEquals(25, applicableEconomyDraftAmount(draft("REWARD", "25"), true))
        assertNull(applicableEconomyDraftAmount(draft("REWARD", "-1"), true))
        assertNull(applicableEconomyDraftAmount(draft("REWARD", "999999999999999"), true))
    }

    @Test fun absenceOfReviewOrConfirmationAndAppliedDraftAreBlocked() {
        val noReview = draft("TASK", "5")
        noReview.getAsJsonObject("economySuggestion").remove("reviewRequired")
        assertNull(applicableEconomyDraftAmount(noReview, true))
        val noConfirmation = draft("TASK", "5")
        noConfirmation.remove("requiresConfirmation")
        assertNull(applicableEconomyDraftAmount(noConfirmation, true))
        assertNull(applicableEconomyDraftAmount(draft("TASK", "5", "\"suggestionAppliedToDraft\":true"), true))
    }

    @Test fun malformedTemplateIdentityAndPolicyFailClosed() {
        val missingId = draft("TASK", "5")
        missingId.remove("templateId")
        assertNull(applicableEconomyDraftAmount(missingId, true))
        val invalidId = draft("TASK", "5")
        invalidId.addProperty("templateId", -4)
        assertNull(applicableEconomyDraftAmount(invalidId, true))
        val unknownState = draft("REWARD", "25")
        unknownState.getAsJsonObject("economySuggestion")
            .addProperty("pricingState", "UNSET")
        assertNull(applicableEconomyDraftAmount(unknownState, true))
        val alreadyApplied = draft("REWARD", "25")
        alreadyApplied.addProperty("suggestionAppliedToDraft", true)
        assertNull(applicableEconomyDraftAmount(alreadyApplied, true))
    }

    @Test fun missingSuggestionAndBooleanReviewSemanticsFailClosed() {
        val missing = draft("TASK", "5")
        missing.remove("economySuggestion")
        assertNull(applicableEconomyDraftAmount(missing, true))
        val reviewAsString = draft("TASK", "5")
        reviewAsString.getAsJsonObject("economySuggestion")
            .addProperty("reviewRequired", "true")
        assertNull(applicableEconomyDraftAmount(reviewAsString, true))
        val confirmationAsString = draft("TASK", "5")
        confirmationAsString.addProperty("requiresConfirmation", "true")
        assertNull(applicableEconomyDraftAmount(confirmationAsString, true))
    }

    @Test fun unknownKindAndForeignAmountFieldsFailClosed() {
        assertNull(applicableEconomyDraftAmount(draft("UNKNOWN", "5"), true))
        val task = draft("TASK", "null")
        task.getAsJsonObject("economySuggestion").addProperty("suggestedPricePoints", 25)
        assertNull(applicableEconomyDraftAmount(task, true))
    }
}
