package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olehkobylianskyi.familypoints.service.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.crypto.SecretKey;
import java.util.*;

/**
 * Test-only native SQL restoration for tenant-owned private TASK/REWARD drafts.
 * Supports exactly PRIVATE_TEMPLATES and never activates catalog entries,
 * schedules tasks, purchases rewards or writes any economic ledger.
 */
final class DisposablePrivateTemplateRestoreExecutor {
    record Result(Map<Long,Long> templateIds,int restored) {}
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final ProtectiveBackupImportInspector inspector;
    private final ProtectiveBackupPortableFile portable;
    private final ProtectiveBackupArtifactCodec codec;
    private final ObjectMapper json=new ObjectMapper();

    DisposablePrivateTemplateRestoreExecutor(JdbcTemplate db,PlatformTransactionManager manager,
            ProtectiveBackupImportInspector inspector,ProtectiveBackupPortableFile portable,
            ProtectiveBackupArtifactCodec codec) {
        this.db=db;this.tx=new TransactionTemplate(manager);this.inspector=inspector;
        this.portable=portable;this.codec=codec;
    }

    Result apply(byte[] file,long source,long target,SecretKey key) {
        if(source<=0 || target<=0 || source==target)
            throw new IllegalArgumentException("DISPOSABLE_DISTINCT_FAMILIES_REQUIRED");
        var proof=inspector.inspect(file,source,key);
        if(!proof.availableCategories().equals(List.of("PRIVATE_TEMPLATES"))
                || !"V50".equals(proof.schemaVersion()))
            throw new IllegalArgumentException("PRIVATE_TEMPLATE_SCOPE_UNSUPPORTED");
        var sealed=portable.decode(file);
        byte[] clear=codec.open(sealed,sealed.manifest().snapshotId(),source,key);
        final JsonNode rows;
        try {rows=json.readTree(clear).path("categories").path("PRIVATE_TEMPLATES");}
        catch(Exception ex){throw new IllegalArgumentException("MALFORMED_TEMPLATE_ARCHIVE",ex);}
        finally {Arrays.fill(clear,(byte)0);}
        var originalIds=new HashSet<Long>();
        for(var row:rows) {
            long id=row.path("id").asLong();
            if(row.path("workspace_id").asLong()!=source || id<=0
                    || !originalIds.add(id)
                    || !Set.of("TASK","REWARD").contains(row.path("kind").asText())
                    || !row.path("title").isTextual()
                    || row.path("title").asText().isBlank()
                    || row.path("title").asText().length()>500
                    || row.path("definition_json").isMissingNode()
                    || row.path("definition_json").isNull())
                throw new IllegalArgumentException("INVALID_ARCHIVED_PRIVATE_TEMPLATE");
        }
        return tx.execute(status->{
            Long id=db.queryForObject("SELECT id FROM families WHERE id=? FOR UPDATE",Long.class,target);
            if(id==null)throw new IllegalArgumentException("TARGET_MISSING");
            Long epoch=db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,target);
            if(epoch==null || epoch!=0L)
                throw new IllegalStateException("TARGET_TEMPLATE_EPOCH_STALE");
            Long count=db.queryForObject("SELECT count(*) FROM workspace_private_templates WHERE workspace_id=?",
                    Long.class,target);
            if(count==null || count>0)
                throw new IllegalStateException("TARGET_TEMPLATES_NOT_EMPTY");
            var mapping=new TreeMap<Long,Long>();
            for(var row:rows) {
                var definition=row.path("definition_json");
                // The exporter preserves the underlying PostgreSQL column as a
                // JSON field or as its stored text. Never reinterpret missing data.
                String serialized=definition.isTextual()?definition.asText():definition.toString();
                Long newId=db.queryForObject("""
                        INSERT INTO workspace_private_templates
                        (workspace_id,kind,title,definition_json,catalog_code,catalog_version)
                        VALUES (?,?,?,?,?,?) RETURNING id
                        """,Long.class,target,row.path("kind").asText(),row.path("title").asText(),
                        serialized,row.path("catalog_code").isNull()?null:row.path("catalog_code").asText(null),
                        row.path("catalog_version").isNull()?null:
                            row.path("catalog_version").isNumber()?row.path("catalog_version").asInt():null);
                if(newId==null || newId.equals(row.path("id").asLong()))
                    throw new IllegalStateException("TEMPLATE_ID_REMAP_FAILED");
                mapping.put(row.path("id").asLong(),newId);
            }
            Long after=db.queryForObject("SELECT count(*) FROM workspace_private_templates WHERE workspace_id=?",
                    Long.class,target);
            if(after==null || after.intValue()!=rows.size())
                throw new IllegalStateException("PRIVATE_TEMPLATE_COUNT_MISMATCH");
            return new Result(Map.copyOf(mapping),rows.size());
        });
    }
}
