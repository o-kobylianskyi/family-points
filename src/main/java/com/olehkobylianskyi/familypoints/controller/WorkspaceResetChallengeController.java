package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Fail-closed placeholder for the agreed BKL-002 challenge contract.
 * Never issues a challenge until reauthentication, concurrency-safe snapshot,
 * FK deletion graph and rollback tests are implemented and approved.
 */
@RestController
@RequestMapping("/workspaces/{workspaceId}/data-reset")
@PreAuthorize("@workspaceSecurity.canManageWorkspace(#workspaceId)")
public class WorkspaceResetChallengeController {
    private final CurrentUserService current;
    public WorkspaceResetChallengeController(CurrentUserService current) { this.current=current; }

    public record ChallengeRequest(UUID operationId, String snapshotDigest) {}

    @PostMapping("/challenge")
    public void challenge(@PathVariable Long workspaceId, @RequestBody ChallengeRequest request) {
        if (!current.belongsToWorkspace(workspaceId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong workspace");
        if (request == null || request.operationId() == null ||
                request.snapshotDigest() == null ||
                !request.snapshotDigest().matches("[0-9a-f]{64}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid challenge request");
        throw new ResponseStatusException(HttpStatus.LOCKED, "RESET_EXECUTION_DISABLED");
    }

    @PostMapping("/execute")
    public void execute(@PathVariable Long workspaceId) {
        if (!current.belongsToWorkspace(workspaceId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong workspace");
        throw new ResponseStatusException(HttpStatus.LOCKED, "RESET_EXECUTION_DISABLED");
    }
}
