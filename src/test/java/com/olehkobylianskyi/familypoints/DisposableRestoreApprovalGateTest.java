package com.olehkobylianskyi.familypoints;

import org.junit.jupiter.api.Test;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DisposableRestoreApprovalGateTest {
    private static SecretKey key() throws Exception {
        var generator=KeyGenerator.getInstance("HmacSHA256");
        generator.init(256);
        return generator.generateKey();
    }
    private static final Instant ISSUED=Instant.parse("2026-11-18T08:00:00Z");
    private static final Instant VALID=Instant.parse("2026-11-18T08:01:00Z");
    private static final Instant EXPIRED=Instant.parse("2026-11-18T08:05:00Z");

    @Test void exactScopeEpochActorMapAndOneUseMustAllMatch() throws Exception {
        var key=key();
        var snapshot=UUID.randomUUID();
        var operation=UUID.randomUUID();
        var bindings=Map.of(11L,41L,12L,42L);
        var permit=DisposableRestoreApprovalGate.syntheticIssue(
                17,31,snapshot,operation,7,3,ISSUED,EXPIRED,bindings,key);
        var gate=new DisposableRestoreApprovalGate();
        assertThrows(SecurityException.class,()->gate.consume(permit,17,31,snapshot,
                operation,8,3,VALID,bindings,key));
        assertThrows(SecurityException.class,()->gate.consume(permit,17,31,snapshot,
                operation,7,4,VALID,bindings,key));
        assertThrows(SecurityException.class,()->gate.consume(permit,17,32,snapshot,
                operation,7,3,VALID,bindings,key));
        assertThrows(SecurityException.class,()->gate.consume(permit,18,31,snapshot,
                operation,7,3,VALID,bindings,key));
        assertThrows(SecurityException.class,()->gate.consume(permit,17,31,UUID.randomUUID(),
                operation,7,3,VALID,bindings,key));
        assertThrows(SecurityException.class,()->gate.consume(permit,17,31,snapshot,
                UUID.randomUUID(),7,3,VALID,bindings,key));
        assertThrows(SecurityException.class,()->gate.consume(permit,17,31,snapshot,
                operation,7,3,ISSUED.minusSeconds(1),bindings,key));
        assertThrows(SecurityException.class,()->gate.consume(permit,17,31,snapshot,
                operation,7,3,EXPIRED,bindings,key));
        assertThrows(SecurityException.class,()->gate.consume(permit,17,31,snapshot,
                operation,7,3,VALID,Map.of(11L,42L,12L,41L),key));
        assertThrows(SecurityException.class,()->gate.consume(permit,17,31,snapshot,
                operation,7,3,VALID,Map.of(11L,41L),key));
        assertThrows(SecurityException.class,()->gate.consume(permit,17,31,snapshot,
                operation,7,3,VALID,bindings,key()));
        gate.consume(permit,17,31,snapshot,operation,7,3,VALID,bindings,key);
        assertThrows(SecurityException.class,()->gate.consume(permit,17,31,snapshot,
                operation,7,3,VALID,bindings,key));
    }

    @Test void signingIsStableAcrossMapOrderAndTamperingCannotConsumePermit()
            throws Exception {
        var key=key();
        var snapshot=UUID.randomUUID();
        var operation=UUID.randomUUID();
        var approval=DisposableRestoreApprovalGate.syntheticIssue(17,31,snapshot,operation,
                7,0,ISSUED,EXPIRED,new LinkedHashMap<>(Map.of(12L,42L,11L,41L)),key);
        var reversed=new LinkedHashMap<Long,Long>();
        reversed.put(12L,42L);reversed.put(11L,41L);
        var gate=new DisposableRestoreApprovalGate();
        byte[] tampered=approval.mac();tampered[0]^=1;
        var counterfeit=new DisposableRestoreApprovalGate.Permit(
                approval.sourceFamily(),approval.targetFamily(),approval.snapshot(),
                approval.operation(),approval.actorId(),approval.targetEpoch(),
                approval.issuedAt(),approval.expiresAt(),
                approval.memberBindingsSha256(),tampered);
        assertThrows(SecurityException.class,()->gate.consume(counterfeit,17,31,snapshot,
                operation,7,0,VALID,reversed,key));
        gate.consume(approval,17,31,snapshot,operation,7,0,VALID,reversed,key);
    }

    @Test void malformedMappingAndApprovalDurationAreDenied() throws Exception {
        var key=key();
        var snapshot=UUID.randomUUID();
        assertThrows(SecurityException.class,()->DisposableRestoreApprovalGate.syntheticIssue(
                17,31,snapshot,UUID.randomUUID(),7,0,ISSUED,EXPIRED,
                Map.of(11L,41L,12L,41L),key));
        assertThrows(IllegalArgumentException.class,()->DisposableRestoreApprovalGate.syntheticIssue(
                17,31,snapshot,UUID.randomUUID(),7,0,ISSUED,ISSUED,
                Map.of(11L,41L),key));
    }
}
