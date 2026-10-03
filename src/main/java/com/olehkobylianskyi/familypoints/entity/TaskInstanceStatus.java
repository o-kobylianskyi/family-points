package com.olehkobylianskyi.familypoints.entity;

public enum TaskInstanceStatus {

    PENDING,
    IN_PROGRESS,
    PAUSED,

    WAITING_APPROVAL,
    COMPLETED,

    MISSED,
    EXCUSED,

    RELEASED,
    CANCELLED
}