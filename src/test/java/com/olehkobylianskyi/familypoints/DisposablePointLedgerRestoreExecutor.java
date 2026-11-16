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
 * TEST ONLY. Bounded manual point-transaction history replay into a disposable
 * PostgreSQL family. NOT a production financial replay engine: source currency
 * codes are supplied by isolated fixtures, not authenticated archive metadata.
 */
final class DisposablePointLedgerRestoreExecutor {
    record Result(int count, Map<Long,Long> transactionIds, Map<String,Long> balances,
                  UUID snapshotId) {}
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final ProtectiveBackupPortableFile files;
    private final ProtectiveBackupArtifactCodec codec;
    private final ProtectiveBackupImportInspector inspector;
    private final ObjectMapper mapper=new ObjectMapper();

    DisposablePointLedgerRestoreExecutor(JdbcTemplate db,PlatformTransactionManager manager,
            ProtectiveBackupPortableFile files,ProtectiveBackupArtifactCodec codec,
            ProtectiveBackupImportInspector inspector) {
        this.db=db;this.tx=new TransactionTemplate(manager);
        this.files=files;this.codec=codec;this.inspector=inspector;
    }

    Result apply(byte[] archive,long source,long target,
            Map<Long,Long> members,Map<Long,Long> currencies,
            Map<Long,String> expectedCurrencyCodes,SecretKey key) {
        if(archive==null || source<=0 || target<=0 || source==target
                || members==null || currencies==null || expectedCurrencyCodes==null || key==null)
            throw new IllegalArgumentException("POINT_LEDGER_REPLAY_INVALID_REQUEST");
        var checked=inspector.inspect(archive,source,key);
        if(!"V50".equals(checked.schemaVersion())
                || !checked.availableCategories().equals(List.of("POINT_LEDGER")))
            throw new IllegalArgumentException("POINT_LEDGER_REPLAY_UNSUPPORTED_SCOPE");
        byte[] raw=codec.open(files.decode(archive),checked.snapshotId(),source,key);
        final JsonNode rows;
        try {rows=mapper.readTree(raw).path("categories").path("POINT_LEDGER");}
        catch(java.io.IOException ex) {throw new IllegalArgumentException("POINT_LEDGER_BAD_ARCHIVE",ex);}
        finally {Arrays.fill(raw,(byte)0);}
        if(!rows.isArray())throw new IllegalArgumentException("POINT_LEDGER_MISSING_ROWS");
        var memberIds=new HashSet<Long>();var currencyIds=new HashSet<Long>();
        var rowIds=new HashSet<Long>();
        var expected=new TreeMap<String,Long>();
        for(JsonNode row:rows) {
            if(!row.path("id").isIntegralNumber() || row.path("id").asLong()<=0
                    || !rowIds.add(row.path("id").asLong())
                    || !row.path("member_id").isIntegralNumber()
                    || row.path("member_id").asLong()<=0
                    || !row.path("point_type_id").isIntegralNumber()
                    || row.path("point_type_id").asLong()<=0
                    || !row.path("amount").isIntegralNumber()
                    || !row.path("type").isTextual()
                    || !row.path("created_at").isTextual()
                    || !"MANUAL".equals(row.path("source_type").asText())
                    || row.hasNonNull("source_id"))
                throw new SecurityException("POINT_LEDGER_HISTORICAL_SOURCE_UNSUPPORTED");
            long member=row.path("member_id").asLong();
            long currency=row.path("point_type_id").asLong();
            memberIds.add(member);currencyIds.add(currency);
            expected.merge(member+":"+currency,row.path("amount").asLong(),Math::addExact);
        }
        if(!members.keySet().equals(memberIds) || !currencies.keySet().equals(currencyIds)
                || !expectedCurrencyCodes.keySet().equals(currencyIds)
                || members.values().stream().anyMatch(x->x==null || x<=0)
                || currencies.values().stream().anyMatch(x->x==null || x<=0)
                || expectedCurrencyCodes.values().stream().anyMatch(x->x==null || x.isBlank())
                || new HashSet<>(members.values()).size()!=members.size()
                || new HashSet<>(currencies.values()).size()!=currencies.size())
            throw new SecurityException("POINT_LEDGER_MAPPING_INCOMPLETE");
        return tx.execute(status->{
            Long locked=db.queryForObject("SELECT id FROM families WHERE id=? FOR UPDATE",
                    Long.class,target);
            if(locked==null)throw new SecurityException("POINT_LEDGER_TARGET_MISSING");
            Long epoch=db.queryForObject("SELECT data_epoch FROM families WHERE id=?",
                    Long.class,target);
            if(!Objects.equals(0L,epoch))
                throw new IllegalStateException("POINT_LEDGER_TARGET_EPOCH_STALE");
            for(Long member:members.values()) {
                Long owner=db.queryForObject(
                        "SELECT count(*) FROM family_members WHERE id=? AND family_id=?",
                        Long.class,member,target);
                if(!Objects.equals(1L,owner))
                    throw new SecurityException("POINT_LEDGER_FOREIGN_MEMBER");
            }
            for(var entry:currencies.entrySet()) {
                String code=db.queryForObject(
                        "SELECT code FROM point_types WHERE id=? AND family_id=? AND active=true",
                        String.class,entry.getValue(),target);
                if(!Objects.equals(expectedCurrencyCodes.get(entry.getKey()),code))
                    throw new SecurityException("POINT_LEDGER_CURRENCY_CODE_MISMATCH");
            }
            Long existing=db.queryForObject("""
                    SELECT count(*) FROM point_transactions p
                    JOIN family_members m ON m.id=p.member_id WHERE m.family_id=?
                    """,Long.class,target);
            if(!Objects.equals(0L,existing))
                throw new IllegalStateException("POINT_LEDGER_TARGET_NOT_EMPTY");
            var remapped=new TreeMap<Long,Long>();
            for(JsonNode row:rows) {
                Long id=db.queryForObject("""
                        INSERT INTO point_transactions
                         (member_id,point_type_id,amount,created_at,type,source_type,description)
                        VALUES (?,?,?,CAST(? AS timestamp),?,?,?) RETURNING id
                        """,Long.class,
                        members.get(row.path("member_id").asLong()),
                        currencies.get(row.path("point_type_id").asLong()),
                        row.path("amount").asInt(),
                        row.path("created_at").asText(),
                        row.path("type").asText(),
                        row.path("source_type").asText(),
                        row.hasNonNull("description")?row.path("description").asText():null);
                if(id==null || id<=0)throw new IllegalStateException("POINT_LEDGER_PK_MISSING");
                remapped.put(row.path("id").asLong(),id);
            }
            var balances=new TreeMap<String,Long>();
            for(var e:expected.entrySet()) {
                var parts=e.getKey().split(":");
                long member=members.get(Long.parseLong(parts[0]));
                long currency=currencies.get(Long.parseLong(parts[1]));
                Long sum=db.queryForObject("""
                        SELECT COALESCE(SUM(amount),0) FROM point_transactions
                        WHERE member_id=? AND point_type_id=?
                        """,Long.class,member,currency);
                if(!Objects.equals(e.getValue(),sum))
                    throw new IllegalStateException("POINT_LEDGER_BALANCE_MISMATCH");
                balances.put(member+":"+currency,sum);
            }
            Long count=db.queryForObject("""
                    SELECT count(*) FROM point_transactions p
                    JOIN family_members m ON m.id=p.member_id WHERE m.family_id=?
                    """,Long.class,target);
            if(count==null || count.intValue()!=rows.size())
                throw new IllegalStateException("POINT_LEDGER_REPLAY_COUNT_MISMATCH");
            return new Result(rows.size(),Map.copyOf(remapped),
                    Map.copyOf(balances),checked.snapshotId());
        });
    }
}
