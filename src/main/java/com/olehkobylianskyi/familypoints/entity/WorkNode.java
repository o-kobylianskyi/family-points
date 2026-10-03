package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "work_nodes")
public class WorkNode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_node_id")
    private WorkNode parentNode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkNodeType type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    protected WorkNode() {}

    public WorkNode(Workspace workspace, WorkNode parentNode, WorkNodeType type, String title, String description) {
        this.workspace = workspace;
        this.parentNode = parentNode;
        this.type = type;
        this.title = title;
        this.description = description;
    }

    public Long getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public WorkNode getParentNode() { return parentNode; }
    public WorkNodeType getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getSortOrder() { return sortOrder; }
    public boolean isActive() { return active; }
    public void setParentNode(WorkNode parentNode) { this.parentNode = parentNode; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public void setActive(boolean active) { this.active = active; }
}
