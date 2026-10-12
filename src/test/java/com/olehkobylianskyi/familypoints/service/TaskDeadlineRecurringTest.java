package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;

class TaskDeadlineRecurringTest {
    @Test
    void missingDueTimeExpiresAtNextMidnight() {
        org.junit.jupiter.api.Assertions.assertEquals(LocalDateTime.of(2026,10,13,0,0),
                TaskDeadlineService.deadlineFor(LocalDate.of(2026,10,12),null));
        org.junit.jupiter.api.Assertions.assertEquals(LocalDateTime.of(2026,10,12,17,0),
                TaskDeadlineService.deadlineFor(LocalDate.of(2026,10,12),LocalTime.of(17,0)));
    }

    @Test
    void materializesOnlyOverdueMandatoryScheduledOccurrences() {
        var instances = mock(TaskInstanceRepository.class);
        var tasks = mock(TaskService.class);
        var definitions = mock(TaskDefinitionRepository.class);
        var generator = mock(TaskGenerationService.class);
        var workspaces = mock(WorkspaceRepository.class);
        var definition = mock(TaskDefinition.class);
        when(definitions.findByWorkspaceIdAndActiveTrue(8L)).thenReturn(List.of(definition));
        when(definition.isMandatory()).thenReturn(true);
        when(generator.shouldGenerate(eq(definition), any(LocalDate.class))).thenReturn(true);
        when(definition.getDueTime()).thenReturn(LocalTime.of(17, 0));
        when(definition.getAssignedMember()).thenReturn(mock(WorkspaceMember.class));
        when(instances.findCandidatesForDeadlineProcessing(any(), anyList())).thenReturn(List.of());

        new TaskDeadlineService(instances, tasks, definitions, workspaces, generator, mock(TaskScheduleSlotRepository.class))
                .processDeadlines(8L, LocalDateTime.of(2026, 10, 12, 16, 0));

        verify(generator, never()).generateForDefinition(definition, LocalDate.of(2026, 10, 12));
        verify(generator).generateForDefinition(definition, LocalDate.of(2026, 10, 11));
        verifyNoInteractions(tasks);
    }
}
