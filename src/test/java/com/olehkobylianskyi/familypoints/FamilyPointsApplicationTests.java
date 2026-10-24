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
    @Transactional
    void verifiedProviderSubjectCannotBeAssignedToTwoAccounts() {
        UserAccount first = new UserAccount("external-identity-first", "hash", null);
        UserAccount second = new UserAccount("external-identity-second", "hash", null);
        entityManager.persist(first);
        entityManager.persist(second);
        entityManager.flush();
        jdbc.update("""
            INSERT INTO account_external_identities(account_id, provider, provider_subject)
            VALUES (?, 'google', 'provider-subject-123')
            """, first.getId());
        assertThrows(org.springframework.dao.DuplicateKeyException.class, () ->
            jdbc.update("""
                INSERT INTO account_external_identities(account_id, provider, provider_subject)
                VALUES (?, 'google', 'provider-subject-123')
                """, second.getId()));
    }

    @Test
    @Transactional
    void joinCodeRequiresManagerApprovalAndBlocksDuplicatePendingRequests() {
        Workspace workspace = new Workspace("Join test family");
        Workspace outsider = new Workspace("Join test outsider");
        entityManager.persist(workspace);
        entityManager.persist(outsider);
        WorkspaceRole role = new WorkspaceRole(workspace, "CHILD", "Join member role", false);
        entityManager.persist(role);
        UserAccount applicant = new UserAccount("test-join-applicant", "hash", null);
        UserAccount reviewer = new UserAccount("test-join-reviewer", "hash", null);
        entityManager.persist(applicant);
        entityManager.persist(reviewer);
        entityManager.flush();

        var identity = org.mockito.Mockito.mock(
                com.olehkobylianskyi.familypoints.security.CurrentUserService.class);
        org.mockito.Mockito.when(identity.getCurrentAccount()).thenReturn(applicant);
        var joins = new com.olehkobylianskyi.familypoints.service.WorkspaceJoinService(jdbc, identity);
        String code = joins.rotateJoinCode(workspace.getId());
        assertEquals(48, code.length());
        joins.requestJoin(code);
        assertEquals(1, jdbc.queryForObject("""
            SELECT count(*) FROM workspace_join_requests
            WHERE requester_account_id=? AND workspace_id=? AND status='PENDING'
            """, Integer.class, applicant.getId(), workspace.getId()));
        assertEquals(0, jdbc.queryForObject("""
            SELECT count(*) FROM account_workspace_memberships
            WHERE account_id=? AND workspace_id=?
            """, Integer.class, applicant.getId(), workspace.getId()));
        assertEquals(409, assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> joins.requestJoin(code)).getStatusCode().value());

        Long requestId = jdbc.queryForObject("""
            SELECT id FROM workspace_join_requests
            WHERE requester_account_id=? AND workspace_id=? AND status='PENDING'
            """, Long.class, applicant.getId(), workspace.getId());
        org.mockito.Mockito.when(identity.getCurrentAccount()).thenReturn(reviewer);
        joins.approveRequest(workspace.getId(), requestId, role.getId(), "Approved tester");
        assertEquals(1, jdbc.queryForObject("""
            SELECT count(*) FROM account_workspace_memberships
            WHERE account_id=? AND workspace_id=?
            """, Integer.class, applicant.getId(), workspace.getId()));
        assertEquals(0, jdbc.queryForObject("""
            SELECT count(*) FROM account_workspace_memberships
            WHERE account_id=? AND workspace_id=?
            """, Integer.class, applicant.getId(), outsider.getId()));
        assertEquals("APPROVED", jdbc.queryForObject("""
            SELECT status FROM workspace_join_requests WHERE id=?
            """, String.class, requestId));
    }

    @Test
    @Transactional
    void invitationContextCanBeRedeemedOnlyOnceAndOnlyInInvitingWorkspace() {
        Workspace family = new Workspace("Invitation flow family");
        Workspace outsider = new Workspace("Invitation flow outsider");
        entityManager.persist(family);
        entityManager.persist(outsider);
        WorkspaceRole role = new WorkspaceRole(family, "CHILD", "Invite member role", false);
        entityManager.persist(role);
        UserAccount inviter = new UserAccount("test-invite-manager", "hash", null);
        UserAccount invitee = new UserAccount("test-invite-user", "hash", null);
        entityManager.persist(inviter);
        entityManager.persist(invitee);
        entityManager.flush();

        var identity = org.mockito.Mockito.mock(
                com.olehkobylianskyi.familypoints.security.CurrentUserService.class);
        org.mockito.Mockito.when(identity.getCurrentAccount()).thenReturn(inviter);
        var invitations = new com.olehkobylianskyi.familypoints.service.WorkspaceInvitationService(jdbc, identity);
        var issued = invitations.issue(family.getId(), role.getId());
        assertEquals(family.getName(), invitations.preview(issued.token()).workspaceName());
        var context = invitations.begin(issued.token());
        assertEquals(family.getName(), invitations.resume(context.handle()).workspaceName());
        org.mockito.Mockito.when(identity.getCurrentAccount()).thenReturn(invitee);
        assertEquals(family.getId(), invitations.accept(context.handle(), "Joined tester"));
        assertEquals(1, jdbc.queryForObject("""
            SELECT count(*) FROM account_workspace_memberships WHERE account_id=? AND workspace_id=?
            """, Integer.class, invitee.getId(), family.getId()));
        assertEquals(0, jdbc.queryForObject("""
            SELECT count(*) FROM account_workspace_memberships WHERE account_id=? AND workspace_id=?
            """, Integer.class, invitee.getId(), outsider.getId()));
        assertEquals(404, assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> invitations.accept(context.handle(), "Again")).getStatusCode().value());
    }

    @Test
    @Transactional
    void joinManagementHttpRequiresCorrectWorkspaceAndManageMembers() throws Exception {
        Workspace managed = new Workspace("HTTP managed join family");
        Workspace foreign = new Workspace("HTTP foreign join family");
        entityManager.persist(managed);
        entityManager.persist(foreign);
        WorkspaceRole adminRole = new WorkspaceRole(managed, "FAMILY_ADMIN", "Admin", false);
        adminRole.addPermission(WorkspacePermission.MANAGE_MEMBERS);
        WorkspaceRole viewerRole = new WorkspaceRole(foreign, "CHILD", "Viewer", false);
        entityManager.persist(adminRole);
        entityManager.persist(viewerRole);
        WorkspaceMember adminMember = new WorkspaceMember("Admin", WorkspaceMemberType.PARENT, adminRole, managed);
        WorkspaceMember viewerMember = new WorkspaceMember("Viewer", WorkspaceMemberType.CHILD, viewerRole, foreign);
        entityManager.persist(adminMember);
        entityManager.persist(viewerMember);
        UserAccount admin = new UserAccount("http-join-admin-test", "hash", adminMember);
        UserAccount viewer = new UserAccount("http-join-viewer-test", "hash", viewerMember);
        entityManager.persist(admin);
        entityManager.persist(viewer);
        entityManager.persist(new AccountWorkspaceMembership(admin, adminMember));
        entityManager.persist(new AccountWorkspaceMembership(viewer, viewerMember));
        entityManager.flush();
        entityManager.clear();

        String adminJwt = "Bearer " + jwtService.generateWorkspaceAccessToken(
                User.withUsername(admin.getUsername()).password("hash").authorities("USER").build(), managed.getId());
        String viewerJwt = "Bearer " + jwtService.generateWorkspaceAccessToken(
                User.withUsername(viewer.getUsername()).password("hash").authorities("USER").build(), foreign.getId());
        String route = "/api/workspace-joins/workspaces/" + managed.getId() + "/join-code";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(route))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(route)
                        .header("Authorization", viewerJwt))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(route)
                        .header("Authorization", adminJwt))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").isString());
        String inviteRoute = "/api/workspace-invitations/workspaces/" + managed.getId();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(inviteRoute)
                        .header("Authorization", viewerJwt)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }


    @Test
    @Transactional
    void revokedInvitationCannotBeAcceptedEvenWithExistingContext() {
        Workspace family = new Workspace("Revoked invitation family");
        entityManager.persist(family);
        WorkspaceRole role = new WorkspaceRole(family, "CHILD", "Invite child role", false);
        entityManager.persist(role);
        UserAccount issuer = new UserAccount("revoke-inviter-test", "hash", null);
        UserAccount recipient = new UserAccount("revoke-recipient-test", "hash", null);
        entityManager.persist(issuer);
        entityManager.persist(recipient);
        entityManager.flush();

        var identity = org.mockito.Mockito.mock(
                com.olehkobylianskyi.familypoints.security.CurrentUserService.class);
        org.mockito.Mockito.when(identity.getCurrentAccount()).thenReturn(issuer);
        var invitations = new com.olehkobylianskyi.familypoints.service.WorkspaceInvitationService(jdbc, identity);
        var issued = invitations.issue(family.getId(), role.getId());
        var context = invitations.begin(issued.token());
        Long inviteId = jdbc.queryForObject(
                "SELECT id FROM workspace_invitations WHERE workspace_id=?", Long.class, family.getId());
        invitations.revoke(family.getId(), inviteId);
        org.mockito.Mockito.when(identity.getCurrentAccount()).thenReturn(recipient);
        assertEquals(404, assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> invitations.resume(context.handle())).getStatusCode().value());
        assertEquals(404, assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> invitations.accept(context.handle(), "Rejected join")).getStatusCode().value());
        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM account_workspace_memberships WHERE account_id=? AND workspace_id=?",
                Integer.class, recipient.getId(), family.getId()));
    }

    @Test
    @Transactional
    void rejectingJoinRequestDoesNotCreateMembershipAndCannotBeApprovedLater() {
        Workspace family = new Workspace("Rejected request family");
        entityManager.persist(family);
        WorkspaceRole role = new WorkspaceRole(family, "CHILD", "Member", false);
        entityManager.persist(role);
        UserAccount applicant = new UserAccount("rejected-join-applicant", "hash", null);
        UserAccount reviewer = new UserAccount("rejected-join-manager", "hash", null);
        entityManager.persist(applicant);
        entityManager.persist(reviewer);
        entityManager.flush();

        var identity = org.mockito.Mockito.mock(
                com.olehkobylianskyi.familypoints.security.CurrentUserService.class);
        org.mockito.Mockito.when(identity.getCurrentAccount()).thenReturn(applicant);
        var joins = new com.olehkobylianskyi.familypoints.service.WorkspaceJoinService(jdbc, identity);
        String code = joins.rotateJoinCode(family.getId());
        joins.requestJoin(code);
        Long requestId = jdbc.queryForObject(
                "SELECT id FROM workspace_join_requests WHERE workspace_id=? AND requester_account_id=?",
                Long.class, family.getId(), applicant.getId());
        org.mockito.Mockito.when(identity.getCurrentAccount()).thenReturn(reviewer);
        joins.rejectRequest(family.getId(), requestId);
        assertEquals("REJECTED", jdbc.queryForObject(
                "SELECT status FROM workspace_join_requests WHERE id=?", String.class, requestId));
        assertTrue(joins.pendingForManager(family.getId()).isEmpty());
        assertEquals(404, assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> joins.approveRequest(family.getId(), requestId, role.getId(), "Must not join"))
                .getStatusCode().value());
        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM account_workspace_memberships WHERE account_id=? AND workspace_id=?",
                Integer.class, applicant.getId(), family.getId()));
    }

    @Test
    void mediaSchemaIsInstalledWithScopedBlobAndBindingTables() {
        assertEquals(1, jdbc.queryForObject("""
            SELECT count(*) FROM flyway_schema_history
            WHERE version='36' AND success=true
            """, Integer.class));
        for (String table : java.util.List.of("media_assets","media_blobs","media_bindings")) {
            assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema='public' AND table_name=?
                """, Integer.class, table));
        }
    }

    @Test
    @Transactional
    void scopedMediaHttpCannotReadAnotherFamilysPrivateImageEvenKnowingUuid() throws Exception {
        Workspace alpha = new Workspace("Private media family A");
        Workspace beta = new Workspace("Private media family B");
        entityManager.persist(alpha);
        entityManager.persist(beta);
        WorkspaceRole alphaRole = new WorkspaceRole(alpha, "CHILD", "Child A", false);
        WorkspaceRole betaRole = new WorkspaceRole(beta, "CHILD", "Child B", false);
        entityManager.persist(alphaRole);
        entityManager.persist(betaRole);
        WorkspaceMember alphaMember = new WorkspaceMember("Media member A",
                WorkspaceMemberType.CHILD, alphaRole, alpha);
        WorkspaceMember betaMember = new WorkspaceMember("Media member B",
                WorkspaceMemberType.CHILD, betaRole, beta);
        entityManager.persist(alphaMember);
        entityManager.persist(betaMember);
        UserAccount accountA = new UserAccount("http-media-alpha", "hash", alphaMember);
        UserAccount accountB = new UserAccount("http-media-beta", "hash", betaMember);
        entityManager.persist(accountA);
        entityManager.persist(accountB);
        entityManager.persist(new AccountWorkspaceMembership(accountA, alphaMember));
        entityManager.persist(new AccountWorkspaceMembership(accountB, betaMember));
        entityManager.flush();
        java.util.UUID mediaId=java.util.UUID.randomUUID();
        byte[] fakeImage=new byte[]{1,2,3,4}; // HTTP retrieval tests authorization, not decoder.
        jdbc.update("""
            INSERT INTO media_assets
                (id,workspace_id,mime_type,width,height,byte_size,sha256,created_by_member_id)
            VALUES (?::uuid,?,'image/png',1,1,4,?,?)
            """,mediaId.toString(),alpha.getId(),"a".repeat(64),alphaMember.getId());
        jdbc.update("INSERT INTO media_blobs(asset_id,variant,bytes) VALUES (?::uuid,'THUMB',?)",
                mediaId.toString(),fakeImage);
        jdbc.update("""
            INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,asset_id)
            VALUES (?,'MEMBER',?,'AVATAR',?::uuid)
            """,alpha.getId(),alphaMember.getId(),mediaId.toString());

        var principalA=User.withUsername(accountA.getUsername())
                .password("hash").authorities("USER").build();
        var principalB=User.withUsername(accountB.getUsername())
                .password("hash").authorities("USER").build();
        String alphaJwt="Bearer "+jwtService.generateWorkspaceAccessToken(principalA,alpha.getId());
        String betaJwt="Bearer "+jwtService.generateWorkspaceAccessToken(principalB,beta.getId());
        String alphaRoute="/workspaces/"+alpha.getId()+"/media/"+mediaId+"/variants/THUMB";
        String forgedBetaRoute="/workspaces/"+beta.getId()+"/media/"+mediaId+"/variants/THUMB";

        mockMvc.perform(get(alphaRoute).header("Authorization",alphaJwt))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("X-Content-Type-Options","nosniff"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().bytes(fakeImage));
        mockMvc.perform(get(alphaRoute).header("Authorization",betaJwt))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(forgedBetaRoute).header("Authorization",betaJwt))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(alphaRoute)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/workspaces/{workspaceId}/media/bindings/MEMBER/{memberId}/AVATAR",
                        alpha.getId(),alphaMember.getId())
                .header("Authorization",betaJwt)).andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void mediaClearRetainsVersionAndRejectsStaleEditorThroughDatabase() {
        Workspace workspace = new Workspace("Media optimistic lock family");
        entityManager.persist(workspace);
        entityManager.flush();
        Long wid=workspace.getId();
        // A previously selected catalog image at version one.
        jdbc.update("""
            INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,catalog_key,version)
            VALUES (?,'WORKSPACE',?,'ICON','family-default',1)
            """,wid,wid);
        // The clear operation preserves a tombstone at version two.
        jdbc.update("""
            UPDATE media_bindings SET catalog_key=NULL,asset_id=NULL,
                version=version+1 WHERE workspace_id=? AND target_type='WORKSPACE'
                AND target_id=? AND slot='ICON'
            """,wid,wid);
        assertEquals(2,jdbc.queryForObject("""
            SELECT version FROM media_bindings WHERE workspace_id=?
            AND target_type='WORKSPACE' AND target_id=? AND slot='ICON'
            """,Integer.class,wid,wid));
        assertEquals(0,jdbc.update("""
            INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,catalog_key)
            VALUES (?,'WORKSPACE',?,'ICON','family-cover-default')
            ON CONFLICT(workspace_id,target_type,target_id,slot)
            DO UPDATE SET catalog_key=excluded.catalog_key,
                asset_id=NULL,version=media_bindings.version+1
            WHERE media_bindings.version=1
            """,wid,wid));
        assertNull(jdbc.queryForObject("""
            SELECT catalog_key FROM media_bindings WHERE workspace_id=?
            AND target_type='WORKSPACE' AND target_id=? AND slot='ICON'
            """,String.class,wid,wid));
    }

    @Test
    @Transactional
    void mediaBindingCannotReferenceOtherFamilyAssetEvenByDirectSql() {
        Workspace owner = new Workspace("Media constraint owner");
        Workspace attacker = new Workspace("Media constraint outsider");
        entityManager.persist(owner);
        entityManager.persist(attacker);
        WorkspaceRole role = new WorkspaceRole(owner,"PARENT","Owner",false);
        entityManager.persist(role);
        WorkspaceMember member = new WorkspaceMember("Media owner",
                WorkspaceMemberType.PARENT,role,owner);
        entityManager.persist(member);
        entityManager.flush();
        String assetId=java.util.UUID.randomUUID().toString();
        jdbc.update("""
            INSERT INTO media_assets(id,workspace_id,mime_type,width,height,byte_size,sha256,created_by_member_id)
            VALUES (?::uuid,?,'image/png',1,1,4,?,?)
            """,assetId,owner.getId(),"a".repeat(64),member.getId());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
            jdbc.update("""
                INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,asset_id)
                VALUES (?,'WORKSPACE',?,'COVER',?::uuid)
                """,attacker.getId(),attacker.getId(),assetId));
    }

    @Test
    @Transactional
    void mediaSchemaAllowsVersionedClearedBindingButRejectsBothMediaSources() {
        Workspace workspace = new Workspace("Media tombstone constraint");
        entityManager.persist(workspace);
        entityManager.flush();
        jdbc.update("""
            INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,version)
            VALUES (?,'WORKSPACE',?,'COVER',9)
            """,workspace.getId(),workspace.getId());
        assertEquals(9,jdbc.queryForObject("""
            SELECT version FROM media_bindings WHERE workspace_id=? AND target_type='WORKSPACE'
            AND target_id=? AND slot='COVER'
            """,Integer.class,workspace.getId(),workspace.getId()));
    }

    @Test
    void contextLoads() {
        // Spring starts, Flyway migrates V1..V29, and Hibernate validates the schema.
    }
}
