package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.exception.ResourceNotFoundException;
import com.olehkobylianskyi.familypoints.repository.WorkspaceMemberRepository;
import com.olehkobylianskyi.familypoints.repository.MemberExceptionPeriodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class MemberExceptionPeriodService {

    private final MemberExceptionPeriodRepository exceptionPeriodRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public MemberExceptionPeriodService(
            MemberExceptionPeriodRepository exceptionPeriodRepository,
            WorkspaceMemberRepository workspaceMemberRepository
    ) {
        this.exceptionPeriodRepository = exceptionPeriodRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Transactional
    public MemberExceptionPeriod create(
            Long workspaceId,
            Long memberId,
            MemberExceptionType type,
            LocalDate startDate,
            LocalDate endDate,
            String comment
    ) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date"
            );
        }

        WorkspaceMember member =
                workspaceMemberRepository
                        .findByIdAndWorkspaceId(memberId, workspaceId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Workspace member "
                                                + memberId
                                                + " not found in workspace "
                                                + workspaceId
                                )
                        );

        return exceptionPeriodRepository.save(
                new MemberExceptionPeriod(
                        member,
                        type,
                        startDate,
                        endDate,
                        comment
                )
        );
    }

    @Transactional(readOnly = true)
    public List<MemberExceptionPeriod> getPeriods(
            Long workspaceId,
            Long memberId
    ) {
        verifyMember(workspaceId, memberId);

        return exceptionPeriodRepository
                .findByMemberIdOrderByStartDateDesc(memberId);
    }

    @Transactional(readOnly = true)
    public boolean isExcused(
            Long workspaceId,
            Long memberId,
            LocalDate date
    ) {
        verifyMember(workspaceId, memberId);

        return exceptionPeriodRepository
                .existsByMemberIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        memberId,
                        date,
                        date
                );
    }

    private void verifyMember(
            Long workspaceId,
            Long memberId
    ) {
        if (!workspaceMemberRepository
                .findByIdAndWorkspaceId(memberId, workspaceId)
                .isPresent()) {

            throw new ResourceNotFoundException(
                    "Workspace member "
                            + memberId
                            + " not found in workspace "
                            + workspaceId
            );
        }
    }
}