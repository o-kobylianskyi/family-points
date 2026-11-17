package com.olehkobylianskyi.familypoints;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * TEST ONLY: verifies an externally approved, snapshot-scoped member mapping
 * receipt. This does not attest that the operator/owner was authenticated and
 * is not a production restore authorization.
 *
 * HMAC binds a reviewed original -> target ID decision to the source and
 * target families, snapshot UUID and a per-decision unique nonce. No names,
 * emails or role-based identity guesses are used.
 */
final class DisposableMemberBindingReceiptVerifier {
    record Binding(long originalMember,long targetMember,UUID nonce,byte[] mac) {
        Binding { mac=mac==null?null:mac.clone(); }
        @Override public byte[] mac() { return mac==null?null:mac.clone(); }
    }
    record Result(int approvedMembers,UUID snapshotId) {}

    static Binding sealForSyntheticApproval(long sourceFamily,long targetFamily,UUID snapshot,
            long originalMember,long targetMember,UUID nonce,SecretKey reviewKey) {
        if(sourceFamily<=0 || targetFamily<=0 || snapshot==null || nonce==null
                || originalMember<=0 || targetMember<=0 || reviewKey==null)
            throw new IllegalArgumentException("MEMBER_APPROVAL_INVALID");
        return new Binding(originalMember,targetMember,nonce,
                mac(sourceFamily,targetFamily,snapshot,originalMember,targetMember,nonce,reviewKey));
    }

    Result verify(long sourceFamily,long targetFamily,UUID snapshot,
                  Map<Long,Long> proposed,Collection<Binding> approvals,SecretKey reviewKey) {
        if(sourceFamily<=0 || targetFamily<=0 || snapshot==null || reviewKey==null
                || proposed==null || proposed.isEmpty() || approvals==null)
            throw new IllegalArgumentException("MEMBER_RECEIPT_INVALID_INPUT");
        if(proposed.keySet().stream().anyMatch(id->id==null || id<=0)
                || proposed.values().stream().anyMatch(id->id==null || id<=0)
                || new HashSet<>(proposed.values()).size()!=proposed.size()
                || approvals.size()!=proposed.size())
            throw new SecurityException("MEMBER_RECEIPT_MAPPING_INCOMPLETE");
        var seenOriginal=new HashSet<Long>();
        var seenNonces=new HashSet<UUID>();
        for(Binding approval:approvals) {
            if(approval==null || approval.originalMember()<=0 || approval.targetMember()<=0
                    || approval.nonce()==null || approval.mac()==null
                    || approval.mac().length!=32 || !seenOriginal.add(approval.originalMember())
                    || !seenNonces.add(approval.nonce())
                    || !Objects.equals(proposed.get(approval.originalMember()),approval.targetMember()))
                throw new SecurityException("MEMBER_RECEIPT_BINDING_INVALID");
            var expected=mac(sourceFamily,targetFamily,snapshot,
                    approval.originalMember(),approval.targetMember(),approval.nonce(),reviewKey);
            if(!MessageDigest.isEqual(expected,approval.mac()))
                throw new SecurityException("MEMBER_RECEIPT_AUTHENTICATION_FAILED");
            Arrays.fill(expected,(byte)0);
        }
        if(!seenOriginal.equals(proposed.keySet()))
            throw new SecurityException("MEMBER_RECEIPT_SOURCE_COVERAGE");
        return new Result(seenOriginal.size(),snapshot);
    }

    private static byte[] mac(long sourceFamily,long targetFamily,UUID snapshot,
            long originalMember,long targetMember,UUID nonce,SecretKey key) {
        try {
            Mac hmac=Mac.getInstance("HmacSHA256");
            hmac.init(key);
            String canonical="FP-MEMBER-BINDING-REVIEW-V1|"+sourceFamily+"|"+
                    targetFamily+"|"+snapshot+"|"+originalMember+"|"+targetMember+"|"+nonce;
            return hmac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
        } catch(java.security.GeneralSecurityException failure) {
            throw new IllegalStateException("MEMBER_RECEIPT_CRYPTO_UNAVAILABLE",failure);
        }
    }
}
