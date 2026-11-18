package com.olehkobylianskyi.familypoints;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FIN-07. Disposable PostgreSQL ledger parity and rollback rehearsal.
 * Only qa_fin07_* tables in a Testcontainers database are written.
 * NOT a production Restore executor, financial endpoint or release permit.
 */
@Testcontainers
class DisposableFinancialLedgerPostgresParityTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("fin07_rehearsal")
                    .withUsername("qa").withPassword("qa");

    private JdbcTemplate db;
    private TransactionTemplate tx;

    @BeforeEach
    void setup() {
        var ds = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        db = new JdbcTemplate(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        db.execute("""
                CREATE TABLE IF NOT EXISTS qa_fin07_ledger (
                    operation_id uuid NOT NULL,
                    id bigint NOT NULL,
                    member_id bigint NOT NULL,
                    point_type_id bigint NOT NULL,
                    amount integer NOT NULL,
                    type text NOT NULL,
                    source_type text NOT NULL,
                    source_id bigint,
                    PRIMARY KEY(operation_id,id)
                )
                """);
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

    private static DisposableFinancialMappingParityTest.Mapping mapping() {
        return new DisposableFinancialMappingParityTest.Mapping(
                Map.of(1L,101L,2L,102L,3L,103L),
                Map.of(4L,14L,5L,15L),
                Map.of(3L,23L,8L,28L),
                Map.of(42L,142L),
                Map.of(51L,151L));
    }

    private static List<DisposableFinancialMappingParityTest.Row> expected() {
        return List.of(
                new DisposableFinancialMappingParityTest.Row(
                        101,14,23,5,"EARN","TASK",142L),
                new DisposableFinancialMappingParityTest.Row(
                        102,14,23,-3,"SPEND","REWARD_PURCHASE",151L),
                new DisposableFinancialMappingParityTest.Row(
                        103,15,28,7,"ADJUSTMENT","MANUAL",null));
    }

    private void insert(UUID operation, DisposableFinancialMappingParityTest.Row row) {
        db.update("""
                INSERT INTO qa_fin07_ledger
                    (operation_id,id,member_id,point_type_id,amount,type,source_type,source_id)
                VALUES (?,?,?,?,?,?,?,?)
                """, operation,row.id(),row.member(),row.currency(),row.amount(),
                row.type(),row.sourceType(),row.sourceId());
    }

    private List<DisposableFinancialMappingParityTest.Row> read(UUID operation) {
        return db.query("""
                SELECT id,member_id,point_type_id,amount,type,source_type,source_id
                FROM qa_fin07_ledger WHERE operation_id=? ORDER BY id
                """, (rs,n) -> new DisposableFinancialMappingParityTest.Row(
                        rs.getLong("id"),rs.getLong("member_id"),
                        rs.getLong("point_type_id"),rs.getInt("amount"),
                        rs.getString("type"),rs.getString("source_type"),
                        (Long)rs.getObject("source_id")), operation);
    }

    private void commit(UUID operation, List<DisposableFinancialMappingParityTest.Row> rows,
                        boolean failAfterSecond) {
        tx.executeWithoutResult(status -> {
            for (int i=0;i<rows.size();i++) {
                insert(operation,rows.get(i));
                if (failAfterSecond && i==1)
                    throw new IllegalStateException("FIN07_INJECTED_ROLLBACK");
            }
        });
    }

    @Test
    void committedFinancialRowsMatchExactSourceMappingAndPartitionedBalances() {
        UUID operation=UUID.randomUUID();
        commit(operation,expected(),false);
        assertEquals(Map.of("14:23",2L,"15:28",7L),
                DisposableFinancialMappingParityTest.reconcile(
                        source(),read(operation),mapping()));
    }

    @Test
    void failedWriteRollsBackAllRowsThenCleanRetryReconciles() {
        UUID operation=UUID.randomUUID();
        assertThrows(IllegalStateException.class,
                () -> commit(operation,expected(),true));
        assertTrue(read(operation).isEmpty());
        commit(operation,expected(),false);
        assertEquals(3,read(operation).size());
        assertEquals(Map.of("14:23",2L,"15:28",7L),
                DisposableFinancialMappingParityTest.reconcile(
                        source(),read(operation),mapping()));
    }

    @Test
    void duplicateReplayCannotInsertAnotherCopyOfLedgerHistory() {
        UUID operation=UUID.randomUUID();
        commit(operation,expected(),false);
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> commit(operation,expected(),false));
        assertEquals(3,read(operation).size());
        assertEquals(Map.of("14:23",2L,"15:28",7L),
                DisposableFinancialMappingParityTest.reconcile(
                        source(),read(operation),mapping()));
    }

    @Test
    void equalTotalButAlteredProvenanceFailsAgainstDatabaseRows() {
        UUID operation=UUID.randomUUID();
        var changed=List.of(
                new DisposableFinancialMappingParityTest.Row(
                        101,14,23,6,"EARN","TASK",142L),
                new DisposableFinancialMappingParityTest.Row(
                        102,14,23,-4,"SPEND","REWARD_PURCHASE",151L),
                expected().get(2));
        commit(operation,changed,false);
        assertEquals(2L,db.queryForObject("""
                SELECT COALESCE(SUM(amount),0) FROM qa_fin07_ledger
                WHERE operation_id=? AND member_id=14 AND point_type_id=23
                """,Long.class,operation));
        assertThrows(SecurityException.class,() ->
                DisposableFinancialMappingParityTest.reconcile(
                        source(),read(operation),mapping()));
    }

    @Test
    void isolatedOperationNeverChangesOtherOperationHistory() {
        UUID preserved=UUID.randomUUID();
        UUID failed=UUID.randomUUID();
        commit(preserved,expected(),false);
        var baseline=read(preserved);
        assertThrows(IllegalStateException.class,
                () -> commit(failed,expected(),true));
        assertTrue(read(failed).isEmpty());
        assertEquals(baseline,read(preserved));
        assertEquals(Map.of("14:23",2L,"15:28",7L),
                DisposableFinancialMappingParityTest.reconcile(
                        source(),read(preserved),mapping()));
    }
}
