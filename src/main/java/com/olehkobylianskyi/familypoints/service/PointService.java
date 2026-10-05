package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.exception.InsufficientPointsException;
import com.olehkobylianskyi.familypoints.exception.ResourceNotFoundException;
import com.olehkobylianskyi.familypoints.repository.WorkspaceMemberRepository;
import com.olehkobylianskyi.familypoints.repository.PointTransactionRepository;
import com.olehkobylianskyi.familypoints.repository.PointTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.olehkobylianskyi.familypoints.repository.EconomySettingsRepository;
import com.olehkobylianskyi.familypoints.dto.PointBalancesResponse;
import com.olehkobylianskyi.familypoints.dto.PointTypeBalanceResponse;

import java.util.List;

@Service
public class PointService {

    //public static final String DEFAULT_POINT_TYPE_CODE = "POINTS";

    private final EconomySettingsRepository economySettingsRepository;

    private final PointTransactionRepository pointTransactionRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final PointTypeRepository pointTypeRepository;

    public PointService(
            PointTransactionRepository pointTransactionRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            PointTypeRepository pointTypeRepository,
            EconomySettingsRepository economySettingsRepository
    ) {
        this.pointTransactionRepository = pointTransactionRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.pointTypeRepository = pointTypeRepository;
        this.economySettingsRepository = economySettingsRepository;
    }

    @Transactional
    public PointTransaction earn(
            Long workspaceId,
            Long memberId,
            int amount,
            String description
    ) {
        WorkspaceMember member =
                getMemberOrThrow(workspaceId, memberId);

        PointType pointType =
                getDefaultPointTypeOrThrow(workspaceId);

        return saveTransaction(
                member,
                pointType,
                amount,
                PointTransactionType.EARN,
                PointTransactionSourceType.MANUAL,
                null,
                description
        );
    }

    @Transactional
    public PointTransaction earn(
            Long workspaceId,
            Long memberId,
            String pointTypeCode,
            int amount,
            PointTransactionSourceType sourceType,
            Long sourceId,
            String description
    ) {
        WorkspaceMember member = getMemberOrThrow(workspaceId, memberId);
        PointType pointType = getPointTypeOrThrow(workspaceId, pointTypeCode);

        return saveTransaction(
                member,
                pointType,
                amount,
                PointTransactionType.EARN,
                sourceType,
                sourceId,
                description
        );
    }

    @Transactional
    public PointTransaction penalty(
            Long workspaceId,
            Long memberId,
            int amount,
            String description
    ) {
        WorkspaceMember member = getMemberOrThrow(workspaceId, memberId);
        PointType pointType = getDefaultPointTypeOrThrow(workspaceId);

        return saveTransaction(
                member,
                pointType,
                -amount,
                PointTransactionType.PENALTY,
                PointTransactionSourceType.MANUAL,
                null,
                description
        );
    }

    @Transactional
    public PointTransaction spend(
            Long workspaceId,
            Long memberId,
            int amount,
            String description
    ) {
        WorkspaceMember member =
                getMemberForUpdateOrThrow(workspaceId, memberId);

        PointType pointType =
                getDefaultPointTypeOrThrow(workspaceId);

        long balance =
                getBalanceInternal(memberId, pointType.getId());

        if (balance < amount) {
            throw new InsufficientPointsException(
                    amount,
                    balance
            );
        }

        return saveTransaction(
                member,
                pointType,
                -amount,
                PointTransactionType.SPEND,
                PointTransactionSourceType.MANUAL,
                null,
                description
        );
    }

    @Transactional(readOnly = true)
    public long getBalance(
            Long workspaceId,
            Long memberId
    ) {
        getMemberOrThrow(workspaceId, memberId);

        PointType pointType =
                getDefaultPointTypeOrThrow(workspaceId);

        return getBalanceInternal(
                memberId,
                pointType.getId()
        );
    }

    @Transactional(readOnly = true)
    public List<PointTransaction> getHistory(
            Long workspaceId,
            Long memberId
    ) {
        getMemberOrThrow(workspaceId, memberId);

        return pointTransactionRepository
                .findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    private PointTransaction saveTransaction(
            WorkspaceMember member,
            PointType pointType,
            int amount,
            PointTransactionType transactionType,
            PointTransactionSourceType sourceType,
            Long sourceId,
            String description
    ) {
        PointTransaction transaction =
                new PointTransaction(
                        member,
                        pointType,
                        amount,
                        transactionType,
                        sourceType,
                        sourceId,
                        description
                );

        return pointTransactionRepository.save(transaction);
    }

    private WorkspaceMember getMemberOrThrow(
            Long workspaceId,
            Long memberId
    ) {
        return workspaceMemberRepository
                .findByIdAndWorkspaceId(memberId, workspaceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace member "
                                        + memberId
                                        + " not found in workspace "
                                        + workspaceId
                        )
                );
    }

    private WorkspaceMember getMemberForUpdateOrThrow(
            Long workspaceId,
            Long memberId
    ) {
        return workspaceMemberRepository
                .findByIdAndWorkspaceIdForUpdate(
                        memberId,
                        workspaceId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace member "
                                        + memberId
                                        + " not found in workspace "
                                        + workspaceId
                        )
                );
    }

    private PointType getDefaultPointTypeOrThrow(
            Long workspaceId
    ) {
        return economySettingsRepository
                .findByWorkspaceId(workspaceId)
                .map(EconomySettings::getDefaultPointType)
                .orElseGet(() ->
                        getPointTypeOrThrow(
                                workspaceId,
                                "POINTS"
                        )
                );
    }

    private PointType getPointTypeOrThrow(
            Long workspaceId,
            String code
    ) {
        return pointTypeRepository
                .findByWorkspaceIdAndCode(workspaceId, code)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Point type "
                                        + code
                                        + " not found in workspace "
                                        + workspaceId
                        )
                );
    }

    private long getBalanceInternal(
            Long memberId,
            Long pointTypeId
    ) {
        Long balance =
                pointTransactionRepository
                        .getBalance(
                                memberId,
                                pointTypeId
                        );

        return balance == null ? 0 : balance;
    }

    @Transactional
    public PointTransaction spend(
            Long workspaceId,
            Long memberId,
            Long pointTypeId,
            int amount,
            PointTransactionSourceType sourceType,
            Long sourceId,
            String description
    ) {
        WorkspaceMember member =
                getMemberForUpdateOrThrow(workspaceId, memberId);

        PointType pointType =
                getPointTypeOrThrow(workspaceId, pointTypeId);

        long balance =
                getBalanceInternal(
                        memberId,
                        pointType.getId()
                );

        if (balance < amount) {
            throw new InsufficientPointsException(
                    amount,
                    balance
            );
        }

        return saveTransaction(
                member,
                pointType,
                -amount,
                PointTransactionType.SPEND,
                sourceType,
                sourceId,
                description
        );
    }

    @Transactional
    public PointTransaction earn(
            Long workspaceId,
            Long memberId,
            Long pointTypeId,
            int amount,
            PointTransactionSourceType sourceType,
            Long sourceId,
            String description
    ) {
        WorkspaceMember member =
                getMemberOrThrow(workspaceId, memberId);

        PointType pointType =
                getPointTypeOrThrow(workspaceId, pointTypeId);

        return saveTransaction(
                member,
                pointType,
                amount,
                PointTransactionType.EARN,
                sourceType,
                sourceId,
                description
        );
    }

    private PointType getPointTypeOrThrow(
            Long workspaceId,
            Long pointTypeId
    ) {
        return pointTypeRepository
                .findByIdAndWorkspaceId(pointTypeId, workspaceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Point type "
                                        + pointTypeId
                                        + " not found in workspace "
                                        + workspaceId
                        )
                );
    }

    @Transactional(readOnly = true)
    public PointBalancesResponse getBalances(
            Long workspaceId,
            Long memberId
    ) {
        getMemberOrThrow(workspaceId, memberId);

        List<PointType> pointTypes =
                pointTypeRepository
                        .findByWorkspaceIdAndActiveTrueOrderBySortOrderAsc(
                                workspaceId
                        );

        List<PointTypeBalanceResponse> balances =
                pointTypes.stream()
                        .map(pointType ->
                                new PointTypeBalanceResponse(
                                        pointType.getId(),
                                        pointType.getCode(),
                                        pointType.getName(),
                                        getBalanceInternal(
                                                memberId,
                                                pointType.getId()
                                        )
                                )
                        )
                        .toList();

        return new PointBalancesResponse(
                memberId,
                balances
        );
    }

    @Transactional
    public PointTransaction refund(
            Long workspaceId,
            Long memberId,
            Long pointTypeId,
            int amount,
            PointTransactionSourceType sourceType,
            Long sourceId,
            String description
    ) {
        WorkspaceMember member = getMemberOrThrow(workspaceId, memberId);
        PointType pointType = getPointTypeOrThrow(workspaceId, pointTypeId);

        return saveTransaction(
                member,
                pointType,
                amount,
                PointTransactionType.REFUND,
                sourceType,
                sourceId,
                description
        );
    }

    @Transactional
    public PointTransaction penalty(
            Long workspaceId,
            Long memberId,
            PointType pointType,
            int amount,
            PointTransactionSourceType sourceType,
            Long sourceId,
            String description
    ) {
        WorkspaceMember member =
                getMemberOrThrow(workspaceId, memberId);

        return saveTransaction(
                member,
                pointType,
                -amount,
                PointTransactionType.PENALTY,
                sourceType,
                sourceId,
                description
        );
    }
}