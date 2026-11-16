package com.olehkobylianskyi.familypoints;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;

/**
 * Read-only source database audit of a completed POINT_EXCHANGE pair.
 * Not portable, not a native restore executor, never writes to PostgreSQL.
 */
final class DisposableExchangeLedgerSourceAuditor {
    record Pair(long requestId,long memberId,long fromType,long toType,
                int debited,int credited) {}
    private final JdbcTemplate db;
    DisposableExchangeLedgerSourceAuditor(JdbcTemplate db) {this.db=db;}

    Pair audit(long family,long requestId) {
        if(family<=0 || requestId<=0)
            throw new IllegalArgumentException("EXCHANGE_AUDIT_INVALID_SCOPE");
        var requests=db.queryForList("""
                SELECT r.member_id, r.from_point_type_id, r.to_point_type_id,
                       r.from_amount, r.to_amount, r.rate_from_amount, r.rate_to_amount, r.status,
                       fm.family_id AS member_family, rate.family_id AS rate_family,
                       from_t.family_id AS from_family, to_t.family_id AS to_family
                  FROM point_exchange_requests r
                  JOIN family_members fm ON fm.id=r.member_id
                  JOIN point_exchange_rates rate ON rate.id=r.exchange_rate_id
                  JOIN point_types from_t ON from_t.id=r.from_point_type_id
                  JOIN point_types to_t ON to_t.id=r.to_point_type_id
                 WHERE r.id=? AND fm.family_id=?
                """,requestId,family);
        if(requests.size()!=1)
            throw new SecurityException("EXCHANGE_SOURCE_REQUEST_NOT_OWNED");
        var row=requests.get(0);
        long member=((Number)row.get("member_id")).longValue();
        long from=((Number)row.get("from_point_type_id")).longValue();
        long to=((Number)row.get("to_point_type_id")).longValue();
        int debit=((Number)row.get("from_amount")).intValue();
        int credit=((Number)row.get("to_amount")).intValue();
        int historicalRateFrom=((Number)row.get("rate_from_amount")).intValue();
        int historicalRateTo=((Number)row.get("rate_to_amount")).intValue();
        boolean historicalRatioValid=false;
        if(debit>0 && credit>0 && historicalRateFrom>0
                && historicalRateTo>0 && debit%historicalRateFrom==0) {
            try {
                historicalRatioValid=Math.multiplyExact(debit/historicalRateFrom,
                        historicalRateTo)==credit;
            }catch(ArithmeticException overflow) {
                throw new SecurityException("EXCHANGE_SOURCE_HISTORIC_RATE_OVERFLOW",overflow);
            }
        }
        if(!historicalRatioValid)
            throw new SecurityException("EXCHANGE_SOURCE_HISTORIC_RATE_MISMATCH");
        if(!Objects.equals("APPROVED",row.get("status"))
                || from==to || debit<=0 || credit<=0
                || ((Number)row.get("member_family")).longValue()!=family
                || ((Number)row.get("rate_family")).longValue()!=family
                || ((Number)row.get("from_family")).longValue()!=family
                || ((Number)row.get("to_family")).longValue()!=family)
            throw new SecurityException("EXCHANGE_SOURCE_GRAPH_INVALID");

        var transactions=db.queryForList("""
                SELECT t.id,t.member_id,t.point_type_id,t.type,t.amount
                  FROM point_transactions t
                 WHERE t.source_type='POINT_EXCHANGE' AND t.source_id=?
                 ORDER BY t.id
                """,requestId);
        if(transactions.size()!=2)
            throw new SecurityException("EXCHANGE_SOURCE_PAIR_INCOMPLETE");
        boolean debitFound=false,creditFound=false;
        var ids=new HashSet<Long>();
        for(var entry:transactions) {
            long id=((Number)entry.get("id")).longValue();
            long owner=((Number)entry.get("member_id")).longValue();
            long currency=((Number)entry.get("point_type_id")).longValue();
            int amount=((Number)entry.get("amount")).intValue();
            String type=(String)entry.get("type");
            if(!ids.add(id) || owner!=member)
                throw new SecurityException("EXCHANGE_SOURCE_PAIR_OWNER_MISMATCH");
            if(currency==from && amount==-debit && "SPEND".equals(type) && !debitFound)
                debitFound=true;
            else if(currency==to && amount==credit && "EARN".equals(type) && !creditFound)
                creditFound=true;
            else throw new SecurityException("EXCHANGE_SOURCE_PAIR_ECONOMICS_MISMATCH");
        }
        if(!debitFound || !creditFound)
            throw new SecurityException("EXCHANGE_SOURCE_PAIR_INCOMPLETE");
        return new Pair(requestId,member,from,to,debit,credit);
    }
}
