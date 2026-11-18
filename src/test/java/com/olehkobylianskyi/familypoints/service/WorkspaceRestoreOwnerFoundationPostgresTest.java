package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.service.WorkspaceOriginalOwnerEnrollmentService;
import com.olehkobylianskyi.familypoints.service.WorkspaceRestorePasswordLimiter;
import com.olehkobylianskyi.familypoints.service.WorkspacePortableMemberFkClosure;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** B-INT-02 executable real PG18 migrated owner provenance and RESTORE budget. */
class WorkspaceRestoreOwnerFoundationPostgresTest {
    @Test void creatorRecordIsTransactionalUniqueAndCannotTransferAcrossFamilies() {
        try(var pg=new PostgreSQLContainer("postgres:18-alpine")
                .withDatabaseName("restore_creator").withUsername("qa").withPassword("qa")) {
            pg.start();
            var ds=new DriverManagerDataSource(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
            Flyway.configure().dataSource(ds).locations("classpath:db/migration")
                    .load().migrate();
            var db=new JdbcTemplate(ds);
            var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
            var owner=new WorkspaceOriginalOwnerEnrollmentService(db);
            var limiter=new WorkspaceRestorePasswordLimiter(db);

            long a=db.queryForObject("INSERT INTO families(name) VALUES ('Owner A') RETURNING id",Long.class);
            long b=db.queryForObject("INSERT INTO families(name) VALUES ('Owner B') RETURNING id",Long.class);
            long roleA=db.queryForObject("""
                    INSERT INTO family_roles(family_id,system_role,code,name)
                    VALUES (?,true,'PARENT','Parent') RETURNING id
                    """,Long.class,a);
            long roleB=db.queryForObject("""
                    INSERT INTO family_roles(family_id,system_role,code,name)
                    VALUES (?,true,'PARENT','Parent') RETURNING id
                    """,Long.class,b);
            long memberA=db.queryForObject("""
                    INSERT INTO family_members(family_id,family_role_id,member_type,name,active)
                    VALUES (?,?,'PARENT','Only for test',true) RETURNING id
                    """,Long.class,a,roleA);
            long memberB=db.queryForObject("""
                    INSERT INTO family_members(family_id,family_role_id,member_type,name,active)
                    VALUES (?,?,'PARENT','Neighbor',true) RETURNING id
                    """,Long.class,b,roleB);
            long accountA=db.queryForObject("""
                    INSERT INTO user_accounts(username,password_hash,enabled,family_member_id)
                    VALUES (?,?,true,?) RETURNING id
                    """,Long.class,"qa_creator_"+UUID.randomUUID(),"$2a$synthetic",memberA);
            db.update("""
                    INSERT INTO account_workspace_memberships(account_id,family_member_id,workspace_id)
                    VALUES (?,?,?)
                    """,accountA,memberA,a);
            assertEquals(0L,db.queryForObject(
                    "SELECT count(*) FROM workspace_original_owner_bindings WHERE workspace_id=?",
                    Long.class,b));
            // Physical catalog review includes original-member FKs from
            // all applied migrations, rather than a fixed 17-edge guess.
            var fkEdges=new WorkspacePortableMemberFkClosure(db).inspect(a);
            assertTrue(fkEdges.size()>=17);
            assertTrue(fkEdges.stream().anyMatch(e->e.table().equals("user_accounts")
                    && e.disposition()==WorkspacePortableMemberFkClosure.Disposition.EXCLUDE));
            assertTrue(fkEdges.stream().anyMatch(e->e.table().equals("task_instances")
                    && e.disposition()==WorkspacePortableMemberFkClosure.Disposition.INCLUDE));
            assertDoesNotThrow(()->new WorkspacePortableMemberFkClosure(db)
                    .requireHistoryClosure(a));

            assertThrows(SecurityException.class,
                    ()->tx.execute(s->{owner.enrollNewCreator(b,accountA,memberA);return null;}));
            assertThrows(SecurityException.class,
                    ()->tx.execute(s->{owner.enrollNewCreator(a,accountA,memberB);return null;}));
            assertEquals(0L,db.queryForObject(
                    "SELECT count(*) FROM workspace_original_owner_bindings WHERE workspace_id=?",
                    Long.class,a));
            // Legitimate service path can mark only a newly provisioned\n            // family/member within its own physical registration transaction.\n            tx.execute(status->{\n                WorkspaceNewFamilyTransactionEvidence.record(a,memberA);\n                owner.enrollNewCreator(a,accountA,memberA);\n                return null;\n            });
            assertEquals(1L,db.queryForObject(
                    "SELECT count(*) FROM workspace_original_owner_bindings WHERE workspace_id=?",
                    Long.class,a));
            assertThrows(SecurityException.class,
                    ()->tx.execute(s->{owner.enrollNewCreator(a,accountA,memberA);return null;}));
            assertEquals(1L,db.queryForObject(
                    "SELECT count(*) FROM workspace_original_owner_bindings WHERE workspace_id=?",
                    Long.class,a));

            // Authorization audit: no update/delete may silently change the creator.
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    ()->db.update("""
                            INSERT INTO workspace_original_owner_bindings(
                              workspace_id,owner_account_id,owner_member_id,provenance,evidence_id)
                            VALUES (?, ?, ?, 'REGISTRATION_VERIFIED',?)
                            """,a,accountA,memberB,UUID.randomUUID()));

            // Separate RESTORE attempt budget: five failures lock, but other
            // family/account cannot inherit a failed counter by numeric coincidence.
            limiter.requireAvailable(a,accountA);
            for(int n=0;n<5;n++)limiter.recordFailure(a,accountA);
            assertThrows(SecurityException.class,()->limiter.requireAvailable(a,accountA));
            limiter.requireAvailable(b,accountA);
            limiter.clearSuccess(a,accountA);
            assertDoesNotThrow(()->limiter.requireAvailable(a,accountA));
        }
    }

    @Test void receiptRowsAreDatabaseImmutable() {
        try(var pg=new PostgreSQLContainer("postgres:18-alpine")
                .withDatabaseName("restore_immutable").withUsername("qa").withPassword("qa")) {
            pg.start();
            var ds=new DriverManagerDataSource(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
            Flyway.configure().dataSource(ds).locations("classpath:db/migration")
                    .load().migrate();
            var db=new JdbcTemplate(ds);
            // Ensure the migration installed the immutable receipt trigger.
            Integer found=db.queryForObject("""
                    SELECT count(*) FROM pg_trigger WHERE tgname='trg_restore_receipt_immutable'
                      AND NOT tgisinternal
                    """,Integer.class);
            assertEquals(1,found);
            assertEquals(0,db.queryForObject(
                    "SELECT count(*) FROM workspace_restore_receipts",Integer.class));
        }
    }
}
