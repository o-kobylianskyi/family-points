package com.olehkobylianskyi.familypoints.entity;

public enum WorkspacePermission {

    // Workspace
    VIEW_WORKSPACE,
    MANAGE_WORKSPACE,
    MANAGE_MEMBERS,
    MANAGE_ROLES,

    // Points
    VIEW_OWN_POINTS,
    VIEW_ALL_POINTS,
    MANAGE_POINTS,
    TRANSFER_POINTS,

    // Economy
    MANAGE_ECONOMY,

    // Tasks
    VIEW_OWN_TASKS,
    VIEW_ALL_TASKS,
    CREATE_TASKS,
    MANAGE_TASKS,
    APPROVE_TASKS,
    REVIEW_TASKS,

    // Rewards
    MANAGE_REWARDS,

    // Funding
    REQUEST_FUNDING,
    MANAGE_FUNDING,

    // Behavior
    VIEW_OWN_BEHAVIOR,
    VIEW_ALL_BEHAVIOR,
    MANAGE_BEHAVIOR,

    // Reputation
    VIEW_OWN_REPUTATION,
    VIEW_ALL_REPUTATION,
    MANAGE_REPUTATION,

    // Wishes / Goals
    CREATE_WISHES,
    MANAGE_WISHES,
    MANAGE_GOALS,

    // Quests
    CREATE_QUESTS,
    ACCEPT_QUESTS,
    MANAGE_QUESTS,

    // Administration
    ADMIN_OVERRIDE
}