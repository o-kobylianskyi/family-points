package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "task_definitions")
public class TaskDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_member_id")
    private WorkspaceMember assignedMember;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_node_id", unique = true)
    private WorkNode workNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_member_id")
    private WorkspaceMember createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_member_id")
    private WorkspaceMember responsibleMember;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_policy", length = 30)
    private AssignmentPolicy assignmentPolicy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_group_id")
    private MemberGroup targetGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_member_id")
    private WorkspaceMember preferredMember;

    @Column(name = "delegation_allowed", nullable = false)
    private boolean delegationAllowed = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_match_mode", length = 10)
    private RoleMatchMode roleMatchMode;

    @ManyToMany
    @JoinTable(
            name = "task_definition_required_group_roles",
            joinColumns = @JoinColumn(name = "task_definition_id"),
            inverseJoinColumns = @JoinColumn(name = "group_role_id")
    )
    private java.util.Set<GroupRole> requiredGroupRoles = new java.util.LinkedHashSet<>();

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean mandatory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskRecurrenceType recurrenceType;

    @Column(nullable = false)
    private boolean active;

    /*
     * Коли TaskDefinition був створений у системі.
     * Це НЕ дата початку виконання завдання.
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    /*
     * Коли TaskDefinition востаннє змінювався.
     */
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    /*
     * Дата, з якої TaskDefinition починає діяти.
     *
     * Для ONCE — дата одноразового завдання.
     * Для recurring task — початок періоду генерації.
     */
    @Column(name = "start_date")
    private LocalDate startDate;

    /*
     * Остання дата дії TaskDefinition.
     *
     * null = кінцевої дати немає.
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /*
     * Для WEEKLY:
     * 1 = Monday
     * ...
     * 7 = Sunday
     */
    @Column
    private Integer recurrenceDayOfWeek;

    /*
     * Для MONTHLY:
     * день місяця, наприклад 15.
     */
    @Column
    private Integer recurrenceDayOfMonth;

    /*
     * Час, до якого конкретний TaskInstance
     * повинен бути виконаний.
     */
    @Column(name = "due_time")
    private LocalTime dueTime;

    /*
     * Reward.
     *
     * Поки одна винагорода.
     * Пізніше винесемо в окрему reward-модель.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_point_type_id")
    private PointType rewardPointType;

    private Integer rewardAmount;

    /*
     * Penalty.
     *
     * null/0 = штрафу немає.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penalty_point_type_id")
    private PointType penaltyPointType;

    private Integer penaltyAmount;


    /*
     * Потрібен JPA/Hibernate.
     */
    protected TaskDefinition() {
    }


    /*
     * Основний конструктор для створення
     * нового TaskDefinition.
     */
    public TaskDefinition(
            Workspace workspace,
            WorkspaceMember assignedMember,
            String title,
            String description,
            boolean mandatory,
            TaskRecurrenceType recurrenceType,
            LocalDate startDate,
            LocalDate endDate,
            Integer recurrenceDayOfWeek,
            Integer recurrenceDayOfMonth,
            PointType rewardPointType,
            Integer rewardAmount,
            PointType penaltyPointType,
            Integer penaltyAmount,
            LocalTime dueTime
    ) {
        this.workspace = workspace;
        this.assignedMember = assignedMember;
        this.assignmentPolicy = AssignmentPolicy.SINGLE_MEMBER;

        this.title = title;
        this.description = description;
        this.mandatory = mandatory;

        this.recurrenceType = recurrenceType;

        this.startDate = startDate;
        this.endDate = endDate;
        this.recurrenceDayOfWeek = recurrenceDayOfWeek;
        this.recurrenceDayOfMonth = recurrenceDayOfMonth;
        this.dueTime = dueTime;

        this.rewardPointType = rewardPointType;
        this.rewardAmount = rewardAmount;

        this.penaltyPointType = penaltyPointType;
        this.penaltyAmount = penaltyAmount;

        this.active = true;
    }


    /*
     * Автоматично викликається Hibernate
     * перед INSERT нового TaskDefinition.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
    }


    /*
     * Автоматично викликається Hibernate
     * перед UPDATE TaskDefinition.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


    public Long getId() {
        return id;
    }


    public Workspace getWorkspace() {
        return workspace;
    }


    public WorkspaceMember getAssignedMember() {
        return assignedMember;
    }


    public WorkNode getWorkNode() { return workNode; }
    public void setWorkNode(WorkNode workNode) { this.workNode = workNode; }
    public WorkspaceMember getCreatedBy() { return createdBy; }
    public void setCreatedBy(WorkspaceMember createdBy) { this.createdBy = createdBy; }
    public WorkspaceMember getResponsibleMember() { return responsibleMember; }
    public void setResponsibleMember(WorkspaceMember responsibleMember) { this.responsibleMember = responsibleMember; }
    public AssignmentPolicy getAssignmentPolicy() { return assignmentPolicy; }
    public void setAssignmentPolicy(AssignmentPolicy assignmentPolicy) { this.assignmentPolicy = assignmentPolicy; }
    public MemberGroup getTargetGroup() { return targetGroup; }
    public void setTargetGroup(MemberGroup targetGroup) { this.targetGroup = targetGroup; }
    public WorkspaceMember getPreferredMember() { return preferredMember; }
    public void setPreferredMember(WorkspaceMember preferredMember) { this.preferredMember = preferredMember; }
    public boolean isDelegationAllowed() { return delegationAllowed; }
    public void setDelegationAllowed(boolean delegationAllowed) { this.delegationAllowed = delegationAllowed; }
    public RoleMatchMode getRoleMatchMode() { return roleMatchMode; }
    public void setRoleMatchMode(RoleMatchMode roleMatchMode) { this.roleMatchMode = roleMatchMode; }
    public java.util.Set<GroupRole> getRequiredGroupRoles() { return requiredGroupRoles; }

    public String getTitle() {
        return title;
    }


    public String getDescription() {
        return description;
    }


    public boolean isMandatory() {
        return mandatory;
    }


    public TaskRecurrenceType getRecurrenceType() {
        return recurrenceType;
    }


    public boolean isActive() {
        return active;
    }


    public void setActive(boolean active) {
        this.active = active;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }


    public LocalDate getStartDate() {
        return startDate;
    }


    public LocalDate getEndDate() {
        return endDate;
    }


    public Integer getRecurrenceDayOfWeek() {
        return recurrenceDayOfWeek;
    }


    public Integer getRecurrenceDayOfMonth() {
        return recurrenceDayOfMonth;
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

    // Definition editing changes future scheduling/configuration only. Existing TaskInstance snapshots stay unchanged.
    public void setAssignedMember(WorkspaceMember assignedMember) { this.assignedMember = assignedMember; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }
    public void setRecurrenceType(TaskRecurrenceType recurrenceType) { this.recurrenceType = recurrenceType; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setRecurrenceDayOfWeek(Integer value) { this.recurrenceDayOfWeek = value; }
    public void setRecurrenceDayOfMonth(Integer value) { this.recurrenceDayOfMonth = value; }
    public void setDueTime(LocalTime dueTime) { this.dueTime = dueTime; }
    public void setRewardPointType(PointType value) { this.rewardPointType = value; }
    public void setRewardAmount(Integer value) { this.rewardAmount = value; }
    public void setPenaltyPointType(PointType value) { this.penaltyPointType = value; }
    public void setPenaltyAmount(Integer value) { this.penaltyAmount = value; }
}