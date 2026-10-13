package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.exception.InvalidTaskStateException;
import com.olehkobylianskyi.familypoints.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskManagerCompletionTest {
    @Mock TaskDefinitionRepository definitions;
    @Mock TaskScheduleSlotRepository taskScheduleSlots;
    @Mock TaskInstanceRepository instances;
    @Mock TaskAuthorizationService authorization;
    @Mock TaskGenerationService generation;
    @Mock PointService points;
    @Mock ReputationService reputation;
    @Mock TaskAuditService audit;
    @InjectMocks TaskService service;

    @Test
    void cannotCompleteFutureOccurrenceOrCreateItsInstance() throws Exception {
        try (var ignored = MockitoAnnotations.openMocks(this)) {
            var definition = mock(TaskDefinition.class);
            when(definitions.findForOccurrenceUpdate(39L, 8L)).thenReturn(Optional.of(definition));
            when(definition.isActive()).thenReturn(true);
            when(definition.getId()).thenReturn(39L);
            when(definition.getAssignedMember()).thenReturn(mock(WorkspaceMember.class));
            var date = LocalDate.now().plusDays(1);
            when(generation.shouldGenerate(definition, date)).thenReturn(true);

            assertThrows(InvalidTaskStateException.class,
                    () -> service.completeScheduledByManager(8L, 39L, date));
            verifyNoInteractions(instances, points, reputation);
        }
    }

    @Test
    void explicitSlotsBlockLegacyDefaultManagerCompletion() throws Exception {
        try (var ignored = MockitoAnnotations.openMocks(this)) {
            var definition = mock(TaskDefinition.class);
            var date = LocalDate.now();
            when(definitions.findForOccurrenceUpdate(39L, 8L)).thenReturn(Optional.of(definition));
            when(definition.getId()).thenReturn(39L);
            when(definition.isActive()).thenReturn(true);
            when(definition.getAssignedMember()).thenReturn(mock(WorkspaceMember.class));
            when(generation.shouldGenerate(definition, date)).thenReturn(true);
            when(taskScheduleSlots.findByTaskDefinitionIdOrderByScheduledTimeAscSlotKeyAsc(39L))
                    .thenReturn(java.util.List.of(mock(TaskScheduleSlot.class)));

            assertThrows(InvalidTaskStateException.class,
                    () -> service.completeScheduledByManager(8L, 39L, date, "default"));
            verifyNoInteractions(instances, points, reputation, audit);
        }
    }

    @Test
    void alreadyCompletedInstanceDoesNotDebitOrRewardAgain() throws Exception {
        try (var ignored = MockitoAnnotations.openMocks(this)) {
            var definition = mock(TaskDefinition.class);
            var done = mock(TaskInstance.class);
            var date = LocalDate.now();
            when(definitions.findForOccurrenceUpdate(39L, 8L)).thenReturn(Optional.of(definition));
            when(definition.isActive()).thenReturn(true);
            when(definition.getAssignedMember()).thenReturn(mock(WorkspaceMember.class));
            when(generation.shouldGenerate(definition, date)).thenReturn(true);
            when(instances.findByTaskDefinitionIdAndScheduledDateAndSlotKey(39L, date, "default")).thenReturn(Optional.of(done));
            when(done.getStatus()).thenReturn(TaskInstanceStatus.COMPLETED);

            assertSame(done, service.completeScheduledByManager(8L, 39L, date));
            verify(done, never()).complete();
            verify(instances, never()).save(any());
            verifyNoInteractions(points, reputation, audit);
        }
    }
}
