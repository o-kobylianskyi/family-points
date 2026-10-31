package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "member_exception_periods",
        indexes = {
                @Index(
                        name = "idx_member_exception_period_member_dates",
                        columnList = "member_id,start_date,end_date"
                )
        }
)
public class MemberExceptionPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private WorkspaceMember member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MemberExceptionType type;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(length = 500)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected MemberExceptionPeriod() {
    }

    public MemberExceptionPeriod(
            WorkspaceMember member,
            MemberExceptionType type,
            LocalDate startDate,
            LocalDate endDate,
            String comment
    ) {
        this.member = member;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.comment = comment;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public WorkspaceMember getMember() {
        return member;
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