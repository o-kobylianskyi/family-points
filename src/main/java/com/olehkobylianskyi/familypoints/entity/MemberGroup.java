package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "member_groups")
public class MemberGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "show_in_navigation", nullable = false, columnDefinition = "boolean default false")
    private boolean showInNavigation = false;

    protected MemberGroup() {}

    public MemberGroup(Workspace workspace, String name, String description) {
        this.workspace = workspace;
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public boolean isShowInNavigation() { return showInNavigation; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setActive(boolean active) { this.active = active; }
    public void setShowInNavigation(boolean showInNavigation) { this.showInNavigation = showInNavigation; }
}
