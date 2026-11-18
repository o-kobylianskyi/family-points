package com.olehkobylianskyi.familypoints.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * IMP-01a.2 fail-closed source-history extension of the existing v2 archive.
 * This is source provenance ONLY, not complete economic replay or RESTORE.
 * Each included row is encrypted later with the same FPB1 AES-GCM archive.
 */
public final class PortableV2HistoryGraph {
    private PortableV2HistoryGraph() {}
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final String DOMAIN="family-points:portable:history-closure:v2";
    private static final List<String> ORDER=List.of(
            "POINT_TYPES","TASK_DEFINITIONS","REWARD_DEFINITIONS",
            "TASK_EXECUTIONS","REWARD_PURCHASES","POINT_LEDGER");
    private record Table(String sql) {}
    // Fixed source queries, never caller-controlled table/column references.
    private static final Map<String,Table> SQL=Map.of(
        "POINT_TYPES",new Table("SELECT to_jsonb(t)::text FROM point_types t WHERE t.family_id=? ORDER BY t.id"),
        "TASK_DEFINITIONS",new Table("SELECT to_jsonb(t)::text FROM task_definitions t WHERE t.family_id=? ORDER BY t.id"),
        "REWARD_DEFINITIONS",new Table("SELECT to_jsonb(t)::text FROM reward_definitions t WHERE t.family_id=? ORDER BY t.id"),
        "TASK_EXECUTIONS",new Table("SELECT to_jsonb(t)::text FROM task_instances t JOIN family_members m ON m.id=t.member_id WHERE m.family_id=? ORDER BY t.id"),
        "REWARD_PURCHASES",new Table("SELECT to_jsonb(t)::text FROM reward_purchases t JOIN family_members m ON m.id=t.member_id WHERE m.family_id=? ORDER BY t.id"),
        "POINT_LEDGER",new Table("SELECT to_jsonb(t)::text FROM point_transactions t JOIN family_members m ON m.id=t.member_id WHERE m.family_id=? ORDER BY t.id")
    );
    private static final Map<String,List<String[]>> PARENTS=Map.of(
        "TASK_DEFINITIONS",List.<String[]>of(new String[]{"reward_point_type_id","POINT_TYPES"},
                new String[]{"penalty_point_type_id","POINT_TYPES"},
                new String[]{"assigned_member_id","FAMILY_MEMBERS"},
                new String[]{"created_by_member_id","FAMILY_MEMBERS"},
                new String[]{"preferred_member_id","FAMILY_MEMBERS"},
                new String[]{"responsible_member_id","FAMILY_MEMBERS"}),
        "REWARD_DEFINITIONS",List.<String[]>of(new String[]{"point_type_id","POINT_TYPES"}),
        "TASK_EXECUTIONS",List.<String[]>of(new String[]{"task_definition_id","TASK_DEFINITIONS"},
                new String[]{"reward_point_type_id","POINT_TYPES"},
                new String[]{"penalty_point_type_id","POINT_TYPES"},
                new String[]{"member_id","FAMILY_MEMBERS"},
                new String[]{"original_assigned_member_id","FAMILY_MEMBERS"}),
        "REWARD_PURCHASES",List.<String[]>of(new String[]{"reward_definition_id","REWARD_DEFINITIONS"},
                new String[]{"point_type_id","POINT_TYPES"},
                new String[]{"member_id","FAMILY_MEMBERS"}),
        "POINT_LEDGER",List.<String[]>of(new String[]{"point_type_id","POINT_TYPES"},
                new String[]{"member_id","FAMILY_MEMBERS"})
    );

    public static void capture(JdbcTemplate db,ObjectNode categories,long workspace) {
        if(categories==null||workspace<=0)
            throw new SecurityException("PORTABLE_V2_HISTORY_SCOPE_INVALID");
        // A member-scoped SELECT must not silently omit an execution whose
        // owning task belongs to this source workspace but whose assignee
        // points to another workspace, even on a historically damaged DB.
        Long badTask=db.queryForObject("""
                SELECT count(*) FROM task_instances t
                JOIN task_definitions d ON d.id=t.task_definition_id
                LEFT JOIN family_members m ON m.id=t.member_id
                WHERE d.family_id=? AND (m.id IS NULL OR m.family_id<>?)
                """,Long.class,workspace,workspace);
        if(badTask==null||badTask!=0L)
            throw new SecurityException("PORTABLE_V2_HIDDEN_FOREIGN_TASK_EXECUTION");
        Long badPurchase=db.queryForObject("""
                SELECT count(*) FROM reward_purchases p
                JOIN reward_definitions d ON d.id=p.reward_definition_id
                LEFT JOIN family_members m ON m.id=p.member_id
                WHERE d.family_id=? AND (m.id IS NULL OR m.family_id<>?)
                """,Long.class,workspace,workspace);
        if(badPurchase==null||badPurchase!=0L)
            throw new SecurityException("PORTABLE_V2_HIDDEN_FOREIGN_REWARD_PURCHASE");
        for(String category:ORDER) {
            var rows=categories.putArray(category);
            // Query returns only rows scoped through actual source family.
            db.query(SQL.get(category).sql(),rs->{
                if(rows.size()>=2000)
                    throw new SecurityException("PORTABLE_V2_HISTORY_ROW_LIMIT");
                try {rows.add(JSON.readTree(rs.getString(1)));}
                catch(Exception invalid){throw new SecurityException("PORTABLE_V2_HISTORY_JSON_INVALID",invalid);}
            },workspace);
        }
        attest(categories,workspace);
    }

    /** Returns per-category content+dependency hashes after strict full source FK review. */
    public static Map<String,PortableManifestRootV2.Category> attest(
            ObjectNode categories,long workspace) {
        if(categories==null||workspace<=0)
            throw new SecurityException("PORTABLE_V2_HISTORY_SCOPE_INVALID");
        var allowed=new HashSet<>(ORDER);
        allowed.add("FAMILY_ROLES");
        allowed.add("FAMILY_MEMBERS");
        if(categories.size()!=allowed.size()
                ||!allowed.equals(toNames(categories)))
            throw new SecurityException("PORTABLE_V2_HISTORY_CATEGORIES_INCOMPLETE");
        // Refuse disallowed private columns in both live source capture
        // and offline AES-GCM plaintext inspection, before content hashing.
        PortableV2HistoryDataPolicy.requireAcceptable(categories);
        var ids=new HashMap<String,Set<Long>>();
        for(String category:allowed) {
            var rows=categories.path(category);
            if(!rows.isArray()||rows.size()>2000)
                throw new SecurityException("PORTABLE_V2_HISTORY_ROWS_INVALID:"+category);
            var seen=new HashSet<Long>();
            long previous=0;
            for(JsonNode row:rows) {
                long id=positive(row,"id");
                if(id<=previous||!seen.add(id))
                    throw new SecurityException("PORTABLE_V2_HISTORY_ID_ORDER:"+category);
                previous=id;
                // Three categories are directly scoped by source family.
                // Missing, null or string-encoded family_id is never evidence
                // of provenance, even when the remaining JSON is well formed.
                if(Set.of("POINT_TYPES","TASK_DEFINITIONS","REWARD_DEFINITIONS")
                        .contains(category)
                        &&(!row.hasNonNull("family_id")
                        ||!row.path("family_id").isIntegralNumber()
                        ||row.path("family_id").asLong()!=workspace))
                    throw new SecurityException("PORTABLE_V2_HISTORY_FOREIGN_FAMILY:"+category);
                // Member-owned categories use the authenticated original
                // member roster (not a synthetic top-level family_id).
                if(!"FAMILY_ROLES".equals(category)
                        &&!"FAMILY_MEMBERS".equals(category)
                        &&row.has("family_id")
                        &&positive(row,"family_id")!=workspace)
                    throw new SecurityException("PORTABLE_V2_HISTORY_FOREIGN_FAMILY:"+category);
            }
            ids.put(category,seen);
        }
        var result=new TreeMap<String,PortableManifestRootV2.Category>();
        for(String category:ORDER) {
            var rows=categories.path(category);
            var edges=new ArrayList<String>();
            for(JsonNode row:rows) {
                long sourceId=positive(row,"id");
                for(String[] parent:PARENTS.getOrDefault(category,List.of())) {
                    String field=parent[0],type=parent[1];
                    if(!row.has(field))
                        throw new SecurityException("PORTABLE_V2_HISTORY_FIELD_OMITTED:"+category+"."+field);
                    JsonNode value=row.path(field);
                    if(value.isNull()) {
                        if(required(category,field))
                            throw new SecurityException("PORTABLE_V2_HISTORY_REQUIRED_PARENT_NULL:"+category+"."+field);
                        continue;
                    }
                    long id=positive(row,field);
                    if(!ids.get(type).contains(id))
                        throw new SecurityException("PORTABLE_V2_HISTORY_PARENT_MISSING:"+category+"."+field);
                    edges.add(sourceId+":"+field+":"+id);
                }
                if("REWARD_PURCHASES".equals(category)) {
                    if(!row.has("reward_request_id"))
                        throw new SecurityException("PORTABLE_V2_PURCHASE_REQUEST_FIELD_MISSING");
                    if(row.hasNonNull("reward_request_id"))
                        throw new SecurityException("PORTABLE_V2_UNSUPPORTED_REQUEST_PURCHASE");
                }
                if("POINT_LEDGER".equals(category)) {
                    if(!row.has("source_id")||!row.path("amount").isNumber())
                        throw new SecurityException("PORTABLE_V2_LEDGER_FIELD_MISSING");
                    String sourceType=row.path("source_type").asText();
                    if(!Set.of("TASK","REWARD_PURCHASE","MANUAL").contains(sourceType))
                        throw new SecurityException("PORTABLE_V2_UNSUPPORTED_FINANCIAL_SOURCE");
                    if("TASK".equals(sourceType)||"REWARD_PURCHASE".equals(sourceType)) {
                        long parent=positive(row,"source_id");
                        String target="TASK".equals(sourceType)?"TASK_EXECUTIONS":"REWARD_PURCHASES";
                        if(!ids.get(target).contains(parent))
                            throw new SecurityException("PORTABLE_V2_HISTORY_LEDGER_ORPHAN");
                        edges.add(sourceId+":source_id:"+sourceType+":"+parent);
                    } else if(row.hasNonNull("source_id"))
                        throw new SecurityException("PORTABLE_V2_MANUAL_LEDGER_SOURCE_INVALID");
                }
            }
            Collections.sort(edges);
            String dep=DOMAIN+"|"+category+"|"+String.join("|",edges);
            result.put(category,new PortableManifestRootV2.Category(rows.size(),
                    PortableMemberAncestryV2.canonicalCategoryDigest(rows),
                    ProtectiveBackupArtifactCodec.sha(dep.getBytes(StandardCharsets.UTF_8))));
        }
        return Map.copyOf(result);
    }
    private static boolean required(String category,String field) {
        return switch(category) {
            case "TASK_EXECUTIONS" -> Set.of("task_definition_id","member_id").contains(field);
            case "REWARD_PURCHASES" -> Set.of("reward_definition_id","point_type_id","member_id").contains(field);
            case "POINT_LEDGER" -> Set.of("point_type_id","member_id").contains(field);
            case "REWARD_DEFINITIONS" -> "point_type_id".equals(field);
            default -> false;
        };
    }
    private static long positive(JsonNode row,String field) {
        JsonNode value=row.path(field);
        if(!value.isIntegralNumber()||value.asLong()<=0)
            throw new SecurityException("PORTABLE_V2_HISTORY_INVALID_REFERENCE:"+field);
        return value.asLong();
    }
    private static Set<String> toNames(ObjectNode node) {
        var result=new HashSet<String>();
        node.fieldNames().forEachRemaining(result::add);
        return result;
    }
}
