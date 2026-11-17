package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olehkobylianskyi.familypoints.service.*;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * TEST ONLY. Read-only, row-by-row comparison of authenticated source exchange
 * history with a reconstructed disposable PostgreSQL target. Neither matching
 * balances nor matching request counts are sufficient proof of history.
 */
final class DisposableExchangeHistoryIntegrityVerifier {
    record Result(String sourceSha256,String targetSha256,int requests,int transactions,
                  Map<String,Long> balanceDeltas) {}
    private final JdbcTemplate db;
    private final ProtectiveBackupImportInspector inspector;
    private final ProtectiveBackupPortableFile portable;
    private final ProtectiveBackupArtifactCodec codec;
    private final ObjectMapper json=new ObjectMapper();

    DisposableExchangeHistoryIntegrityVerifier(JdbcTemplate db,
            ProtectiveBackupImportInspector inspector,
            ProtectiveBackupPortableFile portable,ProtectiveBackupArtifactCodec codec) {
        this.db=db;this.inspector=inspector;this.portable=portable;this.codec=codec;
    }

    Result verify(byte[] archive,long source,long target,Map<Long,Long> memberMap,
            Map<Long,Long> currencyMap,Map<Long,Long> rateMap,
            Map<Long,Long> requestMap,Map<Long,Long> ledgerMap,SecretKey key) {
        if(archive==null || source<=0 || target<=0 || key==null
                || memberMap==null || currencyMap==null || rateMap==null
                || requestMap==null || ledgerMap==null)
            throw new IllegalArgumentException("EXCHANGE_HISTORY_INVALID_REQUEST");
        var inspected=inspector.inspect(archive,source,key);
        if(!"V50".equals(inspected.schemaVersion())
                || !inspected.availableCategories().equals(List.of(
                    "POINT_EXCHANGE_RATES","POINT_EXCHANGE_REQUESTS",
                    "POINT_LEDGER","POINT_TYPES")))
            throw new SecurityException("EXCHANGE_HISTORY_BAD_SCOPE");
        byte[] raw=codec.open(portable.decode(archive),inspected.snapshotId(),source,key);
        final JsonNode categories;
        try {categories=json.readTree(raw).path("categories");}
        catch(java.io.IOException e){throw new IllegalArgumentException("EXCHANGE_HISTORY_BAD_JSON",e);}
        finally {Arrays.fill(raw,(byte)0);}

        var requests=categories.path("POINT_EXCHANGE_REQUESTS");
        var legs=categories.path("POINT_LEDGER");
        if(!requests.isArray() || !legs.isArray() || requests.isEmpty()
                || legs.size()!=requests.size()*2)
            throw new SecurityException("EXCHANGE_HISTORY_INCOMPLETE_SOURCE");
        var originalRequestIds=new HashSet<Long>();
        var originalLedgerIds=new HashSet<Long>();
        var originalMemberIds=new HashSet<Long>();
        var originalCurrencyIds=new HashSet<Long>();
        var originalRateIds=new HashSet<Long>();
        var ordered=new TreeMap<String,String>();
        var targetOrdered=new TreeMap<String,String>();
        for(JsonNode request:requests) {
            long id=positive(request,"id");
            if(!originalRequestIds.add(id))
                throw new SecurityException("EXCHANGE_HISTORY_DUPLICATE_REQUEST");
            originalMemberIds.add(positive(request,"member_id"));
            originalRateIds.add(positive(request,"exchange_rate_id"));
            originalCurrencyIds.add(positive(request,"from_point_type_id"));
            originalCurrencyIds.add(positive(request,"to_point_type_id"));
        }
        for(JsonNode leg:legs) {
            long id=positive(leg,"id");
            if(!originalLedgerIds.add(id))
                throw new SecurityException("EXCHANGE_HISTORY_DUPLICATE_LEDGER");
            if(!originalRequestIds.contains(positive(leg,"source_id")))
                throw new SecurityException("EXCHANGE_HISTORY_ORPHAN_LEDGER");
            originalMemberIds.add(positive(leg,"member_id"));
            originalCurrencyIds.add(positive(leg,"point_type_id"));
        }
        exactMap(memberMap,originalMemberIds,"MEMBER");
        exactMap(currencyMap,originalCurrencyIds,"CURRENCY");
        exactMap(rateMap,originalRateIds,"RATE");
        exactMap(requestMap,originalRequestIds,"REQUEST");
        exactMap(ledgerMap,originalLedgerIds,"LEDGER");

        // Mapped numeric IDs alone cannot prove currency identity or exchange
        // rate ownership. Re-bind signed source catalog to exact target rows.
        var originalTypes=new HashMap<Long,JsonNode>();
        for(JsonNode pointType:categories.path("POINT_TYPES"))
            originalTypes.put(positive(pointType,"id"),pointType);
        for(var mapping:currencyMap.entrySet()) {
            var sourceType=originalTypes.get(mapping.getKey());
            if(sourceType==null || positive(sourceType,"family_id")!=source
                    || !sourceType.path("code").isTextual()
                    || sourceType.path("code").asText().isBlank())
                throw new SecurityException("EXCHANGE_HISTORY_SOURCE_CURRENCY_MISSING");
            var targetCodes=db.queryForList("""
                    SELECT code FROM point_types WHERE id=? AND family_id=? AND active=true
                    """,String.class,mapping.getValue(),target);
            if(targetCodes.size()!=1
                    || !sourceType.path("code").asText().equals(targetCodes.get(0)))
                throw new SecurityException("EXCHANGE_HISTORY_TARGET_CURRENCY_DRIFT");
        }
        var originalRates=new HashMap<Long,JsonNode>();
        for(JsonNode rate:categories.path("POINT_EXCHANGE_RATES"))
            originalRates.put(positive(rate,"id"),rate);
        for(var mapping:rateMap.entrySet()) {
            var sourceRate=originalRates.get(mapping.getKey());
            if(sourceRate==null || positive(sourceRate,"family_id")!=source
                    || !currencyMap.containsKey(positive(sourceRate,"from_point_type_id"))
                    || !currencyMap.containsKey(positive(sourceRate,"to_point_type_id")))
                throw new SecurityException("EXCHANGE_HISTORY_SOURCE_RATE_MISSING");
            Long owned=db.queryForObject("""
                    SELECT count(*) FROM point_exchange_rates
                    WHERE id=? AND family_id=? AND from_point_type_id=? AND to_point_type_id=?
                    """,Long.class,mapping.getValue(),target,
                    currencyMap.get(positive(sourceRate,"from_point_type_id")),
                    currencyMap.get(positive(sourceRate,"to_point_type_id")));
            if(!Objects.equals(1L,owned))
                throw new SecurityException("EXCHANGE_HISTORY_TARGET_RATE_DRIFT");
        }
        // Compare stable economic and timestamp payload fields, never row ID
        // numerals: independent DBs may reuse the same sequence values.
        for(JsonNode request:requests) {
            long old=positive(request,"id"),newId=requestMap.get(old);
            var targetRows=db.queryForList("""
                    SELECT to_jsonb(t)::text FROM point_exchange_requests t
                    WHERE t.id=? AND EXISTS(
                      SELECT 1 FROM family_members m
                      WHERE m.id=t.member_id AND m.family_id=?)
                    """,String.class,newId,target);
            if(targetRows.size()!=1)
                throw new SecurityException("EXCHANGE_HISTORY_TARGET_REQUEST_MISSING");
            JsonNode row=parse(targetRows.get(0));
            String expected=requestSignature(request,memberMap.get(positive(request,"member_id")),
                    rateMap.get(positive(request,"exchange_rate_id")),
                    currencyMap.get(positive(request,"from_point_type_id")),
                    currencyMap.get(positive(request,"to_point_type_id")));
            String actual=requestSignature(row,row.path("member_id").asLong(),
                    row.path("exchange_rate_id").asLong(),
                    row.path("from_point_type_id").asLong(),
                    row.path("to_point_type_id").asLong());
            if(!Objects.equals(expected,actual))
                throw new SecurityException("EXCHANGE_HISTORY_REQUEST_PAYLOAD_DRIFT");
            ordered.put("R:"+old,expected);
            targetOrdered.put("R:"+old,actual);
        }
        var balance=new TreeMap<String,Long>();
        for(JsonNode leg:legs) {
            long old=positive(leg,"id"),newId=ledgerMap.get(old);
            var targetRows=db.queryForList("""
                    SELECT to_jsonb(t)::text FROM point_transactions t
                    JOIN family_members m ON m.id=t.member_id
                    WHERE t.id=? AND m.family_id=?
                    """,String.class,newId,target);
            if(targetRows.size()!=1)
                throw new SecurityException("EXCHANGE_HISTORY_TARGET_LEDGER_MISSING");
            JsonNode row=parse(targetRows.get(0));
            long member=memberMap.get(positive(leg,"member_id"));
            long currency=currencyMap.get(positive(leg,"point_type_id"));
            long request=requestMap.get(positive(leg,"source_id"));
            String expected=ledgerSignature(leg,member,currency,request);
            String actual=ledgerSignature(row,row.path("member_id").asLong(),
                    row.path("point_type_id").asLong(),row.path("source_id").asLong());
            if(!Objects.equals(expected,actual))
                throw new SecurityException("EXCHANGE_HISTORY_LEDGER_PAYLOAD_DRIFT");
            ordered.put("L:"+old,expected);
            targetOrdered.put("L:"+old,actual);
            balance.merge(member+":"+currency,leg.path("amount").asLong(),Math::addExact);
        }
        // A correct mapped subset can conceal extra unrelated history:
        // this bounded fresh-target executor requires *exactly* those rows.
        Long requestCount=db.queryForObject("""
                SELECT count(*) FROM point_exchange_requests t
                JOIN family_members m ON m.id=t.member_id WHERE m.family_id=?
                """,Long.class,target);
        Long ledgerCount=db.queryForObject("""
                SELECT count(*) FROM point_transactions t
                JOIN family_members m ON m.id=t.member_id WHERE m.family_id=?
                """,Long.class,target);
        if(!Objects.equals((long)requests.size(),requestCount)
                || !Objects.equals((long)legs.size(),ledgerCount))
            throw new SecurityException("EXCHANGE_HISTORY_EXTRA_TARGET_ROWS");
        for(var pair:balance.entrySet()) {
            String[] fields=pair.getKey().split(":");
            Long sum=db.queryForObject("""
                    SELECT COALESCE(sum(amount),0) FROM point_transactions
                    WHERE member_id=? AND point_type_id=?
                    """,Long.class,Long.parseLong(fields[0]),Long.parseLong(fields[1]));
            if(!Objects.equals(sum,pair.getValue()))
                throw new SecurityException("EXCHANGE_HISTORY_NET_BALANCE_DRIFT");
        }
        var sourceDigest=sha256(ordered);
        var targetDigest=sha256(targetOrdered);
        if(!sourceDigest.equals(targetDigest))
            throw new SecurityException("EXCHANGE_HISTORY_SHA256_DRIFT");
        return new Result(sourceDigest,targetDigest,requests.size(),legs.size(),Map.copyOf(balance));
    }

    private JsonNode parse(String value) {
        try{return json.readTree(value);}
        catch(java.io.IOException e){throw new IllegalArgumentException("EXCHANGE_HISTORY_TARGET_JSON",e);}
    }
    private static void exactMap(Map<Long,Long> map,Set<Long> keys,String type) {
        if(!map.keySet().equals(keys)
                || map.values().stream().anyMatch(v->v==null || v<=0)
                || new HashSet<>(map.values()).size()!=map.size())
            throw new SecurityException("EXCHANGE_HISTORY_"+type+"_MAP_INVALID");
    }
    private static long positive(JsonNode row,String key) {
        if(!row.path(key).isIntegralNumber() || row.path(key).asLong()<=0)
            throw new SecurityException("EXCHANGE_HISTORY_BAD_ID:"+key);
        return row.path(key).asLong();
    }
    private static String requestSignature(JsonNode r,long member,long rate,long from,long to) {
        return fields(Long.toString(member),Long.toString(rate),
                Long.toString(from),Long.toString(to),
                r.path("status").asText(),r.path("rate_from_amount").asText(),
                r.path("rate_to_amount").asText(),r.path("from_amount").asText(),
                r.path("to_amount").asText(),normalizedTime(r,"requested_at"),
                normalizedTime(r,"processed_at"));
    }
    private static String ledgerSignature(JsonNode r,long member,long currency,long sourceId) {
        return fields(Long.toString(member),Long.toString(currency),
                Long.toString(sourceId),r.path("source_type").asText(),
                r.path("type").asText(),r.path("amount").asText(),
                normalizedTime(r,"created_at"),
                r.path("description").isNull()?"":r.path("description").asText(""));
    }
    private static String fields(String... values) {
        var result=new StringBuilder();
        for(String value:values)result.append(value.length()).append(':').append(value);
        return result.toString();
    }
    private static String normalizedTime(JsonNode r,String field) {
        if(!r.hasNonNull(field))return "";
        try {
            return LocalDateTime.parse(r.path(field).asText().replace(' ','T')).toString();
        }catch(java.time.format.DateTimeParseException ex) {
            throw new SecurityException("EXCHANGE_HISTORY_INVALID_TIMESTAMP:"+field,ex);
        }
    }
    private static String sha256(Map<String,String> semanticRows) {
        try {
            var digest=MessageDigest.getInstance("SHA-256");
            for(var row:semanticRows.entrySet()) {
                byte[] bytes=(row.getKey()+"\n"+row.getValue()+"\n")
                        .getBytes(StandardCharsets.UTF_8);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        }catch(java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
