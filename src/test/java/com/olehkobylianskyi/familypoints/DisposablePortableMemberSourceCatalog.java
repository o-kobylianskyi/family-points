package com.olehkobylianskyi.familypoints;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Connection;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * TEST-ONLY Backend A source-provenance reader for B-A-04 contract rehearsal.
 *
 * This is NOT the approved portable wire schema, exporter implementation,
 * authentication proof or restore target-mapping authority.
 *
 * Reads only source-family member and role provenance, never credentials,
 * emails, OAuth identifiers, account memberships or private profiles.
 * Must run in the same pinned REPEATABLE_READ view as the future exporter.
 */
public final class DisposablePortableMemberSourceCatalog {
    private final JdbcTemplate db;
    public DisposablePortableMemberSourceCatalog(JdbcTemplate db) {
        this.db=Objects.requireNonNull(db);
    }

    public record Role(long sourceRoleId,long sourceWorkspaceId,String roleCode,
                       boolean systemRole) {}
    public record Member(long sourceMemberId,long sourceWorkspaceId,long sourceRoleId,
                         String memberType,boolean activeAtSnapshot,
                         LocalDateTime deletedAtSnapshot) {}
    public record Catalog(long sourceWorkspaceId,List<Role> roles,List<Member> members) {}

    public Catalog capture(long workspaceId) {
        if(workspaceId<=0)throw new IllegalArgumentException("INVALID_SOURCE_WORKSPACE");
        if(!TransactionSynchronizationManager.isActualTransactionActive()
                || !Integer.valueOf(Connection.TRANSACTION_REPEATABLE_READ).equals(
                        TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()))
            throw new IllegalStateException("SOURCE_CATALOG_REPEATABLE_READ_REQUIRED");

        var roleRows=db.queryForList("""
                SELECT id,family_id,code,system_role
                FROM family_roles WHERE family_id=? ORDER BY id
                """,workspaceId);
        if(roleRows.size()>2000)throw new IllegalStateException("SOURCE_ROLE_ROW_LIMIT");
        var roles=new ArrayList<Role>();
        var roleIds=new HashSet<Long>();
        var roleCodes=new HashSet<String>();
        for(var row:roleRows) {
            long id=positive(row.get("id")),family=positive(row.get("family_id"));
            String code=(String)row.get("code");
            if(family!=workspaceId || !roleIds.add(id) || code==null || code.isBlank()
                    || !roleCodes.add(code)
                    || !(row.get("system_role") instanceof Boolean))
                throw new SecurityException("SOURCE_ROLE_PROVENANCE_INVALID");
            roles.add(new Role(id,family,code,(Boolean)row.get("system_role")));
        }

        // LEFT JOIN (rather than filtering via JOIN) is intentional: corrupt
        // foreign/missing role ancestry must be rejected, not silently skipped.
        var memberRows=db.queryForList("""
                SELECT m.id,m.family_id,m.family_role_id,m.member_type,m.active,
                       m.deleted_at,r.family_id AS role_family_id
                FROM family_members m
                LEFT JOIN family_roles r ON r.id=m.family_role_id
                WHERE m.family_id=? ORDER BY m.id
                """,workspaceId);
        if(memberRows.size()>2000)throw new IllegalStateException("SOURCE_MEMBER_ROW_LIMIT");
        var members=new ArrayList<Member>();
        var memberIds=new HashSet<Long>();
        for(var row:memberRows) {
            long id=positive(row.get("id")),family=positive(row.get("family_id")),
                    roleId=positive(row.get("family_role_id"));
            String type=(String)row.get("member_type");
            if(family!=workspaceId || !memberIds.add(id)
                    || !roleIds.contains(roleId)
                    || !(row.get("role_family_id") instanceof Number owner)
                    || owner.longValue()!=workspaceId
                    || !Set.of("PARENT","CHILD","OTHER").contains(type)
                    || !(row.get("active") instanceof Boolean))
                throw new SecurityException("SOURCE_MEMBER_PROVENANCE_INVALID");
            Object rawDeleted=row.get("deleted_at");
            LocalDateTime deleted=rawDeleted==null?null
                    :rawDeleted instanceof Timestamp timestamp?timestamp.toLocalDateTime()
                    :rawDeleted instanceof LocalDateTime dateTime?dateTime:null;
            if(rawDeleted!=null && deleted==null)
                throw new SecurityException("SOURCE_MEMBER_TOMBSTONE_INVALID");
            members.add(new Member(id,family,roleId,type,(Boolean)row.get("active"),deleted));
        }
        // For a source-roster fixture we retain only the signed-graph attributes
        // needed by the consumer. Member display names are excluded deliberately.
        return new Catalog(workspaceId,List.copyOf(roles),List.copyOf(members));
    }

    private static long positive(Object raw) {
        if(!(raw instanceof Long || raw instanceof Integer) || ((Number)raw).longValue()<=0)
            throw new SecurityException("SOURCE_PROVENANCE_ID_INVALID");
        return n.longValue();
    }
}
