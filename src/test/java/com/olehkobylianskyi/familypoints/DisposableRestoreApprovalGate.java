package com.olehkobylianskyi.familypoints;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/**
 * TEST ONLY. Synthetic, in-memory, one-use restore-approval preflight.
 * No HTTP API, authenticated owner proof, persistent nonce store or write.
 * Re-verification must never silently authorize a real restore.
 */
final class DisposableRestoreApprovalGate {
    record Permit(long sourceFamily,long targetFamily,UUID snapshot,UUID operation,
                  long actorId,long targetEpoch,Instant issuedAt,Instant expiresAt,
                  byte[] memberBindingsSha256,byte[] mac) {
        Permit {
            memberBindingsSha256=memberBindingsSha256==null?null:memberBindingsSha256.clone();
            mac=mac==null?null:mac.clone();
        }
        @Override public byte[] memberBindingsSha256() {
            return memberBindingsSha256==null?null:memberBindingsSha256.clone();
        }
        @Override public byte[] mac() {return mac==null?null:mac.clone();}
    }
    private final Set<UUID> consumed=new HashSet<>();

    static Permit syntheticIssue(long sourceFamily,long targetFamily,UUID snapshot,
            UUID operation,long actorId,long epoch,Instant issued,Instant expires,
            Map<Long,Long> reviewedBindings,SecretKey key) {
        if(sourceFamily<=0 || targetFamily<=0 || snapshot==null || operation==null
                || actorId<=0 || epoch<0 || issued==null || expires==null
                || !expires.isAfter(issued) || reviewedBindings==null
                || reviewedBindings.isEmpty() || key==null)
            throw new IllegalArgumentException("RESTORE_APPROVAL_INVALID");
        byte[] digest=bindingsDigest(reviewedBindings);
        byte[] signature=sign(sourceFamily,targetFamily,snapshot,operation,actorId,epoch,
                issued,expires,digest,key);
        return new Permit(sourceFamily,targetFamily,snapshot,operation,actorId,epoch,
                issued,expires,digest,signature);
    }

    synchronized void consume(Permit permit,long sourceFamily,long targetFamily,
            UUID snapshot,UUID operation,long authenticatedActor,long actualEpoch,
            Instant now,Map<Long,Long> actualBindings,SecretKey key) {
        if(permit==null || snapshot==null || operation==null || now==null
                || actualBindings==null || key==null)
            throw new IllegalArgumentException("RESTORE_APPROVAL_INPUT_MISSING");
        if(permit.sourceFamily()!=sourceFamily || permit.targetFamily()!=targetFamily
                || !permit.snapshot().equals(snapshot) || !permit.operation().equals(operation)
                || permit.actorId()!=authenticatedActor || authenticatedActor<=0
                || permit.targetEpoch()!=actualEpoch || actualEpoch<0
                || now.isBefore(permit.issuedAt()) || !now.isBefore(permit.expiresAt())
                || consumed.contains(operation))
            throw new SecurityException("RESTORE_APPROVAL_CONTEXT_STALE");
        byte[] digest=bindingsDigest(actualBindings);
        try {
            if(!MessageDigest.isEqual(digest,permit.memberBindingsSha256()))
                throw new SecurityException("RESTORE_APPROVAL_MAPPING_CHANGED");
            byte[] expected=sign(permit.sourceFamily(),permit.targetFamily(),
                    permit.snapshot(),permit.operation(),permit.actorId(),permit.targetEpoch(),
                    permit.issuedAt(),permit.expiresAt(),digest,key);
            try {
                if(!MessageDigest.isEqual(expected,permit.mac()))
                    throw new SecurityException("RESTORE_APPROVAL_SIGNATURE_INVALID");
            }finally {Arrays.fill(expected,(byte)0);}
            consumed.add(operation);
        }finally {Arrays.fill(digest,(byte)0);}
    }

    private static byte[] bindingsDigest(Map<Long,Long> mappings) {
        if(mappings.isEmpty() || mappings.keySet().stream().anyMatch(k->k==null || k<=0)
                || mappings.values().stream().anyMatch(v->v==null || v<=0)
                || new HashSet<>(mappings.values()).size()!=mappings.size())
            throw new SecurityException("RESTORE_APPROVAL_MAPPING_INVALID");
        var canonical=new StringBuilder("FP-RESTORE-BINDINGS-V1");
        mappings.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->
                canonical.append('|').append(e.getKey()).append(':').append(e.getValue()));
        try {
            return MessageDigest.getInstance("SHA-256").digest(
                    canonical.toString().getBytes(StandardCharsets.UTF_8));
        }catch(java.security.GeneralSecurityException e){throw new IllegalStateException(e);}
    }

    private static byte[] sign(long sourceFamily,long targetFamily,UUID snapshot,
            UUID operation,long actor,long epoch,Instant issued,Instant expires,
            byte[] digest,SecretKey key) {
        try {
            Mac m=Mac.getInstance("HmacSHA256");m.init(key);
            String payload="FP-RESTORE-APPROVAL-V1|"+sourceFamily+'|'+targetFamily+'|'+snapshot
                    +'|'+operation+'|'+actor+'|'+epoch+'|'+issued+'|'+expires+'|'
                    +HexFormat.of().formatHex(digest);
            return m.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        }catch(java.security.GeneralSecurityException e){throw new IllegalStateException(e);}
    }
}
