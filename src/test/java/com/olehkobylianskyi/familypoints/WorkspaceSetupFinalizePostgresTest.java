package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.controller.WorkspaceTemplateController;
import com.olehkobylianskyi.familypoints.service.WorkspaceEpochTransactionGuard;
import com.olehkobylianskyi.familypoints.service.WorkspaceSetupService;
import com.olehkobylianskyi.familypoints.service.WorkspaceSetupFinalizeService;
import com.olehkobylianskyi.familypoints.service.WorkspaceTemplateImportReceiptService;
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

/** V49 isolated disposable PostgreSQL: atomic closure, durable replay, rollback and conflict. */
@Testcontainers
class WorkspaceSetupFinalizePostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("setup_finalize").withUsername("finalize_test").withPassword("finalize_password");

    private record F(JdbcTemplate db,TransactionTemplate tx,WorkspaceSetupFinalizeService service,
            WorkspaceSetupService setup, WorkspaceTemplateImportReceiptService imports,long workspace) {}
    private F fixture() {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long workspace=db.queryForObject("INSERT INTO families(name) VALUES ('Finalize test') RETURNING id",Long.class);
        var setup=mock(WorkspaceSetupService.class);
        var imports=mock(WorkspaceTemplateImportReceiptService.class);
        Map<String,String> steps=new LinkedHashMap<>();
        for(String key:WorkspaceSetupService.STEPS)steps.put(key,"COMPLETE");
        when(setup.current(workspace)).thenReturn(new WorkspaceSetupService.SetupState(
                workspace,0,7,"IN_PROGRESS",steps,null));
        when(setup.complete(workspace,7)).thenReturn(new WorkspaceSetupService.SetupState(
                workspace,0,8,"COMPLETED",steps,null));
        when(imports.importOnce(eq(workspace),any(UUID.class),any())).thenAnswer(inv->{
            var op=inv.<UUID>getArgument(1);
            var codes=inv.<WorkspaceTemplateController.ImportRequest>getArgument(2).selectedCodes();
            return new WorkspaceTemplateImportReceiptService.Result(op,false,
                    new WorkspaceTemplateController.ImportPreview(codes,List.of(),codes));
        });
        return new F(db,new TransactionTemplate(new DataSourceTransactionManager(ds)),
                new WorkspaceSetupFinalizeService(db,new WorkspaceEpochTransactionGuard(db),setup,imports),
                setup,imports,workspace);
    }

    @Test void finalizePersistsReplaysAndRejectsChangedSelection() {
        var f=fixture();
        UUID key=UUID.randomUUID();
        var req=new WorkspaceSetupFinalizeService.Request(7L,List.of("sample.task"));
        var first=f.tx.execute(s->f.service.finalizeWizard(f.workspace,key,req));
        var second=f.tx.execute(s->f.service.finalizeWizard(f.workspace,key,req));
        assertNotNull(first);assertNotNull(second);
        assertEquals("COMPLETED",first.setupStatus());
        assertEquals(8L,first.revision());
        assertFalse(first.replayed());assertTrue(second.replayed());
        assertEquals(first.importResult(),second.importResult());
        assertEquals(first.importResult(),f.service.receipt(f.workspace,key).importResult());
        verify(f.imports,times(1)).importOnce(eq(f.workspace),eq(key),any());
        verify(f.setup,times(1)).complete(f.workspace,7);
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->f.tx.execute(s->f.service.finalizeWizard(f.workspace,key,
                        new WorkspaceSetupFinalizeService.Request(7L,List.of("other.task")))))
                .getStatusCode());
        assertEquals(1L,f.db.queryForObject(
                "SELECT count(*) FROM workspace_setup_finalize_receipts WHERE workspace_id=?",
                Long.class,f.workspace));
    }

    @Test void incompleteStepsDoNotImportOrComplete() {
        var f=fixture();
        when(f.setup.current(f.workspace)).thenReturn(new WorkspaceSetupService.SetupState(
                f.workspace,0,7,"IN_PROGRESS",Map.of("MEMBERS","COMPLETE"),null));
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->f.tx.execute(s->f.service.finalizeWizard(f.workspace,UUID.randomUUID(),
                        new WorkspaceSetupFinalizeService.Request(7L,List.of())))).getStatusCode());
        verifyNoInteractions(f.imports);
        verify(f.setup,never()).complete(anyLong(),anyLong());
    }

    @Test void staleRevisionRejectsBeforeImportAndDoesNotPersistReceipt() {
        var f=fixture();
        var ex=assertThrows(ResponseStatusException.class,
                ()->f.tx.execute(s->f.service.finalizeWizard(f.workspace,UUID.randomUUID(),
                        new WorkspaceSetupFinalizeService.Request(6L,List.of()))));
        assertEquals(HttpStatus.CONFLICT,ex.getStatusCode());
        verifyNoInteractions(f.imports);
        verify(f.setup,never()).complete(anyLong(),anyLong());
        assertEquals(0L,f.db.queryForObject(
                "SELECT count(*) FROM workspace_setup_finalize_receipts WHERE workspace_id=?",
                Long.class,f.workspace));
    }

    @Test void completedWizardCannotFinalizeAgainWithNewKey() {
        var f=fixture();
        when(f.setup.current(f.workspace)).thenReturn(new WorkspaceSetupService.SetupState(
                f.workspace,0,8,"COMPLETED",Map.of(),null));
        var ex=assertThrows(ResponseStatusException.class,
                ()->f.tx.execute(s->f.service.finalizeWizard(f.workspace,UUID.randomUUID(),
                        new WorkspaceSetupFinalizeService.Request(8L,List.of()))));
        assertEquals(HttpStatus.CONFLICT,ex.getStatusCode());
        verifyNoInteractions(f.imports);
        verify(f.setup,never()).complete(anyLong(),anyLong());
    }

    @Test void importFailureRollsBackFamilyMutationAndReceipt() {
        var f=fixture();
        when(f.imports.importOnce(eq(f.workspace),any(UUID.class),any())).thenAnswer(inv->{
            f.db.update("UPDATE families SET name='temporary' WHERE id=?",f.workspace);
            throw new IllegalStateException("simulated import failure");
        });
        assertThrows(IllegalStateException.class,()->f.tx.execute(s->f.service.finalizeWizard(
                f.workspace,UUID.randomUUID(),new WorkspaceSetupFinalizeService.Request(7L,List.of()))));
        assertEquals("Finalize test",f.db.queryForObject(
                "SELECT name FROM families WHERE id=?",String.class,f.workspace));
        assertEquals(0L,f.db.queryForObject(
                "SELECT count(*) FROM workspace_setup_finalize_receipts WHERE workspace_id=?",
                Long.class,f.workspace));
    }

    @Test void sameKeyConcurrentFinalizationOnlyCompletesOnce() throws Exception {
        var f=fixture();
        var calls=new AtomicInteger();
        when(f.imports.importOnce(eq(f.workspace),any(UUID.class),any())).thenAnswer(inv->{
            calls.incrementAndGet();
            UUID key=inv.getArgument(1);
            return new WorkspaceTemplateImportReceiptService.Result(key,false,
                    new WorkspaceTemplateController.ImportPreview(List.of(),List.of(),List.of()));
        });
        UUID key=UUID.randomUUID();
        var pool=Executors.newFixedThreadPool(2);
        var latch=new CountDownLatch(1);
        try {
            Callable<WorkspaceSetupFinalizeService.Result> job=()->{
                latch.await(10,TimeUnit.SECONDS);
                return f.tx.execute(s->f.service.finalizeWizard(f.workspace,key,
                        new WorkspaceSetupFinalizeService.Request(7L,List.of())));
            };
            var a=pool.submit(job);var b=pool.submit(job);
            latch.countDown();
            var one=a.get(20,TimeUnit.SECONDS);var two=b.get(20,TimeUnit.SECONDS);
            assertNotEquals(one.replayed(),two.replayed());
            assertEquals(1,calls.get());
        } finally {pool.shutdownNow();}
    }
}
