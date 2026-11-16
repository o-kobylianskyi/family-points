package com.olehkobylianskyi.familypoints.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * First real, strictly read-only, tenant-scoped database export slice.
 * Only explicitly-scoped rows are exported; media blobs still require separate handling. No API/download
 * is exposed and no VERIFIED status or automatic pre-reset backup is issued.
 * Additional categories MUST be added only after FK/media/restore design.
 */
@Service
public class WorkspacePortableBackupExporter {
    // Every SQL fragment is hard-coded server-side. FK-scoped tables must
    // follow a family-owned parent; no untrusted table or column names.
    private record Scope(String table,String condition) {}
    private static final Map<String,Scope> SCOPED_TABLES=Map.ofEntries(
        Map.entry("PRIVATE_TEMPLATES",new Scope("workspace_private_templates","t.workspace_id=?")),
        Map.entry("MEMBER_GROUPS",new Scope("member_groups","t.family_id=?")),
        Map.entry("TASK_DEFINITIONS",new Scope("task_definitions","t.family_id=?")),
        Map.entry("REWARD_DEFINITIONS",new Scope("reward_definitions","t.family_id=?")),
        Map.entry("TASK_EXECUTIONS",new Scope("task_instances",
            "EXISTS (SELECT 1 FROM family_members m WHERE m.id=t.member_id AND m.family_id=?)")),
        Map.entry("REWARD_PURCHASES",new Scope("reward_purchases",
            "EXISTS (SELECT 1 FROM family_members m WHERE m.id=t.member_id AND m.family_id=?)")),
        Map.entry("TASK_SCHEDULE_SLOTS",new Scope("task_schedule_slots",
            "EXISTS (SELECT 1 FROM task_definitions d WHERE d.id=t.task_definition_id AND d.family_id=?)")),
        Map.entry("REWARD_REQUESTS",new Scope("reward_requests","t.family_id=?")),
        Map.entry("WORK_NODES",new Scope("work_nodes","t.family_id=?")),
        Map.entry("MEDIA_BINDINGS",new Scope("media_bindings","t.workspace_id=?")),
        Map.entry("MEDIA_ASSETS",new Scope("media_assets","t.workspace_id=?")),
        Map.entry("GROUP_POINT_LEDGER",new Scope("group_point_transactions",
            "EXISTS (SELECT 1 FROM member_groups g WHERE g.id=t.group_id AND g.family_id=?)")),
        Map.entry("POINT_LEDGER",new Scope("point_transactions",
            "EXISTS (SELECT 1 FROM family_members m WHERE m.id=t.member_id AND m.family_id=?)")),
        Map.entry("REPUTATION_LEDGER",new Scope("reputation_transactions",
            "EXISTS (SELECT 1 FROM family_members m WHERE m.id=t.member_id AND m.family_id=?)"))
    );
    private static final int MAX_ROWS=2000;
    private static final int MAX_BYTES=12*1024*1024;
    private final JdbcTemplate db;
    private final WorkspaceResetSnapshotService snapshots;
    private final ProtectiveBackupArtifactCodec codec;
    private final ProtectiveBackupPortableFile files;
    private final ObjectMapper mapper=new ObjectMapper();

    public WorkspacePortableBackupExporter(JdbcTemplate db,WorkspaceResetSnapshotService snapshots,
            ProtectiveBackupArtifactCodec codec,ProtectiveBackupPortableFile files) {
        this.db=db;this.snapshots=snapshots;this.codec=codec;this.files=files;
    }
    public record Result(byte[] encryptedFile,String contentDigest,long byteCount,
                         UUID snapshotId,Map<String,Integer> recordCounts) {}

    @Transactional
    public Result export(long workspaceId,UUID snapshotId,UUID operationId,
                         long expectedEpoch,Collection<String> categories,
                         String schemaVersion,SecretKey key) {
        if(workspaceId<=0 || snapshotId==null || operationId==null || expectedEpoch<0
                || categories==null || categories.isEmpty() || categories.stream().anyMatch(Objects::isNull)
                || !SCOPED_TABLES.keySet().containsAll(categories))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"UNSUPPORTED_EXPORT_SCOPE");
        var selected=categories.stream().distinct().sorted().toList();
        // The same row lock must cover both the digest and all exported rows.
        // A late epoch check outside the lock permits a reset to race export.
        String digest=snapshots.digest(workspaceId,selected);
        Long epoch=db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,workspaceId);
        if(epoch!=expectedEpoch)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"BACKUP_SOURCE_EPOCH_STALE");
        ObjectNode root=mapper.createObjectNode();
        root.put("format","family-points-export-v1");
        root.put("workspaceId",workspaceId);
        ObjectNode data=root.putObject("categories");
        var counts=new TreeMap<String,Integer>();
        for(String category:selected) {
            // Group ledger is only portable when each denomination belongs to
            // the same family as its group. A legacy/corrupt cross-tenant FK
            // must not be silently copied into an authenticated archive.
            if("GROUP_POINT_LEDGER".equals(category)) {
                Long invalid=db.queryForObject("""
                        SELECT count(*) FROM group_point_transactions t
                        JOIN member_groups g ON g.id=t.group_id
                        LEFT JOIN point_types p ON p.id=t.point_type_id
                        WHERE g.family_id=? AND (p.id IS NULL OR p.family_id<>g.family_id)
                        """,Long.class,workspaceId);
                if(invalid==null || invalid!=0L)
                    throw new SecurityException("BACKUP_GROUP_LEDGER_FOREIGN_CURRENCY");
            }
            if("POINT_LEDGER".equals(category)) {
                // The member FK alone is insufficient to prove a compatible
                // denomination. Block corrupted legacy cross-family point
                // types rather than producing a plausible encrypted archive.
                Long invalid=db.queryForObject("""
                        SELECT count(*) FROM point_transactions t
                        JOIN family_members m ON m.id=t.member_id
                        LEFT JOIN point_types p ON p.id=t.point_type_id
                        WHERE m.family_id=? AND (p.id IS NULL OR p.family_id<>m.family_id)
                        """,Long.class,workspaceId);
                if(invalid==null || invalid!=0L)
                    throw new SecurityException("BACKUP_POINT_LEDGER_FOREIGN_CURRENCY");
            }
            if("REWARD_DEFINITIONS".equals(category)
                    || "REWARD_REQUESTS".equals(category)) {
                // Reward requirements are not exported/restored yet; a
                // definition or request may carry fulfilment conditions that
                // must survive any FULL or SELECTIVE replacement.
                Long notPortable=db.queryForObject("""
                        SELECT count(*) FROM reward_requirements r
                        WHERE r.family_id=?
                        """,Long.class,workspaceId);
                if(notPortable==null || notPortable!=0L)
                    throw new IllegalStateException("BACKUP_UNEXPORTED_REWARD_REQUIREMENTS");
            }
            if("TASK_EXECUTIONS".equals(category)) {
                Long invalid=db.queryForObject("""
                        SELECT count(*) FROM task_instances t
                        JOIN family_members m ON m.id=t.member_id
                        LEFT JOIN task_definitions d ON d.id=t.task_definition_id
                        WHERE m.family_id=? AND (d.id IS NULL OR d.family_id<>m.family_id)
                        """,Long.class,workspaceId);
                if(invalid==null || invalid!=0L)
                    throw new SecurityException("BACKUP_TASK_EXECUTION_FOREIGN_DEFINITION");
            }
            if("REWARD_PURCHASES".equals(category)) {
                // Reward obligations and their blocked-category/reward scopes
                // are not portable categories yet. Exporting a purchase
                // without this lifecycle history would silently lose its
                // unresolved entitlement/requirement on later restoration.
                Long unexported=db.queryForObject("""
                        SELECT count(*) FROM reward_obligations o
                        JOIN reward_purchases p ON p.id=o.reward_purchase_id
                        JOIN family_members m ON m.id=p.member_id
                        WHERE m.family_id=?
                        """,Long.class,workspaceId);
                if(unexported==null || unexported!=0L)
                    throw new IllegalStateException("BACKUP_UNEXPORTED_REWARD_OBLIGATIONS");
            }
            if("REWARD_PURCHASES".equals(category)) {
                Long invalid=db.queryForObject("""
                        SELECT count(*) FROM reward_purchases t
                        JOIN family_members m ON m.id=t.member_id
                        LEFT JOIN reward_definitions d ON d.id=t.reward_definition_id
                        LEFT JOIN reward_requests q ON q.id=t.reward_request_id
                        LEFT JOIN point_types p ON p.id=t.point_type_id
                        WHERE m.family_id=? AND
                          (p.id IS NULL OR p.family_id<>m.family_id
                           OR (t.reward_definition_id IS NOT NULL
                               AND (d.id IS NULL OR d.family_id<>m.family_id))
                           OR (t.reward_request_id IS NOT NULL
                               AND (q.id IS NULL OR q.family_id<>m.family_id)))
                        """,Long.class,workspaceId);
                if(invalid==null || invalid!=0L)
                    throw new SecurityException("BACKUP_REWARD_PURCHASE_FOREIGN_RELATION");
            }
            if("REWARD_REQUESTS".equals(category)) {
                Long invalid=db.queryForObject("""
                        SELECT count(*) FROM reward_requests t
                        LEFT JOIN family_members m ON m.id=t.requested_by_member_id
                        LEFT JOIN reward_definitions d ON d.id=t.reward_definition_id
                        LEFT JOIN point_types p ON p.id=t.approved_point_type_id
                        WHERE t.family_id=?
                          AND (m.id IS NULL OR m.family_id<>t.family_id
                               OR (t.reward_definition_id IS NOT NULL
                                   AND (d.id IS NULL OR d.family_id<>t.family_id))
                               OR (t.approved_point_type_id IS NOT NULL
                                   AND (p.id IS NULL OR p.family_id<>t.family_id)))
                        """,Long.class,workspaceId);
                if(invalid==null || invalid!=0L)
                    throw new SecurityException("BACKUP_REWARD_REQUEST_FOREIGN_RELATION");
            }
            Scope scope=SCOPED_TABLES.get(category);
            String filter=" FROM public."+scope.table()+" t WHERE "+scope.condition();
            Integer count=db.queryForObject("SELECT count(*)"+filter,Integer.class,workspaceId);
            if(count==null || count>MAX_ROWS)
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"BACKUP_ROW_LIMIT");
            ArrayNode records=data.putArray(category);
            // Preserve the actual binary media variants inside the encrypted
            // archive; a media metadata-only export is not recoverable.
            String projection="MEDIA_ASSETS".equals(category)
                    ? "(to_jsonb(t) || jsonb_build_object('blobs',COALESCE(("
                        + "SELECT jsonb_agg(jsonb_build_object('variant',b.variant,"
                        + "'bytesBase64',encode(b.bytes,'base64')) ORDER BY b.variant)"
                        + " FROM media_blobs b WHERE b.asset_id=t.id),'[]'::jsonb)))::text"
                    : "to_jsonb(t)::text";
            db.query("SELECT "+projection+" AS row_data"+filter+" ORDER BY row_data",rs->{
                String row=rs.getString(1);
                try {records.add(mapper.readTree(row));}
                catch(Exception ex){throw new IllegalStateException("Unable to encode family row",ex);}
            },workspaceId);
            if(records.size()!=count)
                throw new ResponseStatusException(HttpStatus.CONFLICT,"BACKUP_SOURCE_CHANGED");
            counts.put(category,count);
        }
        // Detect direct SQL mutations that bypass the application epoch guard
        // while the family lock is held. Counts alone cannot detect row edits.
        if(!digest.equals(snapshots.digest(workspaceId,selected)))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"BACKUP_SOURCE_CHANGED");
        byte[] plain;
        try {plain=mapper.writeValueAsBytes(root);}
        catch(Exception ex){throw new IllegalStateException("Backup serialization failed",ex);}
        if(plain.length>MAX_BYTES)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"BACKUP_BYTE_LIMIT");
        try {
            var sealed=codec.seal(snapshotId,workspaceId,operationId,expectedEpoch,
                    digest,schemaVersion,plain,key);
            return new Result(files.encode(sealed),digest,plain.length,snapshotId,Map.copyOf(counts));
        } finally {Arrays.fill(plain,(byte)0);}
    }
}
