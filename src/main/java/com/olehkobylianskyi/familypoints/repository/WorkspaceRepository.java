package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
}