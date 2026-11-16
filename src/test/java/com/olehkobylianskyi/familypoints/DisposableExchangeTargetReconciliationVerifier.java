package com.olehkobylianskyi.familypoints;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;

/**
 * TEST ONLY, READ ONLY: proves independent source/target exchange requests,
 * explicit parent remaps and both archived ledger legs have equal economics.
 * It neither moves a ledger entry nor authorizes a production restore.
 */
final class DisposableExchangeTargetReconciliationVerifier {
    record Result(long sourceRequest,long targetRequest,long sourceMember,long targetMember,
                  String fromCode,String toCode,int debited,int credited) {}
    private final JdbcTemplate source;
    private final JdbcTemplate target;
    DisposableExchangeTargetReconciliationVerifier(JdbcTemplate source,JdbcTemplate target) {
        this.source=Objects.requireNonNull(source);
        this.target=Objects.requireNonNull(target);
    }

    Result verify(long sourceFamily,long targetFamily,long sourceRequest,long targetRequest,
            Map<Long,Long> memberIds,Map<Long,Long> pointTypeIds) {
        if(sourceFamily<=0 || targetFamily<=0 || sourceFamily==targetFamily
                || sourceRequest<=0 || targetRequest<=0
                || memberIds==null || pointTypeIds==null)
            throw new IllegalArgumentException("EXCHANGE_RECONCILIATION_INVALID_REQUEST");
        if(memberIds.values().stream().anyMatch(v->v==null || v<=0)
                || pointTypeIds.values().stream().anyMatch(v->v==null || v<=0)
                || new HashSet<>(memberIds.values()).size()!=memberIds.size()
                || new HashSet<>(pointTypeIds.values()).size()!=pointTypeIds.size())
            throw new SecurityException("EXCHANGE_RECONCILIATION_INVALID_REMAP");

        // Independent source and target databases are supplied by the fixture.
        // Each source graph is checked before any identity comparison.
        var a=new DisposableExchangeLedgerSourceAuditor(source)
                .audit(sourceFamily,sourceRequest);
        var b=new DisposableExchangeLedgerSourceAuditor(target)
                .audit(targetFamily,targetRequest);

        if(!memberIds.equals(Map.of(a.memberId(),b.memberId()))
                || !pointTypeIds.equals(Map.of(a.fromType(),b.fromType(),
                                                a.toType(),b.toType())))
            throw new SecurityException("EXCHANGE_RECONCILIATION_REMAP_MISMATCH");
        if(a.debited()!=b.debited() || a.credited()!=b.credited())
            throw new SecurityException("EXCHANGE_RECONCILIATION_AMOUNT_MISMATCH");

        var sourceCodes=readCurrencyCodes(source,sourceFamily,a.fromType(),a.toType());
        var targetCodes=readCurrencyCodes(target,targetFamily,b.fromType(),b.toType());
        if(!sourceCodes.equals(targetCodes))
            throw new SecurityException("EXCHANGE_RECONCILIATION_CURRENCY_CODE_MISMATCH");

        // The original request's historical numerator/denominator are immutable
        // economic facts; mutable current rate rows need not match past terms.
        List<Integer> srcSnapshot=snapshot(source,sourceRequest);
        List<Integer> dstSnapshot=snapshot(target,targetRequest);
        if(!srcSnapshot.equals(dstSnapshot))
            throw new SecurityException("EXCHANGE_RECONCILIATION_RATE_SNAPSHOT_MISMATCH");

        return new Result(sourceRequest,targetRequest,a.memberId(),b.memberId(),
                sourceCodes.get(0),sourceCodes.get(1),a.debited(),a.credited());
    }

    private static List<String> readCurrencyCodes(JdbcTemplate db,long family,
            long from,long to) {
        String fromCode=db.queryForObject(
                "SELECT code FROM point_types WHERE id=? AND family_id=?",
                String.class,from,family);
        String toCode=db.queryForObject(
                "SELECT code FROM point_types WHERE id=? AND family_id=?",
                String.class,to,family);
        if(fromCode==null || toCode==null || fromCode.isBlank() || toCode.isBlank()
                || fromCode.equals(toCode))
            throw new SecurityException("EXCHANGE_RECONCILIATION_CURRENCY_INVALID");
        return List.of(fromCode,toCode);
    }

    private static List<Integer> snapshot(JdbcTemplate db,long requestId) {
        return db.queryForObject("""
                SELECT rate_from_amount,rate_to_amount FROM point_exchange_requests WHERE id=?
                """,(rs,n)->List.of(rs.getInt(1),rs.getInt(2)),requestId);
    }
}
