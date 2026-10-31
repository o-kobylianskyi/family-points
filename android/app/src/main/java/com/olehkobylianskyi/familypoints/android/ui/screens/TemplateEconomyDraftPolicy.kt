package com.olehkobylianskyi.familypoints.android.ui.screens

import com.google.gson.JsonObject

/**
 * Presentation-only eligibility; the server validates every explicit apply call.
 * Missing or malformed fields cannot silently authorize pricing changes.
 */
internal fun applicableEconomyDraftAmount(draft: JsonObject?, canManage: Boolean): Int? {
    if (!canManage || draft == null) return null
    val kind = draft.get("kind")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
    val field = when (kind) {
        "TASK" -> "suggestedRewardPoints"
        "REWARD" -> "suggestedPricePoints"
        else -> return null
    }
    if (draft.get("requiresConfirmation")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean != true) return null
    if (draft.get("suggestionAppliedToDraft")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean == true) return null
    val suggestion = draft.get("economySuggestion")?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
    if (suggestion.get("reviewRequired")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean != true) return null
    if (suggestion.get("pricingState")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString != "OPTIONAL_SUGGESTION") return null
    val amount = suggestion.get(field)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.asString?.toIntOrNull() ?: return null
    val id = draft.get("templateId")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.asString?.toLongOrNull() ?: return null
    return amount.takeIf { amount >= 0 && id > 0L }
}
