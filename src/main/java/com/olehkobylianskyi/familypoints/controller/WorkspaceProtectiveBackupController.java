package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.service.WorkspaceProtectiveBackupService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/** Metadata registration only; does not export, verify, restore or reset data. */
@RestController
@RequestMapping("/workspaces/{workspaceId}/protective-backups")
@PreAuthorize("@workspaceSecurity.canManageWorkspace(#workspaceId)")
public class WorkspaceProtectiveBackupController {
    private final WorkspaceProtectiveBackupService backups;
    private final CurrentUserService current;
    public WorkspaceProtectiveBackupController(WorkspaceProtectiveBackupService backups,
            CurrentUserService current){this.backups=backups;this.current=current;}
    @PostMapping
    public ResponseEntity<WorkspaceProtectiveBackupService.Backup> request(
            @PathVariable Long workspaceId,
            @RequestBody WorkspaceProtectiveBackupService.Request request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(backups.request(workspaceId,current.getCurrentMemberId(),request));
    }
    @GetMapping
    public ResponseEntity<java.util.List<WorkspaceProtectiveBackupService.Backup>> list(
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue="25") int limit) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(backups.list(workspaceId,limit));
    }

    @GetMapping("/{snapshotId}")
    public ResponseEntity<WorkspaceProtectiveBackupService.Backup> get(
            @PathVariable Long workspaceId,@PathVariable UUID snapshotId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(backups.get(workspaceId,snapshotId));
    }
}
