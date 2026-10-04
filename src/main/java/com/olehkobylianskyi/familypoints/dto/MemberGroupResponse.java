package com.olehkobylianskyi.familypoints.dto;
import com.olehkobylianskyi.familypoints.entity.*; import java.util.*;
public record MemberGroupResponse(Long id,String name,String description,boolean active,boolean showInNavigation,List<Role> roles,List<Member> members,List<ChildGroup> childGroups,List<Long> parentGroupIds,List<Balance> balances){
 public record Role(Long id,String name,String description,Long roleSetId){} public record Member(Long membershipId,Long memberId,String memberName,boolean active,List<Long> roleIds){}
 public record ChildGroup(Long compositionId,Long groupId,String groupName){} public record Balance(Long pointTypeId,String code,String name,long amount){}
 public static MemberGroupResponse from(MemberGroup g,List<GroupRole> roles,List<GroupMembership> memberships,List<GroupComposition> children,List<GroupComposition> parents,List<Balance> balances){
  return new MemberGroupResponse(g.getId(),g.getName(),g.getDescription(),g.isActive(),g.isShowInNavigation(),
   roles.stream().map(r->new Role(r.getId(),r.getName(),r.getDescription(),r.getRoleSet()==null?null:r.getRoleSet().getId())).toList(),
   memberships.stream().map(m->new Member(m.getId(),m.getMember().getId(),m.getMember().getName(),m.isActive(),m.getRoles().stream().map(GroupRole::getId).sorted().toList())).toList(),
   children.stream().map(c->new ChildGroup(c.getId(),c.getChildGroup().getId(),c.getChildGroup().getName())).toList(),
   parents.stream().map(c->c.getParentGroup().getId()).toList(), balances==null?List.of():balances);
 }
}
