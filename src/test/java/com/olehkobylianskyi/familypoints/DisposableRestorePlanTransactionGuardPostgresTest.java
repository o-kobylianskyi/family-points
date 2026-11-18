package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.security.WorkspaceDestructivePermitContract;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Synthetic C descriptor matching is not owner issuance or a real grant.
 * All writes take place in disposable Docker PostgreSQL18 only.
 */
@Testcontainers
class DisposableRestorePlanTransactionGuardPostgresTest {
    @Container static final PostgreSQLContainer DB =
            new PostgreSQLContainer("postgres:18-alpine").withDatabaseName("plan_qa")
                    .withUsername("qa").withPassword("qa");
    private static final Instant ISSUED=Instant.parse("2026-11-18T08:00:00Z");
    private static final Instant NOW=ISSUED.plusSeconds(30);
    private record Fixture(JdbcTemplate jdbc,DataSourceTransactionManager manager,
                           long target,long neighbor,UUID operation) {
        DisposableRestorePlanTransactionGuard gate() {
            return new DisposableRestorePlanTransactionGuard(jdbc,manager);
        }
        DisposableRestorePlanFingerprint.Plan plan() {
            return new DisposableRestorePlanFingerprint.Plan(
                    17,target,2,UUID.fromString("11111111-2222-4333-8444-555555555555"),
                    "a".repeat(64),7,0,Map.of(3L,103L,4L,104L),
                    List.of(
                            new DisposableRestorePlanFingerprint.Category("POINT_TYPES",1,true),
                            new DisposableRestorePlanFingerprint.Category("TASK_EXECUTIONS",2,true),
                            new DisposableRestorePlanFingerprint.Category("POINT_LEDGER",3,true)),
                    "FAIL_ON_CONFLICT");
        }
        WorkspaceDestructivePermitContract.Scope scope() {
            var p=plan();
            var digest=new DisposableRestorePlanFingerprint().calculate(p);
            return new WorkspaceDestructivePermitContract.Scope(
                    WorkspaceDestructivePermitContract.Action.RESTORE,operation,target,7,8,
                    p.snapshot(),p.sealedManifestSha256(),digest.selectionDigest(),
                    digest.memberMappingDigest(),0,ISSUED,ISSUED.plusSeconds(300));
        }
        long count(String table) {
            if(!Set.of("qa_restore_plan_operations","qa_restore_plan_events").contains(table))
                throw new IllegalArgumentException("QA_TABLE_UNKNOWN");
            return jdbc.queryForObject(
                    "SELECT count(*) FROM "+table+" WHERE target_family=?",
                    Long.class,target);
        }
    }
    private static Fixture setup() {
        var ds=new DriverManagerDataSource(
                DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var jdbc=new JdbcTemplate(ds);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS qa_restore_plan_operations (
                    operation_id uuid PRIMARY KEY,
                    target_family bigint NOT NULL REFERENCES families(id),
                    plan_digest varchar(64) NOT NULL,
                    before_epoch bigint NOT NULL,
                    after_epoch bigint NOT NULL)
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS qa_restore_plan_events (
                    operation_id uuid PRIMARY KEY
                        REFERENCES qa_restore_plan_operations(operation_id),
                    target_family bigint NOT NULL REFERENCES families(id),
                    category varchar(64) NOT NULL)
                """);
        long neighbor=jdbc.queryForObject(
                "INSERT INTO families(name) VALUES (?) RETURNING id",
                Long.class,"QA neighbor "+UUID.randomUUID());
        long target=jdbc.queryForObject(
                "INSERT INTO families(name) VALUES (?) RETURNING id",
                Long.class,"QA restore target "+UUID.randomUUID());
        return new Fixture(jdbc,new DataSourceTransactionManager(ds),
                target,neighbor,UUID.randomUUID());
    }

    @Test void scopeBoundMutationAdvancesEpochAndCannotReplay() {
        var f=setup();var scope=f.scope();
        var receipt=f.gate().consume(f.plan(),scope,scope,NOW,false);
        assertEquals(f.operation(),receipt.operationId());
        assertEquals(0L,receipt.targetEpochBefore());
        assertEquals(1L,receipt.targetEpochAfter());
        assertEquals(1L,f.count("qa_restore_plan_operations"));
        assertEquals(1L,f.count("qa_restore_plan_events"));
        assertEquals(1L,f.jdbc().queryForObject(
                "SELECT data_epoch FROM families WHERE id=?",Long.class,f.target()));
        assertThrows(SecurityException.class,()->
                f.gate().consume(f.plan(),scope,scope,NOW,false));
        assertEquals(1L,f.count("qa_restore_plan_events"));
        assertEquals(0L,f.jdbc().queryForObject(
                "SELECT count(*) FROM qa_restore_plan_events WHERE target_family=?",
                Long.class,f.neighbor()));
    }

    @Test void lateErrorRollsBackOperationEventAndEpochThenFreshHelperCanRetry() {
        var f=setup();var scope=f.scope();
        assertThrows(IllegalStateException.class,()->
                f.gate().consume(f.plan(),scope,scope,NOW,true));
        assertEquals(0L,f.count("qa_restore_plan_operations"));
        assertEquals(0L,f.count("qa_restore_plan_events"));
        assertEquals(0L,f.jdbc().queryForObject(
                "SELECT data_epoch FROM families WHERE id=?",Long.class,f.target()));
        f.gate().consume(f.plan(),scope,scope,NOW,false);
        assertEquals(1L,f.count("qa_restore_plan_events"));
    }

    @Test void alteredMappingScopeExpiredAndResetActionNeverWrite() {
        var f=setup();var scope=f.scope();
        var changed=f.plan();
        changed=new DisposableRestorePlanFingerprint.Plan(
                changed.sourceWorkspace(),changed.targetWorkspace(),changed.schemaVersion(),
                changed.snapshot(),changed.sealedManifestSha256(),
                changed.sourceEpoch(),changed.targetExpectedEpoch(),
                Map.of(3L,104L,4L,103L),changed.categories(),changed.conflictPolicy());
        var finalChanged=changed;
        assertThrows(SecurityException.class,()->
                f.gate().consume(finalChanged,scope,scope,NOW,false));
        assertThrows(SecurityException.class,()->
                f.gate().consume(f.plan(),scope,scope,ISSUED.plusSeconds(301),false));
        var reset=new WorkspaceDestructivePermitContract.Scope(
                WorkspaceDestructivePermitContract.Action.RESET,scope.operationId(),
                scope.workspaceId(),scope.ownerAccountId(),scope.ownerMemberId(),
                scope.snapshotId(),scope.snapshotDigest(),scope.selectionDigest(),
                null,scope.dataEpoch(),scope.issuedAt(),scope.expiresAt());
        assertThrows(SecurityException.class,()->
                f.gate().consume(f.plan(),reset,reset,NOW,false));
        assertEquals(0L,f.count("qa_restore_plan_operations"));
        assertEquals(0L,f.count("qa_restore_plan_events"));
    }

    @Test void concurrentSameOperationOnlyOneTargetMutationOccurs() throws Exception {
        var f=setup();var scope=f.scope();
        var workers=Executors.newFixedThreadPool(2);
        var start=new CountDownLatch(1);
        try {
            List<Future<Boolean>> results=new ArrayList<>();
            for(int i=0;i<2;i++)results.add(workers.submit(()->{
                start.await();
                try {f.gate().consume(f.plan(),scope,scope,NOW,false);return true;}
                catch(SecurityException rejected){return false;}
            }));
            start.countDown();
            int success=0;
            for(var future:results)
                if(future.get(30,TimeUnit.SECONDS))success++;
            assertEquals(1,success);
            assertEquals(1L,f.count("qa_restore_plan_operations"));
            assertEquals(1L,f.count("qa_restore_plan_events"));
            assertEquals(1L,f.jdbc().queryForObject(
                    "SELECT data_epoch FROM families WHERE id=?",Long.class,f.target()));
        }finally {workers.shutdownNow();}
    }
}
