package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.WorkspacePermission;

import java.util.EnumSet;
import java.util.Set;

public final class DefaultWorkspaceRoles {

    public static final String FAMILY_ADMIN = "FAMILY_ADMIN";
    public static final String PARENT = "PARENT";
    public static final String CHILD = "CHILD";

    private DefaultWorkspaceRoles() {
    }

    public static Set<WorkspacePermission> familyAdminPermissions() {
        return EnumSet.allOf(WorkspacePermission.class);
    }

    public static Set<WorkspacePermission> parentPermissions() {
        return EnumSet.of(
                WorkspacePermission.VIEW_WORKSPACE,
                WorkspacePermission.MANAGE_WORKSPACE,
                WorkspacePermission.MANAGE_MEMBERS,

                WorkspacePermission.VIEW_OWN_POINTS,
                WorkspacePermission.VIEW_ALL_POINTS,
                WorkspacePermission.MANAGE_POINTS,
                WorkspacePermission.TRANSFER_POINTS,

                WorkspacePermission.MANAGE_ECONOMY,

                WorkspacePermission.VIEW_OWN_TASKS,
                WorkspacePermission.VIEW_ALL_TASKS,
                WorkspacePermission.CREATE_TASKS,
                WorkspacePermission.MANAGE_TASKS,
                WorkspacePermission.APPROVE_TASKS,
                WorkspacePermission.REVIEW_TASKS,

                WorkspacePermission.MANAGE_REWARDS,

                WorkspacePermission.REQUEST_FUNDING,
                WorkspacePermission.MANAGE_FUNDING,

                WorkspacePermission.VIEW_OWN_BEHAVIOR,
                WorkspacePermission.VIEW_ALL_BEHAVIOR,
                WorkspacePermission.MANAGE_BEHAVIOR,

                WorkspacePermission.VIEW_OWN_REPUTATION,
                WorkspacePermission.VIEW_ALL_REPUTATION,
                WorkspacePermission.MANAGE_REPUTATION,

                WorkspacePermission.CREATE_WISHES,
                WorkspacePermission.MANAGE_WISHES,
                WorkspacePermission.MANAGE_GOALS,

                WorkspacePermission.CREATE_QUESTS,
                WorkspacePermission.ACCEPT_QUESTS,
                WorkspacePermission.MANAGE_QUESTS
        );
    }

    public static Set<WorkspacePermission> childPermissions() {
        return EnumSet.of(
                WorkspacePermission.VIEW_WORKSPACE,

                WorkspacePermission.VIEW_OWN_POINTS,
                WorkspacePermission.TRANSFER_POINTS,

                WorkspacePermission.VIEW_OWN_TASKS,
                WorkspacePermission.CREATE_TASKS,

                WorkspacePermission.REQUEST_FUNDING,

                WorkspacePermission.VIEW_OWN_BEHAVIOR,
                WorkspacePermission.VIEW_OWN_REPUTATION,

                WorkspacePermission.CREATE_WISHES,

                WorkspacePermission.CREATE_QUESTS,
                WorkspacePermission.ACCEPT_QUESTS
        );
    }
}