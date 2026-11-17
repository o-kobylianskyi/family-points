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
 * Disposable Testcontainers PostgreSQL ONLY. One authenticated source exchange,
 * one restored request and its two transactions in ONE target transaction.
 * No Spring component, production recovery API, member identity proof, or VERIFIED.
 */
final class DisposableExchangeNativeRestoreExecutor {
    record Result(long requestId,Map<Long,Long> transactionIds,
                  Map<Long,Long> balanceDeltas,UUID snapshotId) {}
    private final JdbcTemplate targetDb;
    private final TransactionTemplate tx;
    private final ProtectiveBackupImportInspector inspector;
    private final ProtectiveBackupArtifactCodec codec;
    private final ProtectiveBackupPortableFile file;
    private final ObjectMapper mapper=new ObjectMapper();

    DisposableExchangeNativeRestoreExecutor(JdbcTemplate targetDb,
            PlatformTransactionManager manager,ProtectiveBackupImportInspector inspector,
            ProtectiveBackupArtifactCodec codec,ProtectiveBackupPortableFile file) {
        this.targetDb=targetDb;this.tx=new TransactionTemplate(manager);
        this.inspector=inspector;this.codec=codec;this.file=file;
    }

    Result apply(byte[] encrypted,long sourceFamily,long targetFamily,
            Map<Long,Long> members,Map<Long,Long> currencies,
            Map<Long,Long> rates,SecretKey key) {
        if(encrypted==null || sourceFamily<=0 || targetFamily<=0 || key==null
                || members==null || currencies==null || rates==null)
            throw new IllegalArgumentException("EXCHANGE_REPLAY_INVALID_REQUEST");
        var inspection=inspector.inspect(encrypted,sourceFamily,key);
        if(!"V50".equals(inspection.schemaVersion())
                || !inspection.availableCategories().equals(List.of(
                    "POINT_EXCHANGE_RATES","POINT_EXCHANGE_REQUESTS",
                    "POINT_LEDGER","POINT_TYPES")))
            throw new SecurityException("EXCHANGE_REPLAY_SCOPE_UNSUPPORTED");
        byte[] clear=codec.open(file.decode(encrypted),
                inspection.snapshotId(),sourceFamily,key);
        final JsonNode categories;
        try {categories=mapper.readTree(clear).path("categories");}
        catch(java.io.IOException ex) {
            throw new IllegalArgumentException("EXCHANGE_REPLAY_BAD_ARCHIVE",ex);
        }finally {Arrays.fill(clear,(byte)0);}
        var srcTypes=categories.path("POINT_TYPES");
        var srcRates=categories.path("POINT_EXCHANGE_RATES");
        var srcRequests=categories.path("POINT_EXCHANGE_REQUESTS");
        var srcLedger=categories.path("POINT_LEDGER");
        if(!srcTypes.isArray() || !srcRates.isArray() || !srcRequests.isArray()
                || !srcLedger.isArray() || srcRates.size()!=1 || srcRequests.size()!=1
                || srcLedger.size()!=2 || srcTypes.size()!=2)
            throw new SecurityException("EXCHANGE_REPLAY_BOUNDED_GRAPH_REQUIRED");
        var rate=srcRates.get(0);
        var request=srcRequests.get(0);
        long oldRequest=id(request,"id");
        long oldRate=id(rate,"id");
        long oldMember=id(request,"member_id");
        long from=id(request,"from_point_type_id");
        long to=id(request,"to_point_type_id");
        if(from==to || rate.path("family_id").asLong()!=sourceFamily
                || id(request,"exchange_rate_id")!=oldRate
                || id(rate,"from_point_type_id")!=from
                || id(rate,"to_point_type_id")!=to
                || !"APPROVED".equals(request.path("status").asText())
                || request.hasNonNull("processed_by_member_id"))
            throw new SecurityException("EXCHANGE_REPLAY_UNSUPPORTED_OWNER_OR_RATE");

        var sourceCodes=new HashMap<Long,String>();
        for(JsonNode pointType:srcTypes) {
            long id=id(pointType,"id");
            if(pointType.path("family_id").asLong()!=sourceFamily
                    || !pointType.path("code").isTextual()
                    || pointType.path("code").asText().isBlank()
                    || sourceCodes.putIfAbsent(id,pointType.path("code").asText())!=null)
                throw new SecurityException("EXCHANGE_REPLAY_SOURCE_CURRENCY_INVALID");
        }
        if(!sourceCodes.keySet().equals(Set.of(from,to))
                || !members.keySet().equals(Set.of(oldMember))
                || !currencies.keySet().equals(Set.of(from,to))
                || !rates.keySet().equals(Set.of(oldRate))
                || invalidValues(members) || invalidValues(currencies)
                || invalidValues(rates))
            throw new SecurityException("EXCHANGE_REPLAY_REMAP_INCOMPLETE");
        if(Objects.equals(currencies.get(from),currencies.get(to))
                || Objects.equals(sourceCodes.get(from),sourceCodes.get(to)))
            throw new SecurityException("EXCHANGE_REPLAY_CURRENCIES_NOT_DISTINCT");

        int fromAmount=intPositive(request,"from_amount");
        int toAmount=intPositive(request,"to_amount");
        int rateFrom=intPositive(request,"rate_from_amount");
        int rateTo=intPositive(request,"rate_to_amount");
        if(fromAmount%rateFrom!=0
                || Math.multiplyExact(fromAmount/rateFrom,rateTo)!=toAmount)
            throw new SecurityException("EXCHANGE_REPLAY_HISTORIC_RATIO_INVALID");
        var oldLegs=new TreeMap<Long,JsonNode>();
        boolean debit=false,credit=false;
        for(JsonNode leg:srcLedger) {
            long id=id(leg,"id");
            if(oldLegs.putIfAbsent(id,leg)!=null
                    || id(leg,"member_id")!=oldMember
                    || id(leg,"source_id")!=oldRequest
                    || !"POINT_EXCHANGE".equals(leg.path("source_type").asText())
                    || !leg.path("created_at").isTextual())
                throw new SecurityException("EXCHANGE_REPLAY_LEDGER_SOURCE_INVALID");
            long currency=id(leg,"point_type_id");
            long amount=leg.path("amount").asLong();
            String kind=leg.path("type").asText();
            if(currency==from && amount==-fromAmount && "SPEND".equals(kind) && !debit)
                debit=true;
            else if(currency==to && amount==toAmount && "EARN".equals(kind) && !credit)
                credit=true;
            else throw new SecurityException("EXCHANGE_REPLAY_LEDGER_ECONOMICS_INVALID");
        }
        if(!debit || !credit)throw new SecurityException("EXCHANGE_REPLAY_PAIR_INCOMPLETE");

        return tx.execute(status->{
            Long locked=targetDb.queryForObject(
                    "SELECT id FROM families WHERE id=? FOR UPDATE",Long.class,targetFamily);
            if(locked==null)throw new SecurityException("EXCHANGE_REPLAY_TARGET_NOT_FOUND");
            Long epoch=targetDb.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=?",Long.class,targetFamily);
            if(!Objects.equals(0L,epoch))
                throw new SecurityException("EXCHANGE_REPLAY_TARGET_EPOCH_STALE");
            Long requestsCount=targetDb.queryForObject("""
                    SELECT count(*) FROM point_exchange_requests r
                    JOIN family_members m ON m.id=r.member_id
                    WHERE m.family_id=?
                    """,Long.class,targetFamily);
            Long ledgerCount=targetDb.queryForObject("""
                    SELECT count(*) FROM point_transactions t
                    JOIN family_members m ON m.id=t.member_id
                    WHERE m.family_id=?
                    """,Long.class,targetFamily);
            if(!Objects.equals(0L,requestsCount)||!Objects.equals(0L,ledgerCount))
                throw new IllegalStateException("EXCHANGE_REPLAY_TARGET_NOT_EMPTY");
            Long targetMember=members.get(oldMember);
            Long targetRate=rates.get(oldRate);
            Long targetFrom=currencies.get(from),targetTo=currencies.get(to);
            if(!Objects.equals(1L,targetDb.queryForObject(
                    "SELECT count(*) FROM family_members WHERE id=? AND family_id=?",
                    Long.class,targetMember,targetFamily)))
                throw new SecurityException("EXCHANGE_REPLAY_FOREIGN_MEMBER");
            for(var currency:currencies.entrySet()) {
                var codes=targetDb.queryForList("""
                        SELECT code FROM point_types WHERE id=? AND family_id=? AND active=true
                        """,String.class,currency.getValue(),targetFamily);
                if(codes.size()!=1
                        || !Objects.equals(sourceCodes.get(currency.getKey()),codes.get(0)))
                    throw new SecurityException("EXCHANGE_REPLAY_CURRENCY_CODE_MISMATCH");
            }
            if(!Objects.equals(1L,targetDb.queryForObject("""
                    SELECT count(*) FROM point_exchange_rates
                    WHERE id=? AND family_id=? AND from_point_type_id=? AND to_point_type_id=?
                    """,Long.class,targetRate,targetFamily,targetFrom,targetTo)))
                throw new SecurityException("EXCHANGE_REPLAY_FOREIGN_RATE");
            Long newRequest=targetDb.queryForObject("""
                    INSERT INTO point_exchange_requests
                        (member_id,exchange_rate_id,from_point_type_id,to_point_type_id,
                         rate_from_amount,rate_to_amount,from_amount,to_amount,status,requested_at,processed_at)
                    VALUES (?,?,?,?,?,?,?,?,?,
                            CAST(? AS timestamp),CAST(? AS timestamp)) RETURNING id
                    """,Long.class,targetMember,targetRate,targetFrom,targetTo,
                    rateFrom,rateTo,fromAmount,toAmount,"APPROVED",
                    requiredTimestamp(request,"requested_at"),
                    optionalTimestamp(request,"processed_at"));
            if(newRequest==null || newRequest<=0)
                throw new IllegalStateException("EXCHANGE_REPLAY_REQUEST_NOT_INSERTED");
            var mapped=new TreeMap<Long,Long>();
            for(var entry:oldLegs.entrySet()) {
                var leg=entry.getValue();
                Long newId=targetDb.queryForObject("""
                        INSERT INTO point_transactions
                            (member_id,point_type_id,amount,created_at,
                             type,source_type,source_id,description)
                        VALUES (?,?,?,CAST(? AS timestamp),?,?,?,?) RETURNING id
                        """,Long.class,targetMember,
                        currencies.get(id(leg,"point_type_id")),leg.path("amount").asInt(),
                        requiredTimestamp(leg,"created_at"),leg.path("type").asText(),
                        "POINT_EXCHANGE",newRequest,
                        leg.hasNonNull("description")?leg.path("description").asText():null);
                if(newId==null || newId<=0)
                    throw new IllegalStateException("EXCHANGE_REPLAY_LEDGER_INSERT_FAILED");
                mapped.put(entry.getKey(),newId);
            }
            var restored=new DisposableExchangeLedgerSourceAuditor(targetDb)
                    .audit(targetFamily,newRequest);
            if(restored.debited()!=fromAmount || restored.credited()!=toAmount
                    || restored.memberId()!=targetMember)
                throw new IllegalStateException("EXCHANGE_REPLAY_RECONCILIATION_FAILED");
            Long debitTotal=targetDb.queryForObject("""
                    SELECT COALESCE(SUM(amount),0) FROM point_transactions
                    WHERE member_id=? AND point_type_id=?
                    """,Long.class,targetMember,targetFrom);
            Long creditTotal=targetDb.queryForObject("""
                    SELECT COALESCE(SUM(amount),0) FROM point_transactions
                    WHERE member_id=? AND point_type_id=?
                    """,Long.class,targetMember,targetTo);
            if(!Objects.equals(-((long)fromAmount),debitTotal)
                    || !Objects.equals((long)toAmount,creditTotal))
                throw new IllegalStateException("EXCHANGE_REPLAY_BALANCE_MISMATCH");
            return new Result(newRequest,Map.copyOf(mapped),
                    Map.of(targetFrom,debitTotal,targetTo,creditTotal),inspection.snapshotId());
        });
    }

    private static boolean invalidValues(Map<Long,Long> map) {
        return map.values().stream().anyMatch(v->v==null || v<=0)
                || new HashSet<>(map.values()).size()!=map.size();
    }
    private static long id(JsonNode row,String key) {
        if(!row.path(key).isIntegralNumber() || row.path(key).asLong()<=0)
            throw new SecurityException("EXCHANGE_REPLAY_INVALID_ID:"+key);
        return row.path(key).asLong();
    }
    private static int intPositive(JsonNode row,String key) {
        if(!row.path(key).canConvertToInt() || row.path(key).asInt()<=0)
            throw new SecurityException("EXCHANGE_REPLAY_INVALID_AMOUNT:"+key);
        return row.path(key).asInt();
    }
    private static String requiredTimestamp(JsonNode row,String key) {
        if(!row.path(key).isTextual())
            throw new SecurityException("EXCHANGE_REPLAY_BAD_TIMESTAMP:"+key);
        return row.path(key).asText();
    }
    private static String optionalTimestamp(JsonNode row,String key) {
        if(!row.hasNonNull(key))return null;
        return requiredTimestamp(row,key);
    }
}
