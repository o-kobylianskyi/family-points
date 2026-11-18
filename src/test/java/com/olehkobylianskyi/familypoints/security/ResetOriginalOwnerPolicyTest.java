package com.olehkobylianskyi.familypoints.security;

import org.junit.jupiter.api.Test;

import static com.olehkobylianskyi.familypoints.security.ResetOriginalOwnerPolicy.*;
import static org.junit.jupiter.api.Assertions.*;

class ResetOriginalOwnerPolicyTest {
    private static final Long FAMILY_A = 7L;
    private static final Long FAMILY_B = 8L;
    private static final Long OWNER_ACCOUNT = 23L;
    private static final Long OWNER_MEMBER = 17L;

    private CurrentActor activeOwner() {
        return new CurrentActor(FAMILY_A, OWNER_ACCOUNT, OWNER_MEMBER,
                true, true, true, false, true);
    }

    private VerifiedOwnerBinding verifiedOwner() {
        return new VerifiedOwnerBinding(FAMILY_A, OWNER_ACCOUNT, OWNER_MEMBER,
                Provenance.REGISTRATION_VERIFIED);
    }

    @Test void verifiedOriginalOwnerMayPassOnlyThisIdentityPrerequisite() {
        assertTrue(isVerifiedOriginalOwner(FAMILY_A, activeOwner(), verifiedOwner()));
        assertTrue(isVerifiedOriginalOwner(FAMILY_A, activeOwner(),
                new VerifiedOwnerBinding(FAMILY_A, OWNER_ACCOUNT, OWNER_MEMBER,
                        Provenance.LEGACY_VERIFIED)));
    }

    @Test void neverInfersOwnershipForLegacyUnknownOrMissingBinding() {
        assertFalse(isVerifiedOriginalOwner(FAMILY_A, activeOwner(), null));
        assertFalse(isVerifiedOriginalOwner(FAMILY_A, activeOwner(),
                new VerifiedOwnerBinding(FAMILY_A, OWNER_ACCOUNT, OWNER_MEMBER, Provenance.UNKNOWN)));
        assertFalse(isVerifiedOriginalOwner(FAMILY_A, activeOwner(),
                new VerifiedOwnerBinding(FAMILY_A, OWNER_ACCOUNT, OWNER_MEMBER, null)));
    }

    @Test void differentWorkspaceNeverInheritsAuthorizationFromSharedGlobalAccount() {
        assertFalse(isVerifiedOriginalOwner(FAMILY_B, activeOwner(), verifiedOwner()));
        var sameAccountDifferentMembership = new CurrentActor(FAMILY_B, OWNER_ACCOUNT, 41L,
                true, true, true, false, true);
        assertFalse(isVerifiedOriginalOwner(FAMILY_B, sameAccountDifferentMembership, verifiedOwner()));
        assertFalse(isVerifiedOriginalOwner(FAMILY_A, sameAccountDifferentMembership, verifiedOwner()));
    }

    @Test void delegatedManagerOrAdminOverrideWithoutOriginalBindingIsDenied() {
        // Roles are deliberately not inputs: valid credentials and delegated admin
        // permissions are insufficient unless exact immutable owner IDs match.
        var delegatedManager = new CurrentActor(FAMILY_A, 24L, 18L,
                true, true, true, false, true);
        assertFalse(isVerifiedOriginalOwner(FAMILY_A, delegatedManager, verifiedOwner()));
        var wrongMemberSameAccount = new CurrentActor(FAMILY_A, OWNER_ACCOUNT, 18L,
                true, true, true, false, true);
        assertFalse(isVerifiedOriginalOwner(FAMILY_A, wrongMemberSameAccount, verifiedOwner()));
    }

    @Test void disabledDeletedRevokedOrUnauthenticatedOwnerIsDenied() {
        assertFalse(isVerifiedOriginalOwner(FAMILY_A,
                new CurrentActor(FAMILY_A, OWNER_ACCOUNT, OWNER_MEMBER,
                        false, true, true, false, true), verifiedOwner()));
        assertFalse(isVerifiedOriginalOwner(FAMILY_A,
                new CurrentActor(FAMILY_A, OWNER_ACCOUNT, OWNER_MEMBER,
                        true, false, true, false, true), verifiedOwner()));
        assertFalse(isVerifiedOriginalOwner(FAMILY_A,
                new CurrentActor(FAMILY_A, OWNER_ACCOUNT, OWNER_MEMBER,
                        true, true, false, false, true), verifiedOwner()));
        assertFalse(isVerifiedOriginalOwner(FAMILY_A,
                new CurrentActor(FAMILY_A, OWNER_ACCOUNT, OWNER_MEMBER,
                        true, true, true, true, true), verifiedOwner()));
        assertFalse(isVerifiedOriginalOwner(FAMILY_A,
                new CurrentActor(FAMILY_A, OWNER_ACCOUNT, OWNER_MEMBER,
                        true, true, true, false, false), verifiedOwner()));
    }

    @Test void missingIdentityNeverAuthorizes() {
        assertFalse(isVerifiedOriginalOwner(null, activeOwner(), verifiedOwner()));
        assertFalse(isVerifiedOriginalOwner(FAMILY_A, null, verifiedOwner()));
        assertFalse(isVerifiedOriginalOwner(FAMILY_A, activeOwner(),
                new VerifiedOwnerBinding(FAMILY_A, null, OWNER_MEMBER,
                        Provenance.REGISTRATION_VERIFIED)));
        assertFalse(isVerifiedOriginalOwner(FAMILY_A, activeOwner(),
                new VerifiedOwnerBinding(FAMILY_A, OWNER_ACCOUNT, null,
                        Provenance.REGISTRATION_VERIFIED)));
        assertFalse(isVerifiedOriginalOwner(FAMILY_A,
                new CurrentActor(FAMILY_A, null, OWNER_MEMBER,
                        true, true, true, false, true), verifiedOwner()));
        assertFalse(isVerifiedOriginalOwner(FAMILY_A,
                new CurrentActor(FAMILY_A, OWNER_ACCOUNT, null,
                        true, true, true, false, true), verifiedOwner()));
    }

    @Test void mismatchedOwnerBindingCannotAuthorize() {
        assertFalse(isVerifiedOriginalOwner(FAMILY_A, activeOwner(),
                new VerifiedOwnerBinding(FAMILY_B, OWNER_ACCOUNT, OWNER_MEMBER,
                        Provenance.REGISTRATION_VERIFIED)));
        assertFalse(isVerifiedOriginalOwner(FAMILY_A, activeOwner(),
                new VerifiedOwnerBinding(FAMILY_A, 99L, OWNER_MEMBER,
                        Provenance.REGISTRATION_VERIFIED)));
        assertFalse(isVerifiedOriginalOwner(FAMILY_A, activeOwner(),
                new VerifiedOwnerBinding(FAMILY_A, OWNER_ACCOUNT, 99L,
                        Provenance.REGISTRATION_VERIFIED)));
    }
}
