package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.controller.*;
import com.olehkobylianskyi.familypoints.repository.*;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Real PostgreSQL and real setup, import, preview, receipt, epoch-guard services.
 * Only unrelated task/reward creation controller dependencies are mocked.
 * Disposable Testcontainers DB; never connects to DEV/BETA/PROD.
 */
@Testcontainers
class WorkspaceSetupFinalizeRealServicesPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("setup_real_integration")
            .withUsername("setup_real_test").withPassword("setup_real_password");

    @Test void realReviewImportCompleteReceiptAndReplayRemainAtomic() {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var jdbc=new JdbcTemplate(ds);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var guard=new WorkspaceEpochTransactionGuard(jdbc);
        var setup=new WorkspaceSetupService(jdbc,guard,true);
        var catalog=new TemplateCatalogService();
        var templates=new WorkspaceTemplateController(jdbc,catalog,
                mock(TaskDefinitionRepository.class),mock(RewardDefinitionRepository.class),
                mock(TaskController.class),mock(RewardController.class),guard);
        var imports=new WorkspaceTemplateImportReceiptService(jdbc,guard,templates);
        var finalizer=new WorkspaceSetupFinalizeService(jdbc,guard,setup,imports);
        var review=new WorkspaceSetupReviewController(setup,templates);

        long family=jdbc.queryForObject("INSERT INTO families(name) VALUES ('Setup real integration') RETURNING id",Long.class);
        var state=setup.current(family);
        assertEquals("NOT_STARTED",state.status());
        assertEquals(0,state.revision());

        for(var step:WorkspaceSetupService.STEPS) {
            long revision=state.revision();
            state=tx.execute(s->setup.setStep(family,step,
                    new WorkspaceSetupService.StepRequest("COMPLETE",revision)));
            assertNotNull(state);
        }
        assertEquals(7,state.revision());

        String taskCode=null,rewardCode=null;
        for(var t:catalog.getCatalog().path("templates")) {
            if(taskCode==null && "TASK".equals(t.path("kind").asText()))taskCode=t.path("code").asText();
            if(rewardCode==null && "REWARD".equals(t.path("kind").asText()))rewardCode=t.path("code").asText();
        }
        assertNotNull(taskCode);assertNotNull(rewardCode);
        var selected=List.of(taskCode,rewardCode);
        var preview=review.review(family,new WorkspaceSetupReviewController.ReviewRequest(
                state.revision(),selected)).getBody();
        assertNotNull(preview);
        assertEquals(2,preview.toImportCount());
        assertEquals(0,preview.alreadyImportedCount());
        assertEquals(0L,jdbc.queryForObject(
                "SELECT count(*) FROM workspace_private_templates WHERE workspace_id=?",Long.class,family));

        UUID key=UUID.randomUUID();
        var request=new WorkspaceSetupFinalizeService.Request(state.revision(),selected);
        var first=tx.execute(s->finalizer.finalizeWizard(family,key,request));
        assertNotNull(first);
        assertFalse(first.replayed());
        assertEquals("COMPLETED",first.setupStatus());
        assertEquals(8,first.revision());
        assertEquals(2,first.importResult().toImport().size());
        assertEquals(2L,jdbc.queryForObject(
                "SELECT count(*) FROM workspace_private_templates WHERE workspace_id=?",Long.class,family));
        assertEquals(1L,jdbc.queryForObject(
                "SELECT count(*) FROM workspace_setup_finalize_receipts WHERE workspace_id=?",Long.class,family));
        assertEquals(1L,jdbc.queryForObject(
                "SELECT count(*) FROM workspace_template_import_receipts WHERE workspace_id=?",Long.class,family));

        var replay=tx.execute(s->finalizer.finalizeWizard(family,key,request));
        assertNotNull(replay);
        assertTrue(replay.replayed());
        assertEquals(first.importResult(),replay.importResult());
        assertEquals(first.importResult(),finalizer.receipt(family,key).importResult());
        assertEquals(2L,jdbc.queryForObject(
                "SELECT count(*) FROM workspace_private_templates WHERE workspace_id=?",Long.class,family));
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->tx.execute(s->finalizer.finalizeWizard(family,UUID.randomUUID(),request))).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->tx.execute(s->finalizer.finalizeWizard(family,key,
                        new WorkspaceSetupFinalizeService.Request(7L,List.of(taskCode))))).getStatusCode());
    }

    @Test void unknownCatalogSelectionRollsBackRealWizardCompletion() {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var jdbc=new JdbcTemplate(ds);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var guard=new WorkspaceEpochTransactionGuard(jdbc);
        var setup=new WorkspaceSetupService(jdbc,guard,true);
        var templates=new WorkspaceTemplateController(jdbc,new TemplateCatalogService(),
                mock(TaskDefinitionRepository.class),mock(RewardDefinitionRepository.class),
                mock(TaskController.class),mock(RewardController.class),guard);
        var imports=new WorkspaceTemplateImportReceiptService(jdbc,guard,templates);
        var finalizer=new WorkspaceSetupFinalizeService(jdbc,guard,setup,imports);
        long family=jdbc.queryForObject("INSERT INTO families(name) VALUES ('Invalid selection family') RETURNING id",Long.class);
        var state=setup.current(family);
        for(var step:WorkspaceSetupService.STEPS) {
            long rev=state.revision();
            state=tx.execute(s->setup.setStep(family,step,
                    new WorkspaceSetupService.StepRequest("COMPLETE",rev)));
        }
        long rev=state.revision();
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
                ()->tx.execute(s->finalizer.finalizeWizard(family,UUID.randomUUID(),
                        new WorkspaceSetupFinalizeService.Request(rev,List.of("nonexistent.code"))))).getStatusCode());
        assertEquals("IN_PROGRESS",setup.current(family).status());
        assertEquals(rev,setup.current(family).revision());
        assertEquals(0L,jdbc.queryForObject(
                "SELECT count(*) FROM workspace_setup_finalize_receipts WHERE workspace_id=?",Long.class,family));
        assertEquals(0L,jdbc.queryForObject(
                "SELECT count(*) FROM workspace_private_templates WHERE workspace_id=?",Long.class,family));
    }
}
