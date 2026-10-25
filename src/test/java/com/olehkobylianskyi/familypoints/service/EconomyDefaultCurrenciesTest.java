package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.*;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EconomyDefaultCurrenciesTest {
    private final EconomySettingsRepository settings = mock(EconomySettingsRepository.class);
    private final WorkspaceRepository workspaces = mock(WorkspaceRepository.class);
    private final PointTypeRepository types = mock(PointTypeRepository.class);
    private final EconomyService economy = new EconomyService(settings, workspaces, types);

    @Test void initializesMissingDefaultCurrenciesWithoutDuplicatingExistingTypes() {
        var workspace = mock(Workspace.class);
        when(workspace.getId()).thenReturn(42L);
        when(workspaces.findById(42L)).thenReturn(Optional.of(workspace));
        when(settings.findByWorkspaceId(42L)).thenReturn(Optional.empty());
        var existingCopper = new PointType(workspace, "COPPER", "My copper", 42);
        existingCopper.setActive(false);
        when(types.findByWorkspaceIdAndCode(42L, "COPPER")).thenReturn(Optional.of(existingCopper));
        when(types.save(any(PointType.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(settings.save(any(EconomySettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = economy.getOrCreateSettings(42L);
        assertEquals(EconomyMode.SIMPLE, result.getMode());
        assertEquals("POINTS", result.getDefaultPointType().getCode());
        verify(types, times(3)).save(any(PointType.class));
        verify(types, never()).save(same(existingCopper));
        assertFalse(existingCopper.isActive());
        assertEquals("My copper", existingCopper.getName());
        verify(types).save(argThat(x -> "POINTS".equals(x.getCode())));
        verify(types).save(argThat(x -> "SILVER".equals(x.getCode())));
        verify(types).save(argThat(x -> "GOLD".equals(x.getCode())));
    }

    @Test void existingEconomyConfigurationRemainsUntouched() {
        var workspace = mock(Workspace.class);
        var points = new PointType(workspace, "POINTS", "Custom points", 88);
        var existing = new EconomySettings(workspace, EconomyMode.MULTI_TYPE, points);
        when(settings.findByWorkspaceId(42L)).thenReturn(Optional.of(existing));

        assertSame(existing, economy.getOrCreateSettings(42L));
        verifyNoInteractions(types, workspaces);
        verify(settings, never()).save(any());
    }
}
