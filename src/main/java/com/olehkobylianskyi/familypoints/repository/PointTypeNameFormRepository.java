package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.PointTypeNameForm;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PointTypeNameFormRepository extends JpaRepository<PointTypeNameForm, Long> {
    List<PointTypeNameForm> findByPointTypeId(Long pointTypeId);
    Optional<PointTypeNameForm> findByPointTypeIdAndLanguage(Long pointTypeId, String language);
}
