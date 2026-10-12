package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.dto.TaskScheduleOccurrenceResponse;
import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskVirtualScheduleTest {
    @Test
    void dailyTaskAppearsTomorrowWithoutPersistingInstances() {
        var definitions = mock(TaskDefinitionRepository.class);
        var instances = mock(TaskInstanceRepository.class);
        var audit = mock(TaskAuditService.class);
        var generator = new TaskGenerationService(definitions, instances, audit);
        var definition = mock(TaskDefinition.class);
        when(definition.isActive()).thenReturn(true);
        when(definition.getRecurrenceType()).thenReturn(TaskRecurrenceType.DAILY);
        when(definition.getStartDate()).thenReturn(LocalDate.of(2026, 10, 11));
        when(definition.getEndDate()).thenReturn(null);
        var tomorrow = LocalDate.of(2026, 10, 13);

        assertTrue(generator.shouldGenerate(definition, tomorrow));
        verifyNoInteractions(instances);
    }

    @Test
    void virtualOccurrenceHasStableIdentityAndNoDatabaseId() {
        var member = mock(WorkspaceMember.class);
        var definition = mock(TaskDefinition.class);
        when(definition.getId()).thenReturn(39L);
        when(definition.getTitle()).thenReturn("test everyday task");
        when(definition.getAssignedMember()).thenReturn(member);
        var day = LocalDate.of(2026, 10, 12);
        TaskScheduleOccurrenceResponse planned =
                TaskScheduleOccurrenceResponse.planned(definition, 7L, day);

        assertNull(planned.instanceId());
        assertEquals("PLANNED", planned.status());
        assertEquals("39:2026-10-12:default", planned.occurrenceKey());
        assertEquals(7L, planned.memberId());
    }

    @Test
    void doesNotProjectTaskBeforeStartDate() {
        var generator = new TaskGenerationService(mock(TaskDefinitionRepository.class),
                mock(TaskInstanceRepository.class), mock(TaskAuditService.class));
        var definition = mock(TaskDefinition.class);
        when(definition.isActive()).thenReturn(true);
        when(definition.getRecurrenceType()).thenReturn(TaskRecurrenceType.DAILY);
        when(definition.getStartDate()).thenReturn(LocalDate.of(2026, 10, 15));

        assertFalse(generator.shouldGenerate(definition, LocalDate.of(2026, 10, 12)));
    }
}
