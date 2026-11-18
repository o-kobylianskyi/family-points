package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olehkobylianskyi.familypoints.service.*;

import javax.crypto.SecretKey;
import java.util.*;

/**
 * TEST ONLY, read-only financial source graph preflight. Does not replay money,
 * grant points, infer an unsupported source_type or modify a database.
 */
final class DisposableLedgerSourceGraphVerifier {
    record Result(int manualRows,int taskRows,int purchaseRows,
                  Map<Long,Long> ledgerToTaskSource,
                  Map<Long,Long> ledgerToPurchaseSource,UUID snapshotId,
                  Map<String,Long> signedMemberCurrencyBalances,int ledgerRows) {}
    private final ProtectiveBackupImportInspector inspector;
    private final ProtectiveBackupPortableFile file;
    private final ProtectiveBackupArtifactCodec codec;
    private final ObjectMapper mapper=new ObjectMapper();

    DisposableLedgerSourceGraphVerifier(ProtectiveBackupImportInspector inspector,
            ProtectiveBackupPortableFile file,ProtectiveBackupArtifactCodec codec) {
        this.inspector=inspector;this.file=file;this.codec=codec;
    }

    Result inspect(byte[] archive,long source,SecretKey key) {
        if(archive==null || source<=0 || key==null)
            throw new IllegalArgumentException("LEDGER_SOURCE_GRAPH_INVALID_REQUEST");
        var inspected=inspector.inspect(archive,source,key);
        if(!"V50".equals(inspected.schemaVersion())
                || !inspected.availableCategories().contains("POINT_LEDGER")
                || !inspected.availableCategories().contains("POINT_TYPES"))
            throw new SecurityException("LEDGER_SOURCE_GRAPH_SCOPE_INCOMPLETE");
        byte[] raw=codec.open(file.decode(archive),inspected.snapshotId(),source,key);
        final JsonNode root;
        try {root=mapper.readTree(raw).path("categories");}
        catch(java.io.IOException error) {
            throw new IllegalArgumentException("LEDGER_SOURCE_GRAPH_BAD_ARCHIVE",error);
        }finally {Arrays.fill(raw,(byte)0);}
        // An authenticated manifest cannot be used to justify a missing or
        // malformed category payload. Never interpret missing arrays as empty
        // source history during a restore-safety preflight.
        if(!root.isObject()
                || !root.path("POINT_LEDGER").isArray()
                || !root.path("POINT_TYPES").isArray())
            throw new SecurityException("LEDGER_SOURCE_GRAPH_MISSING_PAYLOAD");
        for(String optional:List.of("TASK_EXECUTIONS","TASK_DEFINITIONS","REWARD_PURCHASES")) {
            if(root.has(optional) && !root.path(optional).isArray())
                throw new SecurityException("LEDGER_SOURCE_GRAPH_INVALID_PAYLOAD:"+optional);
        }
        var definitions=new HashSet<Long>();
        for(JsonNode def:root.path("TASK_DEFINITIONS")) {
            long id=def.path("id").asLong();
            if(!def.path("id").isIntegralNumber() || id<=0
                    || !def.path("family_id").isIntegralNumber()
                    || def.path("family_id").asLong()!=source
                    || !definitions.add(id))
                throw new SecurityException("LEDGER_SOURCE_DEFINITION_INVALID");
        }
        var taskRows=new HashMap<Long,JsonNode>();
        for(JsonNode task:root.path("TASK_EXECUTIONS")) {
            if(!task.path("id").isIntegralNumber() || task.path("id").asLong()<=0
                    || !task.path("member_id").isIntegralNumber()
                    || task.path("member_id").asLong()<=0
                    || !task.path("task_definition_id").isIntegralNumber()
                    || !definitions.contains(task.path("task_definition_id").asLong())
                    || taskRows.putIfAbsent(task.path("id").asLong(),task)!=null)
                throw new SecurityException("LEDGER_SOURCE_TASK_INVALID");
        }
        // Validate EVERY archived TASK parent, including uncharged instances.
        // An incomplete non-null historic economics pair is not portable.
        // Reduced legacy fixtures with neither field do not establish full
        // economic closure and remain a separate explicit QA blocker.
        for(JsonNode historicalTask:taskRows.values()) {
            for(String kind:List.of("reward","penalty")) {
                String amount=kind+"_amount";
                String currency=kind+"_point_type_id";
                boolean hasAmount=historicalTask.hasNonNull(amount);
                boolean hasCurrency=historicalTask.hasNonNull(currency);
                if(hasAmount!=hasCurrency
                        || (hasAmount && (!historicalTask.path(amount).canConvertToInt()
                            || historicalTask.path(amount).asInt()<=0
                            || !historicalTask.path(currency).isIntegralNumber()
                            || historicalTask.path(currency).asLong()<=0)))
                    throw new SecurityException("LEDGER_TASK_ARCHIVED_ECONOMICS_PAIR_INVALID");
            }
        }
        var purchaseRows=new HashMap<Long,JsonNode>();
        for(JsonNode original:root.path("REWARD_PURCHASES")) {
            long id=original.path("id").asLong();
            if(!original.path("id").isIntegralNumber() || id<=0
                    || !original.path("member_id").isIntegralNumber()
                    || !original.path("point_type_id").isIntegralNumber()
                    || !original.path("price_amount").isIntegralNumber()
                    || purchaseRows.putIfAbsent(id,original)!=null)
                throw new SecurityException("LEDGER_SOURCE_PURCHASE_INVALID");
        }
        var typeIds=new HashSet<Long>();
        var typeCodes=new HashSet<String>();
        for(JsonNode pointType:root.path("POINT_TYPES")) {
            if(!pointType.path("family_id").isIntegralNumber()
                    || pointType.path("family_id").asLong()!=source
                    || !pointType.path("id").isIntegralNumber()
                    || !typeIds.add(pointType.path("id").asLong())
                    || pointType.path("id").asLong()<=0
                    || !pointType.path("code").isTextual()
                    || pointType.path("code").asText().isBlank()
                    || !typeCodes.add(pointType.path("code").asText()))
                throw new SecurityException("LEDGER_SOURCE_CURRENCY_INVALID");
        }
        // Historical task economy denomination must belong to the same
        // authenticated source family even when no ledger row references
        // the task. This closes QA-038's unused parent currency gap.
        for(JsonNode taskInstance:taskRows.values()) {
            for(String currencyField:List.of("reward_point_type_id","penalty_point_type_id")) {
                if(taskInstance.hasNonNull(currencyField)
                        && !typeIds.contains(taskInstance.path(currencyField).asLong()))
                    throw new SecurityException("LEDGER_TASK_ARCHIVED_CURRENCY_NOT_FOUND");
            }
        }
        // A purchase's entire economic parent must be anchored in the
        // authenticated point-type catalog even when no matching debit exists.
        // Orphan/dangling archive rows cannot become replay candidates.
        for(JsonNode archivedPurchase:purchaseRows.values()) {
            if(archivedPurchase.path("member_id").asLong()<=0
                    || !typeIds.contains(archivedPurchase.path("point_type_id").asLong())
                    || archivedPurchase.path("price_amount").asLong()<=0
                    || archivedPurchase.path("price_amount").asLong()>Integer.MAX_VALUE)
                throw new SecurityException("LEDGER_PURCHASE_ARCHIVED_PARENT_INVALID");
        }
        int manual=0,task=0,purchaseCount=0;
        var relationships=new TreeMap<Long,Long>();
        var purchaseRelationships=new TreeMap<Long,Long>();
        var purchaseCharged=new HashSet<Long>();
        var taskEvents=new HashSet<String>();
        var ledgerIds=new HashSet<Long>();
        var signedBalances=new TreeMap<String,Long>();
        for(JsonNode row:root.path("POINT_LEDGER")) {
            long id=row.path("id").asLong();
            long member=row.path("member_id").asLong();
            long currency=row.path("point_type_id").asLong();
            if(!row.path("id").isIntegralNumber()
                    || !row.path("member_id").isIntegralNumber()
                    || !row.path("point_type_id").isIntegralNumber()
                    || id<=0 || !ledgerIds.add(id) || member<=0 || !typeIds.contains(currency)
                    || !row.path("amount").isIntegralNumber()
                    || row.path("amount").asLong()>Integer.MAX_VALUE
                    || row.path("amount").asLong()<Integer.MIN_VALUE)
                throw new SecurityException("LEDGER_SOURCE_ROW_INVALID");
            // Independent category-wide signed conservation inventory. This is
            // not sufficient to prove ancestry, so category checks follow.
            signedBalances.merge(member+":"+currency,row.path("amount").asLong(),Math::addExact);
            String sourceType=row.path("source_type").asText();
            if("MANUAL".equals(sourceType)) {
                if(row.hasNonNull("source_id"))
                    throw new SecurityException("LEDGER_MANUAL_SOURCE_ID_FORBIDDEN");
                manual++;
            } else if("TASK".equals(sourceType)) {
                if(!row.path("source_id").isIntegralNumber()
                        || row.path("source_id").asLong()<=0)
                    throw new SecurityException("LEDGER_TASK_SOURCE_ID_INVALID");
                long taskId=row.path("source_id").asLong();
                var taskInstance=taskRows.get(taskId);
                if(taskInstance==null)
                    throw new SecurityException("LEDGER_TASK_SOURCE_NOT_ARCHIVED");
                if(taskInstance.path("member_id").asLong()!=member)
                    throw new SecurityException("LEDGER_TASK_SOURCE_MEMBER_MISMATCH");
                // Production TaskService issues TASK/EARN for approved positive
                // reward and TASK/PENALTY for a mandatory missed task. A
                // numeric balance alone must never authenticate a TASK row.
                long signed=row.path("amount").asLong();
                String transactionType=row.path("type").asText();
                if(signed==0 ||
                        !(signed>0 && "EARN".equals(transactionType)
                          || signed<0 && "PENALTY".equals(transactionType)))
                    throw new SecurityException("LEDGER_TASK_TRANSACTION_SEMANTICS");
                // TaskInstance contains the persisted economic snapshot.
                // When these fields are present in the signed historical
                // source record, bind the ledger to its exact denomination
                // and value rather than trusting a matching source_id alone.
                // Legacy reduced fixtures without these columns cannot claim
                // complete task-economics provenance.
                String amountField="EARN".equals(transactionType)
                        ?"reward_amount":"penalty_amount";
                String currencyField="EARN".equals(transactionType)
                        ?"reward_point_type_id":"penalty_point_type_id";
                if(taskInstance.has(amountField) || taskInstance.has(currencyField)) {
                    if(!taskInstance.path(amountField).canConvertToInt()
                            || taskInstance.path(amountField).asInt()<=0
                            || !taskInstance.path(currencyField).isIntegralNumber()
                            || taskInstance.path(currencyField).asLong()!=currency
                            || Math.abs(signed)!=taskInstance.path(amountField).asInt())
                        throw new SecurityException("LEDGER_TASK_SNAPSHOT_ECONOMICS_MISMATCH");
                }
                if(!taskEvents.add(taskId+":"+transactionType))
                    throw new SecurityException("LEDGER_TASK_DUPLICATE_EVENT");
                relationships.put(id,taskId);
                task++;
            } else if("REWARD_PURCHASE".equals(sourceType)) {
                if(!row.path("source_id").isIntegralNumber()
                        || row.path("source_id").asLong()<=0)
                    throw new SecurityException("LEDGER_PURCHASE_SOURCE_ID_INVALID");
                long purchaseId=row.path("source_id").asLong();
                JsonNode original=purchaseRows.get(purchaseId);
                if(original==null)
                    throw new SecurityException("LEDGER_PURCHASE_SOURCE_NOT_ARCHIVED");
                long price=original.path("price_amount").asLong();
                if(original.path("member_id").asLong()!=member
                        || original.path("point_type_id").asLong()!=currency
                        || price<=0 || price>Integer.MAX_VALUE
                        || row.path("amount").asLong()!=-price
                        || !"SPEND".equals(row.path("type").asText()))
                    throw new SecurityException("LEDGER_PURCHASE_ECONOMICS_MISMATCH");
                if(!purchaseCharged.add(purchaseId))
                    throw new SecurityException("LEDGER_DUPLICATE_PURCHASE_CHARGE");
                purchaseRelationships.put(id,purchaseId);
                purchaseCount++;
            } else {
                // No source_type guessing. REWARD_PURCHASE, EXCHANGE, REFUND,
                // ADJUSTMENT and unknown legacy types need dedicated proof.
                throw new SecurityException("LEDGER_SOURCE_TYPE_UNSUPPORTED:"+sourceType);
            }
        }
        return new Result(manual,task,purchaseCount,
                Map.copyOf(relationships),Map.copyOf(purchaseRelationships),
                inspected.snapshotId(),Map.copyOf(signedBalances),ledgerIds.size());
    }
}
