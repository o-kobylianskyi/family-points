package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;
import java.time.ZoneId;

@Entity
@Table(name = "families")
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone = "UTC";

    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String value) { this.timeZone = ZoneId.of(value).getId(); }

    protected Workspace() {
    }

    public Workspace(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}