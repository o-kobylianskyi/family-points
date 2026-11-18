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
                  Map<String,Long> balances,Map<Long,Long> executionIds) {}

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
        // Strict vNext historical snapshot contract. Reject incomplete/legacy
        // source rows, never rebuild status/date/economy from mutable definitions.
        var snapshots=new TreeMap<Long,DisposableTaskInstanceSnapshotValidator.Snapshot>();
        var uniqueSlots=new HashSet<String>();
        for(JsonNode row:source.path("TASK_EXECUTIONS")) {
            var historical=DisposableTaskInstanceSnapshotValidator.parse(
                    row,maps.members(),maps.tasks(),maps.currencies());
            if(snapshots.putIfAbsent(historical.sourceId(),historical)!=null)
                throw new SecurityException("QA_FINANCE_DUPLICATE_EXECUTION");
            String key=historical.sourceTask()+":"+historical.sourceMember()
                    +":"+historical.scheduledDate()+":"+historical.slotKey();
            if(!uniqueSlots.add(key))
                throw new SecurityException("QA_FINANCE_DUPLICATE_HISTORIC_SLOT");
        }
        DisposableTaskInstanceSnapshotValidator.verifyLedgerEconomy(
                source.path("POINT_LEDGER"),snapshots);
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
                    "qa_restore_executions","qa_restore_purchases","qa_restore_ledger")) {
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
            // TASK ledger source_id is a polymorphic reference to a historic
            // *execution*, never the task definition. Allocate new target
            // execution IDs and preserve the complete old->new mapping.
            var executionIds=new TreeMap<Long,Long>();
            for(var historical:snapshots.values()) {
                Long newExecution=db.queryForObject("""
                    INSERT INTO qa_restore_executions(
                        target_family,source_execution_id,member_id,task_id,
                        original_assigned_member_id,scheduled_date,slot_key,
                        title,description,mandatory,due_time,status,
                        started_at,completed_at,reward_amount,reward_point_type_id,
                        penalty_amount,penalty_point_type_id,
                        reward_processed,penalty_processed)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id
                    """,Long.class,targetFamily,historical.sourceId(),
                        maps.members().get(historical.sourceMember()),
                        maps.tasks().get(historical.sourceTask()),
                        historical.originalAssignedMember()==null?null:
                            maps.members().get(historical.originalAssignedMember()),
                        java.sql.Date.valueOf(historical.scheduledDate()),
                        historical.slotKey(),historical.title(),historical.description(),
                        historical.mandatory(),
                        historical.dueTime()==null?null:java.sql.Time.valueOf(historical.dueTime()),
                        historical.status(),
                        historical.startedAt()==null?null:
                            java.sql.Timestamp.valueOf(historical.startedAt()),
                        historical.completedAt()==null?null:
                            java.sql.Timestamp.valueOf(historical.completedAt()),
                        historical.rewardAmount(),
                        historical.rewardPointType()==null?null:
                            maps.currencies().get(historical.rewardPointType()),
                        historical.penaltyAmount(),
                        historical.penaltyPointType()==null?null:
                            maps.currencies().get(historical.penaltyPointType()),
                        historical.rewardProcessed(),historical.penaltyProcessed());
                if(newExecution==null || newExecution<=0
                        || executionIds.putIfAbsent(historical.sourceId(),newExecution)!=null)
                    throw new SecurityException("QA_FINANCE_EXECUTION_REMAP_INVALID");
            }
            if(failAfterStep==2)throw new IllegalStateException("QA_FAIL_AFTER_EXECUTIONS");
            for(JsonNode row:source.path("REWARD_PURCHASES")) {
                // This bounded implementation only accepts purchase parents
                // tied to archived REWARD_DEFINITIONS; request-owned purchases
                // await the complete approved request/obligation contract.
                if(!row.hasNonNull("reward_definition_id")
                        || row.hasNonNull("reward_request_id"))
                    throw new SecurityException("QA_FINANCE_REQUEST_PURCHASE_UNSUPPORTED");
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
            if(failAfterStep==3)throw new IllegalStateException("QA_FAIL_AFTER_PURCHASES");
            int count=0;
            for(JsonNode row:source.path("POINT_LEDGER")) {
                String sourceType=row.path("source_type").asText();
                Long sourceId=null;
                if("REWARD_PURCHASE".equals(sourceType))
                    sourceId=maps.purchases().get(id(row,"source_id"));
                else if("TASK".equals(sourceType)) {
                    sourceId=executionIds.get(id(row,"source_id"));
                    if(sourceId==null)
                        throw new SecurityException("QA_FINANCE_EXECUTION_SOURCE_UNMAPPED");
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
            if(failAfterStep==4)throw new IllegalStateException("QA_FAIL_AFTER_LEDGER");
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
            // Polymorphic ledger source_id is NOT a physical FK. Validate
            // each mapped event against its actual target parent and buyer.
            for(JsonNode original:source.path("POINT_LEDGER")) {
                String type=original.path("source_type").asText();
                if("MANUAL".equals(type))continue;
                Long mappedParent="TASK".equals(type)
                        ?executionIds.get(id(original,"source_id"))
                        :maps.purchases().get(id(original,"source_id"));
                String table="TASK".equals(type)
                        ?"qa_restore_executions":"qa_restore_purchases";
                Long matches=db.queryForObject("""
                        SELECT count(*) FROM qa_restore_ledger l
                        WHERE l.target_family=? AND l.source_row_id=?
                          AND l.source_type=? AND l.source_id=?
                          AND l.member_id=? AND l.point_type_id=?
                        """,Long.class,targetFamily,id(original,"id"),type,mappedParent,
                        maps.members().get(id(original,"member_id")),
                        maps.currencies().get(id(original,"point_type_id")));
                Long parentMatches=db.queryForObject(
                        "SELECT count(*) FROM "+table+
                        " WHERE id=? AND target_family=? AND member_id=?",
                        Long.class,mappedParent,targetFamily,
                        maps.members().get(id(original,"member_id")));
                if(!Objects.equals(1L,matches)||!Objects.equals(1L,parentMatches))
                    throw new SecurityException("QA_FINANCE_POLYMORPHIC_FK_MISMATCH");
            }
            // Polymorphic source IDs are not physical FKs in POINT_LEDGER.
            // Perform an explicit target-family and target-owner check here.
            for(var entry:executionIds.entrySet()) {
                if(!Objects.equals(1L,db.queryForObject("""
                        SELECT count(*) FROM qa_restore_executions e
                        JOIN qa_restore_tasks t ON t.id=e.task_id
                        WHERE e.id=? AND e.source_execution_id=?
                          AND e.target_family=? AND t.target_family=?
                        """,Long.class,entry.getValue(),entry.getKey(),
                        targetFamily,targetFamily)))
                    throw new SecurityException("QA_FINANCE_EXECUTION_FK_RECONCILIATION");
            }
            return new Result(maps.tasks().size(),maps.rewards().size(),
                    maps.purchases().size(),count,Map.copyOf(actual),
                    Map.copyOf(executionIds));
        });
    }

    private static long id(JsonNode row,String property) {
        if(!row.path(property).isIntegralNumber() || row.path(property).asLong()<=0)
            throw new SecurityException("QA_FINANCE_SOURCE_ID_INVALID:"+property);
        return row.path(property).asLong();
    }
}
