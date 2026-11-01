package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.controller.WorkspaceTemplateController;
import com.olehkobylianskyi.familypoints.service.*;
import org.flywaydb.core.Flyway;
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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real V46/V47/V49 persistence and setup service, with isolated catalog writer. No real user data. */
@Testcontainers
class WorkspaceSetupFinalizeEndToEndPostgresTest {
    @Container static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("setup_e2e").withUsername("setup_e2e").withPassword("setup_password");

    @Test void verifiedStepsImportAndCompletionAreAtomicDurableAndTenantScoped() {
        var ds = new DriverManagerDataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db = new JdbcTemplate(ds);
        long family = db.queryForObject("INSERT INTO families(name) VALUES ('E2E A') RETURNING id",Long.class);
        long other = db.queryForObject("INSERT INTO families(name) VALUES ('E2E B') RETURNING id",Long.class);
        var tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        var guard = new WorkspaceEpochTransactionGuard(db);
        var setup = new WorkspaceSetupService(db,guard,true);
        var catalog = mock(WorkspaceTemplateController.class);
        var calls = new AtomicInteger();
        when(catalog.importSelected(eq(family),any())).thenAnswer(inv -> {
            calls.incrementAndGet();
            var codes=inv.<WorkspaceTemplateController.ImportRequest>getArgument(1).selectedCodes();
            db.update("UPDATE families SET name='Imported private template' WHERE id=?",family);
            return new WorkspaceTemplateController.ImportPreview(codes,List.of(),codes);
        });
        var importService = new WorkspaceTemplateImportReceiptService(db,guard,catalog);
        var finalizeService = new WorkspaceSetupFinalizeService(db,guard,setup,importService);
        var before=setup.current(family);
        assertEquals("NOT_STARTED",before.status());
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                () -> tx.execute(s -> finalizeService.finalizeWizard(family,UUID.randomUUID(),
                        new WorkspaceSetupFinalizeService.Request(0L,List.of("task.a"))))).getStatusCode());
        long revision=0;
        for(var step:WorkspaceSetupService.STEPS) {
            final long expected=revision;
            var changed=tx.execute(s->setup.setStep(family,step,
                    new WorkspaceSetupService.StepRequest("COMPLETE",expected)));
            assertNotNull(changed);
            revision=changed.revision();
        }
        UUID op=UUID.randomUUID();
        var request=new WorkspaceSetupFinalizeService.Request(revision,List.of("task.a","reward.b"));
        var first=tx.execute(s->finalizeService.finalizeWizard(family,op,request));
        assertNotNull(first);
        assertFalse(first.replayed());
        assertEquals("COMPLETED",first.setupStatus());
        assertEquals(8L,first.revision());
        assertEquals(2,first.importResult().toImport().size());
        assertEquals(1,calls.get());
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM workspace_template_import_receipts WHERE workspace_id=?",Long.class,family));
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM workspace_setup_finalize_receipts WHERE workspace_id=?",Long.class,family));
        var replay=tx.execute(s->finalizeService.finalizeWizard(family,op,request));
        assertNotNull(replay);
        assertTrue(replay.replayed());
        assertEquals(first.importResult(),replay.importResult());
        assertEquals(1,calls.get());
        assertEquals(first.importResult(),finalizeService.receipt(family,op).importResult());
        assertEquals("NOT_STARTED",setup.current(other).status());
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
                () -> finalizeService.receipt(other,op)).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                () -> tx.execute(s -> finalizeService.finalizeWizard(family,op,
                        new WorkspaceSetupFinalizeService.Request(7L,List.of("other"))))).getStatusCode());
    }

    @Test void importRollbackLeavesActualSetupUncompleted() {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long family=db.queryForObject("INSERT INTO families(name) VALUES ('Rollback E2E') RETURNING id",Long.class);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var guard=new WorkspaceEpochTransactionGuard(db);
        var setup=new WorkspaceSetupService(db,guard,true);
        long revision=0;
        for(var step:WorkspaceSetupService.STEPS) {
            final long expected=revision;
            revision=Objects.requireNonNull(tx.execute(s->setup.setStep(family,step,
                    new WorkspaceSetupService.StepRequest("COMPLETE",expected)))).revision();
        }
        var catalog=mock(WorkspaceTemplateController.class);
        when(catalog.importSelected(eq(family),any())).thenAnswer(inv->{
            db.update("UPDATE families SET name='SHOULD ROLLBACK' WHERE id=?",family);
            throw new IllegalStateException("Injected failure");
        });
        var finalizeService=new WorkspaceSetupFinalizeService(db,guard,setup,
                new WorkspaceTemplateImportReceiptService(db,guard,catalog));
        final long expected=revision;
        assertThrows(IllegalStateException.class,()->tx.execute(s->
                finalizeService.finalizeWizard(family,UUID.randomUUID(),
                        new WorkspaceSetupFinalizeService.Request(expected,List.of("task.a")))));
        assertEquals("IN_PROGRESS",setup.current(family).status());
        assertEquals(expected,setup.current(family).revision());
        assertEquals("Rollback E2E",db.queryForObject("SELECT name FROM families WHERE id=?",String.class,family));
        assertEquals(0L,db.queryForObject("SELECT count(*) FROM workspace_setup_finalize_receipts WHERE workspace_id=?",Long.class,family));
        assertEquals(0L,db.queryForObject("SELECT count(*) FROM workspace_template_import_receipts WHERE workspace_id=?",Long.class,family));
    }
}
