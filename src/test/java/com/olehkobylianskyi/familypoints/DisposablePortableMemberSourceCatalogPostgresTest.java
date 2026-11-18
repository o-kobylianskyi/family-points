package com.olehkobylianskyi.familypoints;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Contract probe only: no production vNext serialization or privileged account export. */
@Testcontainers
class DisposablePortableMemberSourceCatalogPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("member_source_probe").withUsername("roster_probe")
            .withPassword("roster_probe");

    @Test void capturesHistoricalTombstoneAndOnlySourceFamilyWithPinnedSnapshot() {
        var source=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(source);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        long family=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Roster Source A') RETURNING id",Long.class);
        long neighbor=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Roster Neighbor B') RETURNING id",Long.class);
        long role=db.queryForObject(
                "INSERT INTO family_roles(family_id,system_role,code,name) " +
                "VALUES (?,true,'PARENT','Parent') RETURNING id",Long.class,family);
        long otherRole=db.queryForObject(
                "INSERT INTO family_roles(family_id,system_role,code,name) " +
                "VALUES (?,true,'PARENT','Parent') RETURNING id",Long.class,neighbor);
        long active=db.queryForObject(
                "INSERT INTO family_members(family_id,family_role_id,member_type,name,active) " +
                "VALUES (?,?,'PARENT','Private Real Name',true) RETURNING id",
                Long.class,family,role);
        long deleted=db.queryForObject(
                "INSERT INTO family_members(family_id,family_role_id,member_type,name,active,deleted_at) " +
                "VALUES (?,?,'CHILD','Видалено',false,TIMESTAMP '2026-11-01 08:09:10') RETURNING id",
                Long.class,family,role);
        db.update("INSERT INTO family_members(family_id,family_role_id,member_type,name,active) " +
                "VALUES (?,?,'PARENT','Neighbor Secret',true)",neighbor,otherRole);

        var reader=new DisposablePortableMemberSourceCatalog(db);
        assertEquals("SOURCE_CATALOG_REPEATABLE_READ_REQUIRED",
                assertThrows(IllegalStateException.class,()->reader.capture(family)).getMessage());
        var weak=new TransactionTemplate(new DataSourceTransactionManager(source));
        weak.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        assertEquals("SOURCE_CATALOG_REPEATABLE_READ_REQUIRED",
                assertThrows(IllegalStateException.class,()->weak.execute(s->reader.capture(family)))
                        .getMessage());

        var catalog=tx.execute(s->reader.capture(family));
        assertEquals(family,catalog.sourceWorkspaceId());
        assertEquals(1,catalog.roles().size());
        assertEquals(role,catalog.roles().get(0).sourceRoleId());
        assertEquals("PARENT",catalog.roles().get(0).roleCode());
        assertEquals(2,catalog.members().size());
        assertEquals(Set.of(active,deleted),Set.of(catalog.members().get(0).sourceMemberId(),
                catalog.members().get(1).sourceMemberId()));
        assertTrue(catalog.members().stream().allMatch(m->
                m.sourceWorkspaceId()==family && m.sourceRoleId()==role));
        var tombstone=catalog.members().stream().filter(m->m.sourceMemberId()==deleted)
                .findFirst().orElseThrow();
        assertFalse(tombstone.activeAtSnapshot());
        assertEquals(LocalDateTime.of(2026,11,1,8,9,10),tombstone.deletedAtSnapshot());
        // The immutable test contract never serializes private names or accounts.
        assertFalse(catalog.toString().contains("Private Real Name"));
        assertFalse(catalog.toString().contains("Neighbor Secret"));
        assertEquals(1,tx.execute(s->reader.capture(neighbor)).members().size());
    }

    @Test void rejectsCrossFamilyRoleReferenceWithoutSilentRowDrop() {
        var source=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(source);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        long first=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Role Source A') RETURNING id",Long.class);
        long second=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Role Source B') RETURNING id",Long.class);
        long foreignRole=db.queryForObject(
                "INSERT INTO family_roles(family_id,system_role,code,name) " +
                "VALUES (?,true,'PARENT','Parent') RETURNING id",Long.class,second);
        // Legacy FK family_role_id alone does not prove same-family ownership;
        // the future source capture must reject, not silently filter this row.
        db.update("INSERT INTO family_members(family_id,family_role_id,member_type,name,active) " +
                "VALUES (?,?,'PARENT','Mismatched',true)",first,foreignRole);
        var reader=new DisposablePortableMemberSourceCatalog(db);
        assertEquals("SOURCE_MEMBER_PROVENANCE_INVALID",
                assertThrows(SecurityException.class,()->
                        tx.execute(status->reader.capture(first))).getMessage());
    }
}
