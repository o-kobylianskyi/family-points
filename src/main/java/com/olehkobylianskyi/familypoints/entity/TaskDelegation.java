package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_delegations")
public class TaskDelegation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name="task_instance_id", nullable=false) private TaskInstance taskInstance;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name="from_member_id", nullable=false) private WorkspaceMember fromMember;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name="to_member_id", nullable=false) private WorkspaceMember toMember;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name="delegated_by_member_id", nullable=false) private WorkspaceMember delegatedBy;
    @Column(nullable=false, updatable=false) private LocalDateTime delegatedAt;
    @Column(length=500) private String reason;
    protected TaskDelegation() {}
    public TaskDelegation(TaskInstance taskInstance, WorkspaceMember fromMember, WorkspaceMember toMember, WorkspaceMember delegatedBy, String reason) {
        this.taskInstance=taskInstance; this.fromMember=fromMember; this.toMember=toMember; this.delegatedBy=delegatedBy; this.reason=reason; this.delegatedAt=LocalDateTime.now();
    }
    public Long getId(){return id;} public TaskInstance getTaskInstance(){return taskInstance;} public WorkspaceMember getFromMember(){return fromMember;} public WorkspaceMember getToMember(){return toMember;} public WorkspaceMember getDelegatedBy(){return delegatedBy;} public LocalDateTime getDelegatedAt(){return delegatedAt;} public String getReason(){return reason;}
}
