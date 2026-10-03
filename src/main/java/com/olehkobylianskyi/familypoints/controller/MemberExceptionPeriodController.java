package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.MemberExceptionPeriodCreateRequest;
import com.olehkobylianskyi.familypoints.dto.MemberExceptionPeriodResponse;
import com.olehkobylianskyi.familypoints.entity.MemberExceptionPeriod;
import com.olehkobylianskyi.familypoints.service.MemberExceptionPeriodService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(
        "/workspaces/{workspaceId}/members/{memberId}/exception-periods"
)
public class MemberExceptionPeriodController {

    private final MemberExceptionPeriodService service;

    public MemberExceptionPeriodController(
            MemberExceptionPeriodService service
    ) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<MemberExceptionPeriodResponse> create(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody MemberExceptionPeriodCreateRequest request
    ) {
        MemberExceptionPeriod period =
                service.create(
                        workspaceId,
                        memberId,
                        request.getType(),
                        request.getStartDate(),
                        request.getEndDate(),
                        request.getComment()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        MemberExceptionPeriodResponse.from(period)
                );
    }

    @GetMapping
    public List<MemberExceptionPeriodResponse> getPeriods(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId
    ) {
        return service
                .getPeriods(workspaceId, memberId)
                .stream()
                .map(MemberExceptionPeriodResponse::from)
                .toList();
    }

    @GetMapping("/check")
    public boolean check(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            @RequestParam LocalDate date
    ) {
        return service.isExcused(
                workspaceId,
                memberId,
                date
        );
    }
}