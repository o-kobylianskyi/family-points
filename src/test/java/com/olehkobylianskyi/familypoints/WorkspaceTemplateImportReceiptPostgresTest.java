package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.controller.WorkspaceTemplateController;
import com.olehkobylianskyi.familypoints.service.WorkspaceEpochTransactionGuard;
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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** V47 receipts, real transactional rollback and replay on disposable PostgreSQL only. */
@Testcontainers
class WorkspaceTemplateImportReceiptPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("import_receipts").withUsername("receipt_test").withPassword("receipt_test_password");

    @Test void durableReceiptIsScopedAndReplayableButRejectsChangedPayload() {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long a=db.queryForObject("INSERT INTO families(name) VALUES ('Receipt A') RETURNING id",Long.class);
        long b=db.queryForObject("INSERT INTO families(name) VALUES ('Receipt B') RETURNING id",Long.class);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var template=mock(WorkspaceTemplateController.class);
        var calls=new AtomicInteger();
        when(template.importSelected(eq(a),any())).thenAnswer(invocation->{
            calls.incrementAndGet();
            WorkspaceTemplateController.ImportRequest req=invocation.getArgument(1);
            return new WorkspaceTemplateController.ImportPreview(req.selectedCodes(),List.of(),req.selectedCodes());
        });
        var service=new WorkspaceTemplateImportReceiptService(db,new WorkspaceEpochTransactionGuard(db),template);
        UUID op=UUID.randomUUID();
        var requested=new WorkspaceTemplateController.ImportRequest(List.of("task.sample"));
        var first=tx.execute(status->service.importOnce(a,op,requested));
        assertNotNull(first);
        assertFalse(first.replayed());
        assertEquals(List.of("task.sample"),first.importResult().toImport());
        var replay=tx.execute(status->service.importOnce(a,op,requested));
        assertNotNull(replay);
        assertTrue(replay.replayed());
        assertEquals(first.importResult(),replay.importResult());
        assertEquals(1,calls.get(),"Replay must not perform a second import");
        assertEquals(first.importResult(),service.lookup(a,op).importResult());
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
                ()->service.lookup(b,op)).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->tx.execute(status->service.importOnce(a,op,
                        new WorkspaceTemplateController.ImportRequest(List.of("reward.other")))))
                .getStatusCode());
        assertEquals(1L,db.queryForObject(
                "SELECT count(*) FROM workspace_template_import_receipts WHERE workspace_id=?",
                Long.class,a));
        verify(template,times(1)).importSelected(eq(a),any());
    }

    @Test void importFailureRollsBackDomainAndReceiptTogether() {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long workspace=db.queryForObject("INSERT INTO families(name) VALUES ('Rollback receipt') RETURNING id",Long.class);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var template=mock(WorkspaceTemplateController.class);
        when(template.importSelected(eq(workspace),any())).thenAnswer(invocation->{
            db.update("UPDATE families SET name='temporary mutation' WHERE id=?",workspace);
            throw new IllegalStateException("Injected import failure");
        });
        var service=new WorkspaceTemplateImportReceiptService(db,new WorkspaceEpochTransactionGuard(db),template);
        UUID op=UUID.randomUUID();
        assertThrows(IllegalStateException.class,()->tx.execute(status->service.importOnce(
                workspace,op,new WorkspaceTemplateController.ImportRequest(List.of("sample")))));
        assertEquals("Rollback receipt",db.queryForObject(
                "SELECT name FROM families WHERE id=?",String.class,workspace));
        assertEquals(0L,db.queryForObject(
                "SELECT count(*) FROM workspace_template_import_receipts WHERE workspace_id=?",
                Long.class,workspace));
    }

    @Test void simultaneousSameKeyHasOneImporterAndTwoConsistentResponses() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long workspace=db.queryForObject("INSERT INTO families(name) VALUES ('Concurrent receipt') RETURNING id",Long.class);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var template=mock(WorkspaceTemplateController.class);
        var calls=new AtomicInteger();
        when(template.importSelected(eq(workspace),any())).thenAnswer(invocation->{
            calls.incrementAndGet();
            return new WorkspaceTemplateController.ImportPreview(List.of("sample"),List.of(),List.of("sample"));
        });
        var service=new WorkspaceTemplateImportReceiptService(db,new WorkspaceEpochTransactionGuard(db),template);
        UUID op=UUID.randomUUID();
        var pool=Executors.newFixedThreadPool(2);
        var go=new CountDownLatch(1);
        try {
            Callable<WorkspaceTemplateImportReceiptService.Result> run=()->{
                if(!go.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Timeout");
                return tx.execute(status->service.importOnce(workspace,op,
                        new WorkspaceTemplateController.ImportRequest(List.of("sample"))));
            };
            var one=pool.submit(run); var two=pool.submit(run);
            go.countDown();
            var first=one.get(20,TimeUnit.SECONDS); var second=two.get(20,TimeUnit.SECONDS);
            assertEquals(1,calls.get());
            assertEquals(first.importResult(),second.importResult());
            assertNotEquals(first.replayed(),second.replayed());
        } finally {pool.shutdownNow();}
    }
}
