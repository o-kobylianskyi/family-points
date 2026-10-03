package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.GroupRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GroupRoleRepository extends JpaRepository<GroupRole, Long> {
    List<GroupRole> findByMemberGroupIdOrderByNameAsc(Long memberGroupId);
    Optional<GroupRole> findByIdAndMemberGroupId(Long id, Long memberGroupId);
    boolean existsByMemberGroupIdAndNameIgnoreCase(Long memberGroupId, String name);
}
