package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.entity.Workspace;
import com.olehkobylianskyi.familypoints.entity.WorkspaceMember;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.security.WorkspaceRestorePermitScopeV2;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Actual Spring REQUIRES_NEW on two JDBC connections (PG18), proving denied
 * Google RESTORE credentials survive rollback of the caller's transaction.
 */
class WorkspaceRestoreGoogleBudgetPostgresTest {
    @Configuration
    @EnableTransactionManagement
    static class Transactions {}

    @Test void fiveRejectedGoogleProofsSurviveCallerRollbackAndBlockSixth() {
        try(var pg=new PostgreSQLContainer("postgres:18-alpine")
                .withDatabaseName("restore_google_budget")
                .withUsername("qa").withPassword("qa")) {
            pg.start();
            var ds=new DriverManagerDataSource(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
            Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
            var db=new JdbcTemplate(ds);
            var manager=new DataSourceTransactionManager(ds);
            try(var spring=new AnnotationConfigApplicationContext()) {
                spring.register(Transactions.class);
                spring.registerBean("transactionManager",PlatformTransactionManager.class,()->manager);
                spring.registerBean(JdbcTemplate.class,()->db);
                spring.registerBean(WorkspaceRestorePasswordLimiter.class,
                        ()->new WorkspaceRestorePasswordLimiter(db));
                spring.refresh();
                var limiter=spring.getBean(WorkspaceRestorePasswordLimiter.class);
                assertTrue(org.springframework.aop.support.AopUtils.isAopProxy(limiter),
                        "REQUIRES_NEW must run via actual Spring proxy");

                long family=db.queryForObject(
                        "INSERT INTO families(name) VALUES ('Google budget QA') RETURNING id",
                        Long.class);
                long other=db.queryForObject(
                        "INSERT INTO families(name) VALUES ('Separate QA family') RETURNING id",
                        Long.class);
                long role=db.queryForObject("""
                        INSERT INTO family_roles(family_id,system_role,code,name)
                        VALUES (?,true,'PARENT','Parent') RETURNING id
                        """,Long.class,family);
                long memberId=db.queryForObject("""
                        INSERT INTO family_members(family_id,family_role_id,member_type,name,active)
                        VALUES (?,?,'PARENT','Mock owner',true) RETURNING id
                        """,Long.class,family,role);
                long accountId=db.queryForObject("""
                        INSERT INTO user_accounts(username,password_hash,enabled,family_member_id)
                        VALUES (?,'GOOGLE_ONLY_NO_LOCAL_PASSWORD',true,?) RETURNING id
                        """,Long.class,"qa_google_"+UUID.randomUUID(),memberId);
                db.update("""
                        INSERT INTO account_workspace_memberships(
                            account_id,family_member_id,workspace_id) VALUES (?,?,?)
                        """,accountId,memberId,family);
                db.update("""
                        INSERT INTO workspace_original_owner_bindings
                          (workspace_id,owner_account_id,owner_member_id,provenance,evidence_id)
                        VALUES (?, ?, ?, 'REGISTRATION_VERIFIED', ?)
                        """,family,accountId,memberId,UUID.randomUUID());

                var auth=mock(CurrentUserService.class);
                var account=mock(UserAccount.class);
                var member=mock(WorkspaceMember.class);
                var workspace=mock(Workspace.class);
                when(account.getId()).thenReturn(accountId);
                when(account.isEnabled()).thenReturn(true);
                when(member.getId()).thenReturn(memberId);
                when(member.getWorkspace()).thenReturn(workspace);
                when(workspace.getId()).thenReturn(family);
                when(auth.getCurrentAccount()).thenReturn(account);
                when(auth.getCurrentMember()).thenReturn(member);
                var google=mock(GoogleTokenVerifier.class);
                when(google.verifiedToken("forged-token")).thenThrow(
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid Google token"));
                var service=new WorkspaceRestorePermitService(db,
                        mock(WorkspaceEpochTransactionGuard.class),mock(PasswordEncoder.class),
                        limiter,mock(ProtectiveBackupImportInspector.class),
                        mock(WorkspaceRestoreCategorySelectionV2.class),auth,
                        mock(ProtectiveBackupPortableFile.class),
                        mock(ProtectiveBackupArtifactCodec.class),google);
                var tx=new TransactionTemplate(manager);
                var key=new SecretKeySpec(new byte[16],"AES");
                for(int i=1;i<=5;i++) {
                    var now=Instant.now();
                    var scope=new WorkspaceRestorePermitScopeV2.RestoreScope(
                            family,accountId,memberId,UUID.randomUUID(),
                            new WorkspaceRestorePermitScopeV2.SourceArchive(
                                    888,"FPB2",UUID.randomUUID(),0,
                                    "a".repeat(64),"b".repeat(64)),
                            "c".repeat(64),"d".repeat(64),0,now.minusSeconds(1),
                            now.plusSeconds(240));
                    assertThrows(ResponseStatusException.class,()->tx.execute(status->{
                        service.issueGoogle(scope,new byte[]{1},key,Map.of(1L,memberId),
                                "forged-token");
                        return null;
                    }));
                    Integer failures=db.queryForObject("""
                        SELECT failed_attempts FROM workspace_restore_reauth_limits
                        WHERE workspace_id=? AND account_id=?
                        """,Integer.class,family,accountId);
                    assertEquals(i,failures,"Denied proof must survive caller rollback");
                }
                assertThrows(SecurityException.class,()->limiter.requireAvailable(family,accountId));
                verify(google,times(5)).verifiedToken("forged-token");
                // No count or lock carried over to another family.
                assertDoesNotThrow(()->limiter.requireAvailable(other,accountId));
                assertEquals(0,db.queryForObject("""
                        SELECT failed_attempts FROM workspace_restore_reauth_limits
                        WHERE workspace_id=? AND account_id=?
                        """,Integer.class,other,accountId));
                limiter.clearSuccess(family,accountId);
                assertDoesNotThrow(()->limiter.requireAvailable(family,accountId));
                assertEquals(0,db.queryForObject("""
                        SELECT failed_attempts FROM workspace_restore_reauth_limits
                        WHERE workspace_id=? AND account_id=?
                        """,Integer.class,family,accountId));
                assertEquals(0,db.queryForObject(
                        "SELECT count(*) FROM workspace_restore_permits",Integer.class));
            }
        }
    }
}
