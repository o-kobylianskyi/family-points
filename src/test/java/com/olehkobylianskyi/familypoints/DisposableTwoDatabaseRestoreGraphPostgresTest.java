package com.olehkobylianskyi.familypoints;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Backend B / B-B-02: two independent disposable PG18s, not production. */
@Testcontainers
class DisposableTwoDatabaseRestoreGraphPostgresTest {
    @Container static final PostgreSQLContainer SOURCE=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("restore_source").withUsername("source").withPassword("source");
    @Container static final PostgreSQLContainer TARGET=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("restore_target").withUsername("target").withPassword("target");

    record Family(long id,long member) {}
    record Fixture(JdbcTemplate source,JdbcTemplate target,
                   DataSourceTransactionManager transaction,
                   Family original,Family destination,Family neighbor,
                   long originalGroup,long parentNode,long childNode) {
        DisposableTwoDatabaseRestoreGraphExecutor engine() {
            return new DisposableTwoDatabaseRestoreGraphExecutor(source,target,transaction);
        }
        Map<Long,Long> mapping(){return Map.of(original.member,destination.member);}
        long count(String table,long family){
            if(!Set.of("member_groups","work_nodes").contains(table))
                throw new IllegalArgumentException("QA_INVALID_TABLE");
            return target.queryForObject(
                    "SELECT count(*) FROM "+table+" WHERE family_id=?",
                    Long.class,family);
        }
    }
    private static Family family(JdbcTemplate db,String name) {
        long family=db.queryForObject(
                "INSERT INTO families(name) VALUES (?) RETURNING id",Long.class,name);
        long role=db.queryForObject("""
                INSERT INTO family_roles(family_id,code,name,system_role)
                VALUES (?,'PARENT','Parent',true) RETURNING id
                """,Long.class,family);
        long member=db.queryForObject("""
                INSERT INTO family_members(family_id,family_role_id,name,active,member_type)
                VALUES (?,?,'Synthetic adult',true,'PARENT') RETURNING id
                """,Long.class,family,role);
        return new Family(family,member);
    }
    private static Fixture setup() {
        var sourceDs=new DriverManagerDataSource(
                SOURCE.getJdbcUrl(),SOURCE.getUsername(),SOURCE.getPassword());
        var targetDs=new DriverManagerDataSource(
                TARGET.getJdbcUrl(),TARGET.getUsername(),TARGET.getPassword());
        Flyway.configure().dataSource(sourceDs).locations("classpath:db/migration")
                .load().migrate();
        Flyway.configure().dataSource(targetDs).locations("classpath:db/migration")
                .load().migrate();
        var a=new JdbcTemplate(sourceDs);
        var b=new JdbcTemplate(targetDs);
        var src=family(a,"Source "+UUID.randomUUID());
        var other=family(b,"Neighbor "+UUID.randomUUID());
        var dst=family(b,"Target "+UUID.randomUUID());
        long originalGroup=a.queryForObject("""
                INSERT INTO member_groups(family_id,name,active,show_in_navigation)
                VALUES (?,'Source group',true,false) RETURNING id
                """,Long.class,src.id);
        long parent=a.queryForObject("""
                INSERT INTO work_nodes(family_id,type,title,sort_order,active)
                VALUES (?,'GROUP','Source parent',0,true) RETURNING id
                """,Long.class,src.id);
        long child=a.queryForObject("""
                INSERT INTO work_nodes(family_id,parent_node_id,type,title,sort_order,active)
                VALUES (?,?,'GROUP','Source child',2,true) RETURNING id
                """,Long.class,src.id,parent);
        // Prepopulate a completely unrelated target family: its rows must
        // remain byte-for-byte semantically unchanged after any rejection.
        b.update("""
                INSERT INTO member_groups(family_id,name,active,show_in_navigation)
                VALUES (?,'Neighbor group',true,false)
                """,other.id);
        b.update("""
                INSERT INTO work_nodes(family_id,type,title,sort_order,active)
                VALUES (?,'GROUP','Neighbor folder',1,true)
                """,other.id);
        return new Fixture(a,b,new DataSourceTransactionManager(targetDs),
                src,dst,other,originalGroup,parent,child);
    }

    @Test void missingParentAndForeignTargetDenyWithoutWrites() {
        var f=setup();
        assertThrows(SecurityException.class,()->f.engine().apply(
                f.original.id,f.destination.id,f.originalGroup,
                List.of(f.childNode),f.mapping(),0));
        assertThrows(SecurityException.class,()->f.engine().apply(
                f.original.id,f.destination.id,f.originalGroup,
                List.of(f.parentNode,f.childNode),
                Map.of(f.original.member,f.neighbor.member),0));
        assertThrows(SecurityException.class,()->f.engine().apply(
                f.original.id,f.destination.id,Long.MAX_VALUE,
                List.of(f.parentNode,f.childNode),f.mapping(),0));
        assertEquals(0,f.count("member_groups",f.destination.id));
        assertEquals(0,f.count("work_nodes",f.destination.id));
        assertEquals(1,f.count("member_groups",f.neighbor.id));
        assertEquals(1,f.count("work_nodes",f.neighbor.id));
    }

    @Test void everyStageRollsBackAndFreshExecutorRetriesThenRejectsDuplicate() {
        for(int failedStage=1;failedStage<=3;failedStage++) {
            var f=setup();
            final int stage=failedStage;
            assertThrows(IllegalStateException.class,()->f.engine().apply(
                    f.original.id,f.destination.id,f.originalGroup,
                    List.of(f.childNode,f.parentNode),f.mapping(),stage));
            assertEquals(0,f.count("member_groups",f.destination.id));
            assertEquals(0,f.count("work_nodes",f.destination.id));
            var result=f.engine().apply(
                    f.original.id,f.destination.id,f.originalGroup,
                    List.of(f.childNode,f.parentNode),f.mapping(),0);
            assertEquals(2,result.workNodeIds().size());
            assertNotEquals(f.originalGroup,result.groupId());
            assertEquals(1,f.count("member_groups",f.destination.id));
            assertEquals(2,f.count("work_nodes",f.destination.id));
            var mappedChild=result.workNodeIds().get(f.childNode);
            var mappedParent=result.workNodeIds().get(f.parentNode);
            assertEquals(mappedParent,f.target.queryForObject(
                    "SELECT parent_node_id FROM work_nodes WHERE id=?",
                    Long.class,mappedChild));
            assertThrows(SecurityException.class,()->f.engine().apply(
                    f.original.id,f.destination.id,f.originalGroup,
                    List.of(f.parentNode,f.childNode),f.mapping(),0));
            assertEquals(1,f.count("member_groups",f.neighbor.id));
            assertEquals(1,f.count("work_nodes",f.neighbor.id));
        }
    }

    @Test void staleTargetEpochRejectsWithoutPartialGraph() {
        var f=setup();
        f.target.update("UPDATE families SET data_epoch=1 WHERE id=?",f.destination.id);
        assertThrows(SecurityException.class,()->f.engine().apply(
                f.original.id,f.destination.id,f.originalGroup,
                List.of(f.parentNode,f.childNode),f.mapping(),0));
        assertEquals(0,f.count("member_groups",f.destination.id));
        assertEquals(0,f.count("work_nodes",f.destination.id));
    }
}
