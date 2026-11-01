package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.WorkspaceEpochTransactionGuard;
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
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** BKL-002 concurrency acceptance foundation. Never calls public reset execute.
 * Simulates the atomic epoch transition under the same family FOR UPDATE fence
 * required for a future approved transactional deletion engine.
 */
@Testcontainers
class WorkspaceResetEpochConcurrencyPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("reset_epoch_race")
            .withUsername("race_test").withPassword("race_test_password");

    @Test void staleMutationCannotCrossCommittedFamilyEpochFence() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long family=db.queryForObject("INSERT INTO families(name) VALUES ('Race test family') RETURNING id",Long.class);
        long other=db.queryForObject("INSERT INTO families(name) VALUES ('Untouched family') RETURNING id",Long.class);
        var transactions=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var guard=new WorkspaceEpochTransactionGuard(db);
        var fenceHeld=new CountDownLatch(1);
        var releaseFence=new CountDownLatch(1);
        var pool=Executors.newFixedThreadPool(2);
        try {
            var reset=pool.submit(()->transactions.execute(status->{
                assertEquals(0L,guard.requireCurrentEpoch(family,0L));
                fenceHeld.countDown();
                try {
                    if(!releaseFence.await(10,TimeUnit.SECONDS))
                        throw new IllegalStateException("Test coordination timeout");
                } catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
                assertEquals(1,db.update("UPDATE families SET data_epoch=data_epoch+1 WHERE id=?",family));
                return 1L;
            }));
            assertTrue(fenceHeld.await(10,TimeUnit.SECONDS));
            var staleWriter=pool.submit(()->{
                try {
                    transactions.execute(status->{
                        guard.requireCurrentEpoch(family,0L);
                        db.update("UPDATE families SET name='UNSAFE' WHERE id=?",family);
                        return null;
                    });
                    return false;
                } catch(ResponseStatusException exception) {
                    return exception.getStatusCode().equals(HttpStatus.CONFLICT);
                }
            });
            releaseFence.countDown();
            assertEquals(1L,reset.get(20,TimeUnit.SECONDS));
            assertTrue(staleWriter.get(20,TimeUnit.SECONDS),
                    "An old offline writer must see 409 after the family epoch changes");
            assertEquals("Race test family",db.queryForObject(
                    "SELECT name FROM families WHERE id=?",String.class,family));
            assertEquals(1L,db.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=?",Long.class,family));
            assertEquals(0L,db.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=?",Long.class,other));
        } finally {
            releaseFence.countDown();
            pool.shutdownNow();
        }
    }

    @Test void failedAtomicEpochTransitionRollsBackWithoutAffectingOtherFamily() {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long family=db.queryForObject("INSERT INTO families(name) VALUES ('Failure injection') RETURNING id",Long.class);
        long other=db.queryForObject("INSERT INTO families(name) VALUES ('Preserved peer') RETURNING id",Long.class);
        var transactions=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var guard=new WorkspaceEpochTransactionGuard(db);
        assertThrows(IllegalStateException.class,()->transactions.execute(status->{
            guard.requireCurrentEpoch(family,0L);
            db.update("UPDATE families SET data_epoch=data_epoch+1 WHERE id=?",family);
            throw new IllegalStateException("Inject failure after epoch increment");
        }));
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,family));
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,other));
        assertEquals("Preserved peer",db.queryForObject(
                "SELECT name FROM families WHERE id=?",String.class,other));
    }
}
