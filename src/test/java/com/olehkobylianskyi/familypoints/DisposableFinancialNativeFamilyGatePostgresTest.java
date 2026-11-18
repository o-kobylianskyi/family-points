package com.olehkobylianskyi.familypoints;

import org.flywaydb.core.Flyway;
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
 * FIN-12/13: financial history rehearsal against a Flyway-migrated,
 * disposable database. Family identity comes from the REAL families table.
 * Journal remains qa_fin12_*; this test never touches production point ledger
 * and does not claim native Restore authorization.
 */
@Testcontainers
class DisposableFinancialNativeFamilyGatePostgresTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("fin12_families")
                    .withUsername("qa").withPassword("qa");

    private JdbcTemplate db;
    private TransactionTemplate tx;

    @BeforeEach
    void setup() {
        var ds=new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword());
        Flyway.configure().dataSource(ds)
                .locations("classpath:db/migration").load().migrate();
        db=new JdbcTemplate(ds);
        tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        db.execute("""
                CREATE TABLE IF NOT EXISTS qa_fin12_financial_journal (
                    family_id bigint NOT NULL REFERENCES families(id),
                    operation_id uuid NOT NULL,
                    ledger_id bigint NOT NULL CHECK (ledger_id>0),
                    member_id bigint NOT NULL CHECK (member_id>0),
                    currency_id bigint NOT NULL CHECK (currency_id>0),
                    amount integer NOT NULL,
                    txn_type text NOT NULL,
                    source_type text NOT NULL,
                    source_id bigint,
                    PRIMARY KEY (family_id,operation_id,ledger_id)
                )
                """);
        db.execute("""
                CREATE TABLE IF NOT EXISTS qa_fin12_operation_claim (
                    operation_id uuid PRIMARY KEY,
                    family_id bigint NOT NULL REFERENCES families(id),
                    epoch bigint NOT NULL
                )
                """);
    }

    private long family() {
        return db.queryForObject(
                "INSERT INTO families(name) VALUES (?) RETURNING id",
                Long.class,"FIN-12 disposable "+UUID.randomUUID());
    }

    private static List<DisposableFinancialMappingParityTest.Row> source() {
        return List.of(
                new DisposableFinancialMappingParityTest.Row(
                        1,4,3,5,"EARN","TASK",42L),
                new DisposableFinancialMappingParityTest.Row(
                        2,4,3,-3,"SPEND","REWARD_PURCHASE",51L),
                new DisposableFinancialMappingParityTest.Row(
                        3,5,8,7,"ADJUSTMENT","MANUAL",null));
    }

    private static List<DisposableFinancialMappingParityTest.Row> persisted() {
        return List.of(
                new DisposableFinancialMappingParityTest.Row(
                        101,14,23,5,"EARN","TASK",142L),
                new DisposableFinancialMappingParityTest.Row(
                        102,14,23,-3,"SPEND","REWARD_PURCHASE",151L),
                new DisposableFinancialMappingParityTest.Row(
                        103,15,28,7,"ADJUSTMENT","MANUAL",null));
    }

    private static DisposableFinancialMappingParityTest.Mapping mapping() {
        return new DisposableFinancialMappingParityTest.Mapping(
                Map.of(1L,101L,2L,102L,3L,103L),
                Map.of(4L,14L,5L,15L),
                Map.of(3L,23L,8L,28L),
                Map.of(42L,142L),Map.of(51L,151L));
    }

    private void claimAndWrite(long family,UUID operation,long expectedEpoch,
            List<DisposableFinancialMappingParityTest.Row> rows,int failAfter) {
        tx.executeWithoutResult(status->{
            Long epoch=db.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=? FOR UPDATE",
                    Long.class,family);
            if(epoch==null || epoch!=expectedEpoch)
                throw new SecurityException("FIN12_STALE_TARGET_EPOCH");
            db.update("""
                    INSERT INTO qa_fin12_operation_claim(operation_id,family_id,epoch)
                    VALUES (?,?,?)
                    """,operation,family,expectedEpoch);
            for(int i=0;i<rows.size();i++) {
                var row=rows.get(i);
                db.update("""
                        INSERT INTO qa_fin12_financial_journal
                          (family_id,operation_id,ledger_id,member_id,currency_id,
                           amount,txn_type,source_type,source_id)
                        VALUES (?,?,?,?,?,?,?,?,?)
                        """,family,operation,row.id(),row.member(),row.currency(),
                        row.amount(),row.type(),row.sourceType(),row.sourceId());
                if(i==failAfter)
                    throw new IllegalStateException("FIN12_INJECTED_ATOMIC_FAILURE");
            }
        });
    }

    private List<DisposableFinancialMappingParityTest.Row> read(long family,UUID operation) {
        return db.query("""
                SELECT ledger_id,member_id,currency_id,amount,txn_type,
                       source_type,source_id
                FROM qa_fin12_financial_journal
                WHERE family_id=? AND operation_id=? ORDER BY ledger_id
                """,(rs,i)->new DisposableFinancialMappingParityTest.Row(
                rs.getLong("ledger_id"),rs.getLong("member_id"),
                rs.getLong("currency_id"),rs.getInt("amount"),
                rs.getString("txn_type"),rs.getString("source_type"),
                rs.getObject("source_id",Long.class)),family,operation);
    }

    private void assertParity(long family,UUID operation) {
        assertEquals(Map.of("14:23",2L,"15:28",7L),
                DisposableFinancialMappingParityTest.reconcile(
                        source(),read(family,operation),mapping()));
    }

    private long countClaims(UUID operation) {
        return db.queryForObject(
                "SELECT count(*) FROM qa_fin12_operation_claim WHERE operation_id=?",
                Long.class,operation);
    }

    @Test
    void realFamiliesTableAnchorsSyntheticLedgerAndExactProvenance() {
        long family=family();
        UUID operation=UUID.randomUUID();
        claimAndWrite(family,operation,0,persisted(),-1);
        assertParity(family,operation);
        assertEquals(1L,countClaims(operation));
    }

    @Test
    void failedMidJournalTransactionRollsBackClaimAndAllFinancialRows() {
        long family=family();
        UUID operation=UUID.randomUUID();
        assertThrows(IllegalStateException.class,
                ()->claimAndWrite(family,operation,0,persisted(),1));
        assertEquals(0L,countClaims(operation));
        assertTrue(read(family,operation).isEmpty());
        claimAndWrite(family,operation,0,persisted(),-1);
        assertParity(family,operation);
        assertEquals(1L,countClaims(operation));
    }

    @Test
    void replayCannotDoubleChargeTheSameOperationAfterCommit() {
        long family=family();
        UUID operation=UUID.randomUUID();
        claimAndWrite(family,operation,0,persisted(),-1);
        assertThrows(DataAccessException.class,
                ()->claimAndWrite(family,operation,0,persisted(),-1));
        assertParity(family,operation);
        assertEquals(3,read(family,operation).size());
        assertEquals(1L,countClaims(operation));
    }

    @Test
    void staleEpochNeverPersistsClaimOrFinancialHistory() {
        long family=family();
        UUID operation=UUID.randomUUID();
        db.update("UPDATE families SET data_epoch=1 WHERE id=?",family);
        assertThrows(SecurityException.class,
                ()->claimAndWrite(family,operation,0,persisted(),-1));
        assertEquals(0L,countClaims(operation));
        assertTrue(read(family,operation).isEmpty());
    }

    @Test
    void secondFamilyCannotReuseOperationIdOrChangeFirstFamilyHistory() {
        long first=family(),second=family();
        UUID operation=UUID.randomUUID();
        claimAndWrite(first,operation,0,persisted(),-1);
        var immutable=read(first,operation);
        assertThrows(DataAccessException.class,
                ()->claimAndWrite(second,operation,0,persisted(),-1));
        assertEquals(immutable,read(first,operation));
        assertTrue(read(second,operation).isEmpty());
        assertParity(first,operation);
    }

    @Test
    void concurrentReplayMustCommitOneClaimAndOneExactJournal() throws Exception {
        long family=family();
        UUID operation=UUID.randomUUID();
        var executor=Executors.newFixedThreadPool(2);
        var start=new CountDownLatch(1);
        try {
            var futures=new ArrayList<Future<Boolean>>();
            for(int i=0;i<2;i++)
                futures.add(executor.submit(()->{
                    start.await();
                    try {
                        claimAndWrite(family,operation,0,persisted(),-1);
                        return true;
                    }catch(DataAccessException duplicate) {
                        return false;
                    }
                }));
            start.countDown();
            int successful=0;
            for(var future:futures)
                if(future.get(30,TimeUnit.SECONDS)) successful++;
            assertEquals(1,successful);
            assertEquals(1L,countClaims(operation));
            assertEquals(3,read(family,operation).size());
            assertParity(family,operation);
        }finally {
            executor.shutdownNow();
        }
    }

    @Test
    void compensatedTamperingCannotPassExactFinancialParity() {
        long family=family();
        UUID operation=UUID.randomUUID();
        var tampered=List.of(
                new DisposableFinancialMappingParityTest.Row(
                        101,14,23,6,"EARN","TASK",142L),
                new DisposableFinancialMappingParityTest.Row(
                        102,14,23,-4,"SPEND","REWARD_PURCHASE",151L),
                persisted().get(2));
        claimAndWrite(family,operation,0,tampered,-1);
        assertThrows(SecurityException.class,()->assertParity(family,operation));
    }
}
