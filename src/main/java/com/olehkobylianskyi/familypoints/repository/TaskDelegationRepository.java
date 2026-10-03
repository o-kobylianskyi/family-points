package com.olehkobylianskyi.familypoints.repository;
import com.olehkobylianskyi.familypoints.entity.TaskDelegation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TaskDelegationRepository extends JpaRepository<TaskDelegation,Long>{ List<TaskDelegation> findByTaskInstanceIdOrderByDelegatedAtAsc(Long taskInstanceId); }
