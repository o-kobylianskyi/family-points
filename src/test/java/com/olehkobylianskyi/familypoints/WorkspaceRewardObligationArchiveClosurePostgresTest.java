package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.crypto.KeyGenerator;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** A purchase's dependent obligations must not disappear from a portable
 * family archive while obligations are still an unsupported export category. */
@Testcontainers
class WorkspaceRewardObligationArchiveClosurePostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("obligation_closure")
            .withUsername("obligation").withPassword("disposable_test");

    @Test void unexportedPurchaseObligationsPreventAuthenticatedIncompleteArchive()
            throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long family=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Obligation owner') RETURNING id",Long.class);
        long role=db.queryForObject("""
                INSERT INTO family_roles(family_id,code,name,system_role)
                VALUES (?,'PARENT','Parent',true) RETURNING id
                """,Long.class,family);
        long member=db.queryForObject("""
                INSERT INTO family_members(family_id,family_role_id,name,active,member_type)
                VALUES (?,?,'Purchaser',true,'PARENT') RETURNING id
                """,Long.class,family,role);
        long pointType=db.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,family);
        long reward=db.queryForObject("""
                INSERT INTO reward_definitions(family_id,title,point_type_id,price_amount)
                VALUES (?,'Screen time',?,10) RETURNING id
                """,Long.class,family,pointType);
        long purchase=db.queryForObject("""
                INSERT INTO reward_purchases(
                  reward_definition_id,member_id,reward_title,point_type_id,price_amount,status)
                VALUES (? ,?,'Screen time',?,10,'PURCHASED') RETURNING id
                """,Long.class,reward,member,pointType);
        db.update("""
                INSERT INTO reward_obligations(family_id,member_id,reward_purchase_id,title,status)
                VALUES (?, ?, ?, 'Wait for parent', 'PENDING')
                """,family,member,purchase);

        var codec=new ProtectiveBackupArtifactCodec();
        var portable=new ProtectiveBackupPortableFile();
        var exporter=new WorkspacePortableBackupExporter(db,
                new WorkspaceResetSnapshotService(db,new WorkspaceEpochTransactionGuard(db)),
                codec,portable);
        var k=KeyGenerator.getInstance("AES");k.init(256);var key=k.generateKey();
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var error=assertThrows(IllegalStateException.class,()->tx.execute(s->exporter.export(
                family,UUID.randomUUID(),UUID.randomUUID(),0L,
                List.of("REWARD_PURCHASES","REWARD_DEFINITIONS"),"V50",key)));
        assertEquals("BACKUP_UNEXPORTED_REWARD_OBLIGATIONS",error.getMessage());
        var closure=new WorkspaceFinancialRestoreClosureService(db)
                .assess(family,List.of("REWARD_PURCHASES","REWARD_DEFINITIONS"));
        assertFalse(closure.replaySupported());
        assertTrue(closure.blockers().contains(
                "RESTORE_DEPENDENCY_NOT_SELECTED:REWARD_OBLIGATIONS"));
        assertTrue(closure.blockers().contains(
                "RESTORE_DEPENDENCY_NOT_SELECTED:REWARD_OBLIGATIONS"));
        assertEquals(1L,db.queryForObject(
                "SELECT count(*) FROM reward_obligations WHERE family_id=?",
                Long.class,family).longValue());
        assertEquals(1L,db.queryForObject(
                "SELECT count(*) FROM reward_purchases WHERE member_id=?",
                Long.class,member).longValue());

        db.update("DELETE FROM reward_obligations WHERE reward_purchase_id=?",purchase);
        db.update("""
                INSERT INTO reward_requirements(
                  family_id,reward_definition_id,phase,requirement_type)
                VALUES (?,?,'BEFORE_PURCHASE','TASK_COMPLETION')
                """,family,reward);
        var requirementsError=assertThrows(IllegalStateException.class,
                ()->tx.execute(s->exporter.export(
                        family,UUID.randomUUID(),UUID.randomUUID(),0L,
                        List.of("REWARD_DEFINITIONS"),"V50",key)));
        assertEquals("BACKUP_UNEXPORTED_REWARD_REQUIREMENTS",
                requirementsError.getMessage());
        var requiresClosure=new WorkspaceFinancialRestoreClosureService(db)
                .assess(family,List.of("REWARD_DEFINITIONS"));
        assertTrue(requiresClosure.blockers().contains(
                "RESTORE_DEPENDENCY_NOT_SELECTED:REWARD_REQUIREMENTS"));

        // The newly supported requirement category exports its own source row
        // only with the owning reward definition. It is still not restorable.
        var requirementsFile=tx.execute(s->exporter.export(
                family,UUID.randomUUID(),UUID.randomUUID(),0L,
                List.of("REWARD_DEFINITIONS","REWARD_REQUIREMENTS"),"V50",key));
        var inspector=new ProtectiveBackupImportInspector(portable,codec);
        var inspected=inspector.inspect(requirementsFile.encryptedFile(),family,key);
        assertEquals(1,inspected.counts().get("REWARD_REQUIREMENTS").intValue());
        assertEquals(1,inspected.counts().get("REWARD_DEFINITIONS").intValue());
        assertFalse(inspected.restorable());
        assertFalse(inspected.blockers().contains("MISSING_CATEGORY:REWARD_DEFINITIONS"));
        var omission=assertThrows(IllegalStateException.class,
                ()->tx.execute(s->exporter.export(
                        family,UUID.randomUUID(),UUID.randomUUID(),0L,
                        List.of("REWARD_REQUIREMENTS"),"V50",key)));
        assertEquals("BACKUP_REWARD_REQUIREMENT_PARENT_MISSING",omission.getMessage());

        // Third independent failure: reward-category associations are not yet
        // part of the portable archive even when reward definitions are.
        db.update("DELETE FROM reward_requirements WHERE family_id=?",family);
        long category=db.queryForObject("""
                INSERT INTO reward_categories(family_id,name,active,sort_order)
                VALUES (?,'Media rewards',true,0) RETURNING id
                """,Long.class,family);
        db.update("""
                INSERT INTO reward_definition_categories(reward_definition_id,category_id)
                VALUES (?,?)
                """,reward,category);
        var categoryError=assertThrows(IllegalStateException.class,
                ()->tx.execute(s->exporter.export(
                        family,UUID.randomUUID(),UUID.randomUUID(),0L,
                        List.of("REWARD_DEFINITIONS"),"V50",key)));
        assertEquals("BACKUP_UNEXPORTED_REWARD_CATEGORY_LINKS",
                categoryError.getMessage());
        var categoryClosure=new WorkspaceFinancialRestoreClosureService(db)
                .assess(family,List.of("REWARD_DEFINITIONS"));
        assertTrue(categoryClosure.blockers().contains(
                "RESTORE_DEPENDENCY_NOT_SELECTED:REWARD_DEFINITION_CATEGORIES"));
        assertEquals(1L,db.queryForObject("""
                SELECT count(*) FROM reward_definition_categories WHERE reward_definition_id=?
                """,Long.class,reward).longValue());
        // Positive portable graph: definition, category and association must
        // all be present in the authenticated file, never only the link.
        var complete=tx.execute(s->exporter.export(family,UUID.randomUUID(),
                UUID.randomUUID(),0L,
                List.of("REWARD_DEFINITIONS","REWARD_CATEGORIES",
                        "REWARD_DEFINITION_CATEGORIES"),"V50",key));
        var graph=new ProtectiveBackupImportInspector(portable,codec)
                .inspect(complete.encryptedFile(),family,key);
        assertEquals(1,graph.counts().get("REWARD_DEFINITION_CATEGORIES").intValue());
        assertEquals(1,graph.counts().get("REWARD_CATEGORIES").intValue());
        assertEquals(1,graph.counts().get("REWARD_DEFINITIONS").intValue());
        assertFalse(graph.restorable());
        assertFalse(graph.blockers().contains("MISSING_CATEGORY:REWARD_CATEGORIES"));

        var missingParents=assertThrows(IllegalStateException.class,
                ()->tx.execute(s->exporter.export(family,UUID.randomUUID(),
                        UUID.randomUUID(),0L,
                        List.of("REWARD_DEFINITION_CATEGORIES"),"V50",key)));
        assertEquals("BACKUP_REWARD_CATEGORY_PARENT_MISSING",missingParents.getMessage());
        long neighbor=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Foreign category family') RETURNING id",
                Long.class);
        long foreignCategory=db.queryForObject("""
                INSERT INTO reward_categories(family_id,name,active,sort_order)
                VALUES (?,'External',true,0) RETURNING id
                """,Long.class,neighbor);
        db.update("""
                INSERT INTO reward_definition_categories(reward_definition_id,category_id)
                VALUES (?,?)
                """,reward,foreignCategory);
        assertThrows(SecurityException.class,()->tx.execute(s->exporter.export(
                family,UUID.randomUUID(),UUID.randomUUID(),0L,
                List.of("REWARD_DEFINITIONS","REWARD_CATEGORIES",
                        "REWARD_DEFINITION_CATEGORIES"),"V50",key)));
        assertEquals(1L,db.queryForObject(
                "SELECT count(*) FROM reward_definition_categories WHERE category_id=?",
                Long.class,foreignCategory).longValue());


    }
}
