package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.MemberExceptionPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MemberExceptionPeriodRepository
        extends JpaRepository<MemberExceptionPeriod, Long> {

    List<MemberExceptionPeriod>
    findByMemberIdOrderByStartDateDesc(Long memberId);

    @Query("""
            select e
            from MemberExceptionPeriod e
            where e.member.id = :memberId
              and :date between e.startDate and e.endDate
            order by e.id desc
            """)
    List<MemberExceptionPeriod> findActiveForDate(
            @Param("memberId") Long memberId,
            @Param("date") LocalDate date
    );

    boolean existsByMemberIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long memberId,
            LocalDate date1,
            LocalDate date2
    );
}