package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.exception.InvalidPointExchangeException;
import com.olehkobylianskyi.familypoints.exception.PointExchangeRateAlreadyExistsException;
import com.olehkobylianskyi.familypoints.exception.ResourceNotFoundException;
import com.olehkobylianskyi.familypoints.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.olehkobylianskyi.familypoints.entity.WorkspacePermission;

import java.util.List;

@Service
public class PointExchangeService {

    private final PointExchangeRateRepository rateRepository;
    private final PointExchangeRequestRepository requestRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PointTypeRepository pointTypeRepository;
    private final PointService pointService;

    public PointExchangeService(
            PointExchangeRateRepository rateRepository,
            PointExchangeRequestRepository requestRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            WorkspaceRepository workspaceRepository,
            PointTypeRepository pointTypeRepository,
            PointService pointService
    ) {
        this.rateRepository = rateRepository;
        this.requestRepository = requestRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.workspaceRepository = workspaceRepository;
        this.pointTypeRepository = pointTypeRepository;
        this.pointService = pointService;
    }

    @Transactional
    public PointExchangeRate createRate(
            Long workspaceId,
            Long fromPointTypeId,
            Long toPointTypeId,
            int fromAmount,
            int toAmount,
            PointExchangeApprovalMode approvalMode
    ) {
        if (fromPointTypeId.equals(toPointTypeId)) {
            throw new InvalidPointExchangeException(
                    "Source and target point types must be different"
            );
        }

        if (rateRepository
                .existsByWorkspaceIdAndFromPointTypeIdAndToPointTypeId(
                        workspaceId,
                        fromPointTypeId,
                        toPointTypeId
                )) {

            throw new PointExchangeRateAlreadyExistsException(
                    "Exchange rate already exists"
            );
        }

        if (fromAmount <= 0 || toAmount <= 0) {
            throw new InvalidPointExchangeException(
                    "Exchange amounts must be greater than zero"
            );
        }

        Workspace workspace = workspaceRepository
                .findById(workspaceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace " + workspaceId + " not found"
                        )
                );

        PointType fromType =
                getPointTypeOrThrow(workspaceId, fromPointTypeId);

        PointType toType =
                getPointTypeOrThrow(workspaceId, toPointTypeId);

        if (!fromType.isActive() || !toType.isActive()) {
            throw new InvalidPointExchangeException(
                    "Both point types must be active"
            );
        }

        PointExchangeRate rate =
                new PointExchangeRate(
                        workspace,
                        fromType,
                        toType,
                        fromAmount,
                        toAmount,
                        approvalMode
                );

        return rateRepository.save(rate);
    }

    @Transactional(readOnly = true)
    public List<PointExchangeRate> getRates(Long workspaceId) {
        workspaceRepository
                .findById(workspaceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace " + workspaceId + " not found"
                        )
                );

        return rateRepository
                .findByWorkspaceIdAndActiveTrue(workspaceId);
    }

    @Transactional
    public PointExchangeRequest createExchange(
            Long workspaceId,
            Long memberId,
            Long exchangeRateId,
            int requestedFromAmount
    ) {
        WorkspaceMember member =
                getMemberOrThrow(workspaceId, memberId);

        PointExchangeRate rate =
                rateRepository
                        .findByIdAndWorkspaceIdAndActiveTrue(
                                exchangeRateId,
                                workspaceId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Active exchange rate "
                                                + exchangeRateId
                                                + " not found in workspace "
                                                + workspaceId
                                )
                        );

        if (requestedFromAmount <= 0) {
            throw new InvalidPointExchangeException(
                    "Exchange amount must be greater than zero"
            );
        }

        if (requestedFromAmount % rate.getFromAmount() != 0) {
            throw new InvalidPointExchangeException(
                    "Exchange amount must be a multiple of "
                            + rate.getFromAmount()
            );
        }

        int multiplier =
                requestedFromAmount / rate.getFromAmount();

        int calculatedToAmount;

        try {
            calculatedToAmount = Math.multiplyExact(
                    multiplier,
                    rate.getToAmount()
            );
        } catch (ArithmeticException exception) {
            throw new InvalidPointExchangeException(
                    "Exchange amount is too large"
            );
        }

        PointExchangeRequest request =
                new PointExchangeRequest(
                        member,
                        rate,
                        rate.getFromAmount(),
                        rate.getToAmount(),
                        requestedFromAmount,
                        calculatedToAmount
                );

        requestRepository.save(request);

        if (rate.getApprovalMode()
                == PointExchangeApprovalMode.AUTO) {

            executeExchange(
                    workspaceId,
                    request,
                    null
            );
        }

        return request;
    }

    @Transactional
    public PointExchangeRequest approve(
            Long workspaceId,
            Long requestId,
            Long parentMemberId
    ) {
        PointExchangeRequest request =
                getRequestForUpdateOrThrow(
                        workspaceId,
                        requestId
                );

        if (request.getStatus()
                != PointExchangeStatus.PENDING) {
            throw new InvalidPointExchangeException(
                    "Only pending exchange requests can be approved"
            );
        }

        WorkspaceMember parent =
                getMemberOrThrow(
                        workspaceId,
                        parentMemberId
                );

        if (!parent.hasPermission(WorkspacePermission.MANAGE_ECONOMY)) {
            throw new InvalidPointExchangeException(
                    "Member does not have permission to manage economy"
            );
        }

        executeExchange(
                workspaceId,
                request,
                parent
        );

        return request;
    }

    @Transactional
    public PointExchangeRequest reject(
            Long workspaceId,
            Long requestId,
            Long parentMemberId,
            String reason
    ) {
        PointExchangeRequest request =
                getRequestForUpdateOrThrow(
                        workspaceId,
                        requestId
                );

        if (request.getStatus()
                != PointExchangeStatus.PENDING) {
            throw new InvalidPointExchangeException(
                    "Only pending exchange requests can be rejected"
            );
        }

        WorkspaceMember parent =
                getMemberOrThrow(
                        workspaceId,
                        parentMemberId
                );

        if (!parent.hasPermission(WorkspacePermission.MANAGE_ECONOMY)) {
            throw new InvalidPointExchangeException(
                    "Member does not have permission to manage economy"
            );
        }

        request.reject(parent, reason);

        return request;
    }

    @Transactional(readOnly = true)
    public List<PointExchangeRequest> getPendingRequests(
            Long workspaceId
    ) {
        return requestRepository
                .findByMemberWorkspaceIdAndStatusOrderByRequestedAtDesc(
                        workspaceId,
                        PointExchangeStatus.PENDING
                );
    }

    private void executeExchange(
            Long workspaceId,
            PointExchangeRequest request,
            WorkspaceMember processedBy
    ) {
        Long memberId =
                request.getMember().getId();

        pointService.spend(
                workspaceId,
                memberId,
                request.getFromPointType().getId(),
                request.getFromAmount(),
                PointTransactionSourceType.POINT_EXCHANGE,
                request.getId(),
                "Point exchange"
        );

        pointService.earn(
                workspaceId,
                memberId,
                request.getToPointType().getId(),
                request.getToAmount(),
                PointTransactionSourceType.POINT_EXCHANGE,
                request.getId(),
                "Point exchange"
        );

        if (processedBy == null) {
            request.approveAutomatically();
        } else {
            request.approve(processedBy);
        }
    }

    private PointExchangeRequest getRequestForUpdateOrThrow(
            Long workspaceId,
            Long requestId
    ) {
        return requestRepository
                .findByIdAndWorkspaceIdForUpdate(
                        requestId,
                        workspaceId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Exchange request "
                                        + requestId
                                        + " not found in workspace "
                                        + workspaceId
                        )
                );
    }

    private WorkspaceMember getMemberOrThrow(
            Long workspaceId,
            Long memberId
    ) {
        return workspaceMemberRepository
                .findByIdAndWorkspaceId(
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

    private PointType getPointTypeOrThrow(
            Long workspaceId,
            Long pointTypeId
    ) {
        return pointTypeRepository
                .findByIdAndWorkspaceId(
                        pointTypeId,
                        workspaceId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Point type "
                                        + pointTypeId
                                        + " not found in workspace "
                                        + workspaceId
                        )
                );
    }
}