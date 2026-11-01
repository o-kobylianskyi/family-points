package com.olehkobylianskyi.familypoints.android.data

/**
 * Pure coordinator completion decision, independent of AndroidKeyStore and SQLite.
 * An HTTP 200 is insufficient: final identity and authoritative family generation
 * must still match the operation that was durably prepared before POST.
 */
object ActivationCompletionGate {
    enum class Decision { CONFIRM, HOLD, UNCERTAIN }

    fun decide(
        preparedAccountId: Long,
        activeAccountId: Long,
        preparedWorkspaceId: Long,
        verifiedWorkspaceId: Long,
        preparedEpoch: Long,
        verifiedEpoch: Long,
        preparedToken: String?,
        activeToken: String?
    ): Decision {
        if (preparedAccountId <= 0 || activeAccountId != preparedAccountId ||
            preparedWorkspaceId <= 0 || verifiedWorkspaceId != preparedWorkspaceId ||
            preparedToken.isNullOrBlank() || activeToken != preparedToken ||
            preparedEpoch < 0 || verifiedEpoch < 0
        ) return Decision.UNCERTAIN
        return if (preparedEpoch == verifiedEpoch) Decision.CONFIRM else Decision.HOLD
    }
}
