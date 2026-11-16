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
 * TEST ONLY. Native historical group-point ledger replay on a disposable
 * target PostgreSQL. Explicit source->target group and currency mappings are
 * mandatory; identity, owner, epoch and target row count are enforced.
 *
 * This class cannot execute from application code or HTTP. The financial
 * reconciliation/owner proof still needs a production implementation.
 */
final class DisposableGroupLedgerRestoreExecutor {
    record Replay(Map<Long,Long> transactionIds,Map<Long,Long> balancesByTargetGroup,
                  int replayed,UUID snapshotId) {}

    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final ProtectiveBackupPortableFile files;
    private final ProtectiveBackupArtifactCodec codec;
    private final ProtectiveBackupImportInspector inspector;
    private final ObjectMapper mapper=new ObjectMapper();

    DisposableGroupLedgerRestoreExecutor(JdbcTemplate db,
            PlatformTransactionManager manager,ProtectiveBackupPortableFile files,
            ProtectiveBackupArtifactCodec codec,ProtectiveBackupImportInspector inspector) {
        this.db=db;this.tx=new TransactionTemplate(manager);
        this.files=files;this.codec=codec;this.inspector=inspector;
    }

    Replay apply(byte[] encrypted,long source,long target,
            Map<Long,Long> groupIds,Map<Long,Long> currencyIds,SecretKey key) {
        if(source<=0 || target<=0 || source==target || encrypted==null || key==null
                || groupIds==null || currencyIds==null)
            throw new IllegalArgumentException("INVALID_DISPOSABLE_LEDGER_REPLAY");
        var inspected=inspector.inspect(encrypted,source,key);
        if(!"V50".equals(inspected.schemaVersion())
                || !inspected.availableCategories().equals(
                    List.of("GROUP_POINT_LEDGER","MEMBER_GROUPS")))
            throw new IllegalArgumentException("LEDGER_REPLAY_SCOPE_UNSUPPORTED");
        var envelope=files.decode(encrypted);
        byte[] plaintext=codec.open(envelope,inspected.snapshotId(),source,key);
        final JsonNode groups,rows;
        try {
            var categories=mapper.readTree(plaintext).path("categories");
            groups=categories.path("MEMBER_GROUPS");
            rows=categories.path("GROUP_POINT_LEDGER");
        } catch(Exception ex) {
            throw new IllegalArgumentException("INVALID_LEDGER_REPLAY_ARCHIVE",ex);
        } finally {Arrays.fill(plaintext,(byte)0);}
        var sourceGroups=new HashSet<Long>();
        for(JsonNode group:groups) {
            long old=group.path("id").asLong();
            if(group.path("family_id").asLong()!=source || old<=0
                    || !sourceGroups.add(old))
                throw new SecurityException("LEDGER_SOURCE_GROUP_INVALID");
        }
        if(!groupIds.keySet().equals(sourceGroups)
                || new HashSet<>(groupIds.values()).size()!=groupIds.size())
            throw new SecurityException("LEDGER_INCOMPLETE_GROUP_REMAP");
        var oldTransactions=new HashSet<Long>();
        var oldCurrencies=new HashSet<Long>();
        var expectedBalances=new HashMap<Long,Long>();
        for(JsonNode row:rows) {
            long oldId=row.path("id").asLong();
            long group=row.path("group_id").asLong();
            long pointType=row.path("point_type_id").asLong();
            if(oldId<=0 || !oldTransactions.add(oldId)
                    || !sourceGroups.contains(group) || pointType<=0
                    || !row.path("amount").isIntegralNumber()
                    || !row.path("type").isTextual()
                    || !row.path("source_type").isTextual()
                    || !row.path("created_at").isTextual()
                    || row.path("source_id").isNumber() && !row.path("source_id").isIntegralNumber())
                throw new SecurityException("LEDGER_SOURCE_GRAPH_INVALID");
            // A source_id may refer to a task, purchase, exchange or other
            // historical entity. This bounded proof intentionally supports
            // MANUAL only; other source IDs need a complete remap graph.
            if(!"MANUAL".equals(row.path("source_type").asText())
                    || row.hasNonNull("source_id"))
                throw new IllegalArgumentException("LEDGER_HISTORICAL_SOURCE_REMAP_UNSUPPORTED");
            oldCurrencies.add(pointType);
            expectedBalances.merge(group,(long)row.path("amount").asInt(),Long::sum);
        }
        if(!currencyIds.keySet().equals(oldCurrencies)
                || new HashSet<>(currencyIds.values()).size()!=currencyIds.size())
            throw new SecurityException("LEDGER_INCOMPLETE_CURRENCY_REMAP");

        return tx.execute(status->{
            Long locked=db.queryForObject(
                    "SELECT id FROM families WHERE id=? FOR UPDATE",Long.class,target);
            if(locked==null)throw new SecurityException("LEDGER_TARGET_MISSING");
            Long epoch=db.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=?",Long.class,target);
            if(epoch==null || epoch!=0)
                throw new IllegalStateException("LEDGER_TARGET_EPOCH_NOT_FRESH");
            for(var mapped:groupIds.values()) {
                Long count=db.queryForObject(
                    "SELECT count(*) FROM member_groups WHERE id=? AND family_id=?",
                    Long.class,mapped,target);
                if(!Objects.equals(count,1L))
                    throw new SecurityException("LEDGER_CROSS_TENANT_GROUP_REMAP");
            }
            for(var mapped:currencyIds.values()) {
                Long count=db.queryForObject(
                    "SELECT count(*) FROM point_types WHERE id=? AND family_id=? AND active=true",
                    Long.class,mapped,target);
                if(!Objects.equals(count,1L))
                    throw new SecurityException("LEDGER_CROSS_TENANT_CURRENCY_REMAP");
            }
            Long existing=db.queryForObject("""
                    SELECT count(*) FROM group_point_transactions t
                    JOIN member_groups g ON g.id=t.group_id
                    WHERE g.family_id=?
                    """,Long.class,target);
            if(existing==null || existing!=0L)
                throw new IllegalStateException("LEDGER_TARGET_NOT_EMPTY");
            var remappedTransactions=new TreeMap<Long,Long>();
            for(JsonNode row:rows) {
                Long id=db.queryForObject("""
                        INSERT INTO group_point_transactions
                          (group_id,point_type_id,amount,created_at,type,source_type,description)
                        VALUES (?,?,?,CAST(? AS timestamp),?,?,?) RETURNING id
                        """,Long.class,
                        groupIds.get(row.path("group_id").asLong()),
                        currencyIds.get(row.path("point_type_id").asLong()),
                        row.path("amount").asInt(),
                        row.path("created_at").asText(),
                        row.path("type").asText(),
                        row.path("source_type").asText(),
                        row.path("description").isNull()?null:row.path("description").asText(null));
                if(id==null || id.equals(row.path("id").asLong()))
                    throw new IllegalStateException("LEDGER_TRANSACTION_PK_REMAP_FAILED");
                remappedTransactions.put(row.path("id").asLong(),id);
            }
            Long persisted=db.queryForObject("""
                    SELECT count(*) FROM group_point_transactions t
                    JOIN member_groups g ON g.id=t.group_id
                    WHERE g.family_id=?
                    """,Long.class,target);
            if(persisted==null || persisted.intValue()!=rows.size())
                throw new IllegalStateException("LEDGER_REPLAY_COUNT_MISMATCH");
            var balances=new TreeMap<Long,Long>();
            for(var pair:expectedBalances.entrySet()) {
                long mapped=groupIds.get(pair.getKey());
                Long actual=db.queryForObject(
                    "SELECT COALESCE(SUM(amount),0) FROM group_point_transactions WHERE group_id=?",
                    Long.class,mapped);
                if(!Objects.equals(actual,pair.getValue()))
                    throw new IllegalStateException("LEDGER_REPLAY_BALANCE_MISMATCH");
                balances.put(mapped,actual);
            }
            return new Replay(Map.copyOf(remappedTransactions),
                    Map.copyOf(balances),rows.size(),inspected.snapshotId());
        });
    }
}
