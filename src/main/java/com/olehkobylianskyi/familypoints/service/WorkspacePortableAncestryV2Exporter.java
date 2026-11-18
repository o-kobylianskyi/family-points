package com.olehkobylianskyi.familypoints.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.crypto.SecretKey;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * IMP-01a minimum REAL producer: exports ONLY original role/member historical
 * source ancestry from a pinned PostgreSQL REPEATABLE_READ snapshot.
 *
 * This intentionally cannot restore tasks or finance: missing dependent
 * categories require later versioned additive checkpoints. Does not export
 * names, email, account identifiers or credentials and does not grant a role.
 */
@Service
public class WorkspacePortableAncestryV2Exporter {
    private static final int MAX_ROWS=2000;
    private final JdbcTemplate db;
    private final ProtectiveBackupArtifactCodec codec;
    private final ProtectiveBackupPortableFile portable;
    private final ObjectMapper json=new ObjectMapper();

    public record Result(byte[] encryptedFile,String manifestRootDigest,UUID snapshotId,
                         int roleCount,int memberCount) {}

    public WorkspacePortableAncestryV2Exporter(JdbcTemplate db,
            ProtectiveBackupArtifactCodec codec,ProtectiveBackupPortableFile portable) {
        this.db=Objects.requireNonNull(db);
        this.codec=Objects.requireNonNull(codec);
        this.portable=Objects.requireNonNull(portable);
    }

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Result export(long sourceWorkspaceId,UUID snapshotId,UUID operationId,
                         long expectedEpoch,SecretKey key) {
        return exportInternal(sourceWorkspaceId,snapshotId,operationId,expectedEpoch,key,false);
    }

    /**
     * Full bounded TASK/REWARD/POINT ancestry archive. Unsupported financial
     * event types or source parents reject the entire source before sealing.
     * Never mark it restorable: owner/permit and other data still need approval.
     */
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Result exportWithHistory(long sourceWorkspaceId,UUID snapshotId,UUID operationId,
                                    long expectedEpoch,SecretKey key) {
        return exportInternal(sourceWorkspaceId,snapshotId,operationId,expectedEpoch,key,true);
    }

    private Result exportInternal(long sourceWorkspaceId,UUID snapshotId,UUID operationId,
                          long expectedEpoch,SecretKey key,boolean withHistory) {
        if(sourceWorkspaceId<=0||snapshotId==null||operationId==null||expectedEpoch<0)
            throw new IllegalArgumentException("PORTABLE_V2_SOURCE_SCOPE_INVALID");
        if(!TransactionSynchronizationManager.isActualTransactionActive()
                ||!Objects.equals(Connection.TRANSACTION_REPEATABLE_READ,
                   TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()))
            throw new IllegalStateException("PORTABLE_V2_REPEATABLE_READ_REQUIRED");
        Long epoch=db.queryForObject(
                "SELECT data_epoch FROM families WHERE id=?",Long.class,sourceWorkspaceId);
        if(epoch==null||epoch!=expectedEpoch)
            throw new SecurityException("PORTABLE_V2_SOURCE_EPOCH_STALE");

        var root=json.createObjectNode();
        root.put("format",PortableManifestRootV2.SCHEMA);
        root.put("workspaceId",sourceWorkspaceId);
        root.put("sourceEpoch",expectedEpoch);
        ObjectNode cats=root.putObject("categories");
        var roles=cats.putArray("FAMILY_ROLES");
        var roleRows=db.queryForList("""
                SELECT id,family_id,code,system_role
                FROM family_roles WHERE family_id=? ORDER BY id
                """,sourceWorkspaceId);
        if(roleRows.size()>MAX_ROWS)
            throw new SecurityException("PORTABLE_V2_ROLE_ROW_LIMIT");
        for(var row:roleRows) {
            var role=roles.addObject();
            role.put("id",requiredLong(row,"id"));
            role.put("sourceWorkspaceId",requiredLong(row,"family_id"));
            role.put("code",Objects.toString(row.get("code"),""));
            if(!(row.get("system_role") instanceof Boolean value))
                throw new SecurityException("PORTABLE_V2_ROLE_FLAG_INVALID");
            role.put("systemRole",value);
        }
        var members=cats.putArray("FAMILY_MEMBERS");
        // LEFT JOIN prevents a corrupt foreign role from being silently
        // filtered out; the validator refuses unmatched role ancestry.
        var memberRows=db.queryForList("""
                SELECT m.id,m.family_id,m.family_role_id,m.member_type,m.active,
                       m.deleted_at,r.family_id AS original_role_family
                FROM family_members m
                LEFT JOIN family_roles r ON r.id=m.family_role_id
                WHERE m.family_id=? ORDER BY m.id
                """,sourceWorkspaceId);
        if(memberRows.size()>MAX_ROWS)
            throw new SecurityException("PORTABLE_V2_MEMBER_ROW_LIMIT");
        for(var row:memberRows) {
            if(!(row.get("original_role_family") instanceof Number owner)
                    ||owner.longValue()!=sourceWorkspaceId)
                throw new SecurityException("PORTABLE_V2_MEMBER_FOREIGN_ROLE");
            var member=members.addObject();
            member.put("id",requiredLong(row,"id"));
            member.put("sourceWorkspaceId",requiredLong(row,"family_id"));
            member.put("sourceRoleId",requiredLong(row,"family_role_id"));
            member.put("memberType",Objects.toString(row.get("member_type"),""));
            if(!(row.get("active") instanceof Boolean active))
                throw new SecurityException("PORTABLE_V2_MEMBER_ACTIVE_INVALID");
            member.put("activeAtSnapshot",active);
            Object deleted=row.get("deleted_at");
            if(deleted==null)member.putNull("deletedAtSnapshot");
            else if(deleted instanceof Timestamp timestamp)
                member.put("deletedAtSnapshot",timestamp.toLocalDateTime().toString());
            else if(deleted instanceof LocalDateTime dateTime)
                member.put("deletedAtSnapshot",dateTime.toString());
            else throw new SecurityException("PORTABLE_V2_TOMBSTONE_INVALID");
        }
        if(withHistory)
            PortableV2HistoryGraph.capture(db,cats,sourceWorkspaceId);
        String rootDigest=PortableMemberAncestryV2.computeRootWithoutTrust(
                root,sourceWorkspaceId,snapshotId,expectedEpoch);
        // The root computer validates a defensive copy; embed the independently
        // computed digest into the actual sealed payload before verification.
        root.put("manifestRootDigest",rootDigest);
        var verified=PortableMemberAncestryV2.verify(
                root,sourceWorkspaceId,snapshotId,expectedEpoch);
        if(!rootDigest.equals(verified.manifestRootDigest()))
            throw new SecurityException("PORTABLE_V2_ROOT_RECOMPUTE_MISMATCH");
        Long after=db.queryForObject("SELECT data_epoch FROM families WHERE id=?",
                Long.class,sourceWorkspaceId);
        if(after==null||after!=expectedEpoch)
            throw new SecurityException("PORTABLE_V2_EPOCH_CHANGED");
        byte[] plain;
        try {plain=json.writeValueAsBytes(root);}
        catch(com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("PORTABLE_V2_ENCODE_FAILED",ex);
        }
        try {
            if(plain.length>12*1024*1024)
                throw new SecurityException("PORTABLE_V2_BYTES_LIMIT");
            var sealed=codec.seal(snapshotId,sourceWorkspaceId,operationId,
                    expectedEpoch,verified.sourceDigest(),PortableManifestRootV2.SCHEMA,
                    plain,key);
            return new Result(portable.encode(sealed),rootDigest,snapshotId,
                    roles.size(),members.size());
        } finally {Arrays.fill(plain,(byte)0);}
    }
    private static long requiredLong(Map<String,Object> row,String field) {
        Object raw=row.get(field);
        if(!(raw instanceof Long||raw instanceof Integer)||((Number)raw).longValue()<=0)
            throw new SecurityException("PORTABLE_V2_SOURCE_ID_INVALID:"+field);
        return ((Number)raw).longValue();
    }
}
