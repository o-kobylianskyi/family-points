package com.olehkobylianskyi.familypoints.android.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptReadFailurePolicyTest {
    @Test fun onlyFoundReceiptMayProceedToIdentityAndEpochVerification() {
        assertEquals(ReceiptRecoveryGuard.Decision.VERIFY_IDENTITY_AND_EPOCH,
            ReceiptRecoveryGuard.decision(ReceiptRecoveryGuard.ReceiptRead.FOUND))
    }

    @Test fun missingReceipt404MustKeepOriginalUuidUncertain() {
        assertEquals(ReceiptRecoveryGuard.Decision.KEEP_UNCERTAIN,
            ReceiptRecoveryGuard.decision(ReceiptRecoveryGuard.ReceiptRead.NOT_FOUND))
    }

    @Test fun timeoutAndServerFailureDoNotAuthorizeRepostOrConfirmation() {
        listOf(ReceiptRecoveryGuard.ReceiptRead.TIMEOUT,
            ReceiptRecoveryGuard.ReceiptRead.SERVER_ERROR).forEach {
            assertEquals(ReceiptRecoveryGuard.Decision.KEEP_UNCERTAIN,
                ReceiptRecoveryGuard.decision(it))
        }
    }
}
