package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.WorkspaceEpochTransactionGuard;
import com.olehkobylianskyi.familypoints.service.WorkspaceSetupService;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
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

import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Issue18: actual PostgreSQL V45->V46 upgrade, state lifecycle and cross-family isolation. */
@Testcontainers
class WorkspaceSetupPostgresTest {
    @Container static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("setup_regression").withUsername("setup_test").withPassword("setup_test_password");

    private record Fixture(JdbcTemplate db, WorkspaceSetupService service, TransactionTemplate tx,
                           long legacyFamily, long familyA, long familyB) {}

    private Fixture fixture() {
        var source = new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        var db = new JdbcTemplate(source);
        // Reset only this disposable Testcontainers database between test methods.
        // Never use Flyway.clean() against DEV, BETA, PROD or real family data.
        Flyway.configure().dataSource(source).cleanDisabled(false).load().clean();
        var flyway = Flyway.configure().dataSource(source).locations("classpath:db/migration");
        flyway.target(MigrationVersion.fromVersion("45")).load().migrate();
        long old = db.queryForObject("INSERT INTO families(name) VALUES ('Existing family') RETURNING id",Long.class);
        assertEquals(5,Flyway.configure().dataSource(source).locations("classpath:db/migration")
                .load().migrate().migrationsExecuted,"V46 should be additive");
        assertEquals(0L,db.queryForObject(
                "SELECT count(*) FROM workspace_setup_states WHERE workspace_id=?",Long.class,old),
                "Old families must not be enrolled automatically");
        long a = db.queryForObject("INSERT INTO families(name) VALUES ('New family A') RETURNING id",Long.class);
        long b = db.queryForObject("INSERT INTO families(name) VALUES ('New family B') RETURNING id",Long.class);
        var service = new WorkspaceSetupService(db,new WorkspaceEpochTransactionGuard(db),true);
        return new Fixture(db,service,new TransactionTemplate(new DataSourceTransactionManager(source)),old,a,b);
    }

    @Test void migrationDoesNotEnrollExistingFamiliesAndCreatesOnlyNewFamilyState() {
        var f=fixture();
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
                ()->f.service.current(f.legacyFamily)).getStatusCode());
        var a=f.service.current(f.familyA);
        assertEquals("NOT_STARTED",a.status());
        assertEquals(0L,a.revision());
        assertTrue(a.steps().isEmpty());
        assertEquals(0L,a.dataEpoch());
        assertEquals("NOT_STARTED",f.service.current(f.familyB).status());
        assertEquals(0,Flyway.configure().dataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword())
                .locations("classpath:db/migration").load().migrate().migrationsExecuted);
    }

    @Test void stepChangesAreDurableAndRevisionConflictsHaveNoSideEffects() {
        var f=fixture();
        var changed=f.tx.execute(s->f.service.setStep(f.familyA,"MEMBERS",
                new WorkspaceSetupService.StepRequest("COMPLETE",0L)));
        assertNotNull(changed);
        assertEquals(1L,changed.revision());
        assertEquals("COMPLETE",f.service.current(f.familyA).steps().get("MEMBERS"));
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->f.tx.execute(s->f.service.setStep(f.familyA,"MEMBER_GROUPS",
                        new WorkspaceSetupService.StepRequest("SKIPPED",0L)))).getStatusCode());
        assertEquals(1L,f.service.current(f.familyA).revision());
        assertEquals("NOT_STARTED",f.service.current(f.familyB).status());
        assertTrue(f.service.current(f.familyB).steps().isEmpty());
    }

    @Test void cannotCompleteIncompleteSetupButCanCompleteReviewedSteps() {
        var f=fixture();
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->f.tx.execute(s->f.service.complete(f.familyA,0L))).getStatusCode());
        long revision=0;
        for(var step:WorkspaceSetupService.STEPS) {
            final long expected=revision;
            var state=f.tx.execute(s->f.service.setStep(f.familyA,step,
                    new WorkspaceSetupService.StepRequest(
                            step.equals("MEMBERS")?"COMPLETE":"SKIPPED",expected)));
            assertNotNull(state);
            revision=state.revision();
        }
        final long finalRevision=revision;
        var complete=f.tx.execute(s->f.service.complete(f.familyA,finalRevision));
        assertNotNull(complete);
        assertEquals("COMPLETED",complete.status());
        assertNotNull(complete.completedAt());
        assertEquals(finalRevision+1,complete.revision());
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->f.tx.execute(s->f.service.setStep(f.familyA,"TASK_REVIEW",
                        new WorkspaceSetupService.StepRequest("COMPLETE",finalRevision+1))))
                .getStatusCode());
        assertEquals("NOT_STARTED",f.service.current(f.familyB).status());
    }

    @Test void rollbackRestoresRevisionAndStepValues() {
        var f=fixture();
        assertThrows(IllegalStateException.class,()->f.tx.executeWithoutResult(s->{
            f.service.setStep(f.familyA,"MEMBERS",new WorkspaceSetupService.StepRequest("COMPLETE",0L));
            throw new IllegalStateException("simulated failure");
        }));
        assertEquals(0L,f.service.current(f.familyA).revision());
        assertTrue(f.service.current(f.familyA).steps().isEmpty());
    }

    @Test void staleEpochNeverChangesWizardMetadata() {
        var f=fixture();
        f.db.update("UPDATE families SET data_epoch=1 WHERE id=?",f.familyA);
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->f.tx.execute(s->f.service.setStep(f.familyA,"MEMBERS",
                        new WorkspaceSetupService.StepRequest("COMPLETE",0L)))).getStatusCode());
        assertEquals(0L,f.db.queryForObject(
                "SELECT revision FROM workspace_setup_states WHERE workspace_id=?",Long.class,f.familyA));
        assertEquals("NOT_STARTED",f.service.current(f.familyB).status());
    }

    @Test void concurrentTabsCannotBothCommitSameRevision() throws Exception {
        var f=fixture();
        var pool=Executors.newFixedThreadPool(2);
        var start=new CountDownLatch(1);
        try {
            Callable<Integer> task=()->{
                start.await(10,TimeUnit.SECONDS);
                try {
                    f.tx.execute(s->f.service.setStep(f.familyA,"MEMBERS",
                            new WorkspaceSetupService.StepRequest("COMPLETE",0L)));
                    return 200;
                } catch(ResponseStatusException ex) { return ex.getStatusCode().value(); }
            };
            var one=pool.submit(task);
            var two=pool.submit(task);
            start.countDown();
            var first=one.get(20,TimeUnit.SECONDS);
            var second=two.get(20,TimeUnit.SECONDS);
            assertEquals(1,(first==200?1:0)+(second==200?1:0));
            assertEquals(1,(first==409?1:0)+(second==409?1:0));
            assertEquals(1L,f.service.current(f.familyA).revision());
        } finally {pool.shutdownNow();}
    }
}
