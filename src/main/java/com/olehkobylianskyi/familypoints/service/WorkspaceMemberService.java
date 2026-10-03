package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class WorkspaceMemberService {
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
                                  TaskDefinitionRepository taskDefinitionRepository) {
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceRoleRepository = workspaceRoleRepository;
        this.userAccountRepository = userAccountRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.taskDefinitionRepository = taskDefinitionRepository;
    }

    public Optional<WorkspaceMember> createMember(Long workspaceId, String name, WorkspaceMemberType memberType, Long workspaceRoleId) {
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

    public Optional<WorkspaceMember> updateMember(Long workspaceId, Long memberId, String name, WorkspaceMemberType memberType, Long workspaceRoleId) {
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
        var memberOptional = workspaceMemberRepository.findByIdAndWorkspaceIdAndActiveTrue(memberId, workspaceId);
        if (memberOptional.isEmpty()) return false;
        var member = memberOptional.get();

        // Keep the member row so historical tasks, point transactions and purchases retain a valid FK.
        // The displayed identity is anonymized and the member disappears from active lists.
        userAccountRepository.findByWorkspaceMemberId(memberId).ifPresent(account -> {
            account.setEnabled(false);
            userAccountRepository.save(account);
        });
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
