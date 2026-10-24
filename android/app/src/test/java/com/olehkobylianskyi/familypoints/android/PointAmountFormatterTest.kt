package com.olehkobylianskyi.familypoints.android

import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.PointNameForms
import com.olehkobylianskyi.familypoints.android.i18n.formatPointAmount
import org.junit.Assert.assertEquals
import org.junit.Test

class PointAmountFormatterTest {
    @Test fun ukrainianBuiltInPluralRules() {
        mapOf(
            0L to "0 балів", 1L to "1 бал", 2L to "2 бали",
            4L to "4 бали", 5L to "5 балів", 10L to "10 балів",
            11L to "11 балів", 14L to "14 балів",
            21L to "21 бал", 24L to "24 бали", 25L to "25 балів",
            101L to "101 бал", 112L to "112 балів"
        ).forEach { (amount, expected) ->
            assertEquals(expected, formatPointAmount(amount, AppLanguage.UK, "POINTS", "бали"))
        }
    }

    @Test fun customFormsOverrideBuiltInLabels() {
        val forms = listOf(PointNameForms("uk", "жетон", "жетони", "жетонів"))
        assertEquals("1 жетон", formatPointAmount(1, AppLanguage.UK, "CUSTOM", "жетони", forms))
        assertEquals("3 жетони", formatPointAmount(3, AppLanguage.UK, "CUSTOM", "жетони", forms))
        assertEquals("10 жетонів", formatPointAmount(10, AppLanguage.UK, "CUSTOM", "жетони", forms))
        assertEquals("-22 жетони", formatPointAmount(-22, AppLanguage.UK, "CUSTOM", "жетони", forms))
    }

    @Test fun englishAndGermanHaveTwoNumberForms() {
        assertEquals("1 point", formatPointAmount(1, AppLanguage.EN, "POINTS", "points"))
        assertEquals("2 points", formatPointAmount(2, AppLanguage.EN, "POINTS", "points"))
        assertEquals("1 Punkt", formatPointAmount(1, AppLanguage.DE, "POINTS", "Punkte"))
        assertEquals("10 Punkte", formatPointAmount(10, AppLanguage.DE, "POINTS", "Punkte"))
    }
}
