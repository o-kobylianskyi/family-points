package com.olehkobylianskyi.familypoints.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.SecretKey;
import java.util.*;

/**
 * Read-only tenant-scoped restore preview; it never applies archive rows.
 * Conflict detection is conservative: any existing target rows block replace
 * until FK/remapping and explicit user conflict policy are implemented.
 */
@Service
public class WorkspacePortableRestorePreviewService {
    private static final Map<String,String> COUNT_SQL=Map.of(
        "PRIVATE_TEMPLATES","SELECT count(*) FROM workspace_private_templates WHERE workspace_id=?",
        "MEMBER_GROUPS","SELECT count(*) FROM member_groups WHERE family_id=?",
        "TASK_DEFINITIONS","SELECT count(*) FROM task_definitions WHERE family_id=?",
        "REWARD_DEFINITIONS","SELECT count(*) FROM reward_definitions WHERE family_id=?");
    private final JdbcTemplate db;
    private final WorkspaceEpochTransactionGuard epoch;
    private final ProtectiveBackupImportInspector inspector;
    public WorkspacePortableRestorePreviewService(JdbcTemplate db,WorkspaceEpochTransactionGuard epoch,
            ProtectiveBackupImportInspector inspector) {
        this.db=db;this.epoch=epoch;this.inspector=inspector;
    }
    public record CategoryPreview(String category,int archiveRows,long targetRows,boolean replaceSupported) {}
    public record Preview(long workspaceId,long targetEpoch,UUID snapshotId,
            List<CategoryPreview> categories,List<String> blockers,boolean executable) {}

    @Transactional
    public Preview preview(byte[] file,long workspaceId,long expectedEpoch,
            Collection<String> selected,SecretKey key) {
        if(selected==null || selected.isEmpty() || selected.stream().anyMatch(Objects::isNull))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"RESTORE_SELECTION_REQUIRED");
        var requested=new TreeSet<>(selected);
        var imported=inspector.inspect(file,workspaceId,key);
        if(!imported.availableCategories().containsAll(requested))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"CATEGORY_NOT_IN_ARCHIVE");
        long actual=epoch.lockCurrentEpoch(workspaceId);
        if(actual!=expectedEpoch)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"RESTORE_TARGET_EPOCH_STALE");
        var blockers=new TreeSet<String>(imported.blockers());
        var summaries=new ArrayList<CategoryPreview>();
        for(String category:requested) {
            String sql=COUNT_SQL.get(category);
            if(sql==null) {
                blockers.add("RESTORE_CATEGORY_MAPPING_REQUIRED:"+category);
                summaries.add(new CategoryPreview(category,imported.counts().get(category),-1,false));
                continue;
            }
            Long target=db.queryForObject(sql,Long.class,workspaceId);
            long targetCount=target==null?0:target;
            if(targetCount>0)blockers.add("RESTORE_TARGET_CONFLICT:"+category);
            blockers.add("RESTORE_FK_REMAP_UNVERIFIED:"+category);
            summaries.add(new CategoryPreview(category,imported.counts().get(category),targetCount,false));
        }
        return new Preview(workspaceId,actual,imported.snapshotId(),List.copyOf(summaries),
                List.copyOf(blockers),false);
    }
}
