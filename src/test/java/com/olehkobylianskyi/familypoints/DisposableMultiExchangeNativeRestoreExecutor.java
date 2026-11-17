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
 * TEST-ONLY bounded multi-request native exchange replay in disposable PG.
 * No production bean or HTTP endpoint. The caller must have reconstructed
 * member identities, point types and rates in an independent test target.
 */
final class DisposableMultiExchangeNativeRestoreExecutor {
    record Result(Map<Long,Long> requests,Map<Long,Long> transactions,
                  Map<String,Long> balances,UUID snapshotId) {}
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final ProtectiveBackupImportInspector inspector;
    private final ProtectiveBackupArtifactCodec codec;
    private final ProtectiveBackupPortableFile portable;
    private final ObjectMapper json=new ObjectMapper();

    DisposableMultiExchangeNativeRestoreExecutor(JdbcTemplate db,PlatformTransactionManager manager,
            ProtectiveBackupImportInspector inspector,ProtectiveBackupArtifactCodec codec,
            ProtectiveBackupPortableFile portable) {
        this.db=db;this.tx=new TransactionTemplate(manager);
        this.inspector=inspector;this.codec=codec;this.portable=portable;
    }

    Result apply(byte[] archive,long source,long target,Map<Long,Long> members,
            Map<Long,Long> currencies,Map<Long,Long> rates,SecretKey key) {
        if(archive==null || source<=0 || target<=0 || key==null
                || members==null || currencies==null || rates==null)
            throw new IllegalArgumentException("MULTI_EXCHANGE_INVALID_REQUEST");
        var inspected=inspector.inspect(archive,source,key);
        if(!"V50".equals(inspected.schemaVersion()) ||
                !inspected.availableCategories().equals(List.of(
                    "POINT_EXCHANGE_RATES","POINT_EXCHANGE_REQUESTS","POINT_LEDGER","POINT_TYPES")))
            throw new SecurityException("MULTI_EXCHANGE_UNSUPPORTED_ARCHIVE");
        byte[] clear=codec.open(portable.decode(archive),inspected.snapshotId(),source,key);
        final JsonNode categories;
        try {categories=json.readTree(clear).path("categories");}
        catch(java.io.IOException ex){throw new IllegalArgumentException("MULTI_EXCHANGE_BAD_ARCHIVE",ex);}
        finally {Arrays.fill(clear,(byte)0);}
        var types=categories.path("POINT_TYPES");
        var sourceRates=categories.path("POINT_EXCHANGE_RATES");
        var sourceRequests=categories.path("POINT_EXCHANGE_REQUESTS");
        var ledger=categories.path("POINT_LEDGER");
        if(!types.isArray() || !sourceRates.isArray() || !sourceRequests.isArray()
                || !ledger.isArray() || sourceRequests.isEmpty()
                || sourceRequests.size()>20 || ledger.size()!=2*sourceRequests.size())
            throw new SecurityException("MULTI_EXCHANGE_BOUNDED_SCOPE");

        var codes=new TreeMap<Long,String>();
        for(JsonNode type:types) {
            long id=id(type,"id");
            if(id(type,"family_id")!=source || !type.path("code").isTextual()
                    || type.path("code").asText().isBlank()
                    || codes.putIfAbsent(id,type.path("code").asText())!=null)
                throw new SecurityException("MULTI_EXCHANGE_SOURCE_CURRENCY");
        }
        var oldRates=new TreeMap<Long,JsonNode>();
        for(JsonNode rate:sourceRates) {
            long id=id(rate,"id");
            if(id(rate,"family_id")!=source
                    || !codes.containsKey(id(rate,"from_point_type_id"))
                    || !codes.containsKey(id(rate,"to_point_type_id"))
                    || oldRates.putIfAbsent(id,rate)!=null)
                throw new SecurityException("MULTI_EXCHANGE_SOURCE_RATE");
        }
        var oldRequests=new TreeMap<Long,JsonNode>();
        var oldMemberIds=new HashSet<Long>();
        var usedRates=new HashSet<Long>();
        var usedCurrencies=new HashSet<Long>();
        for(JsonNode request:sourceRequests) {
            long oldId=id(request,"id");
            long member=id(request,"member_id");
            long rateId=id(request,"exchange_rate_id");
            long from=id(request,"from_point_type_id");
            long to=id(request,"to_point_type_id");
            var rate=oldRates.get(rateId);
            if(rate==null || from==to || !codes.containsKey(from) || !codes.containsKey(to)
                    || !"APPROVED".equals(request.path("status").asText())
                    || request.hasNonNull("processed_by_member_id")
                    || id(rate,"from_point_type_id")!=from
                    || id(rate,"to_point_type_id")!=to
                    || oldRequests.putIfAbsent(oldId,request)!=null)
                throw new SecurityException("MULTI_EXCHANGE_SOURCE_REQUEST");
            int fromValue=positive(request,"from_amount");
            int toValue=positive(request,"to_amount");
            int denominator=positive(request,"rate_from_amount");
            int numerator=positive(request,"rate_to_amount");
            if(fromValue%denominator!=0
                    || Math.multiplyExact(fromValue/denominator,numerator)!=toValue)
                throw new SecurityException("MULTI_EXCHANGE_HISTORIC_RATE");
            oldMemberIds.add(member);
            usedRates.add(rateId);
            usedCurrencies.add(from);
            usedCurrencies.add(to);
        }
        var oldTransactions=new TreeMap<Long,JsonNode>();
        var requestsToLegs=new HashMap<Long,List<JsonNode>>();
        for(JsonNode leg:ledger) {
            long oldId=id(leg,"id");
            long requestId=id(leg,"source_id");
            if(!oldRequests.containsKey(requestId)
                    || !"POINT_EXCHANGE".equals(leg.path("source_type").asText())
                    || !leg.path("created_at").isTextual()
                    || oldTransactions.putIfAbsent(oldId,leg)!=null)
                throw new SecurityException("MULTI_EXCHANGE_SOURCE_LEDGER");
            requestsToLegs.computeIfAbsent(requestId,unused->new ArrayList<>()).add(leg);
        }
        for(var entry:oldRequests.entrySet()) {
            JsonNode request=entry.getValue();
            var legs=requestsToLegs.get(entry.getKey());
            if(legs==null || legs.size()!=2)
                throw new SecurityException("MULTI_EXCHANGE_INCOMPLETE_PAIR");
            int debit=0,credit=0;
            for(JsonNode leg:legs) {
                if(id(leg,"member_id")!=id(request,"member_id"))
                    throw new SecurityException("MULTI_EXCHANGE_MEMBER_MISMATCH");
                if(id(leg,"point_type_id")==id(request,"from_point_type_id")
                        && leg.path("amount").asLong()==-positive(request,"from_amount")
                        && "SPEND".equals(leg.path("type").asText()))debit++;
                else if(id(leg,"point_type_id")==id(request,"to_point_type_id")
                        && leg.path("amount").asLong()==positive(request,"to_amount")
                        && "EARN".equals(leg.path("type").asText()))credit++;
                else throw new SecurityException("MULTI_EXCHANGE_PAIR_ECONOMICS");
            }
            if(debit!=1 || credit!=1)
                throw new SecurityException("MULTI_EXCHANGE_DUPLICATE_LEG");
        }
        exactMap(members,oldMemberIds,"MEMBER");
        exactMap(currencies,usedCurrencies,"CURRENCY");
        exactMap(rates,usedRates,"RATE");

        return tx.execute(status->{
            Long locked=db.queryForObject("SELECT id FROM families WHERE id=? FOR UPDATE",
                    Long.class,target);
            if(locked==null)throw new SecurityException("MULTI_EXCHANGE_TARGET_MISSING");
            Long epoch=db.queryForObject("SELECT data_epoch FROM families WHERE id=?",
                    Long.class,target);
            if(!Objects.equals(0L,epoch))
                throw new SecurityException("MULTI_EXCHANGE_TARGET_EPOCH");
            Long oldHistory=db.queryForObject("""
                    SELECT count(*) FROM point_transactions t
                    JOIN family_members m ON m.id=t.member_id WHERE m.family_id=?
                    """,Long.class,target);
            Long oldExchange=db.queryForObject("""
                    SELECT count(*) FROM point_exchange_requests e
                    JOIN family_members m ON m.id=e.member_id WHERE m.family_id=?
                    """,Long.class,target);
            if(!Objects.equals(0L,oldHistory)||!Objects.equals(0L,oldExchange))
                throw new IllegalStateException("MULTI_EXCHANGE_TARGET_NOT_EMPTY");
            for(Long member:members.values()) {
                if(!Objects.equals(1L,db.queryForObject(
                        "SELECT count(*) FROM family_members WHERE id=? AND family_id=?",
                        Long.class,member,target)))
                    throw new SecurityException("MULTI_EXCHANGE_TARGET_FOREIGN_MEMBER");
            }
            for(var currency:currencies.entrySet()) {
                var matched=db.queryForList("""
                        SELECT code FROM point_types WHERE id=? AND family_id=? AND active=true
                        """,String.class,currency.getValue(),target);
                if(matched.size()!=1 || !Objects.equals(matched.get(0),codes.get(currency.getKey())))
                    throw new SecurityException("MULTI_EXCHANGE_TARGET_CURRENCY_MISMATCH");
            }
            for(var rate:rates.entrySet()) {
                var original=oldRates.get(rate.getKey());
                if(!Objects.equals(1L,db.queryForObject("""
                        SELECT count(*) FROM point_exchange_rates
                        WHERE id=? AND family_id=? AND from_point_type_id=? AND to_point_type_id=?
                        """,Long.class,rate.getValue(),target,
                        currencies.get(id(original,"from_point_type_id")),
                        currencies.get(id(original,"to_point_type_id")))))
                    throw new SecurityException("MULTI_EXCHANGE_TARGET_FOREIGN_RATE");
            }
            var requestMap=new TreeMap<Long,Long>();
            var rowMap=new TreeMap<Long,Long>();
            var expected=new TreeMap<String,Long>();
            for(var requestEntry:oldRequests.entrySet()) {
                var request=requestEntry.getValue();
                long member=members.get(id(request,"member_id"));
                Long inserted=db.queryForObject("""
                        INSERT INTO point_exchange_requests(
                            member_id,exchange_rate_id,from_point_type_id,to_point_type_id,
                            rate_from_amount,rate_to_amount,from_amount,to_amount,
                            status,requested_at,processed_at)
                        VALUES (?,?,?,?,?,?,?,?,?,CAST(? AS timestamp),CAST(? AS timestamp))
                        RETURNING id
                        """,Long.class,member,rates.get(id(request,"exchange_rate_id")),
                        currencies.get(id(request,"from_point_type_id")),
                        currencies.get(id(request,"to_point_type_id")),
                        positive(request,"rate_from_amount"),positive(request,"rate_to_amount"),
                        positive(request,"from_amount"),positive(request,"to_amount"),
                        "APPROVED",timestamp(request,"requested_at"),
                        request.hasNonNull("processed_at")?timestamp(request,"processed_at"):null);
                if(inserted==null || inserted<=0)
                    throw new IllegalStateException("MULTI_EXCHANGE_REQUEST_FAILED");
                requestMap.put(requestEntry.getKey(),inserted);
                for(var leg:requestsToLegs.get(requestEntry.getKey())) {
                    long pointType=currencies.get(id(leg,"point_type_id"));
                    long signed=leg.path("amount").asLong();
                    Long insertedLeg=db.queryForObject("""
                            INSERT INTO point_transactions(
                                member_id,point_type_id,amount,created_at,type,
                                source_type,source_id,description)
                            VALUES (?,?,?,CAST(? AS timestamp),?,?,?,?) RETURNING id
                            """,Long.class,member,pointType,(int)signed,
                            timestamp(leg,"created_at"),leg.path("type").asText(),
                            "POINT_EXCHANGE",inserted,
                            leg.hasNonNull("description")?leg.path("description").asText():null);
                    if(insertedLeg==null || insertedLeg<=0)
                        throw new IllegalStateException("MULTI_EXCHANGE_LEG_FAILED");
                    rowMap.put(id(leg,"id"),insertedLeg);
                    expected.merge(member+":"+pointType,signed,Math::addExact);
                }
                var verified=new DisposableExchangeLedgerSourceAuditor(db)
                        .audit(target,inserted);
                if(verified.memberId()!=member
                        || verified.debited()!=positive(request,"from_amount")
                        || verified.credited()!=positive(request,"to_amount"))
                    throw new IllegalStateException("MULTI_EXCHANGE_REQUEST_RECONCILIATION");
            }
            for(var entry:expected.entrySet()) {
                String[] parts=entry.getKey().split(":");
                Long sum=db.queryForObject("""
                        SELECT COALESCE(SUM(amount),0) FROM point_transactions
                        WHERE member_id=? AND point_type_id=?
                        """,Long.class,Long.parseLong(parts[0]),Long.parseLong(parts[1]));
                if(!Objects.equals(entry.getValue(),sum))
                    throw new IllegalStateException("MULTI_EXCHANGE_BALANCE_MISMATCH");
            }
            return new Result(Map.copyOf(requestMap),Map.copyOf(rowMap),
                    Map.copyOf(expected),inspected.snapshotId());
        });
    }

    private static long id(JsonNode row,String field) {
        if(!row.path(field).isIntegralNumber() || row.path(field).asLong()<=0)
            throw new SecurityException("MULTI_EXCHANGE_ID_INVALID:"+field);
        return row.path(field).asLong();
    }
    private static int positive(JsonNode row,String field) {
        if(!row.path(field).canConvertToInt() || row.path(field).asInt()<=0)
            throw new SecurityException("MULTI_EXCHANGE_AMOUNT_INVALID:"+field);
        return row.path(field).asInt();
    }
    private static String timestamp(JsonNode row,String field) {
        if(!row.path(field).isTextual())throw new SecurityException("MULTI_EXCHANGE_BAD_TIMESTAMP");
        return row.path(field).asText();
    }
    private static void exactMap(Map<Long,Long> map,Set<Long> required,String field) {
        if(!map.keySet().equals(required) ||
                map.values().stream().anyMatch(v->v==null || v<=0)
                || new HashSet<>(map.values()).size()!=map.size())
            throw new SecurityException("MULTI_EXCHANGE_"+field+"_REMAP_INVALID");
    }
}
