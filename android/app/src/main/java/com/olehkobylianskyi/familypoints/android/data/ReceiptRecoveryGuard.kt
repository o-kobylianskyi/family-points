package com.olehkobylianskyi.familypoints.android.data

/** Pure policy: a receipt may be accepted only for the same authenticated session
 * and data generation in which the durable operation was originally recorded.
 * Timeouts and missing receipts never authorize an additional POST.
 */
object ReceiptRecoveryGuard {
    fun sameIdentityAndEpoch(
        expectedAccountId: Long,
        activeAccountId: Long,
        expectedWorkspaceId: Long,
        responseWorkspaceId: Long,
        originalEpoch: Long,
        verifiedEpoch: Long,
        initialToken: String?,
        currentToken: String?
    ): Boolean = expectedAccountId > 0 && expectedAccountId == activeAccountId &&
        expectedWorkspaceId > 0 && expectedWorkspaceId == responseWorkspaceId &&
        originalEpoch >= 0 && originalEpoch == verifiedEpoch &&
        !initialToken.isNullOrBlank() && initialToken == currentToken
}
