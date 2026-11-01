package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class WorkspaceProtectiveBackupPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
        .withDatabaseName("protective_backups").withUsername("backups_test").withPassword("backups_password");
    @Test void registrationIsIdempotentTenantScopedAndCannotClaimVerifiedWithoutRestoreProof() {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long a=db.queryForObject("INSERT INTO families(name) VALUES ('Backup family A') RETURNING id",Long.class);
        long b=db.queryForObject("INSERT INTO families(name) VALUES ('Backup family B') RETURNING id",Long.class);
        // FamilyMembers are required by registry FK and actor membership check.
        long member=db.queryForObject("""
            INSERT INTO family_members(family_id,name) VALUES (?, 'Backup owner') RETURNING id
            """,Long.class,a);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var backups=new WorkspaceProtectiveBackupService(db,new WorkspaceEpochTransactionGuard(db));
        var req=new WorkspaceProtectiveBackupService.Request(UUID.randomUUID(),"SELECTIVE",
                List.of("PRIVATE_TEMPLATES"),"a".repeat(64),"V50");
        var first=tx.execute(s->backups.request(a,member,req));
        assertNotNull(first);
        assertEquals("REQUESTED",first.state());
        assertFalse(first.restoreVerified());
        assertFalse(first.executable());
        assertFalse(first.expiresAt().isBefore(first.createdAt().plusDays(30)));
        var replay=tx.execute(s->backups.request(a,member,req));
        assertEquals(first.snapshotId(),replay.snapshotId());
        assertEquals(1L,db.queryForObject(
                "SELECT count(*) FROM workspace_protective_backups WHERE workspace_id=?",Long.class,a));
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
                ()->backups.get(b,first.snapshotId())).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->tx.execute(s->backups.request(a,member,
                    new WorkspaceProtectiveBackupService.Request(req.operationId(),"FULL",
                        req.categories(),req.sourceDigest(),req.schemaVersion())))).getStatusCode());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->
                db.update("UPDATE workspace_protective_backups SET state='VERIFIED' WHERE snapshot_id=?",
                    first.snapshotId()));
        assertEquals("REQUESTED",backups.get(a,first.snapshotId()).state());
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,a));
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,b));
    }
}
