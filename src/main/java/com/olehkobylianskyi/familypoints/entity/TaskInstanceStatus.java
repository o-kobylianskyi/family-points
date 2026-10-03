package com.olehkobylianskyi.familypoints.entity;

public enum TaskInstanceStatus {

    PENDING,
    IN_PROGRESS,

    WAITING_APPROVAL,
    COMPLETED,

    MISSED,
    EXCUSED,

    CANCELLED
}