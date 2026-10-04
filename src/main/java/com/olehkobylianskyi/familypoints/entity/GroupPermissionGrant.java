package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "group_permission_grants")
public class GroupPermissionGrant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_group_id", nullable = false)
    private MemberGroup memberGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_role_id", nullable = false)
    private GroupRole groupRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private GroupPermission permission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GroupPermissionScope scope;

    protected GroupPermissionGrant() {}

    public GroupPermissionGrant(MemberGroup memberGroup, GroupRole groupRole, GroupPermission permission, GroupPermissionScope scope) {
        this.memberGroup = memberGroup;
        this.groupRole = groupRole;
        this.permission = permission;
        this.scope = scope;
    }

    public Long getId() { return id; }
    public MemberGroup getMemberGroup() { return memberGroup; }
    public GroupRole getGroupRole() { return groupRole; }
    public GroupPermission getPermission() { return permission; }
    public GroupPermissionScope getScope() { return scope; }
}
