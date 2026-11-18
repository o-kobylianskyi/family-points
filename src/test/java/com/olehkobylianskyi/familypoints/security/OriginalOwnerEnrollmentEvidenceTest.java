package com.olehkobylianskyi.familypoints.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static com.olehkobylianskyi.familypoints.security.OriginalOwnerEnrollmentEvidence.*;
import static org.junit.jupiter.api.Assertions.*;

class OriginalOwnerEnrollmentEvidenceTest {
    private static final Instant NOW = Instant.parse("2026-11-18T10:15:00Z");
    private Candidate registration() {
        return new Candidate(7,23,17,Method.AUTHENTICATED_REGISTRATION,
                EvidenceKind.REGISTERED_CREATOR_EVENT,UUID.randomUUID(),
                "trusted-registration-service",NOW.minusSeconds(60));
    }
    private Candidate legacy() {
        return new Candidate(7,23,17,Method.LEGACY_INDEPENDENT_VERIFICATION,
                EvidenceKind.VERIFIED_ACCOUNT_RECOVERY_CASE,UUID.randomUUID(),
                "independent-recovery-reviewer",NOW.minusSeconds(300));
    }
    private CurrentMembership member() {
        return new CurrentMembership(7,23,17,true,true,false,true);
    }
    private boolean accepted(Candidate candidate,CurrentMembership membership) {
        return permitsEnrollment(candidate,membership,true,false,NOW);
    }

    @Test void authenticCreatorEventAndIndependentLegacyReviewAreOnlyCandidates() {
        assertTrue(accepted(registration(),member()));
        assertTrue(accepted(legacy(),member()));
        assertThrows(IllegalArgumentException.class,()->new Candidate(7,23,17,
                Method.AUTHENTICATED_REGISTRATION,
                EvidenceKind.VERIFIED_ACCOUNT_RECOVERY_CASE,UUID.randomUUID(),"review",NOW));
        assertThrows(IllegalArgumentException.class,()->new Candidate(7,23,17,
                Method.LEGACY_INDEPENDENT_VERIFICATION,
                EvidenceKind.REGISTERED_CREATOR_EVENT,UUID.randomUUID(),"review",NOW));
    }
    @Test void unverifiedOrAlreadyBoundFamilyCannotCreateOwner() {
        assertFalse(permitsEnrollment(registration(),member(),false,false,NOW));
        assertFalse(permitsEnrollment(registration(),member(),true,true,NOW));
        assertFalse(permitsEnrollment(null,member(),true,false,NOW));
        assertFalse(permitsEnrollment(registration(),null,true,false,NOW));
        assertFalse(permitsEnrollment(registration(),member(),true,false,null));
    }
    @Test void crossFamilyAccountAndMemberSwapsAlwaysDeny() {
        assertFalse(accepted(registration(),new CurrentMembership(
                8,23,17,true,true,false,true)));
        assertFalse(accepted(registration(),new CurrentMembership(
                7,24,17,true,true,false,true)));
        assertFalse(accepted(registration(),new CurrentMembership(
                7,23,18,true,true,false,true)));
    }
    @Test void disabledOrDeletedOrUnlinkedMemberCannotBeEnrolled() {
        assertFalse(accepted(registration(),new CurrentMembership(
                7,23,17,false,true,false,true)));
        assertFalse(accepted(registration(),new CurrentMembership(
                7,23,17,true,false,false,true)));
        assertFalse(accepted(registration(),new CurrentMembership(
                7,23,17,true,true,true,true)));
        assertFalse(accepted(registration(),new CurrentMembership(
                7,23,17,true,true,false,false)));
    }
    @Test void staleRegistrationCannotMasqueradeAsFreshProof() {
        var x=registration();
        var old=new Candidate(7,23,17,x.method(),x.evidenceKind(),x.evidenceId(),
                x.reviewerPrincipal(),NOW.minusSeconds(301));
        assertFalse(accepted(old,member()));
        var future=new Candidate(7,23,17,x.method(),x.evidenceKind(),x.evidenceId(),
                x.reviewerPrincipal(),NOW.plusSeconds(1));
        assertFalse(accepted(future,member()));
    }
    @Test void staleLegacyRecoveryRequiresNewIndependentEvidence() {
        var x=legacy();
        var old=new Candidate(7,23,17,x.method(),x.evidenceKind(),x.evidenceId(),
                x.reviewerPrincipal(),NOW.minusSeconds(901));
        assertFalse(accepted(old,member()));
        assertTrue(accepted(x,member()));
    }
    @Test void malformedProvenanceAndReviewerAreRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class,()->new Candidate(0,23,17,
                Method.AUTHENTICATED_REGISTRATION,EvidenceKind.REGISTERED_CREATOR_EVENT,
                UUID.randomUUID(),"review",NOW));
        assertThrows(IllegalArgumentException.class,()->new Candidate(7,23,17,
                Method.AUTHENTICATED_REGISTRATION,EvidenceKind.REGISTERED_CREATOR_EVENT,
                UUID.randomUUID()," ",NOW));
        assertThrows(IllegalArgumentException.class,()->new Candidate(7,23,17,
                Method.AUTHENTICATED_REGISTRATION,EvidenceKind.REGISTERED_CREATOR_EVENT,
                UUID.randomUUID(),"x".repeat(257),NOW));
    }
    @Test void candidateDefaultLoggingDoesNotLeakIdentityOrEvidenceReference() {
        Candidate c=legacy();
        assertFalse(c.toString().contains(c.evidenceId().toString()));
        assertFalse(c.toString().contains(c.reviewerPrincipal()));
        assertFalse(c.toString().contains("23"));
        assertTrue(c.toString().contains("redacted"));
    }
}
