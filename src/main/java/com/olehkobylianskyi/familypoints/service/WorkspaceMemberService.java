package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class WorkspaceMemberService {
    private final com.olehkobylianskyi.familypoints.service.WorkspaceEpochTransactionGuard epochGuard;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceRoleRepository workspaceRoleRepository;
    private final UserAccountRepository userAccountRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final TaskDefinitionRepository taskDefinitionRepository;

    public WorkspaceMemberService(WorkspaceMemberRepository workspaceMemberRepository,
                                  WorkspaceRepository workspaceRepository,
                                  WorkspaceRoleRepository workspaceRoleRepository,
                                  UserAccountRepository userAccountRepository,
                                  GroupMembershipRepository groupMembershipRepository,
                                  TaskDefinitionRepository taskDefinitionRepository, com.olehkobylianskyi.familypoints.service.WorkspaceEpochTransactionGuard epochGuard) {
        this.epochGuard = epochGuard;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceRoleRepository = workspaceRoleRepository;
        this.userAccountRepository = userAccountRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.taskDefinitionRepository = taskDefinitionRepository;
    }

    @org.springframework.transaction.annotation.Transactional
    public Optional<WorkspaceMember> createMember(Long workspaceId, String name, WorkspaceMemberType memberType, Long workspaceRoleId) {
        epochGuard.requireCurrentRequestEpoch(workspaceId);
        var workspace = workspaceRepository.findById(workspaceId);
        if (workspace.isEmpty()) return Optional.empty();
        var role = workspaceRoleRepository.findByIdAndWorkspaceId(workspaceRoleId, workspaceId);
        if (role.isEmpty()) return Optional.empty();
        return Optional.of(workspaceMemberRepository.save(new WorkspaceMember(name.trim(), memberType, role.get(), workspace.get())));
    }

    public List<WorkspaceMember> getMembers(Long workspaceId) {
        return workspaceMemberRepository.findByWorkspaceIdAndActiveTrue(workspaceId);
    }

    public Optional<WorkspaceMember> getMember(Long workspaceId, Long memberId) {
        return workspaceMemberRepository.findByIdAndWorkspaceIdAndActiveTrue(memberId, workspaceId);
    }

    @org.springframework.transaction.annotation.Transactional
    public Optional<WorkspaceMember> updateMember(Long workspaceId, Long memberId, String name, WorkspaceMemberType memberType, Long workspaceRoleId) {
        epochGuard.requireCurrentRequestEpoch(workspaceId);
        var memberOptional = workspaceMemberRepository.findByIdAndWorkspaceIdAndActiveTrue(memberId, workspaceId);
        if (memberOptional.isEmpty()) return Optional.empty();
        var member = memberOptional.get();
        if (name != null && !name.isBlank()) member.setName(name.trim());
        if (memberType != null) member.setMemberType(memberType);
        if (workspaceRoleId != null) {
            var role = workspaceRoleRepository.findByIdAndWorkspaceId(workspaceRoleId, workspaceId);
            if (role.isEmpty()) return Optional.empty();
            member.setWorkspaceRole(role.get());
        }
        return Optional.of(workspaceMemberRepository.save(member));
    }

    @Transactional
    public boolean deleteMember(Long workspaceId, Long memberId) {
        epochGuard.requireCurrentRequestEpoch(workspaceId);
        var memberOptional = workspaceMemberRepository.findByIdAndWorkspaceIdAndActiveTrue(memberId, workspaceId);
        if (memberOptional.isEmpty()) return false;
        var member = memberOptional.get();

        // Keep the member row so historical tasks, point transactions and purchases retain a valid FK.
        // The displayed identity is anonymized and the member disappears from active lists.
        // A workspace member is not the global account. Revoking this member
        // must not disable authentication or active memberships in other families.
        // CurrentUserService/WorkspaceMembershipResolver reject this member once inactive.
        for (var membership : groupMembershipRepository.findByMemberIdAndActiveTrue(memberId)) {
            membership.setActive(false);
        }

        // Do not generate new work for a deleted actor. Historical definitions/instances stay in DB.
        for (var definition : taskDefinitionRepository.findByWorkspaceIdAndActiveTrue(workspaceId)) {
            boolean routesToDeletedMember =
                    (definition.getAssignedMember() != null && memberId.equals(definition.getAssignedMember().getId())) ||
                    (definition.getResponsibleMember() != null && memberId.equals(definition.getResponsibleMember().getId())) ||
                    (definition.getPreferredMember() != null && memberId.equals(definition.getPreferredMember().getId()));
            if (routesToDeletedMember) definition.setActive(false);
        }

        member.softDelete();
        workspaceMemberRepository.save(member);
        return true;
    }
}
