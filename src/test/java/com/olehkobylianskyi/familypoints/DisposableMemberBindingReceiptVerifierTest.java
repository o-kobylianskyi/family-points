package com.olehkobylianskyi.familypoints;

import org.junit.jupiter.api.Test;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DisposableMemberBindingReceiptVerifierTest {
    private final DisposableMemberBindingReceiptVerifier verifier=
            new DisposableMemberBindingReceiptVerifier();

    private static SecretKey key() throws Exception {
        KeyGenerator generator=KeyGenerator.getInstance("HmacSHA256");
        generator.init(256);
        return generator.generateKey();
    }

    @Test void sameRoleSwapRequiresItsOwnDistinctAuthenticatedOwnerReviewedReceipt()
            throws Exception {
        var signingKey=key();
        UUID snapshot=UUID.randomUUID();
        long source=17,target=31;
        var reviewed=Map.of(4L,40L,5L,50L);
        var first=DisposableMemberBindingReceiptVerifier.sealForSyntheticApproval(
                source,target,snapshot,4,40,UUID.randomUUID(),signingKey);
        var second=DisposableMemberBindingReceiptVerifier.sealForSyntheticApproval(
                source,target,snapshot,5,50,UUID.randomUUID(),signingKey);
        var approvals=List.of(first,second);
        assertEquals(2,verifier.verify(source,target,snapshot,reviewed,approvals,signingKey)
                .approvedMembers());
        // Both source members can have the same type and role: the roster
        // preflight alone cannot detect this semantic switch.
        assertThrows(SecurityException.class,()->verifier.verify(source,target,snapshot,
                Map.of(4L,50L,5L,40L),approvals,signingKey));
        assertThrows(SecurityException.class,()->verifier.verify(source,target,snapshot,
                Map.of(4L,40L),approvals,signingKey));
        assertThrows(SecurityException.class,()->verifier.verify(source,target,snapshot,
                Map.of(4L,40L,5L,40L),approvals,signingKey));
        assertThrows(SecurityException.class,()->verifier.verify(source,target,snapshot,
                reviewed,List.of(first,first),signingKey));
    }

    @Test void immutableReceiptCannotReplayToDifferentSnapshotOrFamiliesOrKey()
            throws Exception {
        var signingKey=key();
        UUID snapshot=UUID.randomUUID();
        var first=DisposableMemberBindingReceiptVerifier.sealForSyntheticApproval(
                17,31,snapshot,4,40,UUID.randomUUID(),signingKey);
        var approved=Map.of(4L,40L);
        assertEquals(snapshot,verifier.verify(17,31,snapshot,approved,
                List.of(first),signingKey).snapshotId());
        assertThrows(SecurityException.class,()->verifier.verify(
                17,31,UUID.randomUUID(),approved,List.of(first),signingKey));
        assertThrows(SecurityException.class,()->verifier.verify(
                18,31,snapshot,approved,List.of(first),signingKey));
        assertThrows(SecurityException.class,()->verifier.verify(
                17,32,snapshot,approved,List.of(first),signingKey));
        assertThrows(SecurityException.class,()->verifier.verify(
                17,31,snapshot,approved,List.of(first),key()));
        var corrupted=first.mac();
        corrupted[0]^=1;
        var bad=new DisposableMemberBindingReceiptVerifier.Binding(
                first.originalMember(),first.targetMember(),first.nonce(),corrupted);
        assertThrows(SecurityException.class,()->verifier.verify(
                17,31,snapshot,approved,List.of(bad),signingKey));
        assertEquals(1,verifier.verify(17,31,snapshot,approved,
                List.of(first),signingKey).approvedMembers());
    }

    @Test void reviewerNonceReuseOrForgedFieldsAreRejected() throws Exception {
        var signingKey=key();
        UUID snapshot=UUID.randomUUID(),nonce=UUID.randomUUID();
        var first=DisposableMemberBindingReceiptVerifier.sealForSyntheticApproval(
                17,31,snapshot,4,40,nonce,signingKey);
        var second=DisposableMemberBindingReceiptVerifier.sealForSyntheticApproval(
                17,31,snapshot,5,50,nonce,signingKey);
        assertThrows(SecurityException.class,()->verifier.verify(17,31,snapshot,
                Map.of(4L,40L,5L,50L),List.of(first,second),signingKey));
        var altered=new DisposableMemberBindingReceiptVerifier.Binding(
                4,50,first.nonce(),first.mac());
        assertThrows(SecurityException.class,()->verifier.verify(17,31,snapshot,
                Map.of(4L,50L),List.of(altered),signingKey));
    }
}
