package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.time.LocalTime;

@Embeddable
public class RewardWeeklyWindow {
    @Column(name = "window_id", nullable = false, length = 64)
    private String windowId;
    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;
    @Column(name = "local_start_time", nullable = false)
    private LocalTime localStartTime;
    @Column(name = "local_end_time", nullable = false)
    private LocalTime localEndTime;
    @Column(name = "effective_from_date")
    private LocalDate effectiveFromDate;
    @Column(name = "effective_to_date")
    private LocalDate effectiveToDate;
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    protected RewardWeeklyWindow() {}
    public RewardWeeklyWindow(String windowId, int dayOfWeek, LocalTime localStartTime,
                              LocalTime localEndTime, LocalDate from, LocalDate to, boolean enabled) {
        this.windowId = windowId;
        this.dayOfWeek = dayOfWeek;
        this.localStartTime = localStartTime;
        this.localEndTime = localEndTime;
        this.effectiveFromDate = from;
        this.effectiveToDate = to;
        this.enabled = enabled;
    }
    public String getWindowId() { return windowId; }
    public int getDayOfWeek() { return dayOfWeek; }
    public LocalTime getLocalStartTime() { return localStartTime; }
    public LocalTime getLocalEndTime() { return localEndTime; }
    public LocalDate getEffectiveFromDate() { return effectiveFromDate; }
    public LocalDate getEffectiveToDate() { return effectiveToDate; }
    public boolean isEnabled() { return enabled; }
}
