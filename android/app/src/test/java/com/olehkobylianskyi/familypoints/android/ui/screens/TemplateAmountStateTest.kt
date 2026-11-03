package com.olehkobylianskyi.familypoints.android.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateAmountStateTest {
    @Test fun missingAmountIsNotAFreeReward() {
        assertEquals(TemplateAmountState.UNSET, templateAmountState(""))
        assertEquals(TemplateAmountState.UNSET, templateAmountState("   "))
    }

    @Test fun explicitZeroIsDistinctFromMissing() {
        assertEquals(TemplateAmountState.EXPLICIT_ZERO, templateAmountState("0"))
        assertEquals(TemplateAmountState.SET, templateAmountState("25"))
        assertEquals(TemplateAmountState.SET, templateAmountState("30"))
    }
}
