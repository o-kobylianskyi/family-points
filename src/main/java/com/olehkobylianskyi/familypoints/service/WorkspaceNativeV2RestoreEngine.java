package com.olehkobylianskyi.familypoints.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.olehkobylianskyi.familypoints.security.WorkspaceRestorePermitScopeV2;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Internal bounded native schema Restore engine. Intentionally NO HTTP
 * mapping, scheduler, production activation or generic destructive merge.
 *
 * Supported source = authenticated eight-category v2 history; existing target
 * roster and point denominations MUST have independently reviewed bindings.
 * Target task/reward/ledger tables must be empty for that family. Unsupported
 * source relationships fail closed. Exactly one family-lock transaction owns
 * permit consumption, native inserts, finance parity, epoch and receipt.
 */
@Service
public final class WorkspaceNativeV2RestoreEngine {
    private static final ObjectMapper JSON=new ObjectMapper();
    private final JdbcTemplate db;
    private final WorkspaceEpochTransactionGuard epochs;
    private final WorkspaceRestorePermitService permits;
    private final ProtectiveBackupImportInspector inspector;
    private final ProtectiveBackupPortableFile files;
    private final ProtectiveBackupArtifactCodec codec;

    public WorkspaceNativeV2RestoreEngine(JdbcTemplate db,
            WorkspaceEpochTransactionGuard epochs,WorkspaceRestorePermitService permits,
            ProtectiveBackupImportInspector inspector,
            ProtectiveBackupPortableFile files,ProtectiveBackupArtifactCodec codec) {
        this.db=db;this.epochs=epochs;this.permits=permits;this.inspector=inspector;
        this.files=files;this.codec=codec;
    }

    public record Result(UUID operationId,long targetFamily,long previousEpoch,
            long newEpoch,Map<String,Integer> insertedCounts,String receiptDigest) {}

    @Transactional
    public Result restore(WorkspaceRestorePermitScopeV2.RestoreScope scope,
            byte[] signedArchive,SecretKey key,Map<Long,Long> reviewedMembers,
            Map<Long,Long> reviewedCurrencies) {
        Objects.requireNonNull(scope,"scope");
        if(signedArchive==null||key==null||reviewedMembers==null
                ||reviewedCurrencies==null||reviewedCurrencies.isEmpty())
            throw new SecurityException("RESTORE_NATIVE_SIGNED_INPUT_REQUIRED");
        long target=scope.targetWorkspaceId();
        long oldEpoch=epochs.requireCurrentEpoch(target,scope.targetExpectedEpoch());
        var accepted=inspector.inspectAncestryV2(
                signedArchive,scope.source().sourceWorkspaceId(),key);
        if(accepted.verifiedCategoryCounts().size()!=8
                ||!accepted.verifiedSourceArchiveV2().equals(scope.source())
                ||!accepted.canonicalMappingDigestForReviewedIds(target,reviewedMembers)
                         .equals(scope.memberMappingDigest()))
            throw new SecurityException("RESTORE_NATIVE_SOURCE_SCOPE_INVALID");
        if(reviewedMembers.isEmpty()||new HashSet<>(reviewedMembers.values()).size()!=reviewedMembers.size())
            throw new SecurityException("RESTORE_NATIVE_MEMBER_MAP_NOT_INJECTIVE");
        for(Long member:reviewedMembers.values()) {
            if(!countEquals("SELECT count(*) FROM family_members WHERE id=? AND family_id=?",
                    1L,member,target))
                throw new SecurityException("RESTORE_NATIVE_FOREIGN_MEMBER");
        }
        // Every point denomination is a positive owner-reviewed target identity,
        // bound by same code; no numeric source ID coincidence is trusted.
        var artifact=files.decode(signedArchive);
        byte[] plain=codec.open(artifact,scope.source().snapshotId(),
                scope.source().sourceWorkspaceId(),key);
        final JsonNode cats;
        try {
            JsonNode root=JSON.readTree(plain);
            cats=root.path("categories");
            if(!cats.isObject()||cats.size()!=8)
                throw new SecurityException("RESTORE_NATIVE_CATEGORY_SCOPE");
        } catch(java.io.IOException invalid) {
            throw new SecurityException("RESTORE_NATIVE_DECODE_INVALID",invalid);
        } finally {Arrays.fill(plain,(byte)0);}
        requireExistingCurrencyMap(cats.path("POINT_TYPES"),reviewedCurrencies,target);
        for(String table:List.of("task_definitions","reward_definitions")) {
            if(!countEquals("SELECT count(*) FROM "+table+" WHERE family_id=?",0L,target))
                throw new SecurityException("RESTORE_NATIVE_TARGET_ALREADY_POPULATED:"+table);
        }
        for(String table:List.of("task_instances","reward_purchases","point_transactions")) {
            if(!countEquals("SELECT count(*) FROM "+table+" t JOIN family_members m ON m.id=t.member_id WHERE m.family_id=?",
                    0L,target))
                throw new SecurityException("RESTORE_NATIVE_TARGET_ALREADY_POPULATED:"+table);
        }
        // Fail closed for dependent subgraphs outside the signed source eight.
        // A separate source-side closure guard runs BEFORE export; target-side
        // empty checks prevent quietly overwriting existing relationships.
        for(JsonNode row:cats.path("TASK_DEFINITIONS"))
            if(row.hasNonNull("target_group_id")||row.hasNonNull("work_node_id"))
                throw new SecurityException("RESTORE_NATIVE_UNSUPPORTED_TASK_PARENT");
        for(JsonNode row:cats.path("REWARD_DEFINITIONS"))
            if(!"ALWAYS".equals(row.path("availability_mode").asText())
                    ||!"DIRECT".equals(row.path("acquisition_mode").asText()))
                throw new SecurityException("RESTORE_NATIVE_UNSUPPORTED_REWARD_MODE");
        // Consumed in SAME target transaction, with family lock already held.
        permits.consumeInRestoreTransaction(scope,signedArchive,key,reviewedMembers);
        var tasks=new TreeMap<Long,Long>();
        var rewards=new TreeMap<Long,Long>();
        var executions=new TreeMap<Long,Long>();
        var purchases=new TreeMap<Long,Long>();
        for(JsonNode row:cats.path("TASK_DEFINITIONS")) {
            ObjectNode copy=objectCopy(row);
            copy.put("family_id",target);
            remapNullable(copy,"assigned_member_id",reviewedMembers);
            remapNullable(copy,"created_by_member_id",reviewedMembers);
            remapNullable(copy,"preferred_member_id",reviewedMembers);
            remapNullable(copy,"responsible_member_id",reviewedMembers);
            remapNullable(copy,"reward_point_type_id",reviewedCurrencies);
            remapNullable(copy,"penalty_point_type_id",reviewedCurrencies);
            tasks.put(id(row,"id"),nativeInsert("task_definitions",copy));
        }
        for(JsonNode row:cats.path("REWARD_DEFINITIONS")) {
            ObjectNode copy=objectCopy(row);
            copy.put("family_id",target);
            remapRequired(copy,"point_type_id",reviewedCurrencies);
            rewards.put(id(row,"id"),nativeInsert("reward_definitions",copy));
        }
        for(JsonNode row:cats.path("TASK_EXECUTIONS")) {
            ObjectNode copy=objectCopy(row);
            remapRequired(copy,"member_id",reviewedMembers);
            remapNullable(copy,"original_assigned_member_id",reviewedMembers);
            remapRequired(copy,"task_definition_id",tasks);
            remapNullable(copy,"reward_point_type_id",reviewedCurrencies);
            remapNullable(copy,"penalty_point_type_id",reviewedCurrencies);
            executions.put(id(row,"id"),nativeInsert("task_instances",copy));
        }
        for(JsonNode row:cats.path("REWARD_PURCHASES")) {
            if(row.hasNonNull("reward_request_id"))
                throw new SecurityException("RESTORE_NATIVE_REQUEST_PURCHASE_UNSUPPORTED");
            ObjectNode copy=objectCopy(row);
            remapRequired(copy,"member_id",reviewedMembers);
            remapRequired(copy,"reward_definition_id",rewards);
            remapRequired(copy,"point_type_id",reviewedCurrencies);
            purchases.put(id(row,"id"),nativeInsert("reward_purchases",copy));
        }
        var expected=new TreeMap<String,Long>();
        for(JsonNode row:cats.path("POINT_LEDGER")) {
            ObjectNode copy=objectCopy(row);
            remapRequired(copy,"member_id",reviewedMembers);
            remapRequired(copy,"point_type_id",reviewedCurrencies);
            String sourceType=row.path("source_type").asText();
            if("TASK".equals(sourceType))remapRequired(copy,"source_id",executions);
            else if("REWARD_PURCHASE".equals(sourceType))
                remapRequired(copy,"source_id",purchases);
            else if(!"MANUAL".equals(sourceType)||row.hasNonNull("source_id"))
                throw new SecurityException("RESTORE_NATIVE_UNSUPPORTED_LEDGER_SOURCE");
            long member=id(copy,"member_id"),currency=id(copy,"point_type_id");
            if(!row.path("amount").isIntegralNumber())
                throw new SecurityException("RESTORE_NATIVE_INVALID_AMOUNT");
            expected.merge(member+":"+currency,row.path("amount").asLong(),Long::sum);
            nativeInsert("point_transactions",copy);
        }
        var actual=new TreeMap<String,Long>();
        db.query("""
                SELECT t.member_id,t.point_type_id,SUM(t.amount) AS total
                FROM point_transactions t JOIN family_members m ON m.id=t.member_id
                WHERE m.family_id=? GROUP BY t.member_id,t.point_type_id
                """,rs->{actual.put(rs.getLong(1)+":"+rs.getLong(2),rs.getLong(3));},target);
        if(!actual.equals(expected))
            throw new SecurityException("RESTORE_NATIVE_FINANCIAL_PARITY_FAILURE");
        int epochChange=db.update("""
                UPDATE families SET data_epoch=data_epoch+1
                WHERE id=? AND data_epoch=?
                """,target,oldEpoch);
        if(epochChange!=1)throw new SecurityException("RESTORE_NATIVE_EPOCH_CONFLICT");
        var counts=new TreeMap<String,Integer>();
        counts.put("POINT_TYPES",cats.path("POINT_TYPES").size());
        counts.put("TASK_DEFINITIONS",tasks.size());
        counts.put("REWARD_DEFINITIONS",rewards.size());
        counts.put("TASK_EXECUTIONS",executions.size());
        counts.put("REWARD_PURCHASES",purchases.size());
        counts.put("POINT_LEDGER",cats.path("POINT_LEDGER").size());
        counts.put("FAMILY_MEMBERS",cats.path("FAMILY_MEMBERS").size());
        counts.put("FAMILY_ROLES",cats.path("FAMILY_ROLES").size());
        String scopeDigest=WorkspaceRestorePermitScopeV2.canonicalScopeDigest(scope);
        String receiptDigest=ProtectiveBackupArtifactCodec.sha((
                "family-points:restore:receipt:v2|"+scopeDigest+"|"
                +scope.source().manifestRootDigest()+"|"+oldEpoch+"|"
                +(oldEpoch+1)+"|"+expected.toString()+"|"+counts
                ).getBytes(StandardCharsets.UTF_8));
        final String countsJson;
        try {countsJson=JSON.writeValueAsString(counts);}
        catch(com.fasterxml.jackson.core.JsonProcessingException impossible) {
            throw new IllegalStateException(impossible);
        }
        int inserted=db.update("""
                INSERT INTO workspace_restore_receipts(
                 operation_id,target_workspace_id,scope_digest,manifest_root_digest,
                 target_epoch_before,target_epoch_after,category_counts,receipt_digest)
                VALUES (?,?,?,?,?,?,?::jsonb,?)
                """,scope.operationId(),target,scopeDigest,
                scope.source().manifestRootDigest(),oldEpoch,oldEpoch+1,countsJson,receiptDigest);
        if(inserted!=1)throw new SecurityException("RESTORE_NATIVE_RECEIPT_INSERT_FAILED");
        return new Result(scope.operationId(),target,oldEpoch,oldEpoch+1,
                Map.copyOf(counts),receiptDigest);
    }

    private void requireExistingCurrencyMap(JsonNode rows,Map<Long,Long> bindings,long target) {
        if(!rows.isArray()||bindings.size()!=rows.size()
                ||new HashSet<>(bindings.values()).size()!=bindings.size())
            throw new SecurityException("RESTORE_NATIVE_CURRENCY_BINDINGS_INCOMPLETE");
        for(JsonNode row:rows) {
            long original=id(row,"id");
            Long mapped=bindings.get(original);
            if(mapped==null||mapped<=0)
                throw new SecurityException("RESTORE_NATIVE_CURRENCY_UNMAPPED");
            var codes=db.queryForList(
                    "SELECT code FROM point_types WHERE id=? AND family_id=?",
                    String.class,mapped,target);
            if(codes.size()!=1||!codes.get(0).equals(row.path("code").asText()))
                throw new SecurityException("RESTORE_NATIVE_CURRENCY_CODE_MISMATCH");
        }
    }

    private static ObjectNode objectCopy(JsonNode row) {
        if(!(row instanceof ObjectNode object))
            throw new SecurityException("RESTORE_NATIVE_ROW_INVALID");
        ObjectNode copy=object.deepCopy();
        copy.remove("id");
        return copy;
    }
    private static long id(JsonNode row,String key) {
        JsonNode value=row.path(key);
        if(!value.isIntegralNumber()||value.asLong()<=0)
            throw new SecurityException("RESTORE_NATIVE_ID_INVALID:"+key);
        return value.asLong();
    }
    private static void remapRequired(ObjectNode row,String field,Map<Long,Long> mapping) {
        long source=id(row,field);
        Long target=mapping.get(source);
        if(target==null||target<=0)
            throw new SecurityException("RESTORE_NATIVE_MAPPING_MISSING:"+field);
        row.put(field,target);
    }
    private static void remapNullable(ObjectNode row,String field,Map<Long,Long> mapping) {
        if(row.hasNonNull(field))remapRequired(row,field,mapping);
    }
    private boolean countEquals(String sql,long expected,Object... args) {
        return Objects.equals(expected,db.queryForObject(sql,Long.class,args));
    }

    /**
     * Reuse exact schema-native PostgreSQL type conversion, not Java enum/date
     * reconstruction. The whitelist prevents arbitrary table writes; columns
     * must exist in the migrated target table and are name-validated.
     */
    private long nativeInsert(String table,ObjectNode row) {
        if(!Set.of("task_definitions","reward_definitions","task_instances",
                "reward_purchases","point_transactions").contains(table))
            throw new SecurityException("RESTORE_NATIVE_TABLE_UNSUPPORTED");
        var names=new ArrayList<String>();
        row.fieldNames().forEachRemaining(names::add);
        if(names.isEmpty()||names.size()>100||names.contains("id"))
            throw new SecurityException("RESTORE_NATIVE_COLUMNS_INVALID");
        for(String col:names)
            if(!col.matches("[a-z][a-z0-9_]*"))
                throw new SecurityException("RESTORE_NATIVE_COLUMN_INVALID");
        Collections.sort(names);
        String columns=String.join(",",names);
        String sql="INSERT INTO public."+table+"("+columns+") SELECT "+columns+
                " FROM jsonb_populate_record(NULL::public."+table+",?::jsonb) RETURNING id";
        final String json;
        try {json=JSON.writeValueAsString(row);}
        catch(com.fasterxml.jackson.core.JsonProcessingException invalid) {
            throw new SecurityException("RESTORE_NATIVE_ROW_JSON_INVALID",invalid);
        }
        Long inserted=db.queryForObject(sql,Long.class,json);
        if(inserted==null||inserted<=0)
            throw new SecurityException("RESTORE_NATIVE_INSERT_FAILED");
        return inserted;
    }
}
