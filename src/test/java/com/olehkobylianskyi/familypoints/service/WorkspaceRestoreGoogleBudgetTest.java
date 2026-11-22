package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.entity.Workspace;
import com.olehkobylianskyi.familypoints.entity.WorkspaceMember;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.security.WorkspaceRestorePermitScopeV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Credential accounting at the actual internal provider reauth entrypoint. */
class WorkspaceRestoreGoogleBudgetTest {
    private final JdbcTemplate db=mock(JdbcTemplate.class);
    private final WorkspaceEpochTransactionGuard epochs=mock(WorkspaceEpochTransactionGuard.class);
    private final WorkspaceRestorePasswordLimiter limiter=mock(WorkspaceRestorePasswordLimiter.class);
    private final ProtectiveBackupImportInspector inspector=mock(ProtectiveBackupImportInspector.class);
    private final WorkspaceRestoreCategorySelectionV2 selection=mock(WorkspaceRestoreCategorySelectionV2.class);
    private final CurrentUserService current=mock(CurrentUserService.class);
    private final ProtectiveBackupPortableFile portable=mock(ProtectiveBackupPortableFile.class);
    private final ProtectiveBackupArtifactCodec codec=mock(ProtectiveBackupArtifactCodec.class);
    private final GoogleTokenVerifier google=mock(GoogleTokenVerifier.class);
    private final WorkspaceRestorePermitService service=new WorkspaceRestorePermitService(
            db,epochs,mock(org.springframework.security.crypto.password.PasswordEncoder.class),
            limiter,inspector,selection,current,portable,codec,google);
    private final byte[] archive=new byte[]{1};
    private final javax.crypto.SecretKey key=new SecretKeySpec(new byte[16],"AES");

    private WorkspaceRestorePermitScopeV2.RestoreScope scope(long workspace,long account,long member) {
        var now=Instant.now();
        return new WorkspaceRestorePermitScopeV2.RestoreScope(workspace,account,member,
            UUID.randomUUID(),
            new WorkspaceRestorePermitScopeV2.SourceArchive(99,"FPB2",UUID.randomUUID(),0,
                    "a".repeat(64),"b".repeat(64)),
            "c".repeat(64),"d".repeat(64),0,now.minusSeconds(1),now.plusSeconds(240));
    }

    @BeforeEach void authenticatedCreator() {
        var account=mock(UserAccount.class);
        var member=mock(WorkspaceMember.class);
        var workspace=mock(Workspace.class);
        when(account.getId()).thenReturn(2L);
        when(account.isEnabled()).thenReturn(true);
        when(member.getId()).thenReturn(3L);
        when(member.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(1L);
        when(current.getCurrentAccount()).thenReturn(account);
        when(current.getCurrentMember()).thenReturn(member);
        when(db.queryForList(contains("SELECT a.password_hash"),eq(String.class),
                eq(1L),eq(2L),eq(3L))).thenReturn(List.of(""));
    }

    @Test void invalidJwtResponseStatusCountsOneFailure() {
        when(google.verifiedToken("invalid")).thenThrow(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid Google token"));
        var s=scope(1,2,3);
        assertThrows(ResponseStatusException.class,
                ()->service.issueGoogle(s,archive,key,Map.of(4L,3L),"invalid"));
        verify(limiter).requireAvailable(1,2);
        verify(limiter,times(1)).recordFailure(1,2);
        verify(limiter,never()).clearSuccess(anyLong(),anyLong());
        verifyNoInteractions(inspector);
    }

    @Test void staleAndFutureJwtEachCountExactlyOnce() {
        for(int i=0;i<2;i++) {
            reset(google,limiter);
            var jwt=mock(Jwt.class);
            when(jwt.getIssuedAt()).thenReturn(i==0
                    ?Instant.now().minusSeconds(350):Instant.now().plusSeconds(60));
            when(google.verifiedToken("bad-iat")).thenReturn(jwt);
            assertThrows(SecurityException.class,
                    ()->service.issueGoogle(scope(1,2,3),archive,key,
                            Map.of(4L,3L),"bad-iat"));
            verify(limiter,times(1)).recordFailure(1,2);
            verify(limiter,never()).clearSuccess(anyLong(),anyLong());
        }
    }

    @Test void missingLinkedGoogleIdentityCountsOnce() {
        var jwt=mock(Jwt.class);
        when(jwt.getIssuedAt()).thenReturn(Instant.now());
        when(google.verifiedToken("valid")).thenReturn(jwt);
        // No account_external_identities linked identity in the stub.
        assertThrows(SecurityException.class,()->service.issueGoogle(
                scope(1,2,3),archive,key,Map.of(4L,3L),"valid"));
        verify(limiter,times(1)).recordFailure(1,2);
    }

    @Test void wrongSelectedAccountIsNotCredentialAttempt() {
        var s=scope(1,100,3);
        assertThrows(SecurityException.class,
                ()->service.issueGoogle(s,archive,key,Map.of(4L,3L),"bad"));
        verifyNoInteractions(limiter,google);
    }

    @Test void expiredScopeIsNotCredentialAttempt() {
        var now=Instant.now();
        var expired=new WorkspaceRestorePermitScopeV2.RestoreScope(1,2,3,UUID.randomUUID(),
            new WorkspaceRestorePermitScopeV2.SourceArchive(
                    99,"FPB2",UUID.randomUUID(),0,"a".repeat(64),"b".repeat(64)),
            "c".repeat(64),"d".repeat(64),0,now.minusSeconds(300),now.minusSeconds(1));
        assertThrows(SecurityException.class,
                ()->service.issueGoogle(expired,archive,key,Map.of(4L,3L),"bad"));
        verify(limiter).requireAvailable(1,2);
        verify(limiter,never()).recordFailure(anyLong(),anyLong());
        verifyNoInteractions(google);
    }

    @Test void signedArchiveFailureAfterSuccessfulCredentialIsNotCounted() {
        var jwt=mock(Jwt.class);
        when(jwt.getIssuedAt()).thenReturn(Instant.now());
        when(jwt.getSubject()).thenReturn("trusted-provider-subject");
        when(google.verifiedToken("valid")).thenReturn(jwt);
        when(db.queryForList(contains("SELECT provider_subject"),eq(String.class),eq(2L)))
                .thenReturn(List.of("trusted-provider-subject"));
        when(inspector.inspectAncestryV2(any(byte[].class),eq(99L),same(key)))
                .thenThrow(new SecurityException("AUTHENTICATED_SOURCE_INVALID"));
        assertThrows(SecurityException.class,()->service.issueGoogle(
                scope(1,2,3),archive,key,Map.of(4L,3L),"valid"));
        verify(limiter).clearSuccess(1,2);
        verify(limiter,never()).recordFailure(anyLong(),anyLong());
    }
}
