package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

/**
 * Backend B / B-B-08: TEST ONLY; verifies an independently supplied original
 * member/source-parent graph before target writes. This does NOT authenticate
 * the archive, source manifest, original owner, or an issuance by Backend C.
 * Only the bounded TASK / PURCHASE / POINT ledger consumer subset is supported.
 */
final class DisposableRestoreRowClosureVerifier {
    record Result(int originalMembers,int taskExecutions,int purchases,int ledgerRows,
                  Map<String,Long> mappedBalances) {}
    private final DisposableRestoreFinancialDependencyPreflight finance=
            new DisposableRestoreFinancialDependencyPreflight();

    Result verify(JsonNode source,long sourceFamily,
            DisposableRestoreFinancialDependencyPreflight.Mapping mapping) {
        if(source==null||!source.isObject()||sourceFamily<=0||mapping==null)
            throw new IllegalArgumentException("RESTORE_ROW_INPUT_INVALID");
        JsonNode members=required(source,"FAMILY_MEMBERS");
        JsonNode tasks=required(source,"TASK_DEFINITIONS");
        JsonNode executions=required(source,"TASK_EXECUTIONS");
        JsonNode rewards=required(source,"REWARD_DEFINITIONS");
        JsonNode purchases=required(source,"REWARD_PURCHASES");
        JsonNode currencies=required(source,"POINT_TYPES");
        JsonNode ledger=required(source,"POINT_LEDGER");
        var original=new HashSet<Long>();
        for(JsonNode member:members) {
            long id=positive(member,"id");
            if(!member.path("family_id").isIntegralNumber()
                    ||member.path("family_id").asLong()!=sourceFamily
                    ||!original.add(id)
                    ||!member.path("member_type").isTextual()
                    ||!Set.of("PARENT","CHILD","OTHER").contains(
                        member.path("member_type").asText())
                    ||!member.path("active").isBoolean()
                    ||!member.has("deleted_at")
                    ||!member.path("family_role_id").isIntegralNumber()
                    ||member.path("family_role_id").asLong()<=0)
                throw new SecurityException("RESTORE_MEMBER_SOURCE_ANCHOR_INVALID");
        }
        if(original.isEmpty()||!mapping.members().keySet().equals(original))
            throw new SecurityException("RESTORE_MEMBER_MAPPING_NOT_EXACT_ROSTER");
        var taskIds=ids(tasks,"TASK_DEFINITIONS",sourceFamily,true);
        var rewardIds=ids(rewards,"REWARD_DEFINITIONS",sourceFamily,true);
        var currencyIds=ids(currencies,"POINT_TYPES",sourceFamily,true);
        var execIds=new HashSet<Long>();
        for(JsonNode execution:executions) {
            long id=positive(execution,"id");
            if(!execIds.add(id)||!taskIds.contains(positive(execution,"task_definition_id"))
                    ||!original.contains(positive(execution,"member_id")))
                throw new SecurityException("RESTORE_TASK_SOURCE_ANCHOR_INVALID");
            if(execution.hasNonNull("original_assigned_member_id")
                    &&!original.contains(positive(execution,"original_assigned_member_id")))
                throw new SecurityException("RESTORE_ASSIGNEE_SOURCE_ANCHOR_INVALID");
        }
        var purchaseIds=new HashSet<Long>();
        for(JsonNode purchase:purchases) {
            long id=positive(purchase,"id");
            if(!purchaseIds.add(id)||!original.contains(positive(purchase,"member_id"))
                    ||!currencyIds.contains(positive(purchase,"point_type_id")))
                throw new SecurityException("RESTORE_PURCHASE_SOURCE_ANCHOR_INVALID");
            if(purchase.hasNonNull("reward_definition_id")
                    &&!rewardIds.contains(positive(purchase,"reward_definition_id")))
                throw new SecurityException("RESTORE_REWARD_SOURCE_ANCHOR_INVALID");
            // Request-backed purchases need a separate category/parent,
            // which is unsupported in the bounded test consumer.
            if(purchase.hasNonNull("reward_request_id")
                    ||!purchase.hasNonNull("reward_definition_id"))
                throw new SecurityException("RESTORE_PURCHASE_UNSUPPORTED_PARENT");
        }
        var ledgerIds=new HashSet<Long>();
        for(JsonNode row:ledger) {
            if(!ledgerIds.add(positive(row,"id"))
                    ||!original.contains(positive(row,"member_id"))
                    ||!currencyIds.contains(positive(row,"point_type_id")))
                throw new SecurityException("RESTORE_LEDGER_SOURCE_ANCHOR_INVALID");
            String type=row.path("source_type").asText();
            if("TASK".equals(type)&&!execIds.contains(positive(row,"source_id"))
                    ||"REWARD_PURCHASE".equals(type)
                      &&!purchaseIds.contains(positive(row,"source_id"))
                    ||!Set.of("TASK","REWARD_PURCHASE","MANUAL").contains(type)
                    ||"MANUAL".equals(type)&&row.hasNonNull("source_id"))
                throw new SecurityException("RESTORE_LEDGER_PARENT_UNSUPPORTED_OR_ABSENT");
        }
        var checked=finance.verify(source,sourceFamily,mapping);
        return new Result(original.size(),checked.taskExecutions(),
                checked.purchases(),checked.ledgerRows(),
                checked.expectedTargetBalances());
    }

    private static JsonNode required(JsonNode root,String category) {
        JsonNode value=root.path(category);
        if(!value.isArray())
            throw new SecurityException("RESTORE_SOURCE_CATEGORY_ABSENT:"+category);
        return value;
    }
    private static long positive(JsonNode row,String field) {
        JsonNode value=row.path(field);
        if(!value.isIntegralNumber()||value.asLong()<=0)
            throw new SecurityException("RESTORE_SOURCE_REFERENCE_INVALID:"+field);
        return value.asLong();
    }
    private static Set<Long> ids(JsonNode rows,String category,long sourceFamily,
                                 boolean familyScoped) {
        var ids=new HashSet<Long>();
        for(JsonNode row:rows) {
            long id=positive(row,"id");
            if(!ids.add(id)||(familyScoped
                    &&(!row.path("family_id").isIntegralNumber()
                        ||row.path("family_id").asLong()!=sourceFamily)))
                throw new SecurityException("RESTORE_SOURCE_PARENT_INVALID:"+category);
        }
        return ids;
    }
}
