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
    @Import({PointMutationService.class, PointService.class, WorkspaceEpochTransactionGuard.class,
            RewardService.class, RewardAvailabilityService.class, ReputationService.class,
            TaskService.class, TaskGenerationService.class, TaskDeadlineService.class,
            TaskAuditService.class, TaskRewardNegotiationService.class, PointExchangeService.class,
            com.olehkobylianskyi.familypoints.controller.TaskScheduleSlotController.class,
            MemberGroupService.class, RoleDefinitionService.class, RoleSetService.class,
            MemberExceptionPeriodService.class, EconomyService.class, StandardCurrencyForms.class,
            MediaService.class, WorkspaceMemberService.class, WorkspaceService.class, CalendarService.class,
            WorkspaceRoleService.class, RoleCatalogBootstrapService.class, LegacyGroupRoleBridgeService.class,
            DomainFoundationMigrationService.class, FamilyWorkspaceProvisioningService.class,
            TemplateCatalogService.class, TemplateEconomySuggestionsService.class,
            WorkspaceResetSnapshotService.class, ResetChallengeAttemptLimiter.class, WorkspaceResetProofService.class,
            WorkspaceInvitationService.class, WorkspaceJoinService.class, AccountMembershipLinkService.class,
            UserAccountService.class,
            com.olehkobylianskyi.familypoints.controller.WorkspaceDataResetPreviewController.class,
            com.olehkobylianskyi.familypoints.controller.WorkspaceDataResetPrepareController.class,
            com.olehkobylianskyi.familypoints.controller.WorkspaceResetChallengeController.class,
            com.olehkobylianskyi.familypoints.controller.TaskSuggestionController.class,
            com.olehkobylianskyi.familypoints.controller.EconomyController.class,
            com.olehkobylianskyi.familypoints.controller.WorkspaceTemplateController.class})
    static class Config {
        @Bean RoleCatalogAuthorizationService roleAuthorization() { return org.mockito.Mockito.mock(RoleCatalogAuthorizationService.class); }
        @Bean com.olehkobylianskyi.familypoints.controller.TaskController templateTaskController() {
            return org.mockito.Mockito.mock(com.olehkobylianskyi.familypoints.controller.TaskController.class);
        }
        @Bean com.olehkobylianskyi.familypoints.controller.RewardController templateRewardController() {
            return org.mockito.Mockito.mock(com.olehkobylianskyi.familypoints.controller.RewardController.class);
        }
        @Bean org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
            return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4);
        }
        @Bean java.time.Clock clock() { return java.time.Clock.systemUTC(); }
        @Bean com.olehkobylianskyi.familypoints.security.CurrentUserService currentUser() {
            return org.mockito.Mockito.mock(com.olehkobylianskyi.familypoints.security.CurrentUserService.class);
        }
        @Bean TaskAuthorizationService taskAuthorization() {
            return org.mockito.Mockito.mock(TaskAuthorizationService.class);
        }
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


    @AfterEach void clearRequest() {
        org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
    }

    private <T> T withEpoch(long epoch, java.util.function.Supplier<T> operation) {
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setMethod("POST");
        request.addHeader("X-Workspace-Data-Epoch", Long.toString(epoch));
        var previous = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(request));
        try { return operation.get(); }
        finally {
            if (previous == null) org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
            else org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(previous);
        }
    }

    private com.olehkobylianskyi.familypoints.entity.TaskDefinition task(Fixture fixture) {
        return tx.execute(status -> {
            var workspace = context.getBean(WorkspaceRepository.class).findById(fixture.workspace()).orElseThrow();
            var member = context.getBean(WorkspaceMemberRepository.class).findById(fixture.member()).orElseThrow();
            var definition = new com.olehkobylianskyi.familypoints.entity.TaskDefinition(
                    workspace, member, "Disposable task", null, false,
                    com.olehkobylianskyi.familypoints.entity.TaskRecurrenceType.DAILY,
                    java.time.LocalDate.now(), null, null, null, null, null, null, null, null);
            return context.getBean(TaskDefinitionRepository.class).saveAndFlush(definition);
        });
    }

    private com.olehkobylianskyi.familypoints.dto.RewardCategoryRequest category() {
        var input = new com.olehkobylianskyi.familypoints.dto.RewardCategoryRequest();
        input.setName("Disposable category");
        return input;
    }

    private long categoryCount(Fixture fixture) {
        return sql.queryForObject("SELECT count(*) FROM reward_categories WHERE family_id=?",
                Long.class, fixture.workspace());
    }

    @Test void taskServiceRejectsStaleRequestAndCurrentRequestPersistsInItsOwnTransaction() {
        var fixture = fixture();
        var definition = task(fixture);
        sql.update("UPDATE families SET data_epoch=1 WHERE id=?", fixture.workspace());
        var tasks = context.getBean(TaskService.class);
        var error = assertThrows(ResponseStatusException.class, () -> withEpoch(0L,
                () -> tasks.setDefinitionActive(fixture.workspace(), definition.getId(), false)));
        assertEquals(409, error.getStatusCode().value());
        assertEquals(true, sql.queryForObject("SELECT active FROM task_definitions WHERE id=?", Boolean.class, definition.getId()));
        var member = context.getBean(WorkspaceMemberRepository.class).findById(fixture.member()).orElseThrow();
        org.mockito.Mockito.when(context.getBean(
                com.olehkobylianskyi.familypoints.security.CurrentUserService.class).getCurrentMember()).thenReturn(member);
        withEpoch(1L, () -> tasks.setDefinitionActive(fixture.workspace(), definition.getId(), false));
        assertEquals(false, sql.queryForObject("SELECT active FROM task_definitions WHERE id=?", Boolean.class, definition.getId()));
        assertEquals(1L, sql.queryForObject("SELECT count(*) FROM task_audit_events WHERE task_definition_id=?",
                Long.class, definition.getId()));
    }

    @Test void rewardServiceRejectsStaleEpochAndRollsBackFlushedCategory() {
        var fixture = fixture();
        var rewards = context.getBean(RewardService.class);
        sql.update("UPDATE families SET data_epoch=1 WHERE id=?", fixture.workspace());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> withEpoch(0L,
                () -> rewards.createCategory(fixture.workspace(), category()))).getStatusCode().value());
        assertEquals(0, categoryCount(fixture));
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> withEpoch(1L, () -> {
            rewards.createCategory(fixture.workspace(), category());
            throw new IllegalStateException("Injected failure after category insert");
        })));
        assertEquals(0, categoryCount(fixture));
        withEpoch(1L, () -> rewards.createCategory(fixture.workspace(), category()));
        assertEquals(1, categoryCount(fixture));
        // GET-derived obligation refresh can run at nonzero epoch without a mutation header.
        assertTrue(rewards.listOpenObligations(fixture.workspace(), fixture.member(), true).isEmpty());
    }

    @Test void resetFencesRealTaskServiceWhileAnotherFamilyCreatesRewardCategory() throws Exception {
        var fixture = fixture();
        var other = fixture();
        var definition = task(fixture);
        var release = new CountDownLatch(1);
        var locked = new CountDownLatch(1);
        var pid = new CompletableFuture<Integer>();
        var executor = Executors.newFixedThreadPool(3);
        try {
            Future<?> reset = executor.submit(() -> tx.executeWithoutResult(status -> {
                pid.complete(sql.queryForObject("SELECT pg_backend_pid()", Integer.class));
                sql.queryForObject("SELECT data_epoch FROM families WHERE id=? FOR UPDATE", Long.class, fixture.workspace());
                locked.countDown();
                await(release);
                sql.update("UPDATE families SET data_epoch=data_epoch+1 WHERE id=?", fixture.workspace());
            }));
            assertTrue(locked.await(15, TimeUnit.SECONDS));
            Future<?> writer = executor.submit(() -> withEpoch(0L, () -> context.getBean(TaskService.class)
                    .setDefinitionActive(fixture.workspace(), definition.getId(), false)));
            awaitDatabaseWait(pid.get(5, TimeUnit.SECONDS));
            Future<?> unrelated = executor.submit(() -> withEpoch(0L, () -> context.getBean(RewardService.class)
                    .createCategory(other.workspace(), category())));
            unrelated.get(10, TimeUnit.SECONDS);
            assertEquals(1, categoryCount(other));
            release.countDown();
            reset.get(15, TimeUnit.SECONDS);
            var failure = assertThrows(ExecutionException.class, () -> writer.get(15, TimeUnit.SECONDS));
            assertEquals(409, assertInstanceOf(ResponseStatusException.class, failure.getCause()).getStatusCode().value());
            assertEquals(true, sql.queryForObject("SELECT active FROM task_definitions WHERE id=?", Boolean.class, definition.getId()));
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS));
        }
    }

    @Test void systemGenerationReloadsAndDoesNotResurrectTaskDeletedByReset() {
        var fixture = fixture();
        var detached = task(fixture);
        tx.executeWithoutResult(status -> {
            context.getBean(TaskDefinitionRepository.class).deleteById(detached.getId());
            sql.update("UPDATE families SET data_epoch=1 WHERE id=?", fixture.workspace());
        });
        assertTrue(context.getBean(TaskGenerationService.class)
                .generateForDefinition(detached, java.time.LocalDate.now()).isEmpty());
        assertEquals(0L, sql.queryForObject("SELECT count(*) FROM task_instances WHERE task_definition_id=?",
                Long.class, detached.getId()));
    }

    @Test void automaticDeadlineScanUsesCurrentGenerationAfterNonzeroEpoch() {
        var fixture = fixture();
        sql.update("UPDATE families SET data_epoch=1 WHERE id=?", fixture.workspace());
        assertTrue(context.getBean(TaskDeadlineService.class)
                .processDeadlines(fixture.workspace(), java.time.LocalDateTime.now()).isEmpty());
        assertEquals(1L, sql.queryForObject("SELECT data_epoch FROM families WHERE id=?", Long.class, fixture.workspace()));
    }


    private long reputationCount(Fixture fixture) {
        return sql.queryForObject("SELECT count(*) FROM reputation_transactions WHERE member_id=?", Long.class, fixture.member());
    }

    private long balance(Fixture fixture, long currency) {
        return sql.queryForObject("SELECT coalesce(sum(amount),0) FROM point_transactions WHERE member_id=? AND point_type_id=?",
                Long.class, fixture.member(), currency);
    }

    @Test void scheduleSlotMutationsRejectStaleEpochAndRollbackAuditWithData() {
        var fixture = fixture();
        var definition = task(fixture);
        var member = context.getBean(WorkspaceMemberRepository.class).findById(fixture.member()).orElseThrow();
        org.mockito.Mockito.when(context.getBean(TaskAuthorizationService.class).currentMember()).thenReturn(member);
        var slots = context.getBean(com.olehkobylianskyi.familypoints.controller.TaskScheduleSlotController.class);
        var created = withEpoch(0L, () -> slots.create(fixture.workspace(), definition.getId(),
                new com.olehkobylianskyi.familypoints.controller.TaskScheduleSlotController.SlotRequest(
                        "morning", java.time.LocalTime.of(8,0))));
        sql.update("UPDATE families SET data_epoch=1 WHERE id=?", fixture.workspace());
        var update = new com.olehkobylianskyi.familypoints.controller.TaskScheduleSlotController.SlotUpdateRequest(
                java.time.LocalTime.of(9,0), false);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> withEpoch(0L,
                () -> slots.update(fixture.workspace(), definition.getId(), created.id(), update))).getStatusCode().value());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> withEpoch(0L,
                () -> slots.setEnabled(fixture.workspace(), definition.getId(), created.id(), false))).getStatusCode().value());
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> withEpoch(1L, () -> {
            slots.update(fixture.workspace(), definition.getId(), created.id(), update);
            throw new IllegalStateException("Injected slot update failure");
        })));
        assertEquals(java.time.LocalTime.of(8,0), sql.queryForObject(
                "SELECT scheduled_time FROM task_schedule_slots WHERE id=?", java.time.LocalTime.class, created.id()));
        assertEquals(true, sql.queryForObject("SELECT enabled FROM task_schedule_slots WHERE id=?", Boolean.class, created.id()));
        assertEquals(1L, sql.queryForObject("SELECT count(*) FROM task_audit_events WHERE task_definition_id=?",
                Long.class, definition.getId()));
        withEpoch(1L, () -> slots.update(fixture.workspace(), definition.getId(), created.id(), update));
        assertEquals(false, sql.queryForObject("SELECT enabled FROM task_schedule_slots WHERE id=?", Boolean.class, created.id()));
    }

    @Test void exchangeEpochFencesTheRequestAndBothCurrencyPostingsAtomically() {
        var fixture = fixture();
        long target = sql.queryForObject(
                "INSERT INTO point_types(family_id,code,name,active,sort_order) VALUES (?,'SILVER','Fixture',true,1) RETURNING id",
                Long.class, fixture.workspace());
        sql.update("UPDATE families SET data_epoch=1 WHERE id=?", fixture.workspace());
        var points = context.getBean(PointService.class);
        var exchange = context.getBean(PointExchangeService.class);
        var rate = withEpoch(1L, () -> exchange.createRate(fixture.workspace(), fixture.currency(), target,
                10, 1, com.olehkobylianskyi.familypoints.entity.PointExchangeApprovalMode.AUTO));
        withEpoch(1L, () -> points.earn(fixture.workspace(), fixture.member(), 20, "Disposable funding"));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> withEpoch(0L,
                () -> exchange.createExchange(fixture.workspace(), fixture.member(), rate.getId(), 10))).getStatusCode().value());
        assertEquals(0L, sql.queryForObject("SELECT count(*) FROM point_exchange_requests WHERE member_id=?", Long.class, fixture.member()));
        var result = withEpoch(1L, () -> exchange.createExchange(fixture.workspace(), fixture.member(), rate.getId(), 10));
        assertEquals(com.olehkobylianskyi.familypoints.entity.PointExchangeStatus.APPROVED, result.getStatus());
        assertEquals(10, balance(fixture, fixture.currency()));
        assertEquals(1, balance(fixture, target));
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> withEpoch(1L, () -> {
            exchange.createExchange(fixture.workspace(), fixture.member(), rate.getId(), 10);
            throw new IllegalStateException("Injected exchange failure after both postings");
        })));
        assertEquals(10, balance(fixture, fixture.currency()));
        assertEquals(1, balance(fixture, target));
        assertEquals(3, postings(fixture));
        assertEquals(1L, sql.queryForObject("SELECT count(*) FROM point_exchange_requests WHERE member_id=?", Long.class, fixture.member()));
    }

    @Test void internalLedgerWritesRequireValidatedParentAndProofExpiresAtTransactionEnd() {
        var fixture = fixture();
        sql.update("UPDATE families SET data_epoch=1 WHERE id=?", fixture.workspace());
        var guard = context.getBean(WorkspaceEpochTransactionGuard.class);
        var points = context.getBean(PointService.class);
        var reputation = context.getBean(ReputationService.class);
        assertThrows(ResponseStatusException.class, () -> points.earn(fixture.workspace(), fixture.member(), 5, "No proof"));
        assertThrows(ResponseStatusException.class, () -> reputation.add(fixture.workspace(), fixture.member(), 3,
                com.olehkobylianskyi.familypoints.entity.ReputationTransactionSourceType.MANUAL_BONUS, null, "No proof"));
        tx.executeWithoutResult(status -> {
            guard.lockCurrentEpoch(fixture.workspace());
            points.earn(fixture.workspace(), fixture.member(), 5, "Server-derived fixture");
            reputation.add(fixture.workspace(), fixture.member(), 3,
                    com.olehkobylianskyi.familypoints.entity.ReputationTransactionSourceType.MANUAL_BONUS, null, "Fixture");
        });
        assertEquals(1, postings(fixture));
        assertEquals(1, reputationCount(fixture));
        assertThrows(ResponseStatusException.class, () -> points.earn(fixture.workspace(), fixture.member(), 5, "Expired proof"));
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            guard.lockCurrentEpoch(fixture.workspace());
            reputation.add(fixture.workspace(), fixture.member(), 3,
                    com.olehkobylianskyi.familypoints.entity.ReputationTransactionSourceType.MANUAL_BONUS, null, "Rollback");
            throw new IllegalStateException("Injected reputation failure");
        }));
        assertEquals(1, reputationCount(fixture));
        assertThrows(ResponseStatusException.class, () -> reputation.add(fixture.workspace(), fixture.member(), 3,
                com.olehkobylianskyi.familypoints.entity.ReputationTransactionSourceType.MANUAL_BONUS, null, "Expired rollback proof"));
    }

    @Test void validatedEpochIsSuspendedForRequiresNewAndRestoredOnlyForItsParent() {
        var fixture = fixture();
        var other = fixture();
        sql.update("UPDATE families SET data_epoch=1 WHERE id IN (?,?)", fixture.workspace(), other.workspace());
        var guard = context.getBean(WorkspaceEpochTransactionGuard.class);
        var inner = new TransactionTemplate(context.getBean(JpaTransactionManager.class));
        inner.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        var rememberedKey = new java.util.concurrent.atomic.AtomicReference<Object>();
        tx.executeWithoutResult(status -> {
            guard.lockCurrentEpoch(fixture.workspace());
            var entry = org.springframework.transaction.support.TransactionSynchronizationManager.getResourceMap()
                    .entrySet().stream().filter(e -> e.getValue() instanceof java.util.Map<?,?> map
                            && map.containsKey(fixture.workspace())).findFirst().orElseThrow();
            rememberedKey.set(entry.getKey());
            Object parentProof = entry.getValue();
            inner.executeWithoutResult(innerStatus -> {
                assertNull(org.springframework.transaction.support.TransactionSynchronizationManager.getResource(entry.getKey()));
                guard.lockCurrentEpoch(other.workspace());
                assertNotSame(parentProof, org.springframework.transaction.support.TransactionSynchronizationManager.getResource(entry.getKey()));
                context.getBean(PointService.class).earn(other.workspace(), other.member(), 2, "Independent inner");
            });
            assertSame(parentProof, org.springframework.transaction.support.TransactionSynchronizationManager.getResource(entry.getKey()));
            assertThrows(IllegalStateException.class, () -> inner.executeWithoutResult(innerStatus -> {
                assertNull(org.springframework.transaction.support.TransactionSynchronizationManager.getResource(entry.getKey()));
                guard.lockCurrentEpoch(other.workspace());
                context.getBean(PointService.class).earn(other.workspace(), other.member(), 2, "Rolled back inner");
                throw new IllegalStateException("Injected independent transaction rollback");
            }));
            assertSame(parentProof, org.springframework.transaction.support.TransactionSynchronizationManager.getResource(entry.getKey()));
            context.getBean(PointService.class).earn(fixture.workspace(), fixture.member(), 5, "Restored parent");
        });
        assertNull(org.springframework.transaction.support.TransactionSynchronizationManager.getResource(rememberedKey.get()));
        assertEquals(1, postings(fixture));
        assertEquals(1, postings(other));
    }

    @Test void capturedEpochCannotBeReplacedWithFreshDatabaseEpochForInternalPosting() {
        var fixture = fixture();
        var guard = context.getBean(WorkspaceEpochTransactionGuard.class);
        assertThrows(ResponseStatusException.class, () -> tx.executeWithoutResult(status -> {
            guard.requireCurrentEpoch(fixture.workspace(), 0L);
            sql.update("UPDATE families SET data_epoch=1 WHERE id=?", fixture.workspace());
            context.getBean(PointService.class).earn(fixture.workspace(), fixture.member(), 5, "Stale captured epoch");
        }));
        assertEquals(0L, sql.queryForObject("SELECT data_epoch FROM families WHERE id=?", Long.class, fixture.workspace()));
        assertEquals(0, postings(fixture));
    }

    @Test void validatedParentCannotAuthorizeOtherFamilyOrOverrideHttpHeader() {
        var fixture = fixture();
        var other = fixture();
        sql.update("UPDATE families SET data_epoch=1 WHERE id IN (?,?)", fixture.workspace(), other.workspace());
        var guard = context.getBean(WorkspaceEpochTransactionGuard.class);
        assertThrows(ResponseStatusException.class, () -> tx.executeWithoutResult(status -> {
            guard.lockCurrentEpoch(fixture.workspace());
            context.getBean(PointService.class).earn(other.workspace(), other.member(), 5, "Wrong family proof");
        }));
        assertThrows(ResponseStatusException.class, () -> tx.executeWithoutResult(status -> {
            guard.lockCurrentEpoch(fixture.workspace());
            withEpoch(0L, () -> context.getBean(PointService.class).earn(fixture.workspace(), fixture.member(), 5, "Stale HTTP"));
        }));
        assertEquals(0, postings(fixture));
        assertEquals(0, postings(other));
    }

    @Test void penaltyReloadsPointTypeInTheSameFamilyInsteadOfTrustingDetachedInput() {
        var fixture = fixture();
        var other = fixture();
        var foreign = context.getBean(PointTypeRepository.class).findById(other.currency()).orElseThrow();
        assertThrows(com.olehkobylianskyi.familypoints.exception.ResourceNotFoundException.class,
                () -> withEpoch(0L, () -> context.getBean(PointService.class).penalty(
                        fixture.workspace(), fixture.member(), foreign, 1,
                        com.olehkobylianskyi.familypoints.entity.PointTransactionSourceType.TASK, null, "Foreign fixture")));
        assertEquals(0, postings(fixture));
    }

    @Test void backgroundDeadlinePenaltiesJoinValidatedGenerationAtNonzeroEpochExactlyOnce() {
        var fixture = fixture();
        var definition = task(fixture);
        var yesterday = java.time.LocalDate.now().minusDays(1);
        tx.executeWithoutResult(status -> {
            var current = context.getBean(TaskDefinitionRepository.class).findById(definition.getId()).orElseThrow();
            current.setMandatory(true);
            current.setStartDate(yesterday);
            current.setEndDate(yesterday);
            current.setPenaltyPointType(context.getBean(PointTypeRepository.class).findById(fixture.currency()).orElseThrow());
            current.setPenaltyAmount(2);
            current.setPenaltyReputationAmount(3);
            sql.update("UPDATE families SET data_epoch=1 WHERE id=?", fixture.workspace());
        });
        var deadlines = context.getBean(TaskDeadlineService.class);
        var now = java.time.LocalDate.now().atTime(12,0);
        assertEquals(1, deadlines.processDeadlines(fixture.workspace(), now).size());
        assertEquals(-2, balance(fixture, fixture.currency()));
        assertEquals(-3L, sql.queryForObject("SELECT sum(amount) FROM reputation_transactions WHERE member_id=?", Long.class, fixture.member()));
        assertTrue(deadlines.processDeadlines(fixture.workspace(), now).isEmpty());
        assertEquals(1, postings(fixture));
        assertEquals(1, reputationCount(fixture));
    }


    private void authorizeFixture(Fixture fixture) {
        var current = context.getBean(com.olehkobylianskyi.familypoints.security.CurrentUserService.class);
        var actor = org.mockito.Mockito.mock(com.olehkobylianskyi.familypoints.entity.WorkspaceMember.class);
        org.mockito.Mockito.when(actor.getId()).thenReturn(fixture.member());
        org.mockito.Mockito.when(actor.getWorkspace()).thenReturn(context.getBean(WorkspaceRepository.class)
                .findById(fixture.workspace()).orElseThrow());
        org.mockito.Mockito.when(actor.getMemberType()).thenReturn(com.olehkobylianskyi.familypoints.entity.WorkspaceMemberType.PARENT);
        org.mockito.Mockito.when(actor.hasPermission(org.mockito.ArgumentMatchers.any(
                com.olehkobylianskyi.familypoints.entity.WorkspacePermission.class))).thenReturn(true);
        org.mockito.Mockito.when(current.belongsToWorkspace(fixture.workspace())).thenReturn(true);
        org.mockito.Mockito.when(current.getCurrentMember()).thenReturn(actor);
        org.mockito.Mockito.when(current.getCurrentMemberId()).thenReturn(fixture.member());
    }

    @Test void groupsRolesAndGroupLedgerShareEpochFenceAndRollback() {
        var fixture = fixture();
        sql.update("UPDATE families SET data_epoch=1 WHERE id=?", fixture.workspace());
        var groups = context.getBean(MemberGroupService.class);
        var sets = context.getBean(RoleSetService.class);
        var roles = context.getBean(RoleDefinitionService.class);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> withEpoch(0L,
                () -> groups.createGroup(fixture.workspace(), "Stale", null))).getStatusCode().value());
        withEpoch(1L, () -> groups.createGroup(fixture.workspace(), "Fixture group", null));
        long group = sql.queryForObject("SELECT id FROM member_groups WHERE family_id=?", Long.class, fixture.workspace());
        withEpoch(1L, () -> groups.addPoints(fixture.workspace(), group, fixture.currency(), 5,
                com.olehkobylianskyi.familypoints.entity.PointTransactionType.EARN, "Fixture"));
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> withEpoch(1L, () -> {
            groups.addPoints(fixture.workspace(), group, fixture.currency(), 2,
                    com.olehkobylianskyi.familypoints.entity.PointTransactionType.SPEND, "Rollback");
            sets.create(fixture.workspace(), "Rollback set", null);
            throw new IllegalStateException("Injected group/role failure");
        })));
        assertEquals(5L, context.getBean(GroupPointTransactionRepository.class).getBalance(group, fixture.currency()));
        assertEquals(0L, sql.queryForObject("SELECT count(*) FROM role_sets WHERE family_id=?", Long.class, fixture.workspace()));
        withEpoch(1L, () -> sets.create(fixture.workspace(), "Fixture set", null));
        var request = new com.olehkobylianskyi.familypoints.dto.RoleDefinitionRequest();
        request.setName("Fixture role");
        withEpoch(1L, () -> roles.create(fixture.workspace(), request));
        assertEquals(1L, sql.queryForObject("SELECT count(*) FROM role_definitions WHERE family_id=?", Long.class, fixture.workspace()));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> withEpoch(0L,
                () -> sets.create(fixture.workspace(), "Stale set", null))).getStatusCode().value());
    }

    @Test void lazyEconomyReadsWorkAfterResetAndPreserveExplicitCurrencyForms() {
        var fixture = fixture();
        sql.update("UPDATE families SET data_epoch=1 WHERE id=?", fixture.workspace());
        var economy = context.getBean(EconomyService.class);
        economy.getOrCreateSettings(fixture.workspace());
        assertEquals(4L, sql.queryForObject("SELECT count(*) FROM point_types WHERE family_id=?", Long.class, fixture.workspace()));
        var controller = context.getBean(com.olehkobylianskyi.familypoints.controller.EconomyController.class);
        withEpoch(1L, () -> controller.saveNameForms(fixture.workspace(), fixture.currency(), "en",
                new com.olehkobylianskyi.familypoints.dto.PointTypeNameFormRequest("custom-one","custom-few","custom-many")));
        economy.getActivePointTypes(fixture.workspace());
        economy.getAllPointTypes(fixture.workspace());
        assertEquals("custom-one", sql.queryForObject(
                "SELECT one FROM point_type_name_forms WHERE point_type_id=? AND language='en'", String.class, fixture.currency()));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> withEpoch(0L,
                () -> economy.createPointType(fixture.workspace(), "EXTRA", "Stale", 1))).getStatusCode().value());
        assertEquals(4L, sql.queryForObject("SELECT count(*) FROM point_types WHERE family_id=?", Long.class, fixture.workspace()));
        assertEquals(0, postings(fixture));
    }

    @Test void templateCrudAndActivationClaimAreFencedBeforeDownstreamCreation() {
        var fixture = fixture();
        sql.update("UPDATE families SET data_epoch=1 WHERE id=?", fixture.workspace());
        var templates = context.getBean(com.olehkobylianskyi.familypoints.controller.WorkspaceTemplateController.class);
        var input = new com.olehkobylianskyi.familypoints.controller.WorkspaceTemplateController.TemplateRequest(
                "Fixture reward", "{\"currencyCode\":\"POINTS\",\"priceAmount\":1}", "REWARD");
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> withEpoch(0L,
                () -> { templates.create(fixture.workspace(), input); return null; })).getStatusCode().value());
        withEpoch(1L, () -> { templates.create(fixture.workspace(), input); return null; });
        long template = sql.queryForObject("SELECT id FROM workspace_private_templates WHERE workspace_id=?", Long.class, fixture.workspace());
        var request = new com.olehkobylianskyi.familypoints.dto.RewardDefinitionRequest();
        request.setTitle("Fixture reward");
        request.setPointTypeId(fixture.currency());
        request.setPriceAmount(1);
        var downstream = context.getBean(com.olehkobylianskyi.familypoints.controller.RewardController.class);
        org.mockito.Mockito.when(downstream.createReward(org.mockito.ArgumentMatchers.eq(fixture.workspace()),
                org.mockito.ArgumentMatchers.same(request))).thenThrow(new IllegalStateException("Injected creation failure"));
        assertThrows(IllegalStateException.class, () -> withEpoch(1L,
                () -> templates.activateReward(fixture.workspace(), template, UUID.randomUUID(), request)));
        assertEquals(0L, sql.queryForObject("SELECT count(*) FROM template_activation_requests WHERE workspace_id=?", Long.class, fixture.workspace()));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> withEpoch(0L,
                () -> templates.activateReward(fixture.workspace(), template, UUID.randomUUID(), request))).getStatusCode().value());
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> withEpoch(1L, () -> {
            templates.update(fixture.workspace(), template,
                    new com.olehkobylianskyi.familypoints.controller.WorkspaceTemplateController.TemplateRequest("Rollback", "{}", "REWARD"));
            throw new IllegalStateException("Injected template edit failure");
        })));
        assertEquals("Fixture reward", sql.queryForObject("SELECT title FROM workspace_private_templates WHERE id=?", String.class, template));
        assertEquals(0, postings(fixture));
    }

    @Test void mediaUploadBindingAndClearRollbackUseOneFamilyGeneration() throws Exception {
        var fixture = fixture();
        authorizeFixture(fixture);
        sql.update("UPDATE families SET data_epoch=1 WHERE id=?", fixture.workspace());
        var output = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB), "png", output);
        var file = new org.springframework.mock.web.MockMultipartFile("file","fixture.png","image/png",output.toByteArray());
        var media = context.getBean(MediaService.class);
        var uploaded = withEpoch(1L, () -> media.upload(fixture.workspace(), file));
        withEpoch(1L, () -> media.bind(fixture.workspace(), "WORKSPACE", fixture.workspace(), "COVER", uploaded.assetId(), null, 0));
        assertTrue(media.getBoundVariant(fixture.workspace(), uploaded.assetId(), "THUMB").bytes().length > 0);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> withEpoch(0L,
                () -> media.clear(fixture.workspace(), "WORKSPACE", fixture.workspace(), "COVER", 1))).getStatusCode().value());
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> withEpoch(1L, () -> {
            media.clear(fixture.workspace(), "WORKSPACE", fixture.workspace(), "COVER", 1);
            throw new IllegalStateException("Injected media clear failure");
        })));
        assertEquals(uploaded.assetId(), sql.queryForObject(
                "SELECT asset_id FROM media_bindings WHERE workspace_id=? AND target_type='WORKSPACE' AND slot='COVER'",
                String.class, fixture.workspace()));
        assertEquals(1L, sql.queryForObject("SELECT count(*) FROM media_assets WHERE workspace_id=?", Long.class, fixture.workspace()));
    }

    @Test void suggestionWritesHaveOuterTransactionAndCannotInventTasksOrPoints() {
        var fixture = fixture();
        authorizeFixture(fixture);
        sql.update("UPDATE families SET data_epoch=1 WHERE id=?", fixture.workspace());
        var suggestions = context.getBean(com.olehkobylianskyi.familypoints.controller.TaskSuggestionController.class);
        var request = new com.olehkobylianskyi.familypoints.controller.TaskSuggestionController.SuggestionRequest("Fixture suggestion", null);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> withEpoch(0L,
                () -> { suggestions.suggest(fixture.workspace(), request); return null; })).getStatusCode().value());
        withEpoch(1L, () -> { suggestions.suggest(fixture.workspace(), request); return null; });
        long id = sql.queryForObject("SELECT id FROM task_suggestions WHERE workspace_id=?", Long.class, fixture.workspace());
        withEpoch(1L, () -> { suggestions.decide(fixture.workspace(), id, "APPROVED"); return null; });
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> withEpoch(1L, () -> {
            suggestions.suggest(fixture.workspace(), request);
            throw new IllegalStateException("Injected suggestion failure");
        })));
        assertEquals(1L, sql.queryForObject("SELECT count(*) FROM task_suggestions WHERE workspace_id=?", Long.class, fixture.workspace()));
        assertEquals(0L, sql.queryForObject("SELECT count(*) FROM task_definitions WHERE family_id=?", Long.class, fixture.workspace()));
        assertEquals(0, postings(fixture));
    }

    @Test void workspaceMemberAndTimezoneEditsShareFenceAndRollback() {
        var fixture = fixture();
        authorizeFixture(fixture);
        sql.update("UPDATE families SET data_epoch=1 WHERE id=?", fixture.workspace());
        var members = context.getBean(WorkspaceMemberService.class);
        var workspaces = context.getBean(WorkspaceService.class);
        var calendar = context.getBean(CalendarService.class);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> withEpoch(0L,
                () -> members.updateMember(fixture.workspace(), fixture.member(), "Stale", null, null))).getStatusCode().value());
        withEpoch(1L, () -> members.updateMember(fixture.workspace(), fixture.member(), "Renamed fixture", null, null));
        withEpoch(1L, () -> workspaces.updateWorkspace(fixture.workspace(), "Renamed family"));
        withEpoch(1L, () -> calendar.updateZone(fixture.workspace(), "Europe/Berlin"));
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> withEpoch(1L, () -> {
            members.updateMember(fixture.workspace(), fixture.member(), "Rollback", null, null);
            workspaces.updateWorkspace(fixture.workspace(), "Rollback");
            calendar.updateZone(fixture.workspace(), "Europe/London");
            throw new IllegalStateException("Injected settings failure");
        })));
        assertEquals("Renamed fixture", sql.queryForObject("SELECT name FROM family_members WHERE id=?", String.class, fixture.member()));
        assertEquals("Renamed family", sql.queryForObject("SELECT name FROM families WHERE id=?", String.class, fixture.workspace()));
        assertEquals("Europe/Berlin", calendar.getZone(fixture.workspace()));
    }

    @Test void startupBackfillReloadsPerFamilyAfterLockAndRemainsIdempotent() {
        var fixture = fixture();
        var definition = task(fixture);
        sql.update("UPDATE families SET data_epoch=1 WHERE id=?", fixture.workspace());
        var migration = context.getBean(DomainFoundationMigrationService.class);
        migration.migrateExistingTaskDefinitions();
        migration.migrateExistingTaskDefinitions();
        assertNotNull(sql.queryForObject("SELECT work_node_id FROM task_definitions WHERE id=?", Long.class, definition.getId()));
        assertEquals(1L, sql.queryForObject("SELECT count(*) FROM task_participants WHERE task_definition_id=?", Long.class, definition.getId()));
        assertEquals(1L, sql.queryForObject("SELECT count(*) FROM work_nodes WHERE family_id=?", Long.class, fixture.workspace()));
        assertEquals(0, postings(fixture));
    }

    @Test void freshFamilyProvisioningDoesNotTreatOldWorkspaceHeaderAsNewFamilyEpoch() {
        var provisioner = context.getBean(FamilyWorkspaceProvisioningService.class);
        var family = withEpoch(99L, () -> provisioner.provision("Disposable fresh family", "Owner fixture"));
        assertEquals(0L, sql.queryForObject("SELECT data_epoch FROM families WHERE id=?", Long.class, family.workspaceId()));
        assertEquals(3L, sql.queryForObject("SELECT count(*) FROM family_roles WHERE family_id=?", Long.class, family.workspaceId()));
        assertEquals(4L, sql.queryForObject("SELECT count(*) FROM point_types WHERE family_id=?", Long.class, family.workspaceId()));
        assertEquals(1L, sql.queryForObject("SELECT count(*) FROM family_members WHERE family_id=? AND active=true", Long.class, family.workspaceId()));
    }


    @Test void revokingOneFamilyMemberPreservesGlobalAccountAndOtherFamilyMembership() {
        var first = fixture();
        var other = fixture();
        long[] ids = tx.execute(status -> {
            var workspace = context.getBean(WorkspaceRepository.class).findById(first.workspace()).orElseThrow();
            var role = context.getBean(WorkspaceMemberRepository.class).findById(first.member()).orElseThrow().getWorkspaceRole();
            var removed = context.getBean(WorkspaceMemberRepository.class).saveAndFlush(
                    new com.olehkobylianskyi.familypoints.entity.WorkspaceMember("Secondary fixture",
                            com.olehkobylianskyi.familypoints.entity.WorkspaceMemberType.PARENT, role, workspace));
            var account = context.getBean(UserAccountRepository.class).saveAndFlush(
                    new com.olehkobylianskyi.familypoints.entity.UserAccount("fixture-"+UUID.randomUUID(), "fixture-hash", removed));
            context.getBean(AccountWorkspaceMembershipRepository.class).save(
                    new com.olehkobylianskyi.familypoints.entity.AccountWorkspaceMembership(account, removed));
            var otherMember = context.getBean(WorkspaceMemberRepository.class).findById(other.member()).orElseThrow();
            context.getBean(AccountWorkspaceMembershipRepository.class).save(
                    new com.olehkobylianskyi.familypoints.entity.AccountWorkspaceMembership(account, otherMember));
            sql.update("UPDATE families SET data_epoch=1 WHERE id=?", first.workspace());
            return new long[]{removed.getId(), account.getId()};
        });
        assertTrue(withEpoch(1L, () -> context.getBean(WorkspaceMemberService.class).deleteMember(first.workspace(), ids[0])));
        assertEquals(false, sql.queryForObject("SELECT active FROM family_members WHERE id=?", Boolean.class, ids[0]));
        assertEquals(true, sql.queryForObject("SELECT enabled FROM user_accounts WHERE id=?", Boolean.class, ids[1]));
        assertEquals(true, sql.queryForObject("SELECT active FROM family_members WHERE id=?", Boolean.class, other.member()));
        assertEquals(1L, sql.queryForObject("SELECT count(*) FROM account_workspace_memberships WHERE account_id=? AND workspace_id=?",
                Long.class, ids[1], other.workspace()));
        assertEquals(true, sql.queryForObject("SELECT active FROM family_members WHERE id=?", Boolean.class, first.member()));
    }


    private com.olehkobylianskyi.familypoints.entity.UserAccount resetActor(Fixture fixture) {
        var account = tx.execute(status -> {
            var member = context.getBean(WorkspaceMemberRepository.class).findById(fixture.member()).orElseThrow();
            var saved = context.getBean(UserAccountRepository.class).saveAndFlush(
                    new com.olehkobylianskyi.familypoints.entity.UserAccount("reset-"+UUID.randomUUID(),
                            context.getBean(org.springframework.security.crypto.password.PasswordEncoder.class).encode("test-password"), member));
            context.getBean(AccountWorkspaceMembershipRepository.class).saveAndFlush(
                    new com.olehkobylianskyi.familypoints.entity.AccountWorkspaceMembership(saved, member));
            return saved;
        });
        var current = context.getBean(com.olehkobylianskyi.familypoints.security.CurrentUserService.class);
        org.mockito.Mockito.when(current.getCurrentAccount()).thenReturn(account);
        org.mockito.Mockito.when(current.getCurrentMemberId()).thenReturn(fixture.member());
        org.mockito.Mockito.when(current.belongsToWorkspace(fixture.workspace())).thenReturn(true);
        return account;
    }

    private String snapshot(Fixture fixture) {
        return tx.execute(status -> context.getBean(WorkspaceResetSnapshotService.class)
                .digest(fixture.workspace(), java.util.List.of("TASK_DEFINITIONS")));
    }

    private com.olehkobylianskyi.familypoints.controller.WorkspaceDataResetPrepareController.PreparedReset prepareReset(Fixture fixture) {
        return withEpoch(0L, () -> context.getBean(com.olehkobylianskyi.familypoints.controller.WorkspaceDataResetPrepareController.class)
                .prepare(fixture.workspace(), new com.olehkobylianskyi.familypoints.controller.WorkspaceDataResetPrepareController.PrepareRequest(
                        UUID.randomUUID(), java.util.Set.of("TASK_DEFINITIONS"))));
    }

    private void confirmReset(Fixture fixture, com.olehkobylianskyi.familypoints.controller.WorkspaceDataResetPrepareController.PreparedReset prepared) {
        withEpoch(0L, () -> context.getBean(com.olehkobylianskyi.familypoints.controller.WorkspaceDataResetPrepareController.class)
                .confirm(fixture.workspace(), new com.olehkobylianskyi.familypoints.controller.WorkspaceDataResetPrepareController.ConfirmRequest(
                        prepared.operationId(), prepared.snapshotDigest(), "RESET "+fixture.workspace())));
    }

    private com.olehkobylianskyi.familypoints.controller.WorkspaceResetChallengeController.ChallengeIssued challengeReset(
            Fixture fixture, com.olehkobylianskyi.familypoints.controller.WorkspaceDataResetPrepareController.PreparedReset prepared, String password) {
        return withEpoch(0L, () -> context.getBean(com.olehkobylianskyi.familypoints.controller.WorkspaceResetChallengeController.class)
                .challenge(fixture.workspace(), new com.olehkobylianskyi.familypoints.controller.WorkspaceResetChallengeController.ChallengeRequest(
                        prepared.operationId(), prepared.snapshotDigest(), password)));
    }

    @Test void contentSnapshotIsDeterministicAndDetectsSameCountEdit() {
        var fixture = fixture();
        var definition = task(fixture);
        String first = snapshot(fixture);
        assertEquals(first, snapshot(fixture));
        sql.update("UPDATE task_definitions SET title='Edited without new rows' WHERE id=?", definition.getId());
        assertNotEquals(first, snapshot(fixture));
        assertEquals(1L, sql.queryForObject("SELECT count(*) FROM task_definitions WHERE family_id=?", Long.class, fixture.workspace()));
    }

    @Test void contentSnapshotDetectsIndirectRolePermissionAndPrivateTemplateEdits() {
        var fixture = fixture();
        long role = sql.queryForObject("SELECT family_role_id FROM family_members WHERE id=?", Long.class, fixture.member());
        String initial = snapshot(fixture);
        sql.update("INSERT INTO family_role_permissions(role_id,permission) VALUES (?,'MANAGE_MEMBERS')", role);
        assertNotEquals(initial, snapshot(fixture));
        long template = sql.queryForObject("""
                INSERT INTO workspace_private_templates(workspace_id,kind,title,definition_json)
                VALUES (?,'TASK','Snapshot fixture','{}') RETURNING id
                """, Long.class, fixture.workspace());
        String before = snapshot(fixture);
        sql.update("UPDATE workspace_private_templates SET definition_json='{}',title='Edited template' WHERE id=?", template);
        assertNotEquals(before, snapshot(fixture));
    }

    @Test void contentSnapshotIgnoresOtherFamilyChangesAndGlobalPasswordButDetectsEpoch() {
        var fixture = fixture();
        var other = fixture();
        var account = resetActor(fixture);
        String initial = snapshot(fixture);
        task(other);
        sql.update("UPDATE families SET name='Unrelated edit' WHERE id=?", other.workspace());
        sql.update("UPDATE user_accounts SET password_hash='protected-global-change' WHERE id=?", account.getId());
        assertEquals(initial, snapshot(fixture));
        sql.update("UPDATE families SET data_epoch=1 WHERE id=?", fixture.workspace());
        assertNotEquals(initial, snapshot(fixture));
    }

    @Test void sameCountEditAfterPrepareRejectsConfirmationAndPreservesAuditState() {
        var fixture = fixture(); resetActor(fixture);
        var definition = task(fixture);
        var prepared = prepareReset(fixture);
        sql.update("UPDATE task_definitions SET title='Changed after prepare' WHERE id=?", definition.getId());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> confirmReset(fixture,prepared)).getStatusCode().value());
        assertEquals("PREPARED", sql.queryForObject("SELECT status FROM workspace_reset_operations WHERE workspace_id=?", String.class, fixture.workspace()));
        assertEquals(0L, sql.queryForObject("SELECT data_epoch FROM families WHERE id=?", Long.class, fixture.workspace()));
    }

    @Test void unchangedResetProtocolRemainsStableAndChallengeCannotExecute() {
        var fixture = fixture(); resetActor(fixture); task(fixture);
        String original = snapshot(fixture);
        var prepared = prepareReset(fixture);
        assertEquals(original, prepared.snapshotDigest());
        confirmReset(fixture, prepared);
        confirmReset(fixture, prepared);
        var challenge = challengeReset(fixture, prepared, "test-password");
        assertFalse(challenge.executionEnabled());
        assertEquals(original, snapshot(fixture));
        assertEquals(1L, sql.queryForObject("SELECT count(*) FROM workspace_reset_challenges WHERE workspace_id=?", Long.class, fixture.workspace()));
        assertEquals(423, assertThrows(ResponseStatusException.class, () -> context.getBean(
                com.olehkobylianskyi.familypoints.controller.WorkspaceResetChallengeController.class)
                .execute(fixture.workspace())).getStatusCode().value());
        assertEquals(1L, sql.queryForObject("SELECT count(*) FROM task_definitions WHERE family_id=?", Long.class, fixture.workspace()));
    }

    @Test void editAfterConfirmationRejectsChallengeWithoutIssuingProof() {
        var fixture = fixture(); resetActor(fixture);
        var definition = task(fixture);
        var prepared = prepareReset(fixture); confirmReset(fixture, prepared);
        sql.update("UPDATE task_definitions SET title='Changed after confirm' WHERE id=?", definition.getId());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> challengeReset(fixture,prepared,"test-password")).getStatusCode().value());
        assertEquals(0L, sql.queryForObject("SELECT count(*) FROM workspace_reset_challenges WHERE workspace_id=?", Long.class, fixture.workspace()));
        assertEquals(1L, sql.queryForObject("SELECT failed_attempts FROM workspace_reset_reauth_limits WHERE workspace_id=?", Long.class, fixture.workspace()));
    }

    @Test void failedPasswordBudgetCommitsDespiteChallengeRollbackAndLimitsRepeatedTries() {
        var fixture = fixture(); resetActor(fixture);
        var prepared = prepareReset(fixture); confirmReset(fixture, prepared);
        for (int attempt=0;attempt<5;attempt++)
            assertEquals(403, assertThrows(ResponseStatusException.class, () -> challengeReset(fixture,prepared,"wrong")).getStatusCode().value());
        assertEquals(429, assertThrows(ResponseStatusException.class, () -> challengeReset(fixture,prepared,"test-password")).getStatusCode().value());
        assertEquals(6L, sql.queryForObject("SELECT failed_attempts FROM workspace_reset_reauth_limits WHERE workspace_id=?", Long.class, fixture.workspace()));
        assertEquals(0L, sql.queryForObject("SELECT count(*) FROM workspace_reset_challenges WHERE workspace_id=?", Long.class, fixture.workspace()));
    }

    @Test void nestedAlreadyFencedChallengeFailsBeforeIndependentLimiterTransaction() {
        var fixture = fixture(); resetActor(fixture);
        var prepared = prepareReset(fixture); confirmReset(fixture, prepared);
        var failure = assertThrows(ResponseStatusException.class, () -> tx.executeWithoutResult(status -> {
            context.getBean(WorkspaceEpochTransactionGuard.class).lockCurrentEpoch(fixture.workspace());
            challengeReset(fixture, prepared, "test-password");
        }));
        assertEquals(409, failure.getStatusCode().value());
        assertTrue(failure.getReason().contains("NEW_TRANSACTION_REQUIRED"));
        assertEquals(0L, sql.queryForObject("SELECT count(*) FROM workspace_reset_reauth_limits WHERE workspace_id=?", Long.class, fixture.workspace()));
    }

    @Test void staleInvitationAndJoinManagerWritesFailBeforeMutation() {
        var fixture = fixture(); resetActor(fixture);
        sql.update("UPDATE families SET data_epoch=1 WHERE id=?", fixture.workspace());
        var invitations = context.getBean(WorkspaceInvitationService.class);
        var joins = context.getBean(WorkspaceJoinService.class);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> withEpoch(0L, () -> invitations.issue(fixture.workspace(),1L))).getStatusCode().value());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> withEpoch(0L, () -> { invitations.revoke(fixture.workspace(),1L); return null; })).getStatusCode().value());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> withEpoch(0L, () -> joins.rotateJoinCode(fixture.workspace()))).getStatusCode().value());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> withEpoch(0L, () -> { joins.approveRequest(fixture.workspace(),1L,1L,"Fixture"); return null; })).getStatusCode().value());
        assertEquals(0L, sql.queryForObject("SELECT count(*) FROM workspace_invitations WHERE workspace_id=?", Long.class, fixture.workspace()));
        assertEquals(0L, sql.queryForObject("SELECT count(*) FROM workspace_join_codes WHERE workspace_id=?", Long.class, fixture.workspace()));
    }

    @Test void publicInvitationRevalidatesRevocationAfterWaitingForFamilyFence() throws Exception {
        var fixture = fixture(); var account=resetActor(fixture);
        long role=sql.queryForObject("SELECT family_role_id FROM family_members WHERE id=?",Long.class,fixture.member());
        // Account is only issuer; accepting a revoked token must fail before duplicate-membership logic.
        var invitations=context.getBean(WorkspaceInvitationService.class);
        var invitation=withEpoch(0L,()->invitations.issue(fixture.workspace(),role));
        var pending=invitations.begin(invitation.token());
        var locked=new CountDownLatch(1); var release=new CountDownLatch(1);
        var pid=new CompletableFuture<Integer>(); var pool=Executors.newFixedThreadPool(2);
        try {
            var holder=pool.submit(()->tx.executeWithoutResult(status->{
                pid.complete(sql.queryForObject("SELECT pg_backend_pid()",Integer.class));
                context.getBean(WorkspaceEpochTransactionGuard.class).lockCurrentEpoch(fixture.workspace());
                locked.countDown(); await(release);
                sql.update("UPDATE workspace_invitations SET revoked_at=now() WHERE workspace_id=?",fixture.workspace());
                sql.update("UPDATE families SET data_epoch=1 WHERE id=?",fixture.workspace());
            }));
            assertTrue(locked.await(15,TimeUnit.SECONDS));
            var accepter=pool.submit(()->invitations.accept(pending.handle(),"New fixture"));
            awaitDatabaseWait(pid.get(5,TimeUnit.SECONDS));
            // If acceptance held the invitation row first this UPDATE would deadlock.
            release.countDown(); holder.get(15,TimeUnit.SECONDS);
            var failed=assertThrows(ExecutionException.class,()->accepter.get(15,TimeUnit.SECONDS));
            assertEquals(404,assertInstanceOf(ResponseStatusException.class,failed.getCause()).getStatusCode().value());
            assertEquals(0L,sql.queryForObject("SELECT count(*) FROM workspace_invitation_redemptions WHERE account_id=?",Long.class,account.getId()));
        } finally { release.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(20,TimeUnit.SECONDS)); }
    }

    @Test void publicJoinCodeRechecksRotationAfterFamilyFenceWithoutGrantingMembership() throws Exception {
        var fixture=fixture(); var account=resetActor(fixture);
        var joins=context.getBean(WorkspaceJoinService.class);
        String code=withEpoch(0L,()->joins.rotateJoinCode(fixture.workspace()));
        var locked=new CountDownLatch(1); var release=new CountDownLatch(1);
        var pid=new CompletableFuture<Integer>(); var pool=Executors.newFixedThreadPool(2);
        try {
            var holder=pool.submit(()->tx.executeWithoutResult(status->{
                pid.complete(sql.queryForObject("SELECT pg_backend_pid()",Integer.class));
                context.getBean(WorkspaceEpochTransactionGuard.class).lockCurrentEpoch(fixture.workspace());
                locked.countDown(); await(release);
                sql.update("UPDATE workspace_join_codes SET code_hash=? WHERE workspace_id=?","f".repeat(64),fixture.workspace());
                sql.update("UPDATE families SET data_epoch=1 WHERE id=?",fixture.workspace());
            }));
            assertTrue(locked.await(15,TimeUnit.SECONDS));
            var applicant=pool.submit(()->joins.requestJoin(code));
            awaitDatabaseWait(pid.get(5,TimeUnit.SECONDS));
            release.countDown(); holder.get(15,TimeUnit.SECONDS);
            var failed=assertThrows(ExecutionException.class,()->applicant.get(15,TimeUnit.SECONDS));
            assertEquals(400,assertInstanceOf(ResponseStatusException.class,failed.getCause()).getStatusCode().value());
            assertEquals(0L,sql.queryForObject("SELECT count(*) FROM workspace_join_requests WHERE workspace_id=?",Long.class,fixture.workspace()));
            assertEquals(1L,sql.queryForObject("SELECT count(*) FROM account_workspace_memberships WHERE account_id=?",Long.class,account.getId()));
        } finally { release.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(20,TimeUnit.SECONDS)); }
    }


    @Test void publicInvitationAtNonzeroEpochUsesRetainedTokenAndKeepsOtherFamilyMembership() {
        var target=fixture(); resetActor(target);
        long role=sql.queryForObject("SELECT family_role_id FROM family_members WHERE id=?",Long.class,target.member());
        var invitations=context.getBean(WorkspaceInvitationService.class);
        var invitation=withEpoch(0L,()->invitations.issue(target.workspace(),role));
        var other=fixture(); var joiner=resetActor(other);
        sql.update("UPDATE families SET data_epoch=1 WHERE id=?",target.workspace());
        var pending=withEpoch(99L,()->invitations.begin(invitation.token()));
        assertEquals(target.workspace(),withEpoch(99L,()->invitations.accept(pending.handle(),"Invited fixture")));
        assertEquals(2L,sql.queryForObject("SELECT count(*) FROM account_workspace_memberships WHERE account_id=?",Long.class,joiner.getId()));
        assertEquals(1L,sql.queryForObject("SELECT use_count FROM workspace_invitations WHERE workspace_id=?",Long.class,target.workspace()));
        assertEquals(true,sql.queryForObject("SELECT enabled FROM user_accounts WHERE id=?",Boolean.class,joiner.getId()));
        assertEquals(404,assertThrows(ResponseStatusException.class,()->invitations.accept(pending.handle(),"Repeat")).getStatusCode().value());
    }

    @Test void explicitMemberLinkAtNonzeroEpochRejectsStaleThenLinksOnlyTargetMember() {
        var target=fixture(); var other=fixture(); var account=resetActor(other);
        sql.update("UPDATE families SET data_epoch=1 WHERE id=?",target.workspace());
        var links=context.getBean(AccountMembershipLinkService.class);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->withEpoch(0L,
                ()->links.linkExistingAccount(account,target.workspace(),target.member()))).getStatusCode().value());
        withEpoch(1L,()->links.linkExistingAccount(account,target.workspace(),target.member()));
        assertEquals(2L,sql.queryForObject("SELECT count(*) FROM account_workspace_memberships WHERE account_id=?",Long.class,account.getId()));
        assertEquals(0L,sql.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,other.workspace()));
    }


    private WorkspaceResetProofService.IssuedProof issueProof(Fixture fixture,
            com.olehkobylianskyi.familypoints.entity.UserAccount account,
            com.olehkobylianskyi.familypoints.controller.WorkspaceDataResetPrepareController.PreparedReset prepared) {
        return tx.execute(status -> withEpoch(0L, () -> context.getBean(WorkspaceResetProofService.class).issuePasswordChallenge(
                fixture.workspace(),prepared.operationId(),account.getId(),fixture.member(),prepared.snapshotDigest(),"test-password")));
    }

    private void consumeProof(Fixture fixture, com.olehkobylianskyi.familypoints.entity.UserAccount account,
            com.olehkobylianskyi.familypoints.controller.WorkspaceDataResetPrepareController.PreparedReset prepared,
            WorkspaceResetProofService.IssuedProof proof,String secret) {
        tx.executeWithoutResult(status -> withEpoch(0L, () -> {
            context.getBean(WorkspaceResetProofService.class).consume(fixture.workspace(),prepared.operationId(),
                    proof.challengeId(),account.getId(),fixture.member(),prepared.snapshotDigest(),secret);
            return null;
        }));
    }

    private long consumed(Fixture fixture) {
        return sql.queryForObject("SELECT count(*) FROM workspace_reset_challenges WHERE workspace_id=? AND consumed_at IS NOT NULL",
                Long.class,fixture.workspace());
    }

    @Test void boundPasswordProofStoresOnlyVerifierAndCommitsConsumptionOnce() {
        var fixture=fixture(); var account=resetActor(fixture);
        var prepared=prepareReset(fixture); confirmReset(fixture,prepared);
        var proof=issueProof(fixture,account,prepared);
        assertEquals(43,proof.secret().length());
        assertFalse(proof.toString().contains(proof.secret()));
        var row=sql.queryForMap("SELECT * FROM workspace_reset_challenges WHERE id=?",proof.challengeId());
        assertNotEquals(proof.secret(),row.get("verifier_hash"));
        assertEquals(64,((String)row.get("verifier_hash")).length());
        assertFalse(row.containsValue(proof.secret()));
        assertEquals(0L,((Number)row.get("data_epoch")).longValue());
        assertEquals(fixture.member(),((Number)row.get("requested_by_member_id")).longValue());
        assertEquals(prepared.snapshotDigest(),row.get("snapshot_digest"));
        consumeProof(fixture,account,prepared,proof,proof.secret());
        assertEquals(1,consumed(fixture));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->consumeProof(fixture,account,prepared,proof,proof.secret())).getStatusCode().value());
        assertEquals(0L,sql.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,fixture.workspace()));
    }

    @Test void passwordProofLifecycleReallyRequiresExistingTransaction() {
        var service=context.getBean(WorkspaceResetProofService.class);
        assertThrows(IllegalTransactionStateException.class,()->service.issuePasswordChallenge(
                1L,UUID.randomUUID(),1L,1L,"a".repeat(64),"password"));
        assertThrows(IllegalTransactionStateException.class,()->service.consume(
                1L,UUID.randomUUID(),UUID.randomUUID(),1L,1L,"a".repeat(64),"x".repeat(43)));
    }

    @Test void wrongProofSecretDoesNotConsumeAndCorrectSecretStillWorks() {
        var fixture=fixture(); var account=resetActor(fixture);
        var prepared=prepareReset(fixture); confirmReset(fixture,prepared);
        var proof=issueProof(fixture,account,prepared);
        assertEquals(403,assertThrows(ResponseStatusException.class,()->consumeProof(fixture,account,prepared,proof,"wrong")).getStatusCode().value());
        String wrong=(proof.secret().charAt(0)=='A'?"B":"A")+proof.secret().substring(1);
        assertEquals(403,assertThrows(ResponseStatusException.class,()->consumeProof(fixture,account,prepared,proof,wrong)).getStatusCode().value());
        assertEquals(0,consumed(fixture));
        consumeProof(fixture,account,prepared,proof,proof.secret());
        assertEquals(1,consumed(fixture));
    }

    @Test void proofCannotCrossOperationOrFamilyAndDoesNotConsumeOriginal() {
        var fixture=fixture(); var account=resetActor(fixture);
        var prepared=prepareReset(fixture); confirmReset(fixture,prepared);
        var proof=issueProof(fixture,account,prepared);
        var second=prepareReset(fixture); confirmReset(fixture,second);
        assertEquals(404,assertThrows(ResponseStatusException.class,()->consumeProof(fixture,account,second,proof,proof.secret())).getStatusCode().value());
        var other=fixture(); var otherAccount=resetActor(other);
        var otherPrepared=prepareReset(other); confirmReset(other,otherPrepared);
        assertEquals(404,assertThrows(ResponseStatusException.class,()->consumeProof(other,otherAccount,otherPrepared,proof,proof.secret())).getStatusCode().value());
        assertEquals(0,consumed(fixture)); assertEquals(0,consumed(other));
        consumeProof(fixture,account,prepared,proof,proof.secret());
    }

    @Test void proofCannotBeConsumedByAnotherAccountOrMember() {
        var fixture=fixture(); var account=resetActor(fixture);
        var prepared=prepareReset(fixture); confirmReset(fixture,prepared);
        var proof=issueProof(fixture,account,prepared);
        var other=fixture(); var wrongAccount=resetActor(other);
        assertEquals(403,assertThrows(ResponseStatusException.class,()->consumeProof(fixture,wrongAccount,prepared,proof,proof.secret())).getStatusCode().value());
        var failure=assertThrows(ResponseStatusException.class,()->tx.executeWithoutResult(status -> withEpoch(0L,()->{
            context.getBean(WorkspaceResetProofService.class).consume(fixture.workspace(),prepared.operationId(),proof.challengeId(),
                    account.getId(),other.member(),prepared.snapshotDigest(),proof.secret()); return null;
        })));
        assertEquals(403,failure.getStatusCode().value()); assertEquals(0,consumed(fixture));
    }

    @Test void sameCountDomainEditInvalidatesAlreadyIssuedProof() {
        var fixture=fixture(); var account=resetActor(fixture); var definition=task(fixture);
        var prepared=prepareReset(fixture); confirmReset(fixture,prepared); var proof=issueProof(fixture,account,prepared);
        sql.update("UPDATE task_definitions SET title='Changed after challenge' WHERE id=?",definition.getId());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->consumeProof(fixture,account,prepared,proof,proof.secret())).getStatusCode().value());
        assertEquals(0,consumed(fixture));
    }

    @Test void epochChangeRejectsProofForOldAndFreshHeadersWithoutConsumption() {
        var fixture=fixture(); var account=resetActor(fixture);
        var prepared=prepareReset(fixture); confirmReset(fixture,prepared); var proof=issueProof(fixture,account,prepared);
        sql.update("UPDATE families SET data_epoch=1 WHERE id=?",fixture.workspace());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->consumeProof(fixture,account,prepared,proof,proof.secret())).getStatusCode().value());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->tx.executeWithoutResult(status->withEpoch(1L,()->{
            context.getBean(WorkspaceResetProofService.class).consume(fixture.workspace(),prepared.operationId(),proof.challengeId(),
                    account.getId(),fixture.member(),prepared.snapshotDigest(),proof.secret()); return null;
        }))).getStatusCode().value());
        assertEquals(0,consumed(fixture));
    }

    @Test void changedPasswordInvalidatesIssuedProofAndDisabledAccountCannotConsume() {
        var fixture=fixture(); var account=resetActor(fixture);
        var prepared=prepareReset(fixture); confirmReset(fixture,prepared); var proof=issueProof(fixture,account,prepared);
        String original=account.getPasswordHash();
        String changed=context.getBean(org.springframework.security.crypto.password.PasswordEncoder.class).encode("changed-password");
        sql.update("UPDATE user_accounts SET password_hash=? WHERE id=?",changed,account.getId());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->consumeProof(fixture,account,prepared,proof,proof.secret())).getStatusCode().value());
        sql.update("UPDATE user_accounts SET password_hash=?,enabled=false WHERE id=?",original,account.getId());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->consumeProof(fixture,account,prepared,proof,proof.secret())).getStatusCode().value());
        assertEquals(0,consumed(fixture));
    }

    @Test void cachedPasswordCannotIssueAfterGlobalCredentialChanges() {
        var fixture=fixture(); var account=resetActor(fixture);
        var prepared=prepareReset(fixture); confirmReset(fixture,prepared);
        String changed=context.getBean(org.springframework.security.crypto.password.PasswordEncoder.class).encode("new-global-password");
        sql.update("UPDATE user_accounts SET password_hash=? WHERE id=?",changed,account.getId());
        // CurrentUser mock deliberately retains the OLD account/password snapshot.
        assertEquals(403,assertThrows(ResponseStatusException.class,()->challengeReset(fixture,prepared,"test-password")).getStatusCode().value());
        assertEquals(0L,sql.queryForObject("SELECT count(*) FROM workspace_reset_challenges WHERE workspace_id=?",Long.class,fixture.workspace()));
        assertEquals(1L,sql.queryForObject("SELECT failed_attempts FROM workspace_reset_reauth_limits WHERE workspace_id=?",Long.class,fixture.workspace()));
    }

    @Test void expiredProofUsesWallClockEvenInsideOlderTransaction() {
        var fixture=fixture(); var account=resetActor(fixture);
        var prepared=prepareReset(fixture); confirmReset(fixture,prepared); var proof=issueProof(fixture,account,prepared);
        // Commit a deadline shortly after the next transaction starts, then pass it.
        var failure=assertThrows(ResponseStatusException.class,()->tx.executeWithoutResult(status->{
            sql.update("UPDATE workspace_reset_challenges SET expires_at=CURRENT_TIMESTAMP+interval '100 milliseconds' WHERE id=?",proof.challengeId());
            sql.execute("SELECT pg_sleep(0.2)");
            withEpoch(0L,()->{
                context.getBean(WorkspaceResetProofService.class).consume(fixture.workspace(),prepared.operationId(),proof.challengeId(),
                        account.getId(),fixture.member(),prepared.snapshotDigest(),proof.secret()); return null;
            });
        }));
        assertEquals(410,failure.getStatusCode().value()); assertEquals(0,consumed(fixture));
    }

    @Test void proofConsumptionAndDomainWriteRollbackTogetherThenProofCanBeRetried() {
        var fixture=fixture(); var account=resetActor(fixture); var definition=task(fixture);
        var prepared=prepareReset(fixture); confirmReset(fixture,prepared); var proof=issueProof(fixture,account,prepared);
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(status->{
            consumeProof(fixture,account,prepared,proof,proof.secret());
            sql.update("UPDATE task_definitions SET title='Rolled back proof transaction' WHERE id=?",definition.getId());
            throw new IllegalStateException("Injected failure after proof consumption and domain write");
        }));
        assertEquals(0,consumed(fixture));
        assertEquals("Disposable task",sql.queryForObject("SELECT title FROM task_definitions WHERE id=?",String.class,definition.getId()));
        assertEquals(0L,sql.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,fixture.workspace()));
        consumeProof(fixture,account,prepared,proof,proof.secret()); assertEquals(1,consumed(fixture));
    }

    @Test void concurrentProofConsumersHaveExactlyOneWinner() throws Exception {
        var fixture=fixture(); var account=resetActor(fixture);
        var prepared=prepareReset(fixture); confirmReset(fixture,prepared); var proof=issueProof(fixture,account,prepared);
        var pool=Executors.newFixedThreadPool(2); var ready=new CountDownLatch(2); var start=new CountDownLatch(1);
        java.util.concurrent.Callable<Integer> consumer=()->{
            ready.countDown(); await(start);
            try { consumeProof(fixture,account,prepared,proof,proof.secret()); return 200; }
            catch (ResponseStatusException exception) { return exception.getStatusCode().value(); }
        };
        try {
            var first=pool.submit(consumer); var second=pool.submit(consumer);
            assertTrue(ready.await(10,TimeUnit.SECONDS)); start.countDown();
            var results=new java.util.ArrayList<>(java.util.List.of(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS)));
            results.sort(Integer::compareTo); assertEquals(java.util.List.of(200,409),results);
            assertEquals(1,consumed(fixture));
        } finally { start.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(20,TimeUnit.SECONDS)); }
    }

    @Test void revokedMemberCannotConsumeProofAndLegacyUnboundRowsFailClosed() {
        var fixture=fixture(); var account=resetActor(fixture);
        var prepared=prepareReset(fixture); confirmReset(fixture,prepared); var proof=issueProof(fixture,account,prepared);
        sql.update("UPDATE workspace_reset_challenges SET data_epoch=NULL,snapshot_digest=NULL,requested_by_member_id=NULL,credential_fingerprint=NULL WHERE id=?",proof.challengeId());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->consumeProof(fixture,account,prepared,proof,proof.secret())).getStatusCode().value());
        sql.update("UPDATE family_members SET active=false WHERE id=?",fixture.member());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->consumeProof(fixture,account,prepared,proof,proof.secret())).getStatusCode().value());
        assertEquals(0,consumed(fixture));
    }

    @Test void publicChallengeDtoNeverExposesInternalSecret() {
        var fixture=fixture(); resetActor(fixture);
        var prepared=prepareReset(fixture); confirmReset(fixture,prepared);
        var response=challengeReset(fixture,prepared,"test-password");
        String json;
        try { json=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().writeValueAsString(response); }
        catch (Exception exception) { throw new AssertionError(exception); }
        assertFalse(json.contains("secret")); assertFalse(json.contains("verifier")); assertFalse(response.executionEnabled());
        assertEquals(4,response.getClass().getRecordComponents().length);
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
