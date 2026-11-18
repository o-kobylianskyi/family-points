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
 * Internal-only B-INT-02 bounded native PostgreSQL replay.
 *
 * Does not expose an HTTP controller or change the disabled Reset/Restore
 * feature gate. The only supported historical shape is the signed 8-category
 * roster/task/reward/point graph. All source IDs are explicitly rebound.
 * Foreign/nonempty target histories, unsupported economic provenance and
 * absent parent maps are rejected before the first INSERT.
 *
 * Applying uses real native tables, not QA shadow tables. Durable permit
 * consumption, native inserts, epoch increment and receipt commit as ONE SQL
 * transaction. This service is not a production release authorization.
 */
@Service
public final class WorkspaceRestoreNativeTargetEngine {
    private static final List<String> ORDER=List.of(
            "POINT_TYPES","TASK_DEFINITIONS","REWARD_DEFINITIONS",
            "TASK_EXECUTIONS","REWARD_PURCHASES","POINT_LEDGER");
    private static final Map<String,String> TABLES=Map.of(
            "POINT_TYPES","point_types",
            "TASK_DEFINITIONS","task_definitions",
            "REWARD_DEFINITIONS","reward_definitions",
            "TASK_EXECUTIONS","task_instances",
            "REWARD_PURCHASES","reward_purchases",
            "POINT_LEDGER","point_transactions");
    private static final Map<String,String> FOREIGN=Map.ofEntries(
            Map.entry("reward_point_type_id","POINT_TYPES"),
            Map.entry("penalty_point_type_id","POINT_TYPES"),
            Map.entry("point_type_id","POINT_TYPES"),
            Map.entry("task_definition_id","TASK_DEFINITIONS"),
            Map.entry("reward_definition_id","REWARD_DEFINITIONS"),
            Map.entry("assigned_member_id","FAMILY_MEMBERS"),
            Map.entry("created_by_member_id","FAMILY_MEMBERS"),
            Map.entry("preferred_member_id","FAMILY_MEMBERS"),
            Map.entry("responsible_member_id","FAMILY_MEMBERS"),
            Map.entry("member_id","FAMILY_MEMBERS"),
            Map.entry("original_assigned_member_id","FAMILY_MEMBERS"));
    private final JdbcTemplate db;
    private final WorkspaceEpochTransactionGuard epochs;
    private final WorkspaceRestorePermitService permits;
    private final ProtectiveBackupPortableFile portable;
    private final ProtectiveBackupArtifactCodec codec;
    private final ObjectMapper json=new ObjectMapper();

    public WorkspaceRestoreNativeTargetEngine(JdbcTemplate db,
            WorkspaceEpochTransactionGuard epochs, WorkspaceRestorePermitService permits,
            ProtectiveBackupPortableFile portable,ProtectiveBackupArtifactCodec codec) {
        this.db=db;this.epochs=epochs;this.permits=permits;
        this.portable=portable;this.codec=codec;
    }

    public record Receipt(UUID operationId,long targetWorkspaceId,long beforeEpoch,
            long afterEpoch,Map<String,Integer> insertedCounts,String receiptDigest) {
        public Receipt {insertedCounts=Map.copyOf(insertedCounts);}
    }

    @Transactional
    public Receipt restoreBounded(WorkspaceRestorePermitScopeV2.RestoreScope scope,
            byte[] encryptedArchive,SecretKey key,Map<Long,Long> reviewedMemberMap) {
        Objects.requireNonNull(scope,"scope");
        if(encryptedArchive==null||key==null||reviewedMemberMap==null)
            throw new SecurityException("RESTORE_ARCHIVE_OR_MAPPING_REQUIRED");
        // The family lock is FIRST, before permit and native domain writes.
        long before=epochs.requireCurrentEpoch(
                scope.targetWorkspaceId(),scope.targetExpectedEpoch());
        var artifact=portable.decode(encryptedArchive);
        if(artifact.manifest().workspaceId()!=scope.source().sourceWorkspaceId()
                ||!artifact.manifest().snapshotId().equals(scope.source().snapshotId()))
            throw new SecurityException("RESTORE_ARCHIVE_WRONG_SOURCE");
        byte[] plaintext=codec.open(artifact,scope.source().snapshotId(),
                scope.source().sourceWorkspaceId(),key);
        final ObjectNode categories;
        try {
            JsonNode root=json.readTree(plaintext);
            if(!(root instanceof ObjectNode obj))
                throw new SecurityException("RESTORE_ARCHIVE_ROOT_INVALID");
            var proof=PortableMemberAncestryV2.verify(obj,
                    scope.source().sourceWorkspaceId(),scope.source().snapshotId(),
                    scope.source().sourceSnapshotEpoch());
            if(!Objects.equals(proof.manifestRootDigest(),scope.source().manifestRootDigest())
                    || !Objects.equals(artifact.manifest().contentSha256(),
                               scope.source().snapshotDigest())
                    || !Objects.equals(proof.sourceDigest(),
                               artifact.manifest().sourceDigest()))
                throw new SecurityException("RESTORE_ARCHIVE_PROOF_CHANGED");
            JsonNode parsed=obj.path("categories");
            if(!(parsed instanceof ObjectNode c)||c.size()!=8)
                throw new SecurityException("RESTORE_FULL_EIGHT_CATEGORY_REQUIRED");
            categories=c;
        } catch(java.io.IOException ex) {
            throw new SecurityException("RESTORE_ARCHIVE_DECODE_FAILED",ex);
        } finally {
            Arrays.fill(plaintext,(byte)0);
        }
        // Both signed source and current target membership are revalidated
        // in the permit consumer. The one-use grant is consumed inside this
        // same SQL transaction, so later FK/financial failure rolls it back.
        permits.consumeInRestoreTransaction(scope,encryptedArchive,key,reviewedMemberMap);

        requireTargetEmpty(scope.targetWorkspaceId(),reviewedMemberMap.values());
        requireEconomicSourceConsistency(categories);

        var maps=new HashMap<String,Map<Long,Long>>();
        maps.put("FAMILY_MEMBERS",Map.copyOf(reviewedMemberMap));
        var counts=new LinkedHashMap<String,Integer>();
        for(String category:ORDER) {
            var allocated=new LinkedHashMap<Long,Long>();
            JsonNode array=categories.path(category);
            for(JsonNode sourceRow:array) {
                if(!sourceRow.isObject()||!sourceRow.path("id").isIntegralNumber())
                    throw new SecurityException("RESTORE_SOURCE_ROW_INVALID");
                long original=sourceRow.path("id").asLong();
                if(original<=0||allocated.containsKey(original))
                    throw new SecurityException("RESTORE_SOURCE_ID_DUPLICATE");
                ObjectNode adjusted=((ObjectNode)sourceRow).deepCopy();
                adjusted.remove("id");
                if(adjusted.has("family_id"))
                    adjusted.put("family_id",scope.targetWorkspaceId());
                for(var fk:FOREIGN.entrySet()) {
                    String field=fk.getKey();
                    if(!adjusted.hasNonNull(field))continue;
                    // 'source_id' is polymorphic, handled separately below.
                    if(!fkApplies(category,field))continue;
                    var parents=maps.get(fk.getValue());
                    long old=positive(sourceRow,field);
                    Long mapped=parents==null?null:parents.get(old);
                    if(mapped==null)
                        throw new SecurityException("RESTORE_UNMAPPED_PARENT:"+category+"."+field);
                    adjusted.put(field,mapped);
                }
                if("POINT_LEDGER".equals(category)) {
                    String type=sourceRow.path("source_type").asText();
                    if("TASK".equals(type)||"REWARD_PURCHASE".equals(type)) {
                        Map<Long,Long> parents=maps.get("TASK".equals(type)
                                ?"TASK_EXECUTIONS":"REWARD_PURCHASES");
                        long sourceId=positive(sourceRow,"source_id");
                        if(parents==null||!parents.containsKey(sourceId))
                            throw new SecurityException("RESTORE_LEDGER_PARENT_UNMAPPED");
                        adjusted.put("source_id",parents.get(sourceId));
                    } else if(!"MANUAL".equals(type))
                        throw new SecurityException("RESTORE_LEDGER_TYPE_UNSUPPORTED");
                }
                long newId=insertNative(category,adjusted);
                if(newId<=0||allocated.put(original,newId)!=null)
                    throw new SecurityException("RESTORE_NATIVE_ID_ALLOCATION_INVALID");
            }
            maps.put(category,Map.copyOf(allocated));
            counts.put(category,allocated.size());
        }
        requireFinancialParity(categories,scope.targetWorkspaceId(),
                reviewedMemberMap,maps);
        int updated=db.update("""
            UPDATE families SET data_epoch=data_epoch+1
            WHERE id=? AND data_epoch=? AND data_epoch<9223372036854775807
            """,scope.targetWorkspaceId(),before);
        if(updated!=1)
            throw new SecurityException("RESTORE_TARGET_EPOCH_COMMIT_STALE");
        String categoryJson;
        try {categoryJson=json.writeValueAsString(counts);}
        catch(com.fasterxml.jackson.core.JsonProcessingException impossible) {
            throw new IllegalStateException(impossible);
        }
        String scopeDigest=WorkspaceRestorePermitScopeV2.canonicalScopeDigest(scope);
        String receiptDigest=ProtectiveBackupArtifactCodec.sha((
                "family-points:restore:receipt:v2|"+scopeDigest+"|"
                +scope.source().manifestRootDigest()+"|"+before+"|"+(before+1)
                +"|"+categoryJson).getBytes(StandardCharsets.UTF_8));
        int recorded=db.update("""
            INSERT INTO workspace_restore_receipts
              (operation_id,target_workspace_id,scope_digest,manifest_root_digest,
               target_epoch_before,target_epoch_after,category_counts,receipt_digest)
            VALUES (?,?,?,?,?,?,?::jsonb,?)
            ON CONFLICT (operation_id) DO NOTHING
            """,scope.operationId(),scope.targetWorkspaceId(),scopeDigest,
                scope.source().manifestRootDigest(),before,before+1,
                categoryJson,receiptDigest);
        if(recorded!=1)
            throw new SecurityException("RESTORE_OPERATION_ALREADY_RECEIPTED");
        return new Receipt(scope.operationId(),scope.targetWorkspaceId(),
                before,before+1,counts,receiptDigest);
    }

    private static boolean fkApplies(String category,String column) {
        return switch(category) {
            case "TASK_DEFINITIONS" -> Set.of("reward_point_type_id","penalty_point_type_id",
                    "assigned_member_id","created_by_member_id","preferred_member_id",
                    "responsible_member_id").contains(column);
            case "REWARD_DEFINITIONS" -> "point_type_id".equals(column);
            case "TASK_EXECUTIONS" -> Set.of("task_definition_id","reward_point_type_id",
                    "penalty_point_type_id","member_id","original_assigned_member_id")
                    .contains(column);
            case "REWARD_PURCHASES" -> Set.of("reward_definition_id","point_type_id",
                    "member_id").contains(column);
            case "POINT_LEDGER" -> Set.of("point_type_id","member_id").contains(column);
            default -> false;
        };
    }

    private void requireTargetEmpty(long workspace,Collection<Long> members) {
        // The target may hold independent member/group/media/reputation
        // histories even if the six directly replayed tables are empty.
        // Do not claim that preserving those independent graphs is safe.
        PortableV2SourceClosurePolicy.requireBoundedSource(db,workspace);
        for(String category:List.of("POINT_TYPES","TASK_DEFINITIONS","REWARD_DEFINITIONS")) {
            Long rows=db.queryForObject("SELECT count(*) FROM public."+TABLES.get(category)
                    +" WHERE family_id=?",Long.class,workspace);
            if(rows==null||rows!=0)
                throw new SecurityException("RESTORE_TARGET_CATEGORY_NOT_EMPTY:"+category);
        }
        for(String table:List.of("task_instances","reward_purchases","point_transactions")) {
            Long rows=db.queryForObject(
                    "SELECT count(*) FROM public."+table+
                    " t JOIN family_members m ON m.id=t.member_id WHERE m.family_id=?",
                    Long.class,workspace);
            if(rows==null||rows!=0)
                throw new SecurityException("RESTORE_TARGET_HISTORY_NOT_EMPTY:"+table);
        }
        if(members.isEmpty())
            throw new SecurityException("RESTORE_TARGET_MEMBER_MAPPING_EMPTY");
    }

    private long insertNative(String category,ObjectNode adjusted) {
        String table=TABLES.get(category);
        if(table==null)throw new SecurityException("RESTORE_CATEGORY_UNSUPPORTED");
        // DB-owned SQL identifiers are taken only from the actual migrated
        // whitelist table, never from archive JSON. Complete column parity
        // prevents silently dropping a source field during native replay.
        var columns=db.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema='public' AND table_name=?
                ORDER BY ordinal_position
                """,String.class,table);
        if(columns.isEmpty()||!columns.contains("id"))
            throw new SecurityException("RESTORE_TARGET_SCHEMA_UNKNOWN");
        var sourceColumns=new HashSet<String>();
        adjusted.fieldNames().forEachRemaining(sourceColumns::add);
        var expected=new HashSet<String>(columns);
        expected.remove("id");
        if(!expected.equals(sourceColumns)
                ||expected.stream().anyMatch(n->!n.matches("[a-z][a-z0-9_]*")))
            throw new SecurityException("RESTORE_SOURCE_TARGET_COLUMN_DRIFT:"+category);
        var ordered=columns.stream().filter(n->!"id".equals(n)).toList();
        String names=String.join(",",ordered);
        String values=String.join(",",ordered.stream().map(n->"p."+n).toList());
        String sql="INSERT INTO public."+table+" ("+names+") SELECT "+values
                +" FROM jsonb_populate_record(NULL::public."+table
                +", ?::jsonb) p RETURNING id";
        try {
            Long id=db.queryForObject(sql,Long.class,json.writeValueAsString(adjusted));
            if(id==null)throw new SecurityException("RESTORE_TARGET_ID_MISSING");
            return id;
        } catch(com.fasterxml.jackson.core.JsonProcessingException invalid) {
            throw new SecurityException("RESTORE_NATIVE_ROW_ENCODE_FAILED",invalid);
        }
    }

    private static long positive(JsonNode row,String field) {
        JsonNode n=row.path(field);
        if(!n.isIntegralNumber()||n.asLong()<=0)
            throw new SecurityException("RESTORE_INVALID_SOURCE_REFERENCE:"+field);
        return n.asLong();
    }

    private static void requireEconomicSourceConsistency(ObjectNode cats) {
        var executions=new HashMap<Long,JsonNode>();
        var purchases=new HashMap<Long,JsonNode>();
        for(JsonNode r:cats.path("TASK_EXECUTIONS"))
            executions.put(positive(r,"id"),r);
        for(JsonNode r:cats.path("REWARD_PURCHASES"))
            purchases.put(positive(r,"id"),r);
        var eventKeys=new HashSet<String>();
        for(JsonNode r:cats.path("POINT_LEDGER")) {
            String source=r.path("source_type").asText();
            String type=r.path("type").asText();
            long member=positive(r,"member_id"),point=positive(r,"point_type_id");
            if(!r.path("amount").isIntegralNumber())
                throw new SecurityException("RESTORE_FINANCIAL_AMOUNT_INVALID");
            long amount=r.path("amount").asLong();
            if("TASK".equals(source)) {
                long parent=positive(r,"source_id");
                JsonNode task=executions.get(parent);
                if(task==null||!"EARN".equals(type)||amount<=0
                        ||!"COMPLETED".equals(task.path("status").asText())
                        ||!task.path("reward_processed").asBoolean(false)
                        ||task.path("penalty_processed").asBoolean(false)
                        ||task.path("member_id").asLong()!=member
                        ||task.path("reward_point_type_id").asLong()!=point
                        ||task.path("reward_amount").asLong()!=amount)
                    throw new SecurityException("RESTORE_TASK_FINANCE_PROVENANCE_INVALID");
                if(!eventKeys.add("TASK:"+parent))
                    throw new SecurityException("RESTORE_FINANCE_EVENT_DUPLICATE");
            } else if("REWARD_PURCHASE".equals(source)) {
                long parent=positive(r,"source_id");
                JsonNode purchase=purchases.get(parent);
                if(purchase==null||!"SPEND".equals(type)||amount>=0
                        ||!Set.of("READY","USED").contains(purchase.path("status").asText())
                        ||purchase.path("member_id").asLong()!=member
                        ||purchase.path("point_type_id").asLong()!=point
                        ||purchase.path("price_amount").asLong()!=-amount)
                    throw new SecurityException("RESTORE_PURCHASE_FINANCE_PROVENANCE_INVALID");
                if(!eventKeys.add("REWARD_PURCHASE:"+parent))
                    throw new SecurityException("RESTORE_FINANCE_EVENT_DUPLICATE");
            } else if(!"MANUAL".equals(source)
                    ||r.hasNonNull("source_id")||!"ADJUSTMENT".equals(type))
                throw new SecurityException("RESTORE_UNSUPPORTED_LEDGER_PROVENANCE");
        }
        // Every reward-bearing completed task and every paid purchase must
        // have exactly one historical ledger event. Missing events cannot
        // silently mutate balances during a supposedly complete replay.
        for(var entry:executions.entrySet()) {
            JsonNode task=entry.getValue();
            if(task.path("penalty_processed").asBoolean(false))
                throw new SecurityException("RESTORE_UNSUPPORTED_TASK_PENALTY_HISTORY");
            if(task.path("reward_processed").asBoolean(false)
                    &&task.path("reward_amount").asLong()>0
                    &&task.hasNonNull("reward_point_type_id")
                    &&!eventKeys.contains("TASK:"+entry.getKey()))
                throw new SecurityException("RESTORE_MISSING_TASK_LEDGER");
        }
        for(var entry:purchases.entrySet())
            if(entry.getValue().path("price_amount").asLong()>0
                    &&!eventKeys.contains("REWARD_PURCHASE:"+entry.getKey()))
                throw new SecurityException("RESTORE_MISSING_PURCHASE_LEDGER");
    }

    private void requireFinancialParity(ObjectNode cats,long workspace,
            Map<Long,Long> members,Map<String,Map<Long,Long>> remap) {
        var expected=new HashMap<String,Long>();
        for(JsonNode row:cats.path("POINT_LEDGER")) {
            long member=positive(row,"member_id"),point=positive(row,"point_type_id");
            String key=member+":"+point;
            expected.merge(key,row.path("amount").asLong(),Long::sum);
        }
        var actual=new HashMap<String,Long>();
        var rows=db.queryForList("""
            SELECT t.member_id,t.point_type_id,sum(t.amount) AS total
            FROM point_transactions t
            JOIN family_members m ON m.id=t.member_id
            WHERE m.family_id=?
            GROUP BY t.member_id,t.point_type_id
            """,workspace);
        Map<Long,Long> reverseMembers=new HashMap<>(),reversePoints=new HashMap<>();
        members.forEach((source,target)->reverseMembers.put(target,source));
        remap.get("POINT_TYPES").forEach((source,target)->reversePoints.put(target,source));
        for(var row:rows) {
            Long originalMember=reverseMembers.get(((Number)row.get("member_id")).longValue());
            Long originalPoint=reversePoints.get(((Number)row.get("point_type_id")).longValue());
            if(originalMember==null||originalPoint==null)
                throw new SecurityException("RESTORE_FINANCE_UNMAPPED_TARGET_BALANCE");
            actual.put(originalMember+":"+originalPoint,
                    ((Number)row.get("total")).longValue());
        }
        if(!expected.equals(actual)
                ||cats.path("POINT_LEDGER").size()!=db.queryForObject("""
                    SELECT count(*) FROM point_transactions t
                    JOIN family_members m ON m.id=t.member_id WHERE m.family_id=?
                    """,Long.class,workspace))
            throw new SecurityException("RESTORE_MEMBER_CURRENCY_PARITY_FAILURE");
    }
}
