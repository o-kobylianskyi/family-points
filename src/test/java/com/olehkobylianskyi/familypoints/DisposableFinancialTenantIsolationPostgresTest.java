package com.olehkobylianskyi.familypoints;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FIN-09/10: QA-only disposable financial ledger isolation.
 * No production schema, user family, point table or native restore is modified.
 */
@Testcontainers
class DisposableFinancialTenantIsolationPostgresTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("fin_isolation")
                    .withUsername("qa").withPassword("qa");

    private JdbcTemplate db;
    private TransactionTemplate tx;

    @BeforeEach
    void setup() {
        var ds = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword());
        db = new JdbcTemplate(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        db.execute("""
                CREATE TABLE IF NOT EXISTS qa_fin09_ledger (
                    family_id bigint NOT NULL CHECK (family_id>0),
                    operation_id uuid NOT NULL,
                    id bigint NOT NULL CHECK (id>0),
                    member_id bigint NOT NULL CHECK (member_id>0),
                    currency_id bigint NOT NULL CHECK (currency_id>0),
                    amount integer NOT NULL,
                    type text NOT NULL,
                    source_type text NOT NULL,
                    source_id bigint,
                    PRIMARY KEY(family_id,operation_id,id)
                )
                """);
        db.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS qa_fin09_exactly_once
                ON qa_fin09_ledger (family_id,operation_id,id)
                """);
    }

    private static final List<DisposableFinancialMappingParityTest.Row> HISTORY=List.of(
            new DisposableFinancialMappingParityTest.Row(
                    101,14,23,5,"EARN","TASK",142L),
            new DisposableFinancialMappingParityTest.Row(
                    102,14,23,-3,"SPEND","REWARD_PURCHASE",151L),
            new DisposableFinancialMappingParityTest.Row(
                    103,15,28,7,"ADJUSTMENT","MANUAL",null));

    private static List<DisposableFinancialMappingParityTest.Row> source() {
        return List.of(new DisposableFinancialMappingParityTest.Row(
                    1,4,3,5,"EARN","TASK",42L),
                new DisposableFinancialMappingParityTest.Row(
                    2,4,3,-3,"SPEND","REWARD_PURCHASE",51L),
                new DisposableFinancialMappingParityTest.Row(
                    3,5,8,7,"ADJUSTMENT","MANUAL",null));
    }

    private static DisposableFinancialMappingParityTest.Mapping mapping() {
        return new DisposableFinancialMappingParityTest.Mapping(
                Map.of(1L,101L,2L,102L,3L,103L),
                Map.of(4L,14L,5L,15L),Map.of(3L,23L,8L,28L),
                Map.of(42L,142L),Map.of(51L,151L));
    }

    private void write(long family,UUID operation,
            List<DisposableFinancialMappingParityTest.Row> rows,int failAfter) {
        tx.executeWithoutResult(status -> {
            for(int i=0;i<rows.size();i++) {
                var row=rows.get(i);
                db.update("""
                        INSERT INTO qa_fin09_ledger
                          (family_id,operation_id,id,member_id,currency_id,amount,
                           type,source_type,source_id)
                        VALUES (?,?,?,?,?,?,?,?,?)
                        """,family,operation,row.id(),row.member(),row.currency(),
                        row.amount(),row.type(),row.sourceType(),row.sourceId());
                if(i==failAfter) throw new IllegalStateException("FIN09_INJECTED_FAILURE");
            }
        });
    }

    private List<DisposableFinancialMappingParityTest.Row> read(long family,UUID operation) {
        return db.query("""
                SELECT id,member_id,currency_id,amount,type,source_type,source_id
                FROM qa_fin09_ledger
                WHERE family_id=? AND operation_id=? ORDER BY id
                """,(rs,i)->new DisposableFinancialMappingParityTest.Row(
                    rs.getLong("id"),rs.getLong("member_id"),rs.getLong("currency_id"),
                    rs.getInt("amount"),rs.getString("type"),
                    rs.getString("source_type"),rs.getObject("source_id",Long.class)),
                family,operation);
    }

    private void assertParity(long family,UUID operation) {
        assertEquals(Map.of("14:23",2L,"15:28",7L),
                DisposableFinancialMappingParityTest.reconcile(
                        source(),read(family,operation),mapping()));
    }

    @Test
    void sameOperationUuidInDifferentFamiliesCannotMixHistories() {
        UUID operation=UUID.randomUUID();
        write(101,operation,HISTORY,-1);
        write(202,operation,HISTORY,-1);
        assertParity(101,operation);
        assertParity(202,operation);
        assertEquals(6L,db.queryForObject("""
                SELECT count(*) FROM qa_fin09_ledger WHERE operation_id=?
                """,Long.class,operation));
    }

    @Test
    void sameFamilyIndependentOperationsRemainStrictlyIsolated() {
        UUID first=UUID.randomUUID(),second=UUID.randomUUID();
        write(101,first,HISTORY,-1);
        write(101,second,HISTORY,-1);
        assertParity(101,first);
        assertParity(101,second);
        assertEquals(6L,db.queryForObject("""
                SELECT count(*) FROM qa_fin09_ledger WHERE family_id=?
                AND operation_id IN (?,?)
                """,Long.class,101,first,second));
    }

    @Test
    void rollbackOfOneFamilyPreservesOtherFamilyWithSharedOperationId() {
        UUID operation=UUID.randomUUID();
        write(202,operation,HISTORY,-1);
        List<DisposableFinancialMappingParityTest.Row> baseline=read(202,operation);
        assertThrows(IllegalStateException.class,
                () -> write(101,operation,HISTORY,1));
        assertTrue(read(101,operation).isEmpty());
        assertEquals(baseline,read(202,operation));
        assertParity(202,operation);
    }

    @Test
    void aSecondWriteToSameFamilyAndOperationIsFullyRolledBack() {
        UUID operation=UUID.randomUUID();
        write(101,operation,HISTORY,-1);
        var before=read(101,operation);
        assertThrows(DataAccessException.class,()->write(101,operation,HISTORY,-1));
        assertEquals(before,read(101,operation));
        assertParity(101,operation);
    }

    @Test
    void concurrentDuplicateSubmissionCanCommitOnlyOneHistory() throws Exception {
        UUID operation=UUID.randomUUID();
        var executor=Executors.newFixedThreadPool(2);
        var start=new CountDownLatch(1);
        try {
            var futures=new ArrayList<Future<Boolean>>();
            for(int i=0;i<2;i++) {
                futures.add(executor.submit(()->{
                    start.await();
                    try {
                        write(101,operation,HISTORY,-1);
                        return true;
                    } catch(DataAccessException duplicate) {return false;}
                }));
            }
            start.countDown();
            int commits=0;
            for(var future:futures) if(future.get(30,TimeUnit.SECONDS)) commits++;
            assertEquals(1,commits);
            assertEquals(3,read(101,operation).size());
            assertParity(101,operation);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void targetHistoryCannotBeSilentlySwappedAcrossCurrencies() {
        UUID operation=UUID.randomUUID();
        var tampered=List.of(HISTORY.get(0),HISTORY.get(1),
                new DisposableFinancialMappingParityTest.Row(
                        103,15,23,7,"ADJUSTMENT","MANUAL",null));
        write(101,operation,tampered,-1);
        assertThrows(SecurityException.class,()->assertParity(101,operation));
    }

    @Test
    void injectedFailureDoesNotLeavePartialFinancialJournal() {
        UUID operation=UUID.randomUUID();
        assertThrows(IllegalStateException.class,
                () -> write(101,operation,HISTORY,0));
        assertTrue(read(101,operation).isEmpty());
        write(101,operation,HISTORY,-1);
        assertParity(101,operation);
    }
}
