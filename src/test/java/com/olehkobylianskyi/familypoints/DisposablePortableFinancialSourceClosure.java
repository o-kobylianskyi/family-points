package com.olehkobylianskyi.familypoints;

import java.util.*;

/**
 * TEST ONLY. Source-side economic and group ancestry checks for an eventual
 * authenticated archive. Does NOT define supported financial replay semantics,
 * authorize refunds, calculate balances or modify the production exporter.
 */
public final class DisposablePortableFinancialSourceClosure {
    private DisposablePortableFinancialSourceClosure() {}

    public record Parent(String category, String column, boolean optional) {}
    private static final Map<String,List<Parent>> REQUIRED_PARENTS=Map.ofEntries(
        Map.entry("POINT_LEDGER",List.of(
            new Parent("POINT_TYPES","point_type_id",false))),
        Map.entry("REPUTATION_LEDGER",List.of()),
        Map.entry("REWARD_PURCHASES",List.of(
            new Parent("REWARD_DEFINITIONS","reward_definition_id",false),
            new Parent("POINT_TYPES","point_type_id",false))),
        Map.entry("GROUP_POINT_LEDGER",List.of(
            new Parent("MEMBER_GROUPS","group_id",false),
            new Parent("POINT_TYPES","point_type_id",false))),
        Map.entry("GROUP_MEMBERSHIPS",List.of(
            new Parent("MEMBER_GROUPS","member_group_id",false))),
        Map.entry("TASK_EXECUTIONS",List.of(
            new Parent("TASK_DEFINITIONS","task_definition_id",false),
            new Parent("POINT_TYPES","reward_point_type_id",true),
            new Parent("POINT_TYPES","penalty_point_type_id",true))),
        Map.entry("TASK_DEFINITIONS",List.of(
            new Parent("POINT_TYPES","reward_point_type_id",true),
            new Parent("POINT_TYPES","penalty_point_type_id",true))),
        Map.entry("REWARD_DEFINITIONS",List.of(
            new Parent("POINT_TYPES","point_type_id",false))),
        Map.entry("POINT_EXCHANGE_REQUESTS",List.of(
            new Parent("POINT_EXCHANGE_RATES","exchange_rate_id",false),
            new Parent("POINT_TYPES","from_point_type_id",false),
            new Parent("POINT_TYPES","to_point_type_id",false))),
        Map.entry("POINT_EXCHANGE_RATES",List.of(
            new Parent("POINT_TYPES","from_point_type_id",false),
            new Parent("POINT_TYPES","to_point_type_id",false)))
    );
    public static Map<String,List<Parent>> reviewedParents() {return REQUIRED_PARENTS;}

    public static void verify(long workspaceId,
                              Map<String,? extends List<? extends Map<String,?>>> categoryRows) {
        if(workspaceId<=0)throw new IllegalArgumentException("INVALID_SOURCE_WORKSPACE");
        Objects.requireNonNull(categoryRows,"categoryRows");
        var ids=new HashMap<String,Set<Long>>();
        for(var parent:List.of("POINT_TYPES","MEMBER_GROUPS","REWARD_DEFINITIONS",
                "TASK_DEFINITIONS","POINT_EXCHANGE_RATES")) {
            var rows=categoryRows.get(parent);
            if(rows==null)continue;
            if(rows.size()>2000)throw new IllegalStateException("SOURCE_FINANCE_ROW_LIMIT");
            var seen=new HashSet<Long>();
            for(var row:rows) {
                long id=id(row.get("id"),parent+".id");
                long family=id(row.get("family_id"),parent+".family_id");
                if(family!=workspaceId)
                    throw new SecurityException("SOURCE_FINANCE_PARENT_FOREIGN_WORKSPACE:"+parent);
                if(!seen.add(id))
                    throw new SecurityException("SOURCE_FINANCE_PARENT_DUPLICATE:"+parent);
            }
            ids.put(parent,seen);
        }
        for(var entry:categoryRows.entrySet()) {
            if(!REQUIRED_PARENTS.containsKey(entry.getKey()) &&
                    !ids.containsKey(entry.getKey()))
                throw new IllegalArgumentException("UNREVIEWED_FINANCE_SOURCE_CATEGORY:"+entry.getKey());
            var rows=entry.getValue();
            if(rows.size()>2000)throw new IllegalStateException("SOURCE_FINANCE_ROW_LIMIT");
            for(var row:rows) {
                for(var parent:REQUIRED_PARENTS.getOrDefault(entry.getKey(),List.of())) {
                    if(!row.containsKey(parent.column()))
                        throw new SecurityException("SOURCE_FINANCE_FIELD_MISSING:"+
                                entry.getKey()+"."+parent.column());
                    Object source=row.get(parent.column());
                    if(source==null && parent.optional())continue;
                    long sourceId=id(source,entry.getKey()+"."+parent.column());
                    if(!ids.containsKey(parent.category()))
                        throw new SecurityException("SOURCE_FINANCE_CATEGORY_MISSING:"+parent.category());
                    if(!ids.get(parent.category()).contains(sourceId))
                        throw new SecurityException("SOURCE_FINANCE_PARENT_MISSING:"+
                                entry.getKey()+"."+parent.column());
                }
            }
        }
    }

    private static long id(Object raw,String field) {
        if(!(raw instanceof Long || raw instanceof Integer) ||
                ((Number)raw).longValue()<=0)
            throw new SecurityException("SOURCE_FINANCE_ID_INVALID:"+field);
        return ((Number)raw).longValue();
    }
}
