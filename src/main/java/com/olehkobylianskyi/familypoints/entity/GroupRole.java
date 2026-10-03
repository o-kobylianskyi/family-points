package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "group_roles", uniqueConstraints = @UniqueConstraint(name = "uk_group_role_name", columnNames = {"member_group_id", "name"}))
public class GroupRole {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_group_id", nullable = false)
    private MemberGroup memberGroup;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    protected GroupRole() {}

    public GroupRole(MemberGroup memberGroup, String name, String description) {
        this.memberGroup = memberGroup;
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public MemberGroup getMemberGroup() { return memberGroup; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
