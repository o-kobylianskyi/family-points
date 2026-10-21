package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.entity.WorkspaceMemberType;
import com.olehkobylianskyi.familypoints.entity.WorkspacePermission;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/workspaces/{workspaceId}/task-suggestions")
@PreAuthorize("@workspaceSecurity.canAccessWorkspace(#workspaceId)")
public class TaskSuggestionController {
    private final JdbcTemplate db;
    private final CurrentUserService current;

    public TaskSuggestionController(JdbcTemplate db, CurrentUserService current) {
        this.db = db;
        this.current = current;
    }

    public record SuggestionRequest(@NotBlank @Size(max=150) String title,
                                    @Size(max=1000) String description) {}
    public record Suggestion(Long id, Long requestedByMemberId, String title,
                             String description, String status, OffsetDateTime createdAt) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void suggest(@PathVariable Long workspaceId, @Valid @RequestBody SuggestionRequest request) {
        var member = current.getCurrentMember();
        if (!member.getWorkspace().getId().equals(workspaceId))
            throw new AccessDeniedException("Wrong workspace");
        // Suggestions cannot contain points, reputation, or assignment details.
        db.update("""
                INSERT INTO task_suggestions(workspace_id,requested_by_member_id,title,description)
                VALUES(?,?,?,?)
                """, workspaceId, member.getId(), request.title().trim(),
                request.description() == null ? "" : request.description().trim());
    }

    @GetMapping
    public List<Suggestion> list(@PathVariable Long workspaceId) {
        var member = current.getCurrentMember();
        boolean manager = member.getMemberType() != WorkspaceMemberType.CHILD
                && (member.hasPermission(WorkspacePermission.MANAGE_TASKS)
                || member.hasPermission(WorkspacePermission.APPROVE_TASKS)
                || member.hasPermission(WorkspacePermission.ADMIN_OVERRIDE));
        String sql = """
                SELECT id, requested_by_member_id, title, description, status, created_at
                FROM task_suggestions WHERE workspace_id=?
                """ + (manager ? "" : " AND requested_by_member_id=? ") + " ORDER BY created_at DESC LIMIT 100";
        return db.query(sql, (rs, index) -> new Suggestion(rs.getLong(1),rs.getLong(2),rs.getString(3),
                rs.getString(4),rs.getString(5),rs.getObject(6,OffsetDateTime.class)),
                manager ? new Object[]{workspaceId} : new Object[]{workspaceId,member.getId()});
    }

    @PostMapping("/{suggestionId}/decision")
    public void decide(@PathVariable Long workspaceId, @PathVariable Long suggestionId,
                       @RequestParam String decision) {
        var member = current.getCurrentMember();
        if (member.getMemberType() == WorkspaceMemberType.CHILD
                || (!member.hasPermission(WorkspacePermission.MANAGE_TASKS)
                && !member.hasPermission(WorkspacePermission.APPROVE_TASKS)
                && !member.hasPermission(WorkspacePermission.ADMIN_OVERRIDE)))
            throw new AccessDeniedException("Adult task approval required");
        if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid decision");
        int updated = db.update("""
                UPDATE task_suggestions
                SET status=?,reviewed_by_member_id=?,reviewed_at=CURRENT_TIMESTAMP
                WHERE id=? AND workspace_id=? AND status='PENDING'
                """, decision,member.getId(),suggestionId,workspaceId);
        if (updated != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Suggestion already reviewed");
        // Approval is conceptual only; a manager must separately create and fund a task.
    }
}
