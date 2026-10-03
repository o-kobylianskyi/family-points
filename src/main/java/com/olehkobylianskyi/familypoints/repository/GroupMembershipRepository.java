package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {
    List<GroupMembership> findByMemberGroupIdAndActiveTrueOrderByIdAsc(Long memberGroupId);
    Optional<GroupMembership> findByMemberGroupIdAndMemberId(Long memberGroupId, Long memberId);
    List<GroupMembership> findByMemberIdAndActiveTrue(Long memberId);
}
