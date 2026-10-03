package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.EconomySettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EconomySettingsRepository
        extends JpaRepository<EconomySettings, Long> {

    Optional<EconomySettings> findByWorkspaceId(Long workspaceId);
}