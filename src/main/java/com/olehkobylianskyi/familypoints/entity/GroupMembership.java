package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "group_memberships", uniqueConstraints = @UniqueConstraint(name = "uk_group_membership", columnNames = {"member_group_id", "member_id"}))
public class GroupMembership {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_group_id", nullable = false)
    private MemberGroup memberGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private WorkspaceMember member;

    @ManyToMany
    @JoinTable(name = "group_membership_roles",
            joinColumns = @JoinColumn(name = "membership_id"),
            inverseJoinColumns = @JoinColumn(name = "group_role_id"))
    private Set<GroupRole> roles = new LinkedHashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    protected GroupMembership() {}

    public GroupMembership(MemberGroup memberGroup, WorkspaceMember member) {
        this.memberGroup = memberGroup;
        this.member = member;
    }

    public Long getId() { return id; }
    public MemberGroup getMemberGroup() { return memberGroup; }
    public WorkspaceMember getMember() { return member; }
    public Set<GroupRole> getRoles() { return roles; }
    public boolean isActive() { return active; }
    public void addRole(GroupRole role) { roles.add(role); }
    public void removeRole(GroupRole role) { roles.remove(role); }
    public void setActive(boolean active) { this.active = active; }
}
