package com.olehkobylianskyi.familypoints.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Read-only preflight for the economic-history restore gap. A count of zero is
 * not a restore proof: a supported native apply and independent ledger
 * reconciliation are still mandatory before restoring any financial category.
 *
 * No controller, reset command, ledger mutation or VERIFIED transition.
 */
@Service
public final class WorkspaceFinancialRestoreClosureService {
    private final JdbcTemplate db;

    public WorkspaceFinancialRestoreClosureService(JdbcTemplate db) {
        this.db=db;
    }

    public record EconomicCount(String category,long sourceRows,boolean selected) {}
    public record Closure(long workspaceId,boolean financialHistoryPresent,
            boolean replaySupported,List<EconomicCount> categories,List<String> blockers) {}

    /**
     * Counts deliberately include both catalog and historical data that the
     * current native restore executor cannot faithfully reconstruct. The group
     * ledger is exportable but not yet replayable or currency-reconciled.
     */
    public Closure assess(long workspaceId,Collection<String> selectedCategories) {
        if(workspaceId<=0 || selectedCategories==null
                || selectedCategories.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("INVALID_FINANCIAL_CLOSURE_REQUEST");
        var selected=Set.copyOf(selectedCategories);
        if(!Objects.equals(1L,db.queryForObject(
                "SELECT count(*) FROM families WHERE id=?",Long.class,workspaceId)))
            throw new SecurityException("FINANCIAL_CLOSURE_WORKSPACE_NOT_FOUND");
        var specs=List.of(
                new Scope("POINT_LEDGER","point_transactions",
                    "EXISTS (SELECT 1 FROM family_members m WHERE m.id=t.member_id AND m.family_id=?)"),
                new Scope("REPUTATION_LEDGER","reputation_transactions",
                    "EXISTS (SELECT 1 FROM family_members m WHERE m.id=t.member_id AND m.family_id=?)"),
                new Scope("GROUP_POINT_LEDGER","group_point_transactions",
                    "EXISTS (SELECT 1 FROM member_groups g WHERE g.id=t.group_id AND g.family_id=?)"),
                new Scope("REWARD_OBLIGATIONS","reward_obligations","t.family_id=?"),
                new Scope("REWARD_REQUIREMENTS","reward_requirements","t.family_id=?"),
                new Scope("REWARD_DEFINITION_CATEGORIES","reward_definition_categories",
                    "EXISTS (SELECT 1 FROM reward_definitions d WHERE d.id=t.reward_definition_id AND d.family_id=?)"),
                new Scope("REWARD_OBLIGATION_BLOCKED_CATEGORIES",
                    "reward_obligation_blocked_categories",
                    "EXISTS (SELECT 1 FROM reward_obligations o WHERE o.id=t.obligation_id AND o.family_id=?)"),
                new Scope("REWARD_OBLIGATION_BLOCKED_REWARDS",
                    "reward_obligation_blocked_rewards",
                    "EXISTS (SELECT 1 FROM reward_obligations o WHERE o.id=t.obligation_id AND o.family_id=?)"),
                new Scope("REWARD_PURCHASES","reward_purchases",
                    "EXISTS (SELECT 1 FROM family_members m WHERE m.id=t.member_id AND m.family_id=?)"),
                new Scope("REWARD_REQUESTS","reward_requests","t.family_id=?"),
                new Scope("TASK_EXECUTIONS","task_instances",
                    "EXISTS (SELECT 1 FROM family_members m WHERE m.id=t.member_id AND m.family_id=?)"),
                new Scope("TASK_SCHEDULE_SLOTS","task_schedule_slots",
                    "EXISTS (SELECT 1 FROM task_definitions d WHERE d.id=t.task_definition_id AND d.family_id=?)")
        );
        var rows=new ArrayList<EconomicCount>();
        var blockers=new TreeSet<String>();
        boolean history=false;
        for(var spec:specs) {
            Long count=db.queryForObject(
                    "SELECT count(*) FROM public."+spec.table()+" t WHERE "+spec.filter(),
                    Long.class,workspaceId);
            if(count==null || count<0)
                throw new IllegalStateException("FINANCIAL_CLOSURE_COUNT_UNAVAILABLE");
            boolean included=selected.contains(spec.category());
            rows.add(new EconomicCount(spec.category(),count,included));
            if(count>0) {
                history=true;
                if(!included)
                    blockers.add("RESTORE_DEPENDENCY_NOT_SELECTED:"+spec.category());
                if("GROUP_POINT_LEDGER".equals(spec.category()))
                    blockers.add("RESTORE_GROUP_LEDGER_REPLAY_UNIMPLEMENTED");
                if(spec.category().endsWith("_UNEXPORTED"))
                    blockers.add("RESTORE_CATEGORY_NOT_EXPORTABLE:"+spec.category());
            }
        }
        // An all-zero history is not sufficient: the native restore must
        // support the selected categories, member identity remap, currencies,
        // immutable ledger order/source IDs, and target conflict handling.
        if(!selected.isEmpty()) {
            var financialRequested=Set.of("POINT_LEDGER","REPUTATION_LEDGER",
                    "REWARD_PURCHASES","REWARD_REQUESTS","TASK_EXECUTIONS",
                    "TASK_SCHEDULE_SLOTS","GROUP_POINT_LEDGER","REWARD_REQUIREMENTS");
            for(var category:selected) {
                if(financialRequested.contains(category))
                    blockers.add("RESTORE_FINANCIAL_NATIVE_REPLAY_UNIMPLEMENTED:"+category);
            }
        }
        if(history)
            blockers.add("RESTORE_FINANCIAL_HISTORY_RECONCILIATION_REQUIRED");
        return new Closure(workspaceId,history,false,List.copyOf(rows),List.copyOf(blockers));
    }

    private record Scope(String category,String table,String filter) {}
}
