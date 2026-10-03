package com.olehkobylianskyi.familypoints.dto;

import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashSet;
import java.util.Set;

public class GroupMembershipCreateRequest {
    @NotNull private Long memberId;
    private Set<Long> roleIds = new LinkedHashSet<>();
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public Set<Long> getRoleIds() { return roleIds; }
    public void setRoleIds(Set<Long> roleIds) { this.roleIds = roleIds == null ? new LinkedHashSet<>() : roleIds; }
}
