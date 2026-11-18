package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.WorkspaceEpochTransactionGuard;
import com.olehkobylianskyi.familypoints.service.WorkspaceResetProofService;
import com.olehkobylianskyi.familypoints.service.WorkspaceResetSnapshotService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * C-C-05: real PostgreSQL 18 row-lock and rollback proof tests, disposable DB only.
 * Never calls the locked HTTP Reset Execute endpoint or performs any family reset.
 */
@Testcontainers
class WorkspaceResetProofConcurrencyPostgresTest {
    @Container
    static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("reset_proof_concurrency")
            .withUsername("reset_proof_qa")
            .withPassword("reset_proof_qa");

    private JdbcTemplate jdbc;
    private TransactionTemplate tx;
    private WorkspaceResetProofService proofs;
    private long familyA, familyB, accountId, memberId;
    private UUID operation;
    private String digest;
    private static final String PASSWORD = "test-only-re-auth-secret";

    @BeforeEach
    void migratedDisposableFixture() {
        var ds = new DriverManagerDataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        var encoder = new BCryptPasswordEncoder();
        var guard = new WorkspaceEpochTransactionGuard(jdbc);
        var snapshots = mock(WorkspaceResetSnapshotService.class);
        proofs = new WorkspaceResetProofService(jdbc, guard, snapshots, encoder);

        familyA = jdbc.queryForObject(
                "INSERT INTO families(name) VALUES (?) RETURNING id",
                Long.class, "Proof A " + UUID.randomUUID());
        familyB = jdbc.queryForObject(
                "INSERT INTO families(name) VALUES (?) RETURNING id",
                Long.class, "Proof B " + UUID.randomUUID());
        long role = jdbc.queryForObject("""
                INSERT INTO family_roles(family_id,system_role,code,name)
                VALUES (?,true,'PARENT','Parent') RETURNING id
                """, Long.class, familyA);
        memberId = jdbc.queryForObject("""
                INSERT INTO family_members(family_id,family_role_id,active,member_type,name)
                VALUES (?,?,true,'PARENT','Owner fixture') RETURNING id
                """, Long.class, familyA, role);
        accountId = jdbc.queryForObject("""
                INSERT INTO user_accounts(username,password_hash,enabled,family_member_id)
                VALUES (?,?,true,?) RETURNING id
                """, Long.class, "reset_qa_" + UUID.randomUUID(), encoder.encode(PASSWORD), memberId);
        jdbc.update("""
                INSERT INTO account_workspace_memberships(account_id,family_member_id,workspace_id)
                VALUES (?,?,?)
                """, accountId, memberId, familyA);
        operation = UUID.randomUUID();
        digest = "a".repeat(64);
        jdbc.update("""
                INSERT INTO workspace_reset_operations
                    (workspace_id,operation_id,requested_by_member_id,selected_categories_json,snapshot_digest)
                VALUES (?,?::uuid,?,'[]',?)
                """, familyA, operation.toString(), memberId, digest);
        assertEquals(1, jdbc.update("""
                UPDATE workspace_reset_operations SET status='CONFIRMED', confirmed_at=CURRENT_TIMESTAMP
                WHERE workspace_id=? AND operation_id=?::uuid AND status='PREPARED'
                """, familyA, operation.toString()));
    }

    private WorkspaceResetProofService.IssuedProof issue() {
        return tx.execute(status -> proofs.issuePasswordChallenge(
                familyA, operation, accountId, memberId, digest, PASSWORD));
    }

    private void consume(WorkspaceResetProofService.IssuedProof proof) {
        tx.executeWithoutResult(status -> proofs.consume(
                familyA, operation, proof.challengeId(), accountId, memberId,
                digest, proof.secret()));
    }

    private int consumedCount(UUID challengeId) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM workspace_reset_challenges
                WHERE id=? AND consumed_at IS NOT NULL
                """, Integer.class, challengeId);
    }

    @Test
    void twoConcurrentConsumersCommitExactlyOneConsumption() throws Exception {
        var proof = issue();
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            List<java.util.concurrent.Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    try {
                        consume(proof);
                        return true;
                    } catch (ResponseStatusException rejected) {
                        assertEquals(HttpStatus.CONFLICT, rejected.getStatusCode());
                        return false;
                    }
                }));
            }
            start.countDown();
            int successes = 0;
            for (var result : results) {
                if (result.get(30, TimeUnit.SECONDS)) successes++;
            }
            assertEquals(1, successes);
            assertEquals(1, consumedCount(proof.challengeId()));
            assertEquals(0L, jdbc.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=?", Long.class, familyA));
            assertEquals(0L, jdbc.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=?", Long.class, familyB));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void failedTransactionRestoresUnconsumedProofThenRetryCanCommit() {
        var proof = issue();
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            proofs.consume(familyA, operation, proof.challengeId(),
                    accountId, memberId, digest, proof.secret());
            throw new IllegalStateException("synthetic failure after proof consumption");
        }));
        assertEquals(0, consumedCount(proof.challengeId()));
        consume(proof);
        assertEquals(1, consumedCount(proof.challengeId()));
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class, () -> consume(proof)).getStatusCode());
    }

    @Test
    void crossWorkspaceAttemptDoesNotConsumeOrChangeNeighbor() {
        var proof = issue();
        var ex = assertThrows(ResponseStatusException.class, () -> tx.executeWithoutResult(
                status -> proofs.consume(familyB, operation, proof.challengeId(),
                        accountId, memberId, digest, proof.secret())));
        assertTrue(ex.getStatusCode().is4xxClientError());
        assertEquals(0, consumedCount(proof.challengeId()));
        assertEquals(0L, jdbc.queryForObject(
                "SELECT data_epoch FROM families WHERE id=?", Long.class, familyB));
        consume(proof);
        assertEquals(1, consumedCount(proof.challengeId()));
    }


    @Test
    void physicallyExpiredProofCannotBeConsumed() {
        var proof = issue();
        jdbc.update("UPDATE workspace_reset_challenges SET expires_at=clock_timestamp()-interval '1 second' WHERE id=?",
                proof.challengeId());
        var ex = assertThrows(ResponseStatusException.class, () -> consume(proof));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
        assertEquals(0, consumedCount(proof.challengeId()));
        assertEquals(0L, jdbc.queryForObject(
                "SELECT data_epoch FROM families WHERE id=?", Long.class, familyB));
    }

    @Test
    void passwordRotationInvalidatesPreviouslyIssuedProof() {
        var proof = issue();
        var newPasswordHash = new BCryptPasswordEncoder().encode("rotated-test-secret");
        assertEquals(1, jdbc.update(
                "UPDATE user_accounts SET password_hash=? WHERE id=?", newPasswordHash, accountId));
        var ex = assertThrows(ResponseStatusException.class, () -> consume(proof));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(0, consumedCount(proof.challengeId()));
        assertEquals(0L, jdbc.queryForObject(
                "SELECT data_epoch FROM families WHERE id=?", Long.class, familyA));
    }

    @Test
    void persistedEpochAdvanceRejectsOldProofBeforeConsumption() {
        var proof = issue();
        jdbc.update("UPDATE families SET data_epoch=data_epoch+1 WHERE id=?", familyA);
        var ex = assertThrows(ResponseStatusException.class, () -> consume(proof));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(0, consumedCount(proof.challengeId()));
        assertEquals(1L, jdbc.queryForObject(
                "SELECT data_epoch FROM families WHERE id=?", Long.class, familyA));
        assertEquals(0L, jdbc.queryForObject(
                "SELECT data_epoch FROM families WHERE id=?", Long.class, familyB));
    }
}
