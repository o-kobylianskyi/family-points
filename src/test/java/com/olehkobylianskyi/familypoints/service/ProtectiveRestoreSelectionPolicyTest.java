package com.olehkobylianskyi.familypoints.service;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProtectiveRestoreSelectionPolicyTest {
    private final ProtectiveRestoreSelectionPolicy policy=new ProtectiveRestoreSelectionPolicy();

    @Test void rejectsOrphanedSelectiveSelections() {
        var decision=policy.inspect(List.of("TASK_EXECUTIONS","REWARD_PURCHASES","MEDIA_BINDINGS"),
                List.of("TASK_EXECUTIONS","TASK_DEFINITIONS","REWARD_PURCHASES",
                        "REWARD_DEFINITIONS","MEDIA_BINDINGS","MEDIA_ASSETS"));
        assertFalse(decision.executable());
        assertTrue(decision.blockers().contains(
                "REQUIRED_CATEGORY_NOT_SELECTED:TASK_EXECUTIONS:TASK_DEFINITIONS"));
        assertTrue(decision.blockers().contains(
                "REQUIRED_CATEGORY_NOT_SELECTED:REWARD_PURCHASES:REWARD_DEFINITIONS"));
        assertTrue(decision.blockers().contains(
                "REQUIRED_CATEGORY_NOT_SELECTED:MEDIA_BINDINGS:MEDIA_ASSETS"));
    }

    @Test void unsupportedArchiveAndFullySelectedGraphStillCannotExecute() {
        var missing=policy.inspect(List.of("TASK_SCHEDULE_SLOTS"),
                List.of("TASK_SCHEDULE_SLOTS"));
        assertTrue(missing.blockers().contains(
                "REQUIRED_CATEGORY_NOT_IN_ARCHIVE:TASK_SCHEDULE_SLOTS:TASK_DEFINITIONS"));
        var complete=policy.inspect(List.of("TASK_SCHEDULE_SLOTS","TASK_DEFINITIONS"),
                List.of("TASK_SCHEDULE_SLOTS","TASK_DEFINITIONS"));
        assertEquals(List.of("RESTORE_EXECUTION_DISABLED"),complete.blockers());
        assertFalse(complete.executable());
        assertThrows(IllegalArgumentException.class,()->policy.inspect(List.of(),List.of()));
    }
}
