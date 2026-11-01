package com.olehkobylianskyi.familypoints.service;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * BKL-BACKUP-003: pure, fail-closed selection dependency review.
 * This policy describes mandatory archive categories, NOT an executable
 * restoration plan. Identifiers, ledger provenance, media bytes, SQL FK
 * closure and target conflicts still require separate validation.
 */
@Component
public final class ProtectiveRestoreSelectionPolicy {
    private static final Map<String,Set<String>> REQUIRES=Map.ofEntries(
        Map.entry("TASK_EXECUTIONS",Set.of("TASK_DEFINITIONS")),
        Map.entry("TASK_SCHEDULE_SLOTS",Set.of("TASK_DEFINITIONS")),
        Map.entry("REWARD_REQUESTS",Set.of("REWARD_DEFINITIONS")),
        Map.entry("REWARD_PURCHASES",Set.of("REWARD_DEFINITIONS")),
        Map.entry("MEDIA_BINDINGS",Set.of("MEDIA_ASSETS"))
    );
    public record Decision(List<String> selected,List<String> blockers,boolean executable) {}

    public Decision inspect(Collection<String> requested,Collection<String> available) {
        if(requested==null || available==null || requested.isEmpty()
                || requested.stream().anyMatch(Objects::isNull)
                || available.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Invalid restore selection");
        var selected=new TreeSet<>(requested);
        var offered=new HashSet<>(available);
        var blockers=new TreeSet<String>();
        for(String category:selected) {
            if(!offered.contains(category))
                blockers.add("CATEGORY_NOT_IN_ARCHIVE:"+category);
            for(String dependency:REQUIRES.getOrDefault(category,Set.of())) {
                if(!offered.contains(dependency))
                    blockers.add("REQUIRED_CATEGORY_NOT_IN_ARCHIVE:"+category+":"+dependency);
                else if(!selected.contains(dependency))
                    blockers.add("REQUIRED_CATEGORY_NOT_SELECTED:"+category+":"+dependency);
            }
        }
        // Conservative policy: no destructive restore until full FK and
        // identity policy, recovery proof and target authorization are wired.
        blockers.add("RESTORE_EXECUTION_DISABLED");
        return new Decision(List.copyOf(selected),List.copyOf(blockers),false);
    }
}
