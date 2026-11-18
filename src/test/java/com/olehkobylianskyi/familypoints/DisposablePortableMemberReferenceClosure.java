package com.olehkobylianskyi.familypoints;

import java.util.*;

/**
 * TEST ONLY — source graph member ancestry preflight for a future signed vNext
 * archive. Does not authorize restore, account association or target roles.
 */
public final class DisposablePortableMemberReferenceClosure {
    private DisposablePortableMemberReferenceClosure() {}

    // Columns exist in real source tables. Explicit nullable parent edges
    // never default to executor and are not inferred from other fields.
    public record MemberField(String column, boolean nullable) {}
    private static final Map<String,List<MemberField>> MEMBER_EDGES=Map.ofEntries(
        Map.entry("TASK_EXECUTIONS",List.of(
            new MemberField("member_id",false),
            new MemberField("original_assigned_member_id",true))),
        Map.entry("TASK_DEFINITIONS",List.of(
            new MemberField("assigned_member_id",true),
            new MemberField("created_by_member_id",true),
            new MemberField("preferred_member_id",true),
            new MemberField("responsible_member_id",true))),
        Map.entry("POINT_LEDGER",List.of(new MemberField("member_id",false))),
        Map.entry("REPUTATION_LEDGER",List.of(new MemberField("member_id",false))),
        Map.entry("REWARD_PURCHASES",List.of(new MemberField("member_id",false))),
        Map.entry("POINT_EXCHANGE_REQUESTS",List.of(
            new MemberField("member_id",false),
            new MemberField("processed_by_member_id",true))),
        Map.entry("GROUP_MEMBERSHIPS",List.of(new MemberField("member_id",false))),
        Map.entry("MEMBER_EXCEPTION_PERIODS",List.of(new MemberField("member_id",false))),
        Map.entry("TASK_DELEGATIONS",List.of(
            new MemberField("delegated_by_member_id",false),
            new MemberField("from_member_id",false),
            new MemberField("to_member_id",false)))
    );

    public static Map<String,List<MemberField>> coveredEdges() {
        return MEMBER_EDGES;
    }

    public static void verify(DisposablePortableMemberSourceCatalog.Catalog roster,
                              Map<String,? extends List<? extends Map<String,?>>> categoryRows) {
        Objects.requireNonNull(roster,"roster");
        Objects.requireNonNull(categoryRows,"categoryRows");
        if(roster.sourceWorkspaceId()<=0)
            throw new SecurityException("SOURCE_MEMBER_CATALOG_WORKSPACE_INVALID");
        if(roster.roles().size()>2000 || roster.members().size()>2000)
            throw new IllegalStateException("SOURCE_MEMBER_CATALOG_ROW_LIMIT");
        var roles=new HashSet<Long>();
        var roleCodes=new HashSet<String>();
        for(var role:roster.roles()) {
            if(role.sourceWorkspaceId()!=roster.sourceWorkspaceId() ||
                    role.sourceRoleId()<=0 || !roles.add(role.sourceRoleId())
                    || role.roleCode()==null || role.roleCode().isBlank()
                    || !roleCodes.add(role.roleCode()))
                throw new SecurityException("SOURCE_ROLE_GRAPH_INVALID");
        }
        var memberIds=new HashSet<Long>();
        for(var member:roster.members()) {
            if(member.sourceWorkspaceId()!=roster.sourceWorkspaceId()
                    || member.sourceMemberId()<=0 || !roles.contains(member.sourceRoleId())
                    || !Set.of("PARENT","CHILD","OTHER").contains(member.memberType())
                    || !memberIds.add(member.sourceMemberId()))
                throw new SecurityException("SOURCE_MEMBER_GRAPH_INVALID");
        }
        for(var entry:categoryRows.entrySet()) {
            var fields=MEMBER_EDGES.get(entry.getKey());
            if(fields==null)
                throw new IllegalArgumentException("UNREVIEWED_MEMBER_EDGE_CATEGORY:"+entry.getKey());
            var rows=Objects.requireNonNull(entry.getValue(),"rows");
            if(rows.size()>2000)throw new IllegalStateException("SOURCE_MEMBER_EDGE_ROW_LIMIT");
            for(var row:rows) {
                Objects.requireNonNull(row,"row");
                for(var field:fields) {
                    // Distinguish explicit NULL from omitted field: full source
                    // snapshot cannot reconstruct lost optional ancestry later.
                    if(!row.containsKey(field.column()))
                        throw new SecurityException("SOURCE_MEMBER_EDGE_FIELD_MISSING:"
                                +entry.getKey()+"."+field.column());
                    Object raw=row.get(field.column());
                    if(raw==null && field.nullable())continue;
                    if(!(raw instanceof Long || raw instanceof Integer)
                            || ((Number)raw).longValue()<=0
                            || !memberIds.contains(((Number)raw).longValue()))
                        throw new SecurityException("SOURCE_MEMBER_EDGE_UNRESOLVED:"
                                +entry.getKey()+"."+field.column());
                }
            }
        }
    }
}
