package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.WorkspaceFinancialRestoreClosureService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class WorkspaceFinancialRestoreClosurePostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("financial_restore_gap")
            .withUsername("financial_gap").withPassword("financial_gap_only");

    @Test void groupLedgerAbsentFromPortableScopeIsDetectedWithoutTouchingNeighborFamily() {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long source=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Financial source') RETURNING id",Long.class);
        long neighbor=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Financial neighbor') RETURNING id",Long.class);
        long group=db.queryForObject("""
                INSERT INTO member_groups(family_id,name,active,show_in_navigation)
                VALUES (?,'Wallet group',true,false) RETURNING id
                """,Long.class,source);
        long pointType=db.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,source);
        db.update("""
                INSERT INTO group_point_transactions
                (group_id,point_type_id,amount,created_at,type,source_type,description)
                VALUES (?,?,10,NOW(),'EARN','MANUAL','Ledger must remain immutable')
                """,group,pointType);
        long otherGroup=db.queryForObject("""
                INSERT INTO member_groups(family_id,name,active,show_in_navigation)
                VALUES (?,'Other wallet',true,false) RETURNING id
                """,Long.class,neighbor);
        long otherPointType=db.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,neighbor);
        db.update("""
                INSERT INTO group_point_transactions
                (group_id,point_type_id,amount,created_at,type,source_type,description)
                VALUES (?,?,20,NOW(),'EARN','MANUAL','Untouched neighbor')
                """,otherGroup,otherPointType);
        var result=new WorkspaceFinancialRestoreClosureService(db).assess(source,
                List.of("POINT_LEDGER","REPUTATION_LEDGER","MEMBER_GROUPS"));
        assertTrue(result.financialHistoryPresent());
        assertFalse(result.replaySupported());
        var groupResult=result.categories().stream()
                .filter(x->x.category().equals("GROUP_POINT_LEDGER"))
                .findFirst().orElseThrow();
        assertEquals(1L,groupResult.sourceRows());
        assertFalse(groupResult.selected());
        assertTrue(result.blockers().contains(
                "RESTORE_GROUP_LEDGER_REPLAY_UNIMPLEMENTED"));
        assertEquals(10,db.queryForObject("""
                SELECT amount FROM group_point_transactions WHERE group_id=?
                """,Integer.class,group).intValue());
        assertEquals(20,db.queryForObject("""
                SELECT amount FROM group_point_transactions WHERE group_id=?
                """,Integer.class,otherGroup).intValue());
    }
}
