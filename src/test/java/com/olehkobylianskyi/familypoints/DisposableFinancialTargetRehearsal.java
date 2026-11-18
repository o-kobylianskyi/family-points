package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;

/**
 * Backend B / B-B-04. TEST ONLY. Simulates an explicitly mapped subset of
 * TASK/REWARD/PURCHASE/POINT_LEDGER into QA-only PostgreSQL tables with real
 * FK constraints and a single rollback boundary. Does NOT write any real
 * production point_transactions, task_instances, or reward_purchases.
 * Source JSON authentication and owner authority must be provided by A/C.
 */
final class DisposableFinancialTargetRehearsal {
    record Result(int definitions,int rewards,int purchases,int ledgerRows,
                  Map<String,Long> balances) {}

    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final DisposableRestoreFinancialDependencyPreflight preflight =
            new DisposableRestoreFinancialDependencyPreflight();

    DisposableFinancialTargetRehearsal(JdbcTemplate db,PlatformTransactionManager manager) {
        this.db=Objects.requireNonNull(db);
        this.tx=new TransactionTemplate(Objects.requireNonNull(manager));
    }

    Result apply(JsonNode source,long sourceFamily,long targetFamily,
            DisposableRestoreFinancialDependencyPreflight.Mapping maps,
            int failAfterStep) {
        var checked=preflight.verify(source,sourceFamily,maps);
        return tx.execute(status->{
            var epochs=db.queryForList("SELECT data_epoch FROM families WHERE id=? FOR UPDATE",
                    Long.class,targetFamily);
            if(epochs.size()!=1 || !Objects.equals(0L,epochs.get(0)))
                throw new SecurityException("QA_FINANCE_TARGET_EPOCH");
            for(Long member:maps.members().values()) {
                if(!Objects.equals(1L,db.queryForObject(
                        "SELECT count(*) FROM family_members WHERE id=? AND family_id=?",
                        Long.class,member,targetFamily)))
                    throw new SecurityException("QA_FINANCE_FOREIGN_MEMBER");
            }
            for(JsonNode type:source.path("POINT_TYPES")) {
                long old=id(type,"id");
                var code=db.queryForList(
                        "SELECT code FROM point_types WHERE id=? AND family_id=? AND active=true",
                        String.class,maps.currencies().get(old),targetFamily);
                if(code.size()!=1 || !code.get(0).equals(type.path("code").asText()))
                    throw new SecurityException("QA_FINANCE_TARGET_CURRENCY");
            }
            for(String table:List.of("qa_restore_tasks","qa_restore_rewards",
                    "qa_restore_purchases","qa_restore_ledger")) {
                Long count=db.queryForObject("SELECT count(*) FROM "+table+
                        " WHERE target_family=?",Long.class,targetFamily);
                if(!Objects.equals(0L,count))
                    throw new SecurityException("QA_FINANCE_TARGET_ALREADY_APPLIED");
            }
            for(JsonNode row:source.path("TASK_DEFINITIONS"))
                db.update("INSERT INTO qa_restore_tasks(id,target_family) VALUES (?,?)",
                        maps.tasks().get(id(row,"id")),targetFamily);
            for(JsonNode row:source.path("REWARD_DEFINITIONS"))
                db.update("INSERT INTO qa_restore_rewards(id,target_family) VALUES (?,?)",
                        maps.rewards().get(id(row,"id")),targetFamily);
            if(failAfterStep==1)throw new IllegalStateException("QA_FAIL_AFTER_DEFINITIONS");
            for(JsonNode row:source.path("REWARD_PURCHASES")) {
                long reward=id(row,"reward_definition_id");
                db.update("""
                    INSERT INTO qa_restore_purchases(
                        id,target_family,member_id,reward_id,point_type_id,price_amount)
                    VALUES (?,?,?,?,?,?)
                    """,maps.purchases().get(id(row,"id")),targetFamily,
                        maps.members().get(id(row,"member_id")),maps.rewards().get(reward),
                        maps.currencies().get(id(row,"point_type_id")),
                        row.path("price_amount").asInt());
            }
            if(failAfterStep==2)throw new IllegalStateException("QA_FAIL_AFTER_PURCHASES");
            int count=0;
            for(JsonNode row:source.path("POINT_LEDGER")) {
                String sourceType=row.path("source_type").asText();
                Long sourceId=null;
                if("REWARD_PURCHASE".equals(sourceType))
                    sourceId=maps.purchases().get(id(row,"source_id"));
                else if("TASK".equals(sourceType)) {
                    // Original task EXECUTION is not a TASK_DEFINITION id.
                    // This bounded rehearsal records its explicit original
                    // ID only; full target execution rebind remains P0.
                    sourceId=id(row,"source_id");
                }
                db.update("""
                    INSERT INTO qa_restore_ledger(
                        target_family,source_row_id,member_id,point_type_id,
                        amount,source_type,source_id)
                    VALUES (?,?,?,?,?,?,?)
                    """,targetFamily,id(row,"id"),
                    maps.members().get(id(row,"member_id")),
                    maps.currencies().get(id(row,"point_type_id")),
                    row.path("amount").asInt(),sourceType,sourceId);
                count++;
            }
            if(failAfterStep==3)throw new IllegalStateException("QA_FAIL_AFTER_LEDGER");
            var actual=new TreeMap<String,Long>();
            db.query("""
                    SELECT member_id,point_type_id,COALESCE(SUM(amount),0) AS balance
                    FROM qa_restore_ledger WHERE target_family=?
                    GROUP BY member_id,point_type_id
                    """,rs->{
                        actual.put(rs.getLong("member_id")+":"+rs.getLong("point_type_id"),
                                rs.getLong("balance"));
                    },targetFamily);
            if(!actual.equals(checked.expectedTargetBalances()))
                throw new SecurityException("QA_FINANCE_BALANCE_RECONCILIATION");
            return new Result(maps.tasks().size(),maps.rewards().size(),
                    maps.purchases().size(),count,Map.copyOf(actual));
        });
    }

    private static long id(JsonNode row,String property) {
        if(!row.path(property).isIntegralNumber() || row.path(property).asLong()<=0)
            throw new SecurityException("QA_FINANCE_SOURCE_ID_INVALID:"+property);
        return row.path(property).asLong();
    }
}
