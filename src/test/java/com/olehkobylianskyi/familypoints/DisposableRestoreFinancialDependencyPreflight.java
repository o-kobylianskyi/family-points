package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

/**
 * Backend B / B-B-03 TEST ONLY: deterministic, read-only restore consumer
 * preflight for source task/reward/point ancestry and exact target mappings.
 * The caller must first authenticate the archive (Backend A contract) and
 * owner/member authorization (Backend C). Never used as permission to restore.
 */
final class DisposableRestoreFinancialDependencyPreflight {
    record Mapping(Map<Long,Long> members,Map<Long,Long> currencies,
                   Map<Long,Long> tasks,Map<Long,Long> rewards,
                   Map<Long,Long> purchases) {}
    record Result(int taskExecutions,int purchases,int ledgerRows,
                  Map<String,Long> expectedTargetBalances) {}

    Result verify(JsonNode source,long sourceFamily,Mapping map) {
        if(source==null || !source.isObject() || sourceFamily<=0 || map==null)
            throw new IllegalArgumentException("RESTORE_FINANCE_INPUT_INVALID");
        for(String category:List.of("TASK_DEFINITIONS","TASK_EXECUTIONS",
                "REWARD_DEFINITIONS","REWARD_PURCHASES","POINT_TYPES","POINT_LEDGER")) {
            if(!source.path(category).isArray())
                throw new SecurityException("RESTORE_FINANCE_CATEGORY_MISSING:"+category);
        }
        exact(map.members(),"MEMBER");exact(map.currencies(),"CURRENCY");
        exact(map.tasks(),"TASK");exact(map.rewards(),"REWARD");
        exact(map.purchases(),"PURCHASE");
        var sourceTask=ids(source.path("TASK_DEFINITIONS"),sourceFamily,true,"TASK");
        var sourceReward=ids(source.path("REWARD_DEFINITIONS"),sourceFamily,true,"REWARD");
        var sourceTypes=ids(source.path("POINT_TYPES"),sourceFamily,true,"CURRENCY");
        var sourcePurchases=ids(source.path("REWARD_PURCHASES"),sourceFamily,false,"PURCHASE");
        var executions=ids(source.path("TASK_EXECUTIONS"),sourceFamily,false,"EXECUTION");
        if(!map.tasks().keySet().equals(sourceTask)
                || !map.rewards().keySet().equals(sourceReward)
                || !map.currencies().keySet().equals(sourceTypes)
                || !map.purchases().keySet().equals(sourcePurchases))
            throw new SecurityException("RESTORE_FINANCE_PARENT_MAP_INCOMPLETE");
        Set<Long> originalMembers=new HashSet<>();
        for(JsonNode row:source.path("TASK_EXECUTIONS")) {
            long member=id(row,"member_id");
            originalMembers.add(member);
            if(!sourceTask.contains(id(row,"task_definition_id")))
                throw new SecurityException("RESTORE_TASK_PARENT_MISSING");
        }
        var purchaseEconomy=new HashMap<Long,JsonNode>();
        for(JsonNode row:source.path("REWARD_PURCHASES")) {
            long purchaseId=id(row,"id");
            originalMembers.add(id(row,"member_id"));
            if(row.hasNonNull("reward_definition_id")
                    && !sourceReward.contains(id(row,"reward_definition_id")))
                throw new SecurityException("RESTORE_PURCHASE_REWARD_PARENT_MISSING");
            if(!sourceTypes.contains(id(row,"point_type_id"))
                    || !row.path("price_amount").canConvertToInt()
                    || row.path("price_amount").asInt()<=0)
                throw new SecurityException("RESTORE_PURCHASE_CURRENCY_INVALID");
            purchaseEconomy.put(purchaseId,row);
        }
        var balances=new TreeMap<String,Long>();
        var charged=new HashSet<Long>();
        var taskEvents=new HashSet<String>();
        var seenLedger=new HashSet<Long>();
        for(JsonNode row:source.path("POINT_LEDGER")) {
            long rowId=id(row,"id");
            if(!seenLedger.add(rowId))
                throw new SecurityException("RESTORE_LEDGER_DUPLICATE_ID");
            long member=id(row,"member_id"),currency=id(row,"point_type_id");
            originalMembers.add(member);
            if(!sourceTypes.contains(currency))
                throw new SecurityException("RESTORE_LEDGER_CURRENCY_MISSING");
            if(!row.path("amount").canConvertToInt() || row.path("amount").asInt()==0)
                throw new SecurityException("RESTORE_LEDGER_AMOUNT_INVALID");
            String kind=row.path("type").asText();
            int amount=row.path("amount").asInt();
            String provenance=row.path("source_type").asText();
            if("TASK".equals(provenance)) {
                long oldTaskExecution=id(row,"source_id");
                if(!executions.contains(oldTaskExecution))
                    throw new SecurityException("RESTORE_TASK_SOURCE_MISSING");
                JsonNode execution=byId(source.path("TASK_EXECUTIONS"),oldTaskExecution);
                if(id(execution,"member_id")!=member
                        || !Set.of("EARN","PENALTY").contains(kind)
                        || ("EARN".equals(kind)!=(amount>0))
                        || !taskEvents.add(oldTaskExecution+":"+kind))
                    throw new SecurityException("RESTORE_TASK_SOURCE_MISMATCH");
            } else if("REWARD_PURCHASE".equals(provenance)) {
                long originalPurchase=id(row,"source_id");
                JsonNode purchase=purchaseEconomy.get(originalPurchase);
                if(purchase==null || !charged.add(originalPurchase)
                        || id(purchase,"member_id")!=member
                        || id(purchase,"point_type_id")!=currency
                        || purchase.path("price_amount").asInt()!=-amount
                        || !"SPEND".equals(kind))
                    throw new SecurityException("RESTORE_PURCHASE_SOURCE_MISMATCH");
            } else if("MANUAL".equals(provenance)) {
                if(row.hasNonNull("source_id"))
                    throw new SecurityException("RESTORE_MANUAL_SOURCE_UNSUPPORTED");
            } else {
                // Finance D owns broader REFUND, EXCHANGE and reputation/group
                // source semantics. Do not reinterpret them as MANUAL.
                throw new SecurityException("RESTORE_FINANCE_UNSUPPORTED_PROVENANCE:"+provenance);
            }
            Long targetMember=map.members().get(member);
            Long targetType=map.currencies().get(currency);
            if(targetMember==null || targetType==null)
                throw new SecurityException("RESTORE_LEDGER_TARGET_MAP_MISSING");
            balances.merge(targetMember+":"+targetType,(long)amount,Math::addExact);
        }
        if(!map.members().keySet().containsAll(originalMembers))
            throw new SecurityException("RESTORE_MEMBER_MAP_INCOMPLETE");
        return new Result(executions.size(),sourcePurchases.size(),
                seenLedger.size(),Map.copyOf(balances));
    }

    private static JsonNode byId(JsonNode list,long id) {
        for(JsonNode row:list)
            if(row.path("id").asLong()==id)return row;
        throw new SecurityException("RESTORE_PARENT_NOT_FOUND");
    }
    private static Set<Long> ids(JsonNode list,long family,boolean familyScoped,String category) {
        var result=new HashSet<Long>();
        for(JsonNode row:list) {
            long id=id(row,"id");
            if(familyScoped && id(row,"family_id")!=family
                    || !result.add(id))
                throw new SecurityException("RESTORE_SOURCE_ID_INVALID:"+category);
        }
        return result;
    }
    private static long id(JsonNode row,String field) {
        if(!row.path(field).isIntegralNumber() || row.path(field).asLong()<=0)
            throw new SecurityException("RESTORE_SOURCE_REFERENCE_INVALID:"+field);
        return row.path(field).asLong();
    }
    private static void exact(Map<Long,Long> mapping,String category) {
        if(mapping==null || mapping.keySet().stream().anyMatch(
                id->id==null || id<=0)
                || mapping.values().stream().anyMatch(id->id==null || id<=0)
                || new HashSet<>(mapping.values()).size()!=mapping.size())
            throw new SecurityException("RESTORE_MAP_INVALID:"+category);
    }
}
