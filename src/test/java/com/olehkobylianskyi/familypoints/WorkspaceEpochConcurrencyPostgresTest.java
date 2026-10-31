package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.WorkspaceEpochTransactionGuard;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Integration proof for the epoch guard on isolated disposable PostgreSQL. */
@Testcontainers
class WorkspaceEpochConcurrencyPostgresTest {
    @Container static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("epoch_lock_regression")
            .withUsername("epoch_test").withPassword("epoch_test_password");

    @Test void concurrentWriterWaitsForEpochIncrementAndThenRejectsStaleWrite() throws Exception {
        Flyway.configure().dataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword())
                .locations("classpath:db/migration").load().migrate();
        var source=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        var sql=new JdbcTemplate(source);
        long family=sql.queryForObject("INSERT INTO families(name) VALUES ('Epoch lock disposable') RETURNING id",Long.class);
        var guard=new WorkspaceEpochTransactionGuard(sql);
        var manager=new DataSourceTransactionManager(source);
        var tx=new TransactionTemplate(manager);
        var locked=new CountDownLatch(1);
        var release=new CountDownLatch(1);
        var writerStarted=new CountDownLatch(1);
        var executor=Executors.newFixedThreadPool(2);
        try {
            Future<?> reset=executor.submit(()->tx.executeWithoutResult(status->{
                sql.queryForObject("SELECT data_epoch FROM families WHERE id=? FOR UPDATE",Long.class,family);
                locked.countDown();
                try {
                    if(!release.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("lock not released");
                } catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                sql.update("UPDATE families SET data_epoch=data_epoch+1 WHERE id=?",family);
            }));
            assertTrue(locked.await(10,TimeUnit.SECONDS));
            Future<Boolean> writer=executor.submit(()->{
                writerStarted.countDown();
                try {
                    tx.executeWithoutResult(status->{
                        guard.requireCurrentEpoch(family,0L);
                        sql.update("UPDATE families SET name='unsafe stale mutation' WHERE id=?",family);
                    });
                    return false;
                } catch(ResponseStatusException conflict) {
                    return conflict.getStatusCode().value()==409;
                }
            });
            assertTrue(writerStarted.await(10,TimeUnit.SECONDS));
            release.countDown();
            reset.get(15,TimeUnit.SECONDS);
            assertTrue(writer.get(15,TimeUnit.SECONDS),"stale writer must fail after locked epoch increment");
            assertEquals(1L,sql.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,family));
            assertEquals("Epoch lock disposable",sql.queryForObject("SELECT name FROM families WHERE id=?",String.class,family));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test void rollbackDoesNotAdvanceEpoch() {
        Flyway.configure().dataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword())
                .locations("classpath:db/migration").load().migrate();
        var source=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        var db=new JdbcTemplate(source);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        long family=db.queryForObject("INSERT INTO families(name) VALUES ('Epoch rollback') RETURNING id",Long.class);
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(status->{
            db.queryForObject("SELECT data_epoch FROM families WHERE id=? FOR UPDATE",Long.class,family);
            db.update("UPDATE families SET data_epoch=data_epoch+1 WHERE id=?",family);
            throw new IllegalStateException("simulate failure");
        }));
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,family));
    }
}
