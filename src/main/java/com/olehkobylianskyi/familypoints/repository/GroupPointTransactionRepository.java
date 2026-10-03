package com.olehkobylianskyi.familypoints.repository;
import com.olehkobylianskyi.familypoints.entity.GroupPointTransaction; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*;
public interface GroupPointTransactionRepository extends JpaRepository<GroupPointTransaction,Long>{
 List<GroupPointTransaction> findByGroupIdOrderByCreatedAtDesc(Long groupId);
 @Query("select coalesce(sum(t.amount),0) from GroupPointTransaction t where t.group.id=:groupId and t.pointType.id=:pointTypeId") Long getBalance(@Param("groupId")Long groupId,@Param("pointTypeId")Long pointTypeId);
}
