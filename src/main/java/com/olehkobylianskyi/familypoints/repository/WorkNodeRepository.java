package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.WorkNode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkNodeRepository extends JpaRepository<WorkNode, Long> {
    java.util.Optional<WorkNode> findByIdAndWorkspaceId(Long id, Long workspaceId);
    java.util.List<WorkNode> findByParentNodeIdOrderBySortOrderAscIdAsc(Long parentNodeId);
}
