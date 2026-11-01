package com.olehkobylianskyi.familypoints.android.data

import org.junit.Assert.*
import org.junit.Test

class ReceiptRecoveryGuardTest {
    private fun allowed(
        account: Long = 11,
        workspace: Long = 22,
        epoch: Long = 7,
        token: String? = "session-a",
        checkedAccount: Long = 11,
        checkedWorkspace: Long = 22,
        checkedEpoch: Long = 7,
        currentToken: String? = "session-a"
    ) = ReceiptRecoveryGuard.sameIdentityAndEpoch(
        account, checkedAccount, workspace, checkedWorkspace,
        epoch, checkedEpoch, token, currentToken
    )

    @Test fun sameAccountWorkspaceEpochAndSessionIsAllowed() {
        assertTrue(allowed())
    }

    @Test fun switchingAccountOrWorkspaceRejectsStaleReceipt() {
        assertFalse(allowed(checkedAccount = 12))
        assertFalse(allowed(checkedWorkspace = 23))
    }

    @Test fun signedInSessionChangeOrLogoutRejectsStaleReceipt() {
        assertFalse(allowed(currentToken = "session-b"))
        assertFalse(allowed(currentToken = null))
        assertFalse(allowed(token = ""))
    }

    @Test fun epochMismatchOrInvalidIdentityRejectsStaleReceipt() {
        assertFalse(allowed(checkedEpoch = 8))
        assertFalse(allowed(epoch = -1))
        assertFalse(allowed(account = 0, checkedAccount = 0))
        assertFalse(allowed(workspace = 0, checkedWorkspace = 0))
    }
}
