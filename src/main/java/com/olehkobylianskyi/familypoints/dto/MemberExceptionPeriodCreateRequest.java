package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.MemberExceptionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class MemberExceptionPeriodCreateRequest {

    @NotNull
    private MemberExceptionType type;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @Size(max = 500)
    private String comment;

    public MemberExceptionPeriodCreateRequest() {
    }

    public MemberExceptionType getType() {
        return type;
    }

    public void setType(MemberExceptionType type) {
        this.type = type;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}