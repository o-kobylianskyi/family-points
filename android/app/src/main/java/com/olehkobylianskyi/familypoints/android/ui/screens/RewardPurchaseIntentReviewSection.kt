package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.rewardPurchaseRecoveryStrings
import com.olehkobylianskyi.familypoints.android.storage.RewardPurchaseIntentStore

/**
 * Read-only synthetic UI building block for an eventual family-scoped review page.
 * Deliberately NOT connected to RewardsScreen or any network method. No actions.
 * The parent must supply only verified-account-scoped local journal rows.
 */
@Composable
internal fun RewardPurchaseIntentReviewSection(
    language: AppLanguage,
    accountId: Long,
    workspaceId: Long,
    records: List<RewardPurchaseIntentStore.Intent>
) {
    val labels = rewardPurchaseRecoveryStrings(language)
    val entries = RewardPurchaseIntentReviewModel.scoped(records, accountId, workspaceId)
    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(labels.needsReview, style = MaterialTheme.typography.titleMedium)
        Text(labels.pendingExplanation, style = MaterialTheme.typography.bodySmall)
        if (entries.isEmpty()) {
            Text(
                when (language) {
                    AppLanguage.UK -> "Немає локальних записів для цього акаунта."
                    AppLanguage.RU -> "Нет локальных записей для этого аккаунта."
                    AppLanguage.DE -> "Keine lokalen Einträge für dieses Konto."
                    AppLanguage.EN -> "No local records for this account."
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
        for (entry in entries) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val stateLabel = when (entry.state) {
                    "CONFIRMED" -> labels.confirmed
                    "HELD" -> labels.needsReview
                    "UNCERTAIN" -> labels.pendingExplanation
                    else -> labels.checkReceipt
                }
                Text(stateLabel, style = MaterialTheme.typography.titleSmall)
                Text(
                    when (language) {
                        AppLanguage.UK -> "Нагорода #${entry.rewardId} · Учасник #${entry.memberId}"
                        AppLanguage.RU -> "Награда #${entry.rewardId} · Участник #${entry.memberId}"
                        AppLanguage.DE -> "Belohnung #${entry.rewardId} · Mitglied #${entry.memberId}"
                        AppLanguage.EN -> "Reward #${entry.rewardId} · Member #${entry.memberId}"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    entry.operationId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(labels.noAutomaticRetry, style = MaterialTheme.typography.bodySmall)
    }
}
