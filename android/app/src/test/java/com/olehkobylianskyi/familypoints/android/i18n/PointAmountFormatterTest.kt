package com.olehkobylianskyi.familypoints.android.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PointAmountFormatterTest {
    @Test fun ukrainianCasesForEveryBuiltInCurrency() {
        val expected = mapOf(
            "POINTS" to listOf("бал", "бали", "балів"),
            "COPPER" to listOf("мідна монета", "мідні монети", "мідних монет"),
            "SILVER" to listOf("срібна монета", "срібні монети", "срібних монет"),
            "GOLD" to listOf("золота монета", "золоті монети", "золотих монет")
        )
        val values = listOf(1L, 2L, 5L, 11L, 21L, 22L, 25L)
        val expectedIndices = listOf(0, 1, 2, 2, 0, 1, 2)
        expected.forEach { (code, names) ->
            values.forEachIndexed { index, value ->
                assertEquals("$value ${names[expectedIndices[index]]}",
                    formatPointAmount(value, AppLanguage.UK, code, code))
            }
        }
    }

    @Test fun allSupportedLanguagesHaveBuiltInForms() {
        for (code in listOf("POINTS", "COPPER", "SILVER", "GOLD")) {
            for (lang in listOf("uk", "ru", "en", "de")) {
                val forms = builtInCurrencyForms(code, lang)!!
                assertEquals(lang, forms.language)
                assert(forms.one.isNotBlank() && forms.few.isNotBlank() && forms.many.isNotBlank())
            }
        }
        assertNull(builtInCurrencyForms("CUSTOM", "uk"))
    }

    @Test fun explicitFormsOverrideDefaultsButMissingFieldsFallBackSafely() {
        assertEquals("2 мої жетони", formatPointAmount(2, AppLanguage.UK, "COPPER", "Fallback",
            listOf(PointNameForms("uk", "мій жетон", "мої жетони", "моїх жетонів"))))
        assertEquals("5 мідних монет", formatPointAmount(5, AppLanguage.UK, "COPPER", "Fallback",
            listOf(PointNameForms("uk", "", "", ""))))
        assertEquals("1 спеціальний", formatPointAmount(1, AppLanguage.UK, "CUSTOM", "Fallback",
            listOf(PointNameForms("uk", "спеціальний", "спеціальні", "спеціальних"))))
    }

    @Test fun englishAndGermanPluralForms() {
        assertEquals("1 gold coin", formatPointAmount(1, AppLanguage.EN, "GOLD", "Gold"))
        assertEquals("2 gold coins", formatPointAmount(2, AppLanguage.EN, "GOLD", "Gold"))
        assertEquals("1 Silbermünze", formatPointAmount(1, AppLanguage.DE, "SILVER", "Silver"))
        assertEquals("2 Silbermünzen", formatPointAmount(2, AppLanguage.DE, "SILVER", "Silver"))
    }
}
