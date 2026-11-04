package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.olehkobylianskyi.familypoints.service.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.crypto.SecretKey;
import java.util.*;

/**
 * Disposable PostgreSQL only. Restores source-verified task/reward definitions
 * with remapped protected members, groups, work folders and currencies.
 *
 * Inbound FK history/schedule/category tables must be empty in the source.
 * This is NOT a FULL restore and never edits balances or historical rows.
 * Column names are read from PostgreSQL catalog, never from the archive.
 */
final class DisposableDefinitionRestoreExecutor {
    record Result(Map<Long,Long> taskIds,Map<Long,Long> rewardIds) {}
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final ProtectiveBackupPortableFile portable;
    private final ProtectiveBackupArtifactCodec codec;
    private final ProtectiveBackupImportInspector inspector;
    private final ObjectMapper json=new ObjectMapper();

    private static final Map<String,String> TABLES=Map.of(
            "TASK_DEFINITIONS","task_definitions","REWARD_DEFINITIONS","reward_definitions");
    private static final Map<String,String> MEMBER_FKS=Map.of(
            "assigned_member_id","family_members","created_by_member_id","family_members",
            "responsible_member_id","family_members","preferred_member_id","family_members");
    private static final Set<String> TASK_FKS=Set.of(
            "assigned_member_id","created_by_member_id","responsible_member_id",
            "preferred_member_id","target_group_id","work_node_id",
            "reward_point_type_id","penalty_point_type_id");
    private static final Set<String> REWARD_FKS=Set.of("point_type_id");
    private static final Set<String> BASE_FKS=Set.of("family_id");

    DisposableDefinitionRestoreExecutor(JdbcTemplate db,PlatformTransactionManager manager,
            ProtectiveBackupPortableFile portable,ProtectiveBackupArtifactCodec codec,
            ProtectiveBackupImportInspector inspector) {
        this.db=db;this.tx=new TransactionTemplate(manager);
        this.portable=portable;this.codec=codec;this.inspector=inspector;
    }

    Result apply(byte[] file,long source,long target,Map<Long,Long> members,
            Map<Long,Long> groups,Map<Long,Long> nodes,SecretKey key) {
        if(source<=0 || target<=0 || source==target)
            throw new IllegalArgumentException("DISPOSABLE_DISTINCT_FAMILIES_REQUIRED");
        var proof=inspector.inspect(file,source,key);
        if(!"V50".equals(proof.schemaVersion()) || proof.availableCategories().isEmpty()
                || !TABLES.keySet().containsAll(proof.availableCategories()))
            throw new IllegalArgumentException("DEFINITIONS_SCOPE_UNSUPPORTED");
        var envelope=portable.decode(file);
        byte[] plain=codec.open(envelope,envelope.manifest().snapshotId(),source,key);
        final JsonNode cats;
        try {cats=json.readTree(plain).path("categories");}
        catch(Exception ex){throw new IllegalArgumentException("INVALID_DEFINITIONS_ARCHIVE",ex);}
        finally {Arrays.fill(plain,(byte)0);}
        for(var category:proof.availableCategories()) {
            for(JsonNode row:cats.path(category)) {
                if(!row.path("family_id").isIntegralNumber()
                        || row.path("family_id").asLong()!=source
                        || !row.path("id").isIntegralNumber()
                        || row.path("id").asLong()<=0)
                    throw new SecurityException("INVALID_DEFINITION_OWNER_OR_ID");
            }
        }
        return tx.execute(status->{
            Long locked=db.queryForObject("SELECT id FROM families WHERE id=? FOR UPDATE",
                    Long.class,target);
            Long epoch=db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,target);
            if(locked==null || epoch==null || epoch!=0L)
                throw new IllegalStateException("DEFINITION_TARGET_EPOCH_STALE");
            for(var category:proof.availableCategories()) {
                var table=TABLES.get(category);
                Long existing=db.queryForObject("SELECT count(*) FROM "+table+" WHERE family_id=?",
                        Long.class,target);
                if(existing==null || existing!=0L)
                    throw new IllegalStateException("TARGET_DEFINITIONS_NOT_EMPTY:"+category);
                ensureNoUnexportedDependents(table,source);
                assertMappedFkSchema(table,category);
            }
            validateMemberMap(source,target,members);
            var task=new TreeMap<Long,Long>();var reward=new TreeMap<Long,Long>();
            for(var category:proof.availableCategories()) {
                var table=TABLES.get(category);
                var map="TASK_DEFINITIONS".equals(category)?task:reward;
                var columns=insertColumns(table);
                for(JsonNode archived:cats.path(category)) {
                    var row=(ObjectNode)archived.deepCopy();
                    long oldId=archived.path("id").asLong();
                    row.remove("id");
                    row.put("family_id",target);
                    if("TASK_DEFINITIONS".equals(category)) {
                        for(var fk:MEMBER_FKS.keySet())
                            remap(row,fk,members,"UNMAPPED_DEFINITION_MEMBER");
                        remap(row,"target_group_id",groups,"UNMAPPED_DEFINITION_GROUP");
                        remap(row,"work_node_id",nodes,"UNMAPPED_DEFINITION_WORK_NODE");
                        remapCurrency(row,"reward_point_type_id",source,target);
                        remapCurrency(row,"penalty_point_type_id",source,target);
                    } else remapCurrency(row,"point_type_id",source,target);
                    // Source archive cannot add arbitrary columns or override
                    // protected/generated target fields.
                    for(var field=row.fieldNames();field.hasNext();) {
                        String name=field.next();
                        if(!columns.contains(name))
                            throw new IllegalArgumentException("UNKNOWN_DEFINITION_FIELD:"+name);
                    }
                    var ordered=columns.stream().filter(row::has).toList();
                    if(ordered.isEmpty())throw new IllegalArgumentException("EMPTY_DEFINITION_ROW");
                    String projection=String.join(",",ordered);
                    String sql="INSERT INTO "+table+" ("+projection+") SELECT "+projection
                            +" FROM jsonb_populate_record(NULL::public."+table+",CAST(? AS jsonb)) RETURNING id";
                    final String payload;
                    try {payload=json.writeValueAsString(row);}
                    catch(Exception ex){throw new IllegalArgumentException("INVALID_DEFINITION_JSON",ex);}
                    Long fresh=db.queryForObject(sql,Long.class,payload);
                    if(fresh==null || fresh.equals(oldId))
                        throw new IllegalStateException("DEFINITION_PK_REMAP_FAILED");
                    map.put(oldId,fresh);
                }
            }
            // Every restored TASK WorkNode must have its unique TaskDefinition.
            if(!task.isEmpty() || !nodes.isEmpty()) {
                Long orphan=db.queryForObject("""
                    SELECT count(*) FROM work_nodes w
                    LEFT JOIN task_definitions t ON t.work_node_id=w.id AND t.family_id=w.family_id
                    WHERE w.family_id=? AND w.type='TASK' AND t.id IS NULL
                    """,Long.class,target);
                if(orphan==null || orphan!=0L)
                    throw new IllegalStateException("WORK_NODE_TASK_UNRESOLVED");
            }
            for(var category:proof.availableCategories()) {
                Long count=db.queryForObject("SELECT count(*) FROM "+TABLES.get(category)
                        +" WHERE family_id=?",Long.class,target);
                if(count==null || count.intValue()!=cats.path(category).size())
                    throw new IllegalStateException("DEFINITION_RESTORE_COUNT_MISMATCH");
            }
            return new Result(Map.copyOf(task),Map.copyOf(reward));
        });
    }

    private void validateMemberMap(long source,long target,Map<Long,Long> mapping) {
        if(mapping==null)throw new SecurityException("MEMBER_MAP_REQUIRED");
        for(var pair:mapping.entrySet()) {
            Long valid=db.queryForObject("""
                SELECT count(*) FROM family_members a
                JOIN family_members b ON b.id=?
                WHERE a.id=? AND a.family_id=? AND b.family_id=?
                """,Long.class,pair.getValue(),pair.getKey(),source,target);
            if(valid==null || valid!=1L)throw new SecurityException("CROSS_TENANT_MEMBER_MAP");
        }
    }

    private void remap(ObjectNode row,String column,Map<Long,Long> ids,String error) {
        JsonNode node=row.path(column);
        if(node.isMissingNode() || node.isNull())return;
        if(!node.isIntegralNumber() || ids==null || !ids.containsKey(node.asLong()))
            throw new SecurityException(error+":"+column);
        row.put(column,ids.get(node.asLong()));
    }

    private void remapCurrency(ObjectNode row,String column,long source,long target) {
        var node=row.path(column);
        if(node.isMissingNode() || node.isNull())return;
        if(!node.isIntegralNumber() || node.asLong()<=0)
            throw new SecurityException("INVALID_CURRENCY_REFERENCE");
        String code=db.query("""
                SELECT code FROM point_types WHERE id=? AND family_id=?
                """,rs->rs.next()?rs.getString(1):null,node.asLong(),source);
        if(code==null)throw new SecurityException("SOURCE_CURRENCY_FOREIGN_FAMILY");
        var destination=db.query("""
                SELECT id FROM point_types WHERE family_id=? AND code=? AND active=true
                """,(rs,i)->rs.getLong(1),target,code);
        if(destination.size()!=1)throw new SecurityException("TARGET_CURRENCY_UNAVAILABLE:"+code);
        row.put(column,destination.get(0));
    }

    private List<String> insertColumns(String table) {
        return db.query("""
                SELECT a.attname
                FROM pg_attribute a
                JOIN pg_class c ON c.oid=a.attrelid
                JOIN pg_namespace n ON n.oid=c.relnamespace
                WHERE n.nspname='public' AND c.relname=? AND a.attnum>0
                  AND NOT a.attisdropped AND a.attgenerated=''
                  AND a.attidentity=''
                  AND a.attname<>'id'
                ORDER BY a.attnum
                """,(rs,i)->rs.getString(1),table);
    }

    private void assertMappedFkSchema(String table,String category) {
        var supported=new HashSet<>(BASE_FKS);
        supported.addAll("TASK_DEFINITIONS".equals(category)?TASK_FKS:REWARD_FKS);
        var actual=db.query("""
                SELECT child.attname FROM pg_constraint fk
                JOIN pg_class c ON c.oid=fk.conrelid
                JOIN pg_namespace n ON n.oid=c.relnamespace
                JOIN LATERAL unnest(fk.conkey) k(attnum) ON true
                JOIN pg_attribute child ON child.attrelid=c.oid AND child.attnum=k.attnum
                WHERE n.nspname='public' AND c.relname=? AND fk.contype='f'
                """,(rs,i)->rs.getString(1),table);
        for(var fk:actual)if(!supported.contains(fk))
            throw new IllegalStateException("UNKNOWN_DEFINITION_FK:"+table+"."+fk);
    }

    private void ensureNoUnexportedDependents(String table,long source) {
        // Never quietly restore a definition while dropping existing purchase,
        // schedule, task execution or association rows. All inbound FKs must
        // be independently closed by a later archive implementation.
        var links=db.query("""
                SELECT child.relname, ca.attname, pa.attname
                FROM pg_constraint fk
                JOIN pg_class parent ON parent.oid=fk.confrelid
                JOIN pg_namespace pn ON pn.oid=parent.relnamespace
                JOIN pg_class child ON child.oid=fk.conrelid
                JOIN pg_namespace cn ON cn.oid=child.relnamespace
                JOIN LATERAL unnest(fk.conkey) WITH ORDINALITY c(attnum,ord) ON true
                JOIN LATERAL unnest(fk.confkey) WITH ORDINALITY p(attnum,ord)
                   ON p.ord=c.ord
                JOIN pg_attribute ca ON ca.attrelid=child.oid AND ca.attnum=c.attnum
                JOIN pg_attribute pa ON pa.attrelid=parent.oid AND pa.attnum=p.attnum
                WHERE fk.contype='f' AND pn.nspname='public' AND cn.nspname='public'
                  AND parent.relname=?
                """,(rs,i)->new String[]{rs.getString(1),rs.getString(2),rs.getString(3)},table);
        for(var link:links) {
            for(var identifier:link)
                if(!identifier.matches("[a-z_][a-z0-9_]*"))
                    throw new SecurityException("UNSAFE_METADATA_IDENTIFIER");
            // Composite FKs are not supported by this limited restore.
            if(!"id".equals(link[2]))
                throw new IllegalStateException("UNSUPPORTED_DEFINITION_PARENT_FK");
            Long dependent=db.queryForObject("SELECT count(*) FROM "+link[0]
                    +" d JOIN "+table+" p ON d."+link[1]+"=p.id WHERE p.family_id=?",
                    Long.class,source);
            if(dependent==null || dependent!=0L)
                throw new IllegalStateException("UNEXPORTED_DEFINITION_DEPENDENCY:"+link[0]);
        }
    }
}
