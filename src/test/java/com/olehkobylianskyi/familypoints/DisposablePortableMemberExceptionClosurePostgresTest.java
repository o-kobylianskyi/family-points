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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B-A-04e test-only source graph integration. Real PostgreSQL rows, not an
 * implementation of vNext export, import, user mapping or Restore.
 */
@Testcontainers
class DisposablePortableMemberExceptionClosurePostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("source_member_exception")
            .withUsername("source_member_exception")
            .withPassword("source_member_exception");

    @Test void scopeExceptionRowsToOriginalFamilyAndRejectAccidentalNeighborLeakage() {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long family=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Exception source A') RETURNING id",Long.class);
        long neighbor=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Exception source B') RETURNING id",Long.class);
        long roleA=db.queryForObject(
                "INSERT INTO family_roles(family_id,system_role,code,name) " +
                "VALUES (?,true,'PARENT','Parent') RETURNING id",Long.class,family);
        long roleB=db.queryForObject(
                "INSERT INTO family_roles(family_id,system_role,code,name) " +
                "VALUES (?,true,'PARENT','Parent') RETURNING id",Long.class,neighbor);
        long memberA=db.queryForObject(
                "INSERT INTO family_members(family_id,family_role_id,member_type,name,active) " +
                "VALUES (?,?,'CHILD','Source A',true) RETURNING id",Long.class,family,roleA);
        long memberB=db.queryForObject(
                "INSERT INTO family_members(family_id,family_role_id,member_type,name,active) " +
                "VALUES (?,?,'CHILD','Source B',true) RETURNING id",Long.class,neighbor,roleB);
        db.update("""
                INSERT INTO member_exception_periods
                    (member_id,start_date,end_date,created_at,type)
                VALUES (?,CURRENT_DATE,CURRENT_DATE,NOW(),'ILLNESS')
                """,memberA);
        db.update("""
                INSERT INTO member_exception_periods
                    (member_id,start_date,end_date,created_at,type)
                VALUES (?,CURRENT_DATE,CURRENT_DATE,NOW(),'VACATION')
                """,memberB);

        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        tx.execute(status->{
            var catalog=new DisposablePortableMemberSourceCatalog(db).capture(family);
            var rows=db.queryForList("""
                    SELECT e.member_id
                    FROM member_exception_periods e
                    JOIN family_members m ON m.id=e.member_id
                    WHERE m.family_id=?
                    ORDER BY e.id
                    """,family);
            assertEquals(1,rows.size());
            assertEquals(memberA,((Number)rows.get(0).get("member_id")).longValue());
            assertDoesNotThrow(()->DisposablePortableMemberReferenceClosure.verify(
                    catalog,Map.of("MEMBER_EXCEPTION_PERIODS",rows)));

            // A mistargeted exporter that includes a neighbor row must be
            // rejected instead of silently treating the original numeric ID
            // as a valid identity in this family.
            var accidental=List.<Map<String,?>>of(
                    Map.of("member_id",memberA),Map.of("member_id",memberB));
            assertEquals("SOURCE_MEMBER_EDGE_UNRESOLVED:MEMBER_EXCEPTION_PERIODS.member_id",
                    assertThrows(SecurityException.class,()->
                            DisposablePortableMemberReferenceClosure.verify(
                                    catalog,Map.of("MEMBER_EXCEPTION_PERIODS",accidental)))
                            .getMessage());
            return null;
        });
    }
}
