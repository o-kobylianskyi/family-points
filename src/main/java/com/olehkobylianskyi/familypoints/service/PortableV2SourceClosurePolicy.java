package com.olehkobylianskyi.familypoints.service;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;

/**
 * Exact bounded v2 selection policy, not a claim to restore all 23 V1 tables.
 * A real source with rows in any unsupported selected-family graph must fail
 * BEFORE sealing: never manufacture a complete-looking eight-category backup
 * while silently omitting financial, member or media dependencies.
 */
public final class PortableV2SourceClosurePolicy {
    private PortableV2SourceClosurePolicy() {}
    public enum Decision { INCLUDE, DEPENDENCY, EXCLUDE, BLOCK }
    public record MemberForeignKey(String table,String column,Decision decision) {}

    /** Historical 17+ FK edges, including exclusions in global account space.
     * Refuse the source itself, not merely its typed subset, whenever BLOCK
     * edges have data. New schema FKs must be reviewed before expanding scope.
     */
    public static final List<MemberForeignKey> MEMBER_EDGES=List.of(
        new MemberForeignKey("user_accounts","family_member_id",Decision.EXCLUDE),
        new MemberForeignKey("account_workspace_memberships","family_member_id",Decision.EXCLUDE),
        new MemberForeignKey("task_definitions","created_by_member_id",Decision.DEPENDENCY),
        new MemberForeignKey("task_definitions","assigned_member_id",Decision.DEPENDENCY),
        new MemberForeignKey("task_definitions","preferred_member_id",Decision.DEPENDENCY),
        new MemberForeignKey("task_definitions","responsible_member_id",Decision.DEPENDENCY),
        new MemberForeignKey("task_instances","member_id",Decision.INCLUDE),
        new MemberForeignKey("task_instances","original_assigned_member_id",Decision.INCLUDE),
        new MemberForeignKey("point_transactions","member_id",Decision.INCLUDE),
        new MemberForeignKey("reward_purchases","member_id",Decision.INCLUDE),
        new MemberForeignKey("reputation_transactions","member_id",Decision.BLOCK),
        new MemberForeignKey("group_memberships","member_id",Decision.BLOCK),
        new MemberForeignKey("point_exchange_requests","member_id",Decision.BLOCK),
        new MemberForeignKey("point_exchange_requests","processed_by_member_id",Decision.BLOCK),
        new MemberForeignKey("task_delegations","from_member_id",Decision.BLOCK),
        new MemberForeignKey("task_delegations","to_member_id",Decision.BLOCK),
        new MemberForeignKey("task_delegations","delegated_by_member_id",Decision.BLOCK),
        new MemberForeignKey("member_exception_periods","member_id",Decision.BLOCK),
        new MemberForeignKey("reward_requests","requested_by_member_id",Decision.BLOCK),
        new MemberForeignKey("reward_obligations","member_id",Decision.BLOCK),
        new MemberForeignKey("task_reward_requests","requested_by_member_id",Decision.BLOCK),
        new MemberForeignKey("task_suggestions","requested_by_member_id",Decision.BLOCK),
        new MemberForeignKey("task_suggestions","reviewed_by_member_id",Decision.BLOCK)
    );

    private record Unsupported(String category,String sql) {}
    private static final List<Unsupported> UNSUPPORTED=List.of(
        new Unsupported("REPUTATION_LEDGER",
            "SELECT count(*) FROM reputation_transactions t JOIN family_members m ON m.id=t.member_id WHERE m.family_id=?"),
        new Unsupported("GROUP_POINT_LEDGER",
            "SELECT count(*) FROM group_point_transactions t JOIN member_groups g ON g.id=t.group_id WHERE g.family_id=?"),
        new Unsupported("POINT_EXCHANGE_REQUESTS",
            "SELECT count(*) FROM point_exchange_requests t JOIN family_members m ON m.id=t.member_id WHERE m.family_id=?"),
        new Unsupported("REWARD_REQUESTS",
            "SELECT count(*) FROM reward_requests WHERE family_id=?"),
        new Unsupported("REWARD_REQUIREMENTS",
            "SELECT count(*) FROM reward_requirements WHERE family_id=?"),
        new Unsupported("REWARD_OBLIGATIONS",
            "SELECT count(*) FROM reward_obligations WHERE family_id=?"),
        new Unsupported("REWARD_CATEGORIES",
            "SELECT count(*) FROM reward_categories WHERE family_id=?"),
        new Unsupported("REWARD_DEFINITION_CATEGORIES",
            "SELECT count(*) FROM reward_definition_categories x JOIN reward_definitions d ON d.id=x.reward_definition_id WHERE d.family_id=?"),
        new Unsupported("TASK_SCHEDULE_SLOTS",
            "SELECT count(*) FROM task_schedule_slots s JOIN task_definitions d ON d.id=s.task_definition_id WHERE d.family_id=?"),
        new Unsupported("TASK_REWARD_REQUESTS",
            "SELECT count(*) FROM task_reward_requests r JOIN task_instances t ON t.id=r.task_instance_id JOIN task_definitions d ON d.id=t.task_definition_id WHERE d.family_id=?"),
        new Unsupported("TASK_SUGGESTIONS",
            "SELECT count(*) FROM task_suggestions WHERE workspace_id=?"),
        new Unsupported("MEMBER_EXCEPTION_PERIODS",
            "SELECT count(*) FROM member_exception_periods t JOIN family_members m ON m.id=t.member_id WHERE m.family_id=?"),
        new Unsupported("TASK_DELEGATIONS",
            "SELECT count(*) FROM task_delegations t JOIN family_members m ON m.id=t.from_member_id WHERE m.family_id=?"),
        new Unsupported("MEMBER_GROUPS",
            "SELECT count(*) FROM member_groups WHERE family_id=?"),
        new Unsupported("GROUP_MEMBERSHIPS",
            "SELECT count(*) FROM group_memberships g JOIN member_groups m ON m.id=g.member_group_id WHERE m.family_id=?"),
        new Unsupported("WORK_NODES",
            "SELECT count(*) FROM work_nodes WHERE family_id=?"),
        new Unsupported("MEDIA_ASSETS",
            "SELECT count(*) FROM media_assets WHERE workspace_id=?"),
        new Unsupported("MEDIA_BINDINGS",
            "SELECT count(*) FROM media_bindings WHERE workspace_id=?"),
        new Unsupported("PRIVATE_TEMPLATES",
            "SELECT count(*) FROM workspace_private_templates WHERE workspace_id=?")
    );

    public static void requireBoundedSource(JdbcTemplate db,long sourceWorkspace) {
        if(db==null||sourceWorkspace<=0)
            throw new SecurityException("PORTABLE_V2_SELECTION_INVALID");
        for(Unsupported spec:UNSUPPORTED) {
            Long count=db.queryForObject(spec.sql(),Long.class,sourceWorkspace);
            if(count==null||count<0)
                throw new SecurityException("PORTABLE_V2_SELECTION_COUNT_UNKNOWN:"+spec.category());
            if(count>0)
                throw new SecurityException("PORTABLE_V2_UNSUPPORTED_NONEMPTY_CATEGORY:"+spec.category());
        }
    }
}
