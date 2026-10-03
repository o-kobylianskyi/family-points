package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "task_instances",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_task_instance_definition_member_date",
                        columnNames = {
                                "task_definition_id",
                                "member_id",
                                "scheduled_date"
                        }
                )
        }
)
public class TaskInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_definition_id", nullable = false)
    private TaskDefinition taskDefinition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private WorkspaceMember member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_assigned_member_id")
    private WorkspaceMember originalAssignedMember;

    private LocalDateTime claimedAt;

    @Column(nullable = false)
    private LocalDate scheduledDate;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean mandatory;

    private LocalTime dueTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_point_type_id")
    private PointType rewardPointType;

    private Integer rewardAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penalty_point_type_id")
    private PointType penaltyPointType;

    private Integer penaltyAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskInstanceStatus status;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    /*
     * EXCUSED
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TaskExcuseReason excuseReason;

    @Column(length = 500)
    private String excuseComment;

    /*
     * Захист від повторного reward/penalty.
     */
    @Column(nullable = false)
    private boolean rewardProcessed;

    @Column(nullable = false)
    private boolean penaltyProcessed;

    protected TaskInstance() {
    }

    public TaskInstance(
            TaskDefinition taskDefinition,
            WorkspaceMember member,
            LocalDate scheduledDate
    ) {
        this.taskDefinition = taskDefinition;
        this.member = member;
        this.originalAssignedMember = taskDefinition.getAssignedMember();
        this.scheduledDate = scheduledDate;

        this.title = taskDefinition.getTitle();
        this.description = taskDefinition.getDescription();
        this.mandatory = taskDefinition.isMandatory();
        this.dueTime = taskDefinition.getDueTime();

        this.rewardPointType =
                taskDefinition.getRewardPointType();

        this.rewardAmount =
                taskDefinition.getRewardAmount();

        this.penaltyPointType =
                taskDefinition.getPenaltyPointType();

        this.penaltyAmount =
                taskDefinition.getPenaltyAmount();

        this.status = TaskInstanceStatus.PENDING;

        this.rewardProcessed = false;
        this.penaltyProcessed = false;
    }

    public Long getId() {
        return id;
    }

    public TaskDefinition getTaskDefinition() {
        return taskDefinition;
    }

    public WorkspaceMember getMember() {
        return member;
    }

    public WorkspaceMember getOriginalAssignedMember() { return originalAssignedMember; }
    public LocalDateTime getClaimedAt() { return claimedAt; }
    public void markClaimed() { this.claimedAt = LocalDateTime.now(); }
    public void changeExecutor(WorkspaceMember member) { this.member = member; }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public TaskInstanceStatus getStatus() {
        return status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public TaskExcuseReason getExcuseReason() {
        return excuseReason;
    }

    public String getExcuseComment() {
        return excuseComment;
    }

    public boolean isRewardProcessed() {
        return rewardProcessed;
    }

    public boolean isPenaltyProcessed() {
        return penaltyProcessed;
    }

    public void start() {
        this.status = TaskInstanceStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void pause() {
        this.status = TaskInstanceStatus.PAUSED;
    }

    public void resume() {
        this.status = TaskInstanceStatus.IN_PROGRESS;
    }

    public void cancel() {
        this.status = TaskInstanceStatus.CANCELLED;
    }

    public void release() {
        this.status = TaskInstanceStatus.RELEASED;
    }

    public void reclaim(WorkspaceMember member) {
        this.member = member;
        this.claimedAt = LocalDateTime.now();
        this.status = TaskInstanceStatus.PENDING;
    }

    public void complete() {
        this.status = TaskInstanceStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markMissed() {
        this.status = TaskInstanceStatus.MISSED;
    }

    public void excuse(
            TaskExcuseReason reason,
            String comment
    ) {
        this.status = TaskInstanceStatus.EXCUSED;
        this.excuseReason = reason;
        this.excuseComment = comment;
    }

    public void markRewardProcessed() {
        this.rewardProcessed = true;
    }

    public void markPenaltyProcessed() {
        this.penaltyProcessed = true;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public LocalTime getDueTime() {
        return dueTime;
    }

    public PointType getRewardPointType() {
        return rewardPointType;
    }

    public Integer getRewardAmount() {
        return rewardAmount;
    }

    public PointType getPenaltyPointType() {
        return penaltyPointType;
    }

    public Integer getPenaltyAmount() {
        return penaltyAmount;
    }
}