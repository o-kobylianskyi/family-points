package com.olehkobylianskyi.familypoints.repository;
import com.olehkobylianskyi.familypoints.entity.GroupComposition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface GroupCompositionRepository extends JpaRepository<GroupComposition,Long>{
 List<GroupComposition> findByParentGroupIdAndActiveTrueOrderByIdAsc(Long parentGroupId);
 List<GroupComposition> findByChildGroupIdAndActiveTrueOrderByIdAsc(Long childGroupId);
 Optional<GroupComposition> findByParentGroupIdAndChildGroupId(Long parentGroupId,Long childGroupId);
}
