package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.security.ResetOriginalOwnerPolicy;
import com.olehkobylianskyi.familypoints.security.ResetOriginalOwnerPolicy.CurrentActor;
import com.olehkobylianskyi.familypoints.security.ResetOriginalOwnerPolicy.Provenance;
import com.olehkobylianskyi.familypoints.security.ResetOriginalOwnerPolicy.VerifiedOwnerBinding;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proposed ORIGINAL owner binding, disposable PG18 only. This is not a real
 * enrollment authority: tests explicitly create QA-only verified provenance.
 * No automatic legacy backfill or production Flyway migration.
 */
@Testcontainers
class DisposableOriginalOwnerBindingPostgresTest {
    @Container static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("owner_binding_qa").withUsername("qa").withPassword("qa");

    private JdbcTemplate db;
    private long a, b, ownerAccount, ownerMember, managerAccount, managerMember;

    @BeforeEach void setup() {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        db=new JdbcTemplate(ds);
        db.execute("""
                CREATE TABLE IF NOT EXISTS qa_c_original_owner_binding (
                    workspace_id bigint PRIMARY KEY REFERENCES families(id) ON DELETE RESTRICT,
                    account_id bigint NOT NULL REFERENCES user_accounts(id) ON DELETE RESTRICT,
                    member_id bigint NOT NULL REFERENCES family_members(id) ON DELETE RESTRICT,
                    provenance varchar(32) NOT NULL
                        CHECK (provenance IN ('REGISTRATION_VERIFIED','LEGACY_VERIFIED')),
                    recorded_at timestamptz NOT NULL DEFAULT clock_timestamp()
                )
                """);
        a=family();
        b=family();
        ownerMember=member(a,"Original owner");
        managerMember=member(a,"Delegated manager");
        ownerAccount=account(ownerMember);
        managerAccount=account(managerMember);
        link(ownerAccount,ownerMember,a);
        link(managerAccount,managerMember,a);
    }

    private long family() {
        return db.queryForObject("INSERT INTO families(name) VALUES (?) RETURNING id",
                Long.class,"Owner identity QA "+UUID.randomUUID());
    }

    private long member(long family,String name) {
        // Family role codes are workspace-unique: reuse the fixture's role
        // rather than inserting PARENT twice for owner and delegated manager.
        List<Long> existingRoles=db.queryForList(
                "SELECT id FROM family_roles WHERE family_id=? AND code='PARENT'",
                Long.class,family);
        Long role=existingRoles.isEmpty() ? db.queryForObject("""
                INSERT INTO family_roles(family_id,system_role,code,name)
                VALUES (?,true,'PARENT','QA role') RETURNING id
                """,Long.class,family) : existingRoles.get(0);
        return db.queryForObject("""
                INSERT INTO family_members(family_id,family_role_id,active,member_type,name)
                VALUES (?,?,true,'PARENT',?) RETURNING id
                """,Long.class,family,role,name);
    }

    private long account(long member) {
        return db.queryForObject("""
                INSERT INTO user_accounts(username,password_hash,enabled,family_member_id)
                VALUES (?,?,true,?) RETURNING id
                """,Long.class,"qa_original_owner_"+UUID.randomUUID(),"$2a$qa-only",member);
    }

    private void link(long account,long member,long family) {
        db.update("""
                INSERT INTO account_workspace_memberships(account_id,family_member_id,workspace_id)
                VALUES (?,?,?)
                """,account,member,family);
    }

    /** A real writer must provide independently verified evidence before storing. */
    private void syntheticEnroll(long family,long account,long member,String provenance) {
        db.update("""
                INSERT INTO qa_c_original_owner_binding(workspace_id,account_id,member_id,provenance)
                VALUES (?,?,?,?)
                """,family,account,member,provenance);
    }

    /** SQL resolver never falls back to admin role, minimum ID or shared account. */
    private boolean eligible(long workspace,long account,long member) {
        List<VerifiedOwnerBinding> bindings=db.query("""
                SELECT o.workspace_id,o.account_id,o.member_id,o.provenance
                FROM qa_c_original_owner_binding o
                JOIN user_accounts u ON u.id=o.account_id AND u.enabled=true
                JOIN family_members m ON m.id=o.member_id AND m.family_id=o.workspace_id
                  AND m.active=true AND m.deleted_at IS NULL
                JOIN account_workspace_memberships am
                  ON am.account_id=o.account_id AND am.family_member_id=o.member_id
                 AND am.workspace_id=o.workspace_id
                WHERE o.workspace_id=?
                """,(rs,i)->new VerifiedOwnerBinding(
                rs.getLong(1),rs.getLong(2),rs.getLong(3),
                Provenance.valueOf(rs.getString(4))),workspace);
        if (bindings.size()!=1) return false;
        var actor=new CurrentActor(workspace,account,member,true,true,true,false,true);
        return ResetOriginalOwnerPolicy.isVerifiedOriginalOwner(workspace,actor,bindings.get(0));
    }

    @Test void unknownLegacyFamilyIsNeverAutomaticallyGranted() {
        assertFalse(eligible(a,ownerAccount,ownerMember));
        assertFalse(eligible(a,managerAccount,managerMember));
        assertFalse(eligible(b,ownerAccount,ownerMember));
    }

    @Test void verifiedOwnerAllowedButDelegatedManagerDenied() {
        syntheticEnroll(a,ownerAccount,ownerMember,"REGISTRATION_VERIFIED");
        assertTrue(eligible(a,ownerAccount,ownerMember));
        assertFalse(eligible(a,managerAccount,managerMember));
        assertFalse(eligible(a,ownerAccount,managerMember));
        assertFalse(eligible(b,ownerAccount,ownerMember));
    }

    @Test void disabledAccountAndDeletedMemberInvalidateBinding() {
        syntheticEnroll(a,ownerAccount,ownerMember,"LEGACY_VERIFIED");
        assertTrue(eligible(a,ownerAccount,ownerMember));
        db.update("UPDATE user_accounts SET enabled=false WHERE id=?",ownerAccount);
        assertFalse(eligible(a,ownerAccount,ownerMember));
        db.update("UPDATE user_accounts SET enabled=true WHERE id=?",ownerAccount);
        db.update("UPDATE family_members SET active=false,deleted_at=now() WHERE id=?",ownerMember);
        assertFalse(eligible(a,ownerAccount,ownerMember));
    }

    @Test void revokedLinkInvalidatesOwnerWithoutTransferringIt() {
        syntheticEnroll(a,ownerAccount,ownerMember,"REGISTRATION_VERIFIED");
        assertTrue(eligible(a,ownerAccount,ownerMember));
        db.update("""
                DELETE FROM account_workspace_memberships
                WHERE account_id=? AND family_member_id=? AND workspace_id=?
                """,ownerAccount,ownerMember,a);
        assertFalse(eligible(a,ownerAccount,ownerMember));
        assertFalse(eligible(a,managerAccount,managerMember));
    }

    @Test void ownerBindingIsUniqueAndBadProvenanceRejected() {
        syntheticEnroll(a,ownerAccount,ownerMember,"REGISTRATION_VERIFIED");
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                ()->syntheticEnroll(a,managerAccount,managerMember,"LEGACY_VERIFIED"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                ()->syntheticEnroll(b,ownerAccount,ownerMember,"INFERRED_ADMIN"));
    }

    @Test void sharedAccountWithDifferentWorkspaceMembershipIsNotOwnerOfNeighbor() {
        syntheticEnroll(a,ownerAccount,ownerMember,"REGISTRATION_VERIFIED");
        long neighborMember=member(b,"Another family member");
        link(ownerAccount,neighborMember,b);
        assertTrue(eligible(a,ownerAccount,ownerMember));
        assertFalse(eligible(b,ownerAccount,neighborMember));
        assertFalse(eligible(b,ownerAccount,ownerMember));
    }
}
