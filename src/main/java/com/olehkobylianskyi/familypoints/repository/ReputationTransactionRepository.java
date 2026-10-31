package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.ReputationTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReputationTransactionRepository extends JpaRepository<ReputationTransaction, Long> {

    List<ReputationTransaction> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    @Query("""
            select coalesce(sum(t.amount), 0)
            from ReputationTransaction t
            where t.member.id = :memberId
            """)
    Long getBalance(@Param("memberId") Long memberId);
}
