package com.olehkobylianskyi.familypoints;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class DisposableDurableRestoreApprovalStorePostgresTest {
    @Container static final PostgreSQLContainer POSTGRES=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("approval_rehearsal").withUsername("qa").withPassword("qa");

    private static final Instant ISSUED=Instant.parse("2026-11-18T08:00:00Z");
    private static final Instant NOW=ISSUED.plusSeconds(60);

    private record Setup(JdbcTemplate db,DataSourceTransactionManager manager,
                         long family,SecretKey key,UUID snapshot,UUID operation,
                         DisposableRestoreApprovalGate.Permit permit) {
        DisposableDurableRestoreApprovalStore store() {
            return new DisposableDurableRestoreApprovalStore(db,manager);
        }
        void consume(DisposableDurableRestoreApprovalStore store,boolean fail) {
            store.consume(permit,17,family,snapshot,operation,7,NOW,
                    Map.of(11L,41L,12L,42L),key,fail);
        }
        long rows() {
            return db.queryForObject(
                    "SELECT count(*) FROM qa_restore_approval_consumption WHERE operation_id=?",
                    Long.class,operation);
        }
    }

    private static Setup setup() throws Exception {
        var ds=new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword());
        var db=new JdbcTemplate(ds);
        // Migrate the isolated database BEFORE creating any QA-only table:
        // Flyway must not mistake a nonempty unmigrated schema for production.
        org.flywaydb.core.Flyway.configure().dataSource(ds)
                .locations("classpath:db/migration").load().migrate();
        db.execute("""
                CREATE TABLE IF NOT EXISTS qa_restore_approval_consumption (
                    operation_id uuid PRIMARY KEY,
                    source_family bigint NOT NULL,
                    target_family bigint NOT NULL,
                    snapshot_id uuid NOT NULL,
                    actor_id bigint NOT NULL,
                    target_epoch bigint NOT NULL
                )
                """);
        long family=db.queryForObject(
                "INSERT INTO families(name) VALUES (?) RETURNING id",
                Long.class,"Approval target "+UUID.randomUUID());
        var manager=new DataSourceTransactionManager(ds);
        var generator=KeyGenerator.getInstance("HmacSHA256");
        generator.init(256);
        var key=generator.generateKey();
        var snapshot=UUID.randomUUID();
        var operation=UUID.randomUUID();
        var permit=DisposableRestoreApprovalGate.syntheticIssue(
                17,family,snapshot,operation,7,0,ISSUED,ISSUED.plusSeconds(300),
                Map.of(11L,41L,12L,42L),key);
        return new Setup(db,manager,family,key,snapshot,operation,permit);
    }

    @Test void rollbackAfterReservationAllowsRetryThenRestartDeny() throws Exception {
        var a=setup();
        assertThrows(IllegalStateException.class,()->a.consume(a.store(),true));
        assertEquals(0L,a.rows());
        a.consume(a.store(),false);
        assertEquals(1L,a.rows());
        // New helper object, same durable PostgreSQL operation unique index.
        assertThrows(SecurityException.class,()->a.consume(a.store(),false));
        assertEquals(1L,a.rows());
    }

    @Test void epochMutationAndMappingSwapCannotConsume() throws Exception {
        var a=setup();
        a.db().update("UPDATE families SET data_epoch=1 WHERE id=?",a.family());
        assertThrows(SecurityException.class,()->a.consume(a.store(),false));
        assertEquals(0L,a.rows());
        a.db().update("UPDATE families SET data_epoch=0 WHERE id=?",a.family());
        var store=a.store();
        assertThrows(SecurityException.class,()->store.consume(a.permit(),17,a.family(),
                a.snapshot(),a.operation(),7,NOW,Map.of(11L,42L,12L,41L),a.key(),false));
        assertEquals(0L,a.rows());
        a.consume(store,false);
        assertEquals(1L,a.rows());
    }

    @Test void concurrentSameOperationOnlyOneCanCommit() throws Exception {
        var a=setup();
        var executor=Executors.newFixedThreadPool(2);
        var start=new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures=new ArrayList<>();
            for(int i=0;i<2;i++) {
                futures.add(executor.submit(()->{
                    start.await();
                    try {a.consume(a.store(),false);return true;}
                    catch(SecurityException rejected){return false;}
                }));
            }
            start.countDown();
            int committed=0,rejected=0;
            for(var future:futures) {
                if(future.get(20,TimeUnit.SECONDS))committed++;
                else rejected++;
            }
            assertEquals(1,committed);
            assertEquals(1,rejected);
            assertEquals(1L,a.rows());
        } finally {
            executor.shutdownNow();
        }
    }
}
