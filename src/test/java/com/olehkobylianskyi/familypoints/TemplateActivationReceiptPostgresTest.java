package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.controller.WorkspaceTemplateController;
import com.olehkobylianskyi.familypoints.dto.TaskDefinitionCreateRequest;
import com.olehkobylianskyi.familypoints.service.TemplateActivationReceiptService;
import com.olehkobylianskyi.familypoints.service.WorkspaceEpochTransactionGuard;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** V48: database-backed replay, rollback, concurrency, tenant isolation; disposable PostgreSQL. */
@Testcontainers
class TemplateActivationReceiptPostgresTest {
    @Container static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("activation_receipts")
            .withUsername("activation_test").withPassword("activation_test_password");

    private record Fixture(JdbcTemplate db, TransactionTemplate tx,
            TemplateActivationReceiptService service, WorkspaceTemplateController templates,
            long a,long b) {}
    private Fixture fixture() {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long a=db.queryForObject("INSERT INTO families(name) VALUES ('Activation A') RETURNING id",Long.class);
        long b=db.queryForObject("INSERT INTO families(name) VALUES ('Activation B') RETURNING id",Long.class);
        var templates=mock(WorkspaceTemplateController.class);
        return new Fixture(db,new TransactionTemplate(new DataSourceTransactionManager(ds)),
                new TemplateActivationReceiptService(db,new WorkspaceEpochTransactionGuard(db),templates),
                templates,a,b);
    }

    @Test void returnsStableTaskIdentityWithoutRecreatingAndIsolatesFamily() {
        var f=fixture();
        var calls=new AtomicInteger();
        // Real SQL family row and receipt, mock the domain creator so the test
        // isolates the transactional receipt contract from TaskDefinition fixtures.
        var task=mock(com.olehkobylianskyi.familypoints.dto.TaskDefinitionResponse.class);
        when(task.getId()).thenReturn(987L);
        when(f.templates.activateTask(eq(f.a),eq(11L),any(UUID.class),any()))
                .thenAnswer(invocation->{calls.incrementAndGet();return ResponseEntity.status(201).body(task);});
        UUID key=UUID.randomUUID();
        var request=new TaskDefinitionCreateRequest();
        var one=f.tx.execute(s->f.service.activateTask(f.a,11,key,request));
        var two=f.tx.execute(s->f.service.activateTask(f.a,11,key,request));
        assertNotNull(one);assertNotNull(two);
        assertEquals(987L,one.createdDefinitionId());
        assertEquals(one.createdDefinitionId(),two.createdDefinitionId());
        assertFalse(one.replayed());assertTrue(two.replayed());
        assertEquals(1,calls.get());
        assertEquals(987L,f.service.receipt(f.a,key).createdDefinitionId());
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
                ()->f.service.receipt(f.b,key)).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->f.tx.execute(s->f.service.activateTask(f.a,12,key,request))).getStatusCode());
    }

    @Test void failureRollsBackDomainAndReceiptAndRetryCanSucceed() {
        var f=fixture();
        UUID key=UUID.randomUUID();
        var request=new TaskDefinitionCreateRequest();
        when(f.templates.activateTask(eq(f.a),eq(5L),eq(key),any()))
                .thenAnswer(invocation->{
                    f.db.update("UPDATE families SET name='temporary' WHERE id=?",f.a);
                    throw new IllegalStateException("injected failure");
                });
        assertThrows(IllegalStateException.class,()->f.tx.execute(s->f.service.activateTask(f.a,5,key,request)));
        assertEquals("Activation A",f.db.queryForObject("SELECT name FROM families WHERE id=?",String.class,f.a));
        assertEquals(0L,f.db.queryForObject("SELECT count(*) FROM template_activation_receipts WHERE workspace_id=?",Long.class,f.a));
    }

    @Test void sameKeyConcurrentCallsHaveExactlyOneCreator() throws Exception {
        var f=fixture();
        var task=mock(com.olehkobylianskyi.familypoints.dto.TaskDefinitionResponse.class);
        when(task.getId()).thenReturn(543L);
        AtomicInteger count=new AtomicInteger();
        when(f.templates.activateTask(eq(f.a),eq(3L),any(UUID.class),any()))
                .thenAnswer(invocation->{count.incrementAndGet();return ResponseEntity.status(201).body(task);});
        UUID key=UUID.randomUUID();
        var req=new TaskDefinitionCreateRequest();
        var go=new CountDownLatch(1);
        var pool=Executors.newFixedThreadPool(2);
        try {
            Callable<TemplateActivationReceiptService.Receipt> call=()->{
                go.await(10,TimeUnit.SECONDS);
                return f.tx.execute(s->f.service.activateTask(f.a,3,key,req));
            };
            var one=pool.submit(call);var two=pool.submit(call);
            go.countDown();
            var a=one.get(20,TimeUnit.SECONDS);var b=two.get(20,TimeUnit.SECONDS);
            assertEquals(1,count.get());
            assertEquals(a.createdDefinitionId(),b.createdDefinitionId());
            assertNotEquals(a.replayed(),b.replayed());
        } finally {pool.shutdownNow();}
    }
}
