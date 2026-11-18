package com.olehkobylianskyi.familypoints.security;

import org.junit.jupiter.api.Test;
import java.util.List;

import static com.olehkobylianskyi.familypoints.security.WorkspaceRestoreSelectionClosureV2.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceRestoreSelectionClosureV2Test {
    private static final String A="a".repeat(64),B="b".repeat(64),
            C="c".repeat(64),D="d".repeat(64);

    private Category tasks() {
        return new Category("TASK_DEFINITIONS",Decision.RESTORE,11,A,B,C);
    }
    private Category rewards() {
        return new Category("REWARD_PURCHASES",Decision.PRESERVE,8,B,C,D);
    }
    private String hash(List<Category> values) {
        return canonicalDigest("VNext-2",7,8,values);
    }

    @Test void categoryIterationOrderDoesNotAffectDigest() {
        assertEquals(hash(List.of(tasks(),rewards())),hash(List.of(rewards(),tasks())));
        assertTrue(hash(List.of(tasks(),rewards())).matches("[0-9a-f]{64}"));
    }
    @Test void countsAndDecisionAreBoundNotOnlyCategoryNames() {
        String original=hash(List.of(tasks(),rewards()));
        assertNotEquals(original,hash(List.of(
                new Category("TASK_DEFINITIONS",Decision.RESTORE,12,A,B,C),rewards())));
        assertNotEquals(original,hash(List.of(
                new Category("TASK_DEFINITIONS",Decision.PRESERVE,11,A,B,C),rewards())));
        assertNotEquals(original,hash(List.of(
                new Category("REWARD_PURCHASES",Decision.BLOCK,8,B,C,D),tasks())));
    }
    @Test void dependencySourceAndConflictDigestsAreAllBound() {
        String original=hash(List.of(tasks()));
        assertNotEquals(original,hash(List.of(new Category(
                "TASK_DEFINITIONS",Decision.RESTORE,11,D,B,C))));
        assertNotEquals(original,hash(List.of(new Category(
                "TASK_DEFINITIONS",Decision.RESTORE,11,A,D,C))));
        assertNotEquals(original,hash(List.of(new Category(
                "TASK_DEFINITIONS",Decision.RESTORE,11,A,B,D))));
    }
    @Test void schemaTenantsAndExcludedCategoriesAffectScope() {
        String original=hash(List.of(tasks(),rewards()));
        assertNotEquals(original,hash(List.of(tasks())));
        assertNotEquals(original,canonicalDigest("VNext-3",7,8,List.of(tasks(),rewards())));
        assertNotEquals(original,canonicalDigest("VNext-2",9,8,List.of(tasks(),rewards())));
        assertNotEquals(original,canonicalDigest("VNext-2",7,9,List.of(tasks(),rewards())));
    }
    @Test void duplicateCategoryNullAndInvalidDigestFailClosed() {
        assertThrows(IllegalArgumentException.class,()->hash(List.of(tasks(),tasks())));
        assertThrows(IllegalArgumentException.class,()->hash(List.of()));
        assertThrows(IllegalArgumentException.class,()->new Category(
                "task-definitions",Decision.RESTORE,1,A,B,C));
        assertThrows(IllegalArgumentException.class,()->new Category(
                "TASK_DEFINITIONS",Decision.RESTORE,-1,A,B,C));
        assertThrows(IllegalArgumentException.class,()->new Category(
                "TASK_DEFINITIONS",Decision.RESTORE,1,"bad",B,C));
        assertThrows(IllegalArgumentException.class,()->canonicalDigest(
                "VNext-2",0,8,List.of(tasks())));
    }
}
