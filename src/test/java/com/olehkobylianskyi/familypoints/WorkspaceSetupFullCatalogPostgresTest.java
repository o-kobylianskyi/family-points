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
 * Full V46-V49 wiring with the REAL bundled catalog and the REAL SQL importer.
 * Only infrastructure/service edges are constructed manually; no live family data.
 */
@Testcontainers
class WorkspaceSetupFullCatalogPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("full_catalog_setup")
            .withUsername("full_catalog_test").withPassword("full_catalog_password");

    private record Fixture(JdbcTemplate db,TransactionTemplate tx,
        WorkspaceSetupService setup,WorkspaceSetupReviewController review,
        WorkspaceSetupFinalizeService finalizeService,long family,long unrelated) {}

    private Fixture fixture() {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long family=db.queryForObject("INSERT INTO families(name) VALUES ('Full catalog setup') RETURNING id",Long.class);
        long other=db.queryForObject("INSERT INTO families(name) VALUES ('Other family') RETURNING id",Long.class);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var guard=new WorkspaceEpochTransactionGuard(db);
        var setup=new WorkspaceSetupService(db,guard,true);
        var templates=new WorkspaceTemplateController(db,new TemplateCatalogService(),
                mock(TaskDefinitionRepository.class),mock(RewardDefinitionRepository.class),
                mock(TaskController.class),mock(RewardController.class),guard);
        var imports=new WorkspaceTemplateImportReceiptService(db,guard,templates);
        return new Fixture(db,tx,setup,new WorkspaceSetupReviewController(setup,templates),
                new WorkspaceSetupFinalizeService(db,guard,setup,imports),family,other);
    }

    @Test void all142CatalogTemplatesPreviewImportFinalizeAndReplayWithoutActivation() {
        var f=fixture();
        var catalog=new TemplateCatalogService().getCatalog().path("templates");
        List<String> selected=new ArrayList<>();
        for(var entry:catalog)selected.add(entry.path("code").asText());
        assertEquals(142,selected.size(),"Review designer-approved catalog before changing this assertion");

        var preview=f.review.review(f.family,
                new WorkspaceSetupReviewController.ReviewRequest(0L,selected)).getBody();
        assertNotNull(preview);
        assertEquals(142,preview.toImportCount());
        assertEquals(0,preview.alreadyImportedCount());
        assertEquals(0L,count(f,"workspace_private_templates",f.family));
        assertEquals("NOT_STARTED",f.setup.current(f.family).status());

        long revision=0;
        for(var step:WorkspaceSetupService.STEPS) {
            final long expected=revision;
            revision=Objects.requireNonNull(f.tx.execute(status->f.setup.setStep(f.family,step,
                    new WorkspaceSetupService.StepRequest("COMPLETE",expected)))).revision();
        }
        UUID id=UUID.randomUUID();
        var request=new WorkspaceSetupFinalizeService.Request(revision,selected);
        var result=f.tx.execute(status->f.finalizeService.finalizeWizard(f.family,id,request));
        assertNotNull(result);
        assertEquals("COMPLETED",result.setupStatus());
        assertEquals(142,result.importResult().toImport().size());
        assertEquals(142L,count(f,"workspace_private_templates",f.family));
        assertEquals(0L,count(f,"workspace_private_templates",f.unrelated));
        assertEquals(1L,count(f,"workspace_template_import_receipts",f.family));
        assertEquals(1L,count(f,"workspace_setup_finalize_receipts",f.family));

        var replay=f.tx.execute(status->f.finalizeService.finalizeWizard(f.family,id,request));
        assertNotNull(replay);
        assertTrue(replay.replayed());
        assertEquals(result.importResult(),replay.importResult());
        assertEquals(142L,count(f,"workspace_private_templates",f.family));
        assertEquals(result.importResult(),f.finalizeService.receipt(f.family,id).importResult());
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
                ()->f.finalizeService.receipt(f.unrelated,id)).getStatusCode());
        assertEquals("NOT_STARTED",f.setup.current(f.unrelated).status());
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->f.tx.execute(status->f.finalizeService.finalizeWizard(f.family,id,
                        new WorkspaceSetupFinalizeService.Request(request.expectedRevision(),List.of()))))
                .getStatusCode());
    }

    @Test void invalidCatalogCodeAndIncompleteWizardNeverCommitAnything() {
        var f=fixture();
        var malformed=new WorkspaceSetupFinalizeService.Request(0L,List.of("not.in.catalog"));
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->f.tx.execute(s->f.finalizeService.finalizeWizard(f.family,UUID.randomUUID(),malformed)))
                .getStatusCode(),"Incomplete wizard must fail before importing");
        long revision=0;
        for(var step:WorkspaceSetupService.STEPS) {
            final long old=revision;
            revision=Objects.requireNonNull(f.tx.execute(s->f.setup.setStep(f.family,step,
                    new WorkspaceSetupService.StepRequest("SKIPPED",old)))).revision();
        }
        final long expected=revision;
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
                ()->f.tx.execute(s->f.finalizeService.finalizeWizard(f.family,UUID.randomUUID(),
                        new WorkspaceSetupFinalizeService.Request(expected,List.of("not.in.catalog")))))
                .getStatusCode());
        assertEquals("IN_PROGRESS",f.setup.current(f.family).status());
        assertEquals(expected,f.setup.current(f.family).revision());
        assertEquals(0L,count(f,"workspace_private_templates",f.family));
        assertEquals(0L,count(f,"workspace_template_import_receipts",f.family));
        assertEquals(0L,count(f,"workspace_setup_finalize_receipts",f.family));
    }


    @Test void staleEpochBlocksFinalizeBeforeAnyImportOrReceipt() {
        var f=fixture();
        long revision=0;
        for(var step:WorkspaceSetupService.STEPS) {
            final long expected=revision;
            revision=Objects.requireNonNull(f.tx.execute(s->f.setup.setStep(f.family,step,
                    new WorkspaceSetupService.StepRequest("COMPLETE",expected)))).revision();
        }
        var selected=List.of(new TemplateCatalogService().getCatalog().path("templates").get(0).path("code").asText());
        f.db.update("UPDATE families SET data_epoch=1 WHERE id=?",f.family);
        final long expected=revision;
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->f.tx.execute(s->f.finalizeService.finalizeWizard(f.family,UUID.randomUUID(),
                        new WorkspaceSetupFinalizeService.Request(expected,selected)))).getStatusCode());
        assertEquals(0L,count(f,"workspace_private_templates",f.family));
        assertEquals(0L,count(f,"workspace_template_import_receipts",f.family));
        assertEquals(0L,count(f,"workspace_setup_finalize_receipts",f.family));
        assertEquals("NOT_STARTED",f.setup.current(f.unrelated).status());
    }

    @Test void legacyFamilyWithoutWizardCannotFinalizeOrImport() {
        // This fixture uses an existing migrated disposable container. An old-style
        // family can be modeled by deleting only its setup metadata in this test DB.
        var f=fixture();
        f.db.update("DELETE FROM workspace_setup_states WHERE workspace_id=?",f.family);
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
                ()->f.tx.execute(s->f.finalizeService.finalizeWizard(f.family,UUID.randomUUID(),
                        new WorkspaceSetupFinalizeService.Request(0L,List.of())))).getStatusCode());
        assertEquals(0L,count(f,"workspace_private_templates",f.family));
        assertEquals(0L,count(f,"workspace_setup_finalize_receipts",f.family));
    }

    private static long count(Fixture f,String table,long family) {
        // Table names are fixed test literals only.
        if(!Set.of("workspace_private_templates","workspace_template_import_receipts",
                "workspace_setup_finalize_receipts").contains(table))throw new IllegalArgumentException();
        return f.db.queryForObject("SELECT count(*) FROM "+table+" WHERE workspace_id=?",
                Long.class,family);
    }
}
