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
 * Only the two family-direct tables below are exported. No API/download
 * is exposed and no VERIFIED status or automatic pre-reset backup is issued.
 * Additional categories MUST be added only after FK/media/restore design.
 */
@Service
public class WorkspacePortableBackupExporter {
    private static final Map<String,String> SCOPED_TABLES=Map.of(
        "PRIVATE_TEMPLATES","workspace_private_templates",
        "MEMBER_GROUPS","member_groups");
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
            String table=SCOPED_TABLES.get(category); // hard-coded trusted SQL identifier
            Integer count=db.queryForObject("SELECT count(*) FROM "+table+" WHERE "+
                    ("workspace_private_templates".equals(table)?"workspace_id":"family_id")+"=?",
                    Integer.class,workspaceId);
            if(count==null || count>MAX_ROWS)
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"BACKUP_ROW_LIMIT");
            ArrayNode records=data.putArray(category);
            String sql="SELECT to_jsonb(t)::text FROM public."+table+" t WHERE "+
                    ("workspace_private_templates".equals(table)?"workspace_id":"family_id")+
                    "=? ORDER BY id";
            db.query(sql,rs->{
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
