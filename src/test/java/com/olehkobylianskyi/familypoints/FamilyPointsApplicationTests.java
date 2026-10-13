package com.olehkobylianskyi.familypoints;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.userdetails.User;
import com.olehkobylianskyi.familypoints.security.JwtService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.AccountWorkspaceMembershipRepository;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class FamilyPointsApplicationTests {

    // Disposable integration-test database; never connect to local DEV PostgreSQL.
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("family_points_test")
            .withUsername("fp_test")
            .withPassword("fp_test_password");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.baseline-on-migrate", () -> "false");
        registry.add("security.jwt.secret", () -> java.util.Base64.getEncoder().encodeToString(
                "family-points-integration-test-jwt-secret-at-least-32-characters"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Autowired EntityManager entityManager;
    @Autowired AccountWorkspaceMembershipRepository memberships;
    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    @Transactional
    void sameAccountCanBelongToTwoIndependentWorkspaces() {
        Workspace first = new Workspace("Integration family A");
        Workspace second = new Workspace("Integration family B");
        entityManager.persist(first);
        entityManager.persist(second);

        WorkspaceRole firstRole = new WorkspaceRole(first, "PARENT", "Parent A", false);
        WorkspaceRole secondRole = new WorkspaceRole(second, "PARENT", "Parent B", false);
        entityManager.persist(firstRole);
        entityManager.persist(secondRole);

        WorkspaceMember firstMember = new WorkspaceMember("Member A",
                WorkspaceMemberType.PARENT, firstRole, first);
        WorkspaceMember secondMember = new WorkspaceMember("Member B",
                WorkspaceMemberType.PARENT, secondRole, second);
        entityManager.persist(firstMember);
        entityManager.persist(secondMember);

        UserAccount account = new UserAccount("integration-membership-account",
                "not-used-for-login", firstMember);
        entityManager.persist(account);
        entityManager.persist(new AccountWorkspaceMembership(account, firstMember));
        entityManager.persist(new AccountWorkspaceMembership(account, secondMember));
        entityManager.flush();
        entityManager.clear();

        var linked = memberships.findAllByAccountIdOrderByIdAsc(account.getId());
        assertEquals(2, linked.size());
        assertTrue(memberships.existsByAccountIdAndWorkspaceMemberWorkspaceId(account.getId(), first.getId()));
        assertTrue(memberships.existsByAccountIdAndWorkspaceMemberWorkspaceId(account.getId(), second.getId()));
        assertEquals(firstMember.getId(), memberships.findByAccountIdAndWorkspaceMemberWorkspaceId(
                account.getId(), first.getId()).orElseThrow().getWorkspaceMember().getId());
        assertEquals(secondMember.getId(), memberships.findByAccountIdAndWorkspaceMemberWorkspaceId(
                account.getId(), second.getId()).orElseThrow().getWorkspaceMember().getId());
    }


    @Test
    @Transactional
    void scopedJwtCannotReadAnotherFamilysHttpWorkspaceRoute() throws Exception {
        Workspace first = new Workspace("HTTP isolation family A");
        Workspace second = new Workspace("HTTP isolation family B");
        Workspace unrelated = new Workspace("HTTP isolation outsider");
        entityManager.persist(first);
        entityManager.persist(second);
        entityManager.persist(unrelated);

        WorkspaceRole firstRole = new WorkspaceRole(first, "PARENT", "Parent A", false);
        WorkspaceRole secondRole = new WorkspaceRole(second, "CHILD", "Child B", false);
        entityManager.persist(firstRole);
        entityManager.persist(secondRole);

        WorkspaceMember memberA = new WorkspaceMember("HTTP A",
                WorkspaceMemberType.PARENT, firstRole, first);
        WorkspaceMember memberB = new WorkspaceMember("HTTP B",
                WorkspaceMemberType.CHILD, secondRole, second);
        entityManager.persist(memberA);
        entityManager.persist(memberB);
        UserAccount account = new UserAccount("http-isolation-user", "unused", memberA);
        entityManager.persist(account);
        entityManager.persist(new AccountWorkspaceMembership(account, memberA));
        entityManager.persist(new AccountWorkspaceMembership(account, memberB));
        entityManager.flush();

        var principal = User.withUsername(account.getUsername()).password("unused")
                .authorities("USER").build();
        String scopedToken = jwtService.generateWorkspaceAccessToken(principal, second.getId());
        String authorization = "Bearer " + scopedToken;

        mockMvc.perform(get("/workspaces/{id}", second.getId())
                        .header("Authorization", authorization))
                .andExpect(status().isOk());
        mockMvc.perform(get("/workspaces/{id}", first.getId())
                        .header("Authorization", authorization))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/workspaces/{id}", unrelated.getId())
                        .header("Authorization", authorization))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/me").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.workspaceId").value(second.getId()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.memberId").value(memberB.getId()));

        // A previously issued token must stop working after membership revocation.
        memberB.softDelete();
        entityManager.flush();
        entityManager.clear();
        mockMvc.perform(get("/workspaces/{id}", second.getId())
                        .header("Authorization", authorization))
                .andExpect(status().isUnauthorized());

        // Disabling the global account also invalidates its legacy JWT.
        var storedAccount = entityManager.find(UserAccount.class, account.getId());
        storedAccount.setEnabled(false);
        entityManager.flush();
        entityManager.clear();
        String legacyToken = jwtService.generateAccessToken(principal);
        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + legacyToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void flywayAppliedAllIdentityAndInvitationMigrations() {
        Integer version = jdbc.queryForObject("""
            SELECT count(*) FROM flyway_schema_history
            WHERE version IN ('27', '28', '29') AND success = true
            """, Integer.class);
        assertEquals(3, version);
        assertEquals(1, jdbc.queryForObject("""
            SELECT count(*) FROM information_schema.tables
            WHERE table_schema = 'public' AND table_name = 'account_external_identities'
            """, Integer.class));
    }

    @Test
    @Transactional
    void databaseRejectsRoleFromDifferentWorkspace() {
        Workspace first = new Workspace("Role tenant A");
        Workspace second = new Workspace("Role tenant B");
        entityManager.persist(first);
        entityManager.persist(second);
        WorkspaceRole foreign = new WorkspaceRole(second, "CHILD", "Foreign role", false);
        entityManager.persist(foreign);
        entityManager.flush();

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
            jdbc.update("""
                INSERT INTO family_members(name, member_type, family_role_id, family_id, active)
                VALUES ('Cross-tenant injection', 'OTHER', ?, ?, true)
                """, foreign.getId(), first.getId()));
    }

    @Test
    @Transactional
    void globalAccountMayExistWithoutPrimaryFamily() {
        UserAccount account = new UserAccount("no-family-test-user", "password-hash", null);
        entityManager.persist(account);
        entityManager.flush();
        assertNotNull(account.getId());
        assertNull(account.getWorkspaceMember());
    }

    @Test
    @Transactional
    void databaseRejectsInvitationWithRoleFromOtherWorkspace() {
        Workspace first = new Workspace("Invite tenant A");
        Workspace second = new Workspace("Invite tenant B");
        entityManager.persist(first);
        entityManager.persist(second);
        WorkspaceRole foreign = new WorkspaceRole(second, "CHILD", "Foreign invite role", false);
        entityManager.persist(foreign);
        WorkspaceRole own = new WorkspaceRole(first, "PARENT", "Inviter", false);
        entityManager.persist(own);
        WorkspaceMember member = new WorkspaceMember("Inviter", WorkspaceMemberType.PARENT, own, first);
        entityManager.persist(member);
        UserAccount issuer = new UserAccount("invitation-issuer-test", "hash", member);
        entityManager.persist(issuer);
        entityManager.flush();

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
            jdbc.update("""
                INSERT INTO workspace_invitations
                (workspace_id, created_by_account_id, token_hash, expires_at, member_role_id)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP + interval '7 days', ?)
                """, first.getId(), issuer.getId(), "f".repeat(64), foreign.getId()));
    }

    @Test
    void contextLoads() {
        // Spring starts, Flyway migrates V1..V29, and Hibernate validates the schema.
    }
}
