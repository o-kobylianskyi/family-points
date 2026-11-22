package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.dto.RewardPurchaseResponse;
import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.RewardPurchaseRepository;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
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

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real PG18 family-row serialization + Flyway receipt/actor FK fixture.
 * RewardService is mocked ONLY to isolate the receipt boundary; this is NOT
 * financial-ledger end-to-end proof (the separate full test remains required).
 */
@Testcontainers
class RewardPurchaseOperationPostgresTest {
    @Container static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("reward_receipt").withUsername("reward_test")
            .withPassword("disposable_test");

    @Test void durableReplayAndConcurrentDuplicateHaveOneReceiptAndPurchaseInvocation()
            throws Exception {
        var ds = new DriverManagerDataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db = new JdbcTemplate(ds);
        long family = db.queryForObject(
                "INSERT INTO families(name) VALUES ('Reward receipt test') RETURNING id", Long.class);
        long role = db.queryForObject("""
                INSERT INTO family_roles(family_id,code,name,system_role)
                VALUES (?,'PARENT','Parent',true) RETURNING id
                """,Long.class,family);
        long memberId = db.queryForObject("""
                INSERT INTO family_members(family_id,family_role_id,name,active,member_type)
                VALUES (?,?,'Member',true,'PARENT') RETURNING id
                """,Long.class,family,role);
        long actor = db.queryForObject("""
                INSERT INTO user_accounts(family_member_id,username,password_hash,enabled)
                VALUES (?,'disposable-reward-actor','unused-test-password',true) RETURNING id
                """,Long.class,memberId);
        long type = db.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,family);
        long rewardId = db.queryForObject("""
                INSERT INTO reward_definitions(family_id,title,point_type_id,price_amount)
                VALUES (?,'Time',?,0) RETURNING id
                """,Long.class,family,type);
        long purchaseId = db.queryForObject("""
                INSERT INTO reward_purchases
                (reward_definition_id,member_id,reward_title,point_type_id,price_amount,status)
                VALUES (?,?,'Time',?,0,'READY') RETURNING id
                """,Long.class,rewardId,memberId,type);

        var current = mock(CurrentUserService.class);
        var account = mock(UserAccount.class);
        when(account.getId()).thenReturn(actor);
        when(current.getCurrentAccount()).thenReturn(account);
        when(current.belongsToWorkspace(family)).thenReturn(true);
        when(current.getCurrentMemberId()).thenReturn(memberId);
        var actual = mock(RewardService.class);
        var purchaseRepo = mock(RewardPurchaseRepository.class);
        var purchase = mock(RewardPurchase.class);
        var member = mock(WorkspaceMember.class);
        var workspace = mock(Workspace.class);
        var reward = mock(RewardDefinition.class);
        var pointType = mock(PointType.class);
        when(workspace.getId()).thenReturn(family);
        when(member.getWorkspace()).thenReturn(workspace);
        when(member.getId()).thenReturn(memberId);
        when(member.getName()).thenReturn("Member");
        when(reward.getId()).thenReturn(rewardId);
        when(pointType.getId()).thenReturn(type);
        when(pointType.getCode()).thenReturn("POINTS");
        when(purchase.getId()).thenReturn(purchaseId);
        when(purchase.getMember()).thenReturn(member);
        when(purchase.getRewardDefinition()).thenReturn(reward);
        when(purchase.getPointType()).thenReturn(pointType);
        when(purchase.getStatus()).thenReturn(RewardPurchaseStatus.READY);
        when(purchase.getRewardTitle()).thenReturn("Time");
        when(purchaseRepo.findById(purchaseId)).thenReturn(Optional.of(purchase));
        var response = mock(RewardPurchaseResponse.class);
        when(response.getId()).thenReturn(purchaseId);
        when(actual.purchaseReward(family,memberId,rewardId)).thenReturn(response);

        var service = new RewardPurchaseOperationService(db,
                new WorkspaceEpochTransactionGuard(db), current,actual,purchaseRepo);
        var tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        UUID intent = UUID.randomUUID();
        var workers = Executors.newFixedThreadPool(2);
        var go = new CountDownLatch(1);
        try {
            Callable<RewardPurchaseOperationService.Receipt> work = () -> {
                go.await(10,TimeUnit.SECONDS);
                return tx.execute(status -> service.purchaseSelf(family,rewardId,intent));
            };
            var first = workers.submit(work);
            var second = workers.submit(work);
            go.countDown();
            var a = first.get(30,TimeUnit.SECONDS);
            var b = second.get(30,TimeUnit.SECONDS);
            assertEquals(intent,a.operationId());
            assertEquals(intent,b.operationId());
            assertNotEquals(a.replayed(),b.replayed());
            assertEquals(purchaseId,a.purchase().getId());
            assertEquals(purchaseId,b.purchase().getId());
            verify(actual,times(1)).purchaseReward(family,memberId,rewardId);
            assertEquals(1L,db.queryForObject("""
                    SELECT count(*) FROM reward_purchase_operation_receipts
                    WHERE workspace_id=? AND actor_account_id=? AND operation_id=?
                    """,Long.class,family,actor,intent));

            var changed=assertThrows(ResponseStatusException.class,()->
                    tx.execute(status->service.purchaseSelf(family,rewardId+1,intent)));
            assertEquals(HttpStatus.CONFLICT,changed.getStatusCode());
            assertEquals(purchaseId,tx.execute(status->service.receipt(family,intent))
                    .purchase().getId());
            assertEquals(1L,db.queryForObject(
                    "SELECT count(*) FROM reward_purchase_operation_receipts WHERE workspace_id=?",
                    Long.class,family));
        } finally {
            workers.shutdownNow();
        }
    }
}
