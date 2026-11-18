package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.crypto.KeyGenerator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Test the actual Spring transaction proxy on the staging coordinator. */
@Testcontainers
class WorkspaceBackupStagingProxyIsolationPostgresTest {
    @Container static final PostgreSQLContainer DB =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("backup_proxy_isolation")
                    .withUsername("backup_proxy")
                    .withPassword("backup_proxy");

    @Test void springProxyStartsRepeatableReadAndRejectsWeakOuterTransaction() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var jdbc=new JdbcTemplate(ds);
        var manager=new DataSourceTransactionManager(ds);
        var registry=mock(WorkspaceProtectiveBackupService.class);
        var staging=mock(WorkspaceBackupArtifactStagingService.class);
        var store=mock(WorkspaceBackupLocalArtifactStore.class);
        var target=new WorkspaceBackupRegistryStagingCoordinator(registry,staging,store);
        var factory=new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(manager,
                new AnnotationTransactionAttributeSource()));
        var proxied=(WorkspaceBackupRegistryStagingCoordinator)factory.getProxy();

        var generator=KeyGenerator.getInstance("AES");
        generator.init(256);
        var key=generator.generateKey();
        var request=new WorkspaceProtectiveBackupService.Request(
                UUID.randomUUID(),"SELECTIVE",List.of("MEMBER_GROUPS"),"a".repeat(64),"V50");

        when(registry.request(eq(1L),eq(3L),same(request))).thenAnswer(invocation->{
            assertEquals("repeatable read",
                    jdbc.queryForObject("SHOW transaction_isolation",String.class));
            // Intentional stop after transaction boundary verification: no file writes.
            throw new IllegalStateException("TRANSACTION_ISOLATION_OBSERVED");
        });
        var observed=assertThrows(IllegalStateException.class,
                ()->proxied.registerAndStage(1L,3L,request,0L,key));
        assertEquals("TRANSACTION_ISOLATION_OBSERVED",observed.getMessage());
        verify(registry).request(eq(1L),eq(3L),same(request));
        verifyNoInteractions(staging,store);

        clearInvocations(registry,staging,store);
        var weak=new TransactionTemplate(manager);
        weak.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        var rejected=assertThrows(IllegalStateException.class,()->weak.execute(status->
                proxied.registerAndStage(1L,3L,request,0L,key)));
        assertEquals("BACKUP_SNAPSHOT_ISOLATION_REQUIRED",rejected.getMessage());
        verifyNoInteractions(registry,staging,store);

        // DB connection isn't left in weak isolation by the nested failure.
        assertEquals("read committed",jdbc.queryForObject("SHOW transaction_isolation",String.class));
    }
}
