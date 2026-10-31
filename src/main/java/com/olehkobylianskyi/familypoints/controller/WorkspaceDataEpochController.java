package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Read-only BKL-002 generation handshake for authenticated offline clients. */
@RestController
@RequestMapping("/workspaces/{workspaceId}/data-epoch")
public class WorkspaceDataEpochController {
    private final JdbcTemplate db;
    private final CurrentUserService current;

    public WorkspaceDataEpochController(JdbcTemplate db, CurrentUserService current) {
        this.db=db;
        this.current=current;
    }

    public record DataEpoch(Long workspaceId, long dataEpoch) {}

    @GetMapping
    public DataEpoch current(@PathVariable Long workspaceId) {
        if (!current.belongsToWorkspace(workspaceId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong workspace");
        Long epoch=db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,workspaceId);
        if(epoch==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Workspace not found");
        return new DataEpoch(workspaceId,epoch);
    }
}
