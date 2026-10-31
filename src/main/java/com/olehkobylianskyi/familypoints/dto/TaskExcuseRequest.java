package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.TaskExcuseReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TaskExcuseRequest {

    @NotNull
    private TaskExcuseReason reason;

    @Size(max = 500)
    private String comment;

    public TaskExcuseRequest() {
    }

    public TaskExcuseReason getReason() {
        return reason;
    }

    public void setReason(TaskExcuseReason reason) {
        this.reason = reason;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}