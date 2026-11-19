package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.PortableV2SourceClosurePolicy;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/** B-INT-02 stable finite disposition for the source-member FK inventory. */
class PortableV2SourceClosurePolicyTest {
    @Test void allInventoriedMemberEdgesHaveUniqueExplicitDecisions() {
        var all=PortableV2SourceClosurePolicy.MEMBER_EDGES;
        assertTrue(all.size()>=17,"Review all originally inventoried member FK edges");
        Set<String> names=new HashSet<>();
        for(var edge:all) {
            assertNotNull(edge.decision());
            assertTrue(names.add(edge.table()+"."+edge.column()),
                    "No source-member FK edge may be listed twice");
        }
        assertTrue(all.stream().anyMatch(e->e.table().equals("user_accounts")
                &&e.decision()==PortableV2SourceClosurePolicy.Decision.EXCLUDE));
        assertTrue(all.stream().anyMatch(e->e.table().equals("account_workspace_memberships")
                &&e.decision()==PortableV2SourceClosurePolicy.Decision.EXCLUDE));
        for(String table:Set.of("task_instances","reward_purchases","point_transactions"))
            assertTrue(all.stream().anyMatch(e->e.table().equals(table)
                    &&e.decision()==PortableV2SourceClosurePolicy.Decision.INCLUDE),
                    table);
        for(String table:Set.of("point_exchange_requests",
                "reputation_transactions","task_delegations","member_exception_periods",
                "reward_requests","reward_obligations"))
            assertTrue(all.stream().anyMatch(e->e.table().equals(table)
                    &&e.decision()==PortableV2SourceClosurePolicy.Decision.BLOCK),
                    table);
    }
}
