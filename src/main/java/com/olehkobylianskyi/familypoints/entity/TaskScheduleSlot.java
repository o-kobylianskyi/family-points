package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;
import java.time.LocalTime;

/** Recurrence-time template; does not represent a dated execution. */
@Entity
@Table(name = "task_schedule_slots", uniqueConstraints = @UniqueConstraint(
        name = "uq_task_schedule_slot_key", columnNames = {"task_definition_id", "slot_key"}))
public class TaskScheduleSlot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_definition_id", nullable = false)
    private TaskDefinition taskDefinition;

    @Column(name = "slot_key", nullable = false, length = 40)
    private String slotKey;

    @Column(name = "scheduled_time")
    private LocalTime scheduledTime;

    @Column(nullable = false)
    private boolean enabled = true;

    protected TaskScheduleSlot() {}

    public TaskScheduleSlot(TaskDefinition definition, String key, LocalTime time) {
        this.taskDefinition = definition;
        this.slotKey = key;
        this.scheduledTime = time;
    }

    public Long getId() { return id; }
    public TaskDefinition getTaskDefinition() { return taskDefinition; }
    public String getSlotKey() { return slotKey; }
    public LocalTime getScheduledTime() { return scheduledTime; }
    public boolean isEnabled() { return enabled; }

    public void reschedule(LocalTime time) { this.scheduledTime = time; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
