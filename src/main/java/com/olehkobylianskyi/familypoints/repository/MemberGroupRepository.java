package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.MemberGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MemberGroupRepository extends JpaRepository<MemberGroup, Long> {
    List<MemberGroup> findByWorkspaceIdAndActiveTrueOrderByNameAsc(Long workspaceId);
    Optional<MemberGroup> findByIdAndWorkspaceId(Long id, Long workspaceId);
}
