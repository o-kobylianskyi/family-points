package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.controller.WorkspaceTemplateController;
import com.olehkobylianskyi.familypoints.dto.TaskDefinitionCreateRequest;
import com.olehkobylianskyi.familypoints.dto.TaskDefinitionResponse;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Exercises real LocalDate fingerprint, definition SQL and durable receipt in
 * the same disposable PostgreSQL transaction. Template controller is an SQL
 * fixture, not an authenticated HTTP/TaskService integration test.
 */
@Testcontainers
class TemplateActivationDateTransactionPostgresTest {
    @Container static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("activation_java_time").withUsername("task_date")
            .withPassword("synthetic_only");

    private TaskDefinitionCreateRequest request(LocalDate date) {
        var r = new TaskDefinitionCreateRequest();
        r.setTitle("Date-based synthetic task");
        r.setRecurrenceType(com.olehkobylianskyi.familypoints.entity.TaskRecurrenceType.ONCE);
        r.setStartDate(date);
        r.setEndDate(date.plusDays(2));
        r.setDueTime(LocalTime.of(16, 45));
        return r;
    }

    @Test void localDatePurchaseLikeActivationIsOneDefinitionOneReceiptAndConflictSafe() {
        var ds = new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db = new JdbcTemplate(ds);
        var tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        long family = db.queryForObject(
                "INSERT INTO families(name) VALUES ('Date transaction family') RETURNING id",Long.class);
        var controller = mock(WorkspaceTemplateController.class);
        var service = new TemplateActivationReceiptService(
                db,new WorkspaceEpochTransactionGuard(db),controller);
        var calls = new AtomicInteger();
        when(controller.activateTask(eq(family),eq(43L),any(UUID.class),any()))
                .thenAnswer(inv -> {
                    TaskDefinitionCreateRequest data = inv.getArgument(3);
                    long id = db.queryForObject("""
                        INSERT INTO task_definitions
                          (family_id,title,active,delegation_allowed,mandatory,
                           recurrence_type,start_date,end_date,due_time,created_at,updated_at)
                        VALUES (?,?,true,false,false,'ONCE',?,?,?,now(),now()) RETURNING id
                        """,Long.class,family,data.getTitle(),data.getStartDate(),
                            data.getEndDate(),data.getDueTime());
                    calls.incrementAndGet();
                    var response=mock(TaskDefinitionResponse.class);
                    when(response.getId()).thenReturn(id);
                    return ResponseEntity.status(201).body(response);
                });
        UUID uuid = UUID.randomUUID();
        var data=request(LocalDate.of(2026,11,21));
        var first=tx.execute(status -> service.activateTask(family,43L,uuid,data));
        var repeat=tx.execute(status -> service.activateTask(family,43L,uuid,
                request(LocalDate.of(2026,11,21))));
        assertNotNull(first);
        assertNotNull(repeat);
        assertEquals(first.createdDefinitionId(),repeat.createdDefinitionId());
        assertFalse(first.replayed());
        assertTrue(repeat.replayed());
        assertEquals(1,calls.get());
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM task_definitions WHERE family_id=?",
                Long.class,family));
        assertEquals(1L,db.queryForObject(
                "SELECT count(*) FROM template_activation_receipts WHERE workspace_id=?",
                Long.class,family));
        assertEquals(first.createdDefinitionId(),service.receipt(family,uuid).createdDefinitionId());
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->tx.execute(status -> service.activateTask(family,43L,uuid,
                    request(LocalDate.of(2026,11,22))))).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->tx.execute(status -> service.activateTask(family,44L,uuid,data))).getStatusCode());
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM task_definitions WHERE family_id=?",
                Long.class,family));
    }

    @Test void failedCreatorLeavesNeitherDefinitionNorReceiptAndSameUuidCanSucceed() {
        var ds = new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        long family=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Rollback date family') RETURNING id",Long.class);
        var controller=mock(WorkspaceTemplateController.class);
        var service=new TemplateActivationReceiptService(
                db,new WorkspaceEpochTransactionGuard(db),controller);
        UUID uuid=UUID.randomUUID();
        var data=request(LocalDate.of(2026,12,1));
        when(controller.activateTask(eq(family),eq(43L),eq(uuid),any()))
                .thenAnswer(inv -> {
                    long created=db.queryForObject("""
                        INSERT INTO task_definitions
                          (family_id,title,active,delegation_allowed,mandatory,
                           recurrence_type,start_date,end_date,created_at,updated_at)
                        VALUES (?,'Transient',true,false,false,'ONCE',?,?,now(),now()) RETURNING id
                        """,Long.class,family,data.getStartDate(),data.getEndDate());
                    throw new IllegalStateException("Injected after definition SQL "+created);
                });
        assertThrows(IllegalStateException.class,
                ()->tx.execute(status -> service.activateTask(family,43L,uuid,data)));
        assertEquals(0L,db.queryForObject("SELECT count(*) FROM task_definitions WHERE family_id=?",
                Long.class,family));
        assertEquals(0L,db.queryForObject(
                "SELECT count(*) FROM template_activation_receipts WHERE workspace_id=?",
                Long.class,family));
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
                ()->service.receipt(family,uuid)).getStatusCode());
    }
}
