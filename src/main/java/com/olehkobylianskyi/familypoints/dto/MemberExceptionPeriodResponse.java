package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.MemberExceptionPeriod;
import com.olehkobylianskyi.familypoints.entity.MemberExceptionType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MemberExceptionPeriodResponse {

    private final Long id;
    private final Long memberId;
    private final MemberExceptionType type;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String comment;
    private final LocalDateTime createdAt;

    public MemberExceptionPeriodResponse(
            Long id,
            Long memberId,
            MemberExceptionType type,
            LocalDate startDate,
            LocalDate endDate,
            String comment,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.memberId = memberId;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public static MemberExceptionPeriodResponse from(
            MemberExceptionPeriod period
    ) {
        return new MemberExceptionPeriodResponse(
                period.getId(),
                period.getMember().getId(),
                period.getType(),
                period.getStartDate(),
                period.getEndDate(),
                period.getComment(),
                period.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public MemberExceptionType getType() {
        return type;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}