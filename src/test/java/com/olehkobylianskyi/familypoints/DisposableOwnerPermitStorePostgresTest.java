package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.security.DisposableOwnerPermitStore;
import com.olehkobylianskyi.familypoints.security.WorkspaceDestructivePermitContract.Action;
import com.olehkobylianskyi.familypoints.security.WorkspaceDestructivePermitContract.Scope;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Fully synthetic two-family PG18 permit issuance/consume. No real owner auth or reset. */
@Testcontainers
class DisposableOwnerPermitStorePostgresTest {
    @Container static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("owner_permit_rehearsal").withUsername("qa").withPassword("qa");

    private JdbcTemplate db;
    private DisposableOwnerPermitStore store;
    private long a, b;
    private static final String S = "a".repeat(64), C = "b".repeat(64), M = "c".repeat(64);

    @BeforeEach void setup() {
        var ds = new DriverManagerDataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        db = new JdbcTemplate(ds);
        store = new DisposableOwnerPermitStore(db,
                new TransactionTemplate(new DataSourceTransactionManager(ds)));
        store.createQaTables();
        a = db.queryForObject("INSERT INTO families(name) VALUES (?) RETURNING id",
                Long.class, "Owner QA A " + UUID.randomUUID());
        b = db.queryForObject("INSERT INTO families(name) VALUES (?) RETURNING id",
                Long.class, "Owner QA B " + UUID.randomUUID());
    }

    private Scope scope(Action action) {
        Instant issued = Instant.now().truncatedTo(ChronoUnit.MICROS).minusSeconds(5);
        return new Scope(action, UUID.randomUUID(), a, 23L, 17L, UUID.randomUUID(),
                S, C, action == Action.RESTORE ? M : null, 0L,
                issued, issued.plusSeconds(300));
    }

    private long epoch(long family) {
        return db.queryForObject("SELECT data_epoch FROM families WHERE id=?", Long.class, family);
    }

    private int receipts(UUID id) {
        return db.queryForObject("SELECT count(*) FROM qa_c_owner_permit_receipts WHERE permit_id=?",
                Integer.class, id);
    }

    @Test void successfulRestoreReceiptIsAtomicAndCannotReplay() {
        Scope scope = scope(Action.RESTORE);
        var permit = store.issueSynthetic(scope);
        assertFalse(permit.toString().contains(permit.secret()));
        store.consumeSynthetic(permit,scope,false);
        assertEquals(1L,epoch(a));
        assertEquals(0L,epoch(b));
        assertEquals(1,receipts(permit.id()));
        assertEquals("RESTORE",db.queryForObject(
                "SELECT action FROM qa_c_owner_permit_receipts WHERE permit_id=?",
                String.class, permit.id()));
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                () -> store.consumeSynthetic(permit,scope,false)).getStatusCode());
    }

    @Test void rollbackRestoresPermitAndEpochThenRetrySucceeds() {
        Scope scope=scope(Action.RESET);
        var permit=store.issueSynthetic(scope);
        assertThrows(IllegalStateException.class, () -> store.consumeSynthetic(permit,scope,true));
        assertEquals(0L,epoch(a));
        assertEquals(0,receipts(permit.id()));
        assertEquals(0,db.queryForObject(
                "SELECT count(*) FROM qa_c_owner_permits WHERE id=? AND consumed_at IS NOT NULL",
                Integer.class,permit.id()));
        // Fresh helper object simulates process restart against same persisted database.
        store.consumeSynthetic(permit,scope,false);
        assertEquals(1L,epoch(a));
        assertEquals(1,receipts(permit.id()));
        assertEquals(0L,epoch(b));
    }

    @Test void crossActionAndCrossTenantReplaysFailWithoutMutations() {
        Scope reset=scope(Action.RESET);
        var permit=store.issueSynthetic(reset);
        Scope wrongAction=new Scope(Action.RESTORE, reset.operationId(), a,
                reset.ownerAccountId(),reset.ownerMemberId(),reset.snapshotId(),S,C,M,0,
                reset.issuedAt(),reset.expiresAt());
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                () -> store.consumeSynthetic(permit,wrongAction,false)).getStatusCode());
        Scope wrongTenant=new Scope(Action.RESET,reset.operationId(),b,
                reset.ownerAccountId(),reset.ownerMemberId(),reset.snapshotId(),S,C,null,0,
                reset.issuedAt(),reset.expiresAt());
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
                () -> store.consumeSynthetic(permit,wrongTenant,false)).getStatusCode());
        assertEquals(0L,epoch(a));
        assertEquals(0L,epoch(b));
        assertEquals(0,receipts(permit.id()));
    }

    @Test void wrongOwnerScopeEpochAndSecretFailClosed() {
        Scope scope=scope(Action.RESTORE);
        var permit=store.issueSynthetic(scope);
        Scope wrongOwner=new Scope(Action.RESTORE,scope.operationId(),a,99L,17L,
                scope.snapshotId(),S,C,M,0,scope.issuedAt(),scope.expiresAt());
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                () -> store.consumeSynthetic(permit,wrongOwner,false)).getStatusCode());
        var wrongSecret=new DisposableOwnerPermitStore.Issued(permit.id(),"wrong");
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,
                () -> store.consumeSynthetic(wrongSecret,scope,false)).getStatusCode());
        db.update("UPDATE families SET data_epoch=1 WHERE id=?",a);
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                () -> store.consumeSynthetic(permit,scope,false)).getStatusCode());
        assertEquals(0,receipts(permit.id()));
        assertEquals(0L,epoch(b));
    }

    @Test void durableExpiryAndDuplicateOperationReject() {
        Scope scope=scope(Action.RESET);
        var permit=store.issueSynthetic(scope);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> store.issueSynthetic(scope)).getStatusCode());
        db.update("UPDATE qa_c_owner_permits SET expires_at=clock_timestamp()-interval '1 second' WHERE id=?",
                permit.id());
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                () -> store.consumeSynthetic(permit,scope,false)).getStatusCode());
        assertEquals(0L,epoch(a));
        assertEquals(0,receipts(permit.id()));
    }

    @Test void concurrentConsumersOfOnePermitCommitOnlyOnce() throws Exception {
        Scope scope=scope(Action.RESTORE);
        var permit=store.issueSynthetic(scope);
        var pool=Executors.newFixedThreadPool(2);
        var latch=new CountDownLatch(1);
        try {
            List<java.util.concurrent.Future<Boolean>> futures=new ArrayList<>();
            for(int i=0;i<2;i++) futures.add(pool.submit(()->{
                latch.await();
                try {
                    store.consumeSynthetic(permit,scope,false);
                    return true;
                } catch(ResponseStatusException rejected) {
                    assertEquals(HttpStatus.CONFLICT,rejected.getStatusCode());
                    return false;
                }
            }));
            latch.countDown();
            int successes=0;
            for(var f:futures) if(f.get(30,TimeUnit.SECONDS)) successes++;
            assertEquals(1,successes);
            assertEquals(1L,epoch(a));
            assertEquals(0L,epoch(b));
            assertEquals(1,receipts(permit.id()));
        } finally {
            pool.shutdownNow();
        }
    }
}
