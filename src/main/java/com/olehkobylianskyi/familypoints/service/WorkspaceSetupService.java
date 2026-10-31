package com.olehkobylianskyi.familypoints.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Durable metadata only. Never imports, activates, or deletes content. */
@Service
public class WorkspaceSetupService {
    public static final List<String> STEPS = List.of("MEMBERS", "MEMBER_GROUPS",
            "WORK_NODE_GROUPS", "CATALOG_SELECTION", "TASK_REVIEW",
            "REWARD_REVIEW", "FINAL_REVIEW");
    private final JdbcTemplate db;
    private final WorkspaceEpochTransactionGuard epochGuard;
    private final ObjectMapper mapper = new ObjectMapper();
    private final boolean enabled;

    public WorkspaceSetupService(JdbcTemplate db, WorkspaceEpochTransactionGuard epochGuard,
            @Value("${familypoints.setup.enabled:false}") boolean enabled) {
        this.db = db;
        this.epochGuard = epochGuard;
        this.enabled = enabled;
    }

    public record SetupState(Long workspaceId, long dataEpoch, long revision, String status,
                             Map<String, String> steps, OffsetDateTime completedAt) {}
    public record StepRequest(String status, Long expectedRevision) {}

    public SetupState current(long workspaceId) {
        requireEnabled();
        return read(workspaceId);
    }

    private SetupState read(long workspaceId) {
        var result = db.query("""
              SELECT s.workspace_id,s.data_epoch,s.revision,s.status,s.steps_json,s.completed_at
              FROM workspace_setup_states s JOIN families f ON f.id=s.workspace_id
              WHERE s.workspace_id=? AND f.data_epoch=s.data_epoch
              """, (rs, index) -> new SetupState(rs.getLong(1),rs.getLong(2),rs.getLong(3),
                 rs.getString(4),decode(rs.getString(5)),
                 rs.getObject(6, OffsetDateTime.class)), workspaceId);
        if (result.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SETUP_NOT_AVAILABLE");
        return result.get(0);
    }

    private Map<String,String> decode(String json) {
        try { return mapper.readValue(json, new TypeReference<LinkedHashMap<String,String>>() {}); }
        catch (Exception ex) { throw new IllegalStateException("Corrupt setup state", ex); }
    }

    private String encode(Map<String,String> value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    @Transactional
    public SetupState setStep(long workspaceId, String step, StepRequest request) {
        requireEnabled();
        if (!STEPS.contains(step) || request == null || request.expectedRevision() == null
                || !List.of("COMPLETE","SKIPPED").contains(request.status()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SETUP_INVALID_STEP");
        epochGuard.requireCurrentRequestEpoch(workspaceId);
        SetupState before = read(workspaceId);
        if (before.revision()!=request.expectedRevision())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SETUP_REVISION_STALE");
        if ("COMPLETED".equals(before.status()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SETUP_ALREADY_COMPLETED");
        var steps = new LinkedHashMap<>(before.steps());
        steps.put(step,request.status());
        int count = db.update("""
             UPDATE workspace_setup_states
             SET status='IN_PROGRESS',steps_json=?,revision=revision+1,updated_at=now()
             WHERE workspace_id=? AND revision=? AND data_epoch=?
             """, encode(steps), workspaceId, before.revision(), before.dataEpoch());
        if (count!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"SETUP_REVISION_STALE");
        return read(workspaceId);
    }

    @Transactional
    public SetupState complete(long workspaceId, long expectedRevision) {
        requireEnabled();
        epochGuard.requireCurrentRequestEpoch(workspaceId);
        var before=read(workspaceId);
        if (before.revision()!=expectedRevision)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"SETUP_REVISION_STALE");
        if ("COMPLETED".equals(before.status())) return before;
        if (!before.steps().keySet().containsAll(STEPS)
                || before.steps().values().stream().anyMatch(v -> !List.of("COMPLETE","SKIPPED").contains(v)))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"SETUP_STEPS_INCOMPLETE");
        int count=db.update("""
            UPDATE workspace_setup_states SET status='COMPLETED',
            revision=revision+1,completed_at=now(),updated_at=now()
            WHERE workspace_id=? AND revision=? AND data_epoch=?
            """,workspaceId,expectedRevision,before.dataEpoch());
        if(count!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"SETUP_REVISION_STALE");
        return read(workspaceId);
    }

    private void requireEnabled() {
        if(!enabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"SETUP_DISABLED");
    }
}
