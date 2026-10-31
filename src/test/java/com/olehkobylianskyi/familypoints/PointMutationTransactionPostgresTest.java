package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.dto.OfflinePointOperationRequest;
import com.olehkobylianskyi.familypoints.repository.*;
import com.olehkobylianskyi.familypoints.service.*;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real Spring proxies, JPA ledger repositories and JDBC family fence share one transaction. */
@Testcontainers
class PointMutationTransactionPostgresTest {
    @Container static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18-alpine");
    private static AnnotationConfigApplicationContext context;
    private static JdbcTemplate sql;
    private static PointMutationService mutations;
    private static TransactionTemplate tx;

    @BeforeAll static void startContext() {
        Flyway.configure().dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword())
                .locations("classpath:db/migration").load().migrate();
        context = new AnnotationConfigApplicationContext(Config.class);
        sql = context.getBean(JdbcTemplate.class);
        mutations = context.getBean(PointMutationService.class);
        tx = new TransactionTemplate(context.getBean(JpaTransactionManager.class));
    }

    @AfterAll static void closeContext() {
        if (context != null) context.close();
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = PointTransactionRepository.class)
    @Import({PointMutationService.class, PointService.class, WorkspaceEpochTransactionGuard.class})
    static class Config {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
        }
        @Bean JdbcTemplate jdbcTemplate(DataSource source) { return new JdbcTemplate(source); }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(source);
            factory.setPackagesToScan("com.olehkobylianskyi.familypoints.entity");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            // Match the application's snake_case columns without Boot auto-configuration.
            factory.getJpaPropertyMap().put("hibernate.physical_naming_strategy", new SnakeCaseColumns());
            return factory;
        }
        @Bean JpaTransactionManager transactionManager(EntityManagerFactory factory, DataSource source) {
            var manager = new JpaTransactionManager(factory);
            manager.setDataSource(source);
            return manager;
        }
    }

    public static class SnakeCaseColumns extends PhysicalNamingStrategyStandardImpl {
        @Override public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment environment) {
            if (name == null) return null;
            return Identifier.toIdentifier(name.getText().replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                    .toLowerCase(java.util.Locale.ROOT), name.isQuoted());
        }
    }

    private record Fixture(long workspace, long member, long currency) {}

    private Fixture fixture() {
        long workspace = sql.queryForObject(
                "INSERT INTO families(name) VALUES ('Disposable point epoch fixture') RETURNING id", Long.class);
        long role = sql.queryForObject("""
                INSERT INTO family_roles(family_id,system_role,code,name)
                VALUES (?,true,'PARENT','Fixture') RETURNING id
                """, Long.class, workspace);
        long member = sql.queryForObject("""
                INSERT INTO family_members(family_id,family_role_id,name,member_type,active)
                VALUES (?,?,'Fixture','PARENT',true) RETURNING id
                """, Long.class, workspace, role);
        long currency = sql.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Fixture',true,0) RETURNING id
                """, Long.class, workspace);
        return new Fixture(workspace, member, currency);
    }

    private OfflinePointOperationRequest request(Fixture fixture) {
        var request = new OfflinePointOperationRequest();
        request.setOperationId(UUID.randomUUID());
        request.setPointTypeId(fixture.currency());
        request.setAmount(5);
        request.setDescription("Disposable fixture");
        return request;
    }

    private long postings(Fixture fixture) {
        return sql.queryForObject("SELECT count(*) FROM point_transactions WHERE member_id=?",
                Long.class, fixture.member());
    }

    @Test void currentEpochPostsOnceAndStaleReplayCannotReturnOldSuccess() {
        var fixture = fixture();
        var request = request(fixture);
        var first = mutations.applyOfflineOperation(fixture.workspace(), fixture.member(), "earn", request, 0L);
        var replay = mutations.applyOfflineOperation(fixture.workspace(), fixture.member(), "earn", request, 0L);
        assertEquals(first.getId(), replay.getId());
        assertEquals(1, postings(fixture));
        sql.update("UPDATE families SET data_epoch=1 WHERE id=?", fixture.workspace());
        var stale = assertThrows(ResponseStatusException.class, () -> mutations.applyOfflineOperation(
                fixture.workspace(), fixture.member(), "earn", request, 0L));
        assertEquals(409, stale.getStatusCode().value());
        assertEquals(1, postings(fixture));
    }

    @Test void missingEpochWorksOnlyAtZeroAndLedgerRollsBackWithOuterTransaction() {
        var fixture = fixture();
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            mutations.applyOfflineOperation(fixture.workspace(), fixture.member(), "earn", request(fixture), null);
            throw new IllegalStateException("Injected failure after JPA flush");
        }));
        assertEquals(0, postings(fixture));
        mutations.applyOfflineOperation(fixture.workspace(), fixture.member(), "earn", request(fixture), null);
        sql.update("UPDATE families SET data_epoch=1 WHERE id=?", fixture.workspace());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> mutations.applyOfflineOperation(
                fixture.workspace(), fixture.member(), "earn", request(fixture), null)).getStatusCode().value());
        assertEquals(1, postings(fixture));
    }

    @Test void mandatoryGuardReallyRequiresSpringTransaction() {
        assertThrows(IllegalTransactionStateException.class, () -> context.getBean(
                WorkspaceEpochTransactionGuard.class).requireCurrentEpoch(1L, 0L));
    }

    @Test void resetBlocksStaleLedgerWriterButAnotherFamilyCanPost() throws Exception {
        var fixture = fixture();
        var other = fixture();
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var holderPid = new CompletableFuture<Integer>();
        var executor = Executors.newFixedThreadPool(3);
        try {
            Future<?> reset = executor.submit(() -> tx.executeWithoutResult(status -> {
                holderPid.complete(sql.queryForObject("SELECT pg_backend_pid()", Integer.class));
                sql.queryForObject("SELECT data_epoch FROM families WHERE id=? FOR UPDATE",
                        Long.class, fixture.workspace());
                locked.countDown();
                await(release);
                sql.update("UPDATE families SET data_epoch=data_epoch+1 WHERE id=?", fixture.workspace());
            }));
            assertTrue(locked.await(15, TimeUnit.SECONDS));
            Future<?> writer = executor.submit(() -> mutations.applyOfflineOperation(
                    fixture.workspace(), fixture.member(), "earn", request(fixture), 0L));
            awaitDatabaseWait(holderPid.get(5, TimeUnit.SECONDS));
            Future<?> unrelated = executor.submit(() -> mutations.applyOfflineOperation(
                    other.workspace(), other.member(), "earn", request(other), 0L));
            unrelated.get(10, TimeUnit.SECONDS);
            assertEquals(1, postings(other));
            assertEquals(0, postings(fixture));
            release.countDown();
            reset.get(15, TimeUnit.SECONDS);
            var failure = assertThrows(ExecutionException.class, () -> writer.get(15, TimeUnit.SECONDS));
            var conflict = assertInstanceOf(ResponseStatusException.class, failure.getCause());
            assertEquals(409, conflict.getStatusCode().value());
            assertEquals(0, postings(fixture));
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS));
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("Fixture lock timeout");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private static void awaitDatabaseWait(int holderPid) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            Boolean blocked = sql.queryForObject("""
                    SELECT EXISTS(SELECT 1 FROM pg_stat_activity
                    WHERE wait_event_type='Lock' AND ?=ANY(pg_blocking_pids(pid))
                    AND query LIKE 'SELECT data_epoch FROM families%')
                    """, Boolean.class, holderPid);
            if (Boolean.TRUE.equals(blocked)) return;
            Thread.sleep(25);
        }
        fail("Writer never reached the database family lock");
    }
}
