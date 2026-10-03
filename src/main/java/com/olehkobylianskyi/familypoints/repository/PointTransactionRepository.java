package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PointTransactionRepository
        extends JpaRepository<PointTransaction, Long> {

    List<PointTransaction>
    findByMemberIdOrderByCreatedAtDesc(Long memberId);

    @Query("""
            select coalesce(sum(t.amount), 0)
            from PointTransaction t
            where t.member.id = :memberId
              and t.pointType.id = :pointTypeId
            """)
    Long getBalance(
            @Param("memberId") Long memberId,
            @Param("pointTypeId") Long pointTypeId
    );
}