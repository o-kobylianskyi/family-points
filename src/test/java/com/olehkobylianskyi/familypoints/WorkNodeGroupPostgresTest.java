package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.WorkNodeGroupService;
import com.olehkobylianskyi.familypoints.service.WorkspaceEpochTransactionGuard;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.*;

/** Disposable DB only: tenant, nested folders, cycles, duplicate names, rollback. */
@Testcontainers
class WorkNodeGroupPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("folder_groups").withUsername("folder_test").withPassword("folder_test_password");

    private record F(JdbcTemplate db,TransactionTemplate tx,WorkNodeGroupService groups,long a,long b) {}
    private F fixture() {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        var db=new JdbcTemplate(ds);
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        long a=db.queryForObject("INSERT INTO families(name) VALUES ('Folder A') RETURNING id",Long.class);
        long b=db.queryForObject("INSERT INTO families(name) VALUES ('Folder B') RETURNING id",Long.class);
        return new F(db,new TransactionTemplate(new DataSourceTransactionManager(ds)),
                new WorkNodeGroupService(db,new WorkspaceEpochTransactionGuard(db)),a,b);
    }
    private static WorkNodeGroupService.Input input(String name,Long parent) {
        return new WorkNodeGroupService.Input(name,null,parent,0);
    }
    @Test void hierarchicalCrudAndTenantProtection() {
        var f=fixture();
        var root=f.tx.execute(s->f.groups.create(f.a,input("Chores",null)));
        assertNotNull(root);
        var child=f.tx.execute(s->f.groups.create(f.a,input("Kitchen",root.id())));
        assertNotNull(child);
        assertEquals(root.id(),child.parentGroupId());
        assertEquals(2,f.groups.list(f.a).size());
        assertTrue(f.groups.list(f.b).isEmpty());
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
                ()->f.tx.execute(s->f.groups.create(f.b,input("Alien",root.id())))).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->f.tx.execute(s->f.groups.deactivate(f.a,root.id()))).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->f.tx.execute(s->f.groups.update(f.a,root.id(),input("Chores",child.id())))).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->f.tx.execute(s->f.groups.create(f.a,input("chores",null)))).getStatusCode());
        f.tx.executeWithoutResult(s->f.groups.deactivate(f.a,child.id()));
        f.tx.executeWithoutResult(s->f.groups.deactivate(f.a,root.id()));
        assertTrue(f.groups.list(f.a).isEmpty());
    }
    @Test void failedTransactionRollsBackEntireFolderCreation() {
        var f=fixture();
        assertThrows(IllegalStateException.class,()->f.tx.executeWithoutResult(s->{
            f.groups.create(f.a,input("Temporary",null));
            throw new IllegalStateException("inject failure");
        }));
        assertTrue(f.groups.list(f.a).isEmpty());
    }
    @Test void rejectsMalformedTitleAndDoesNotTouchOtherFamilies() {
        var f=fixture();
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
                ()->f.tx.execute(s->f.groups.create(f.a,input("  ",null)))).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
                ()->f.tx.execute(s->f.groups.create(f.a,input("x".repeat(151),null)))).getStatusCode());
        assertTrue(f.groups.list(f.b).isEmpty());
    }
}
