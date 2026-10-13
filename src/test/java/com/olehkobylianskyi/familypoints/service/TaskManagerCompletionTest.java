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
    @Mock TaskRewardNegotiationService negotiations;
    @Mock MemberExceptionPeriodRepository exceptionPeriods;
    @Mock com.olehkobylianskyi.familypoints.security.CurrentUserService currentUser;
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
    void distinctSlotsEarnIndependentlyAndRepeatConfirmationDoesNotRewardAgain() throws Exception {
        try (var ignored = MockitoAnnotations.openMocks(this)) {
            var definition = mock(TaskDefinition.class);
            var member = mock(WorkspaceMember.class);
            var pointType = mock(PointType.class);
            var slotMorning = mock(TaskScheduleSlot.class);
            var slotEvening = mock(TaskScheduleSlot.class);
            var date = LocalDate.now();
            when(definition.getId()).thenReturn(39L);
            when(definition.isActive()).thenReturn(true);
            when(definition.getAssignedMember()).thenReturn(member);
            when(definition.getTitle()).thenReturn("Clean room");
            when(definition.getRewardAmount()).thenReturn(1);
            when(definition.getRewardPointType()).thenReturn(pointType);
            when(definition.getRewardReputationAmount()).thenReturn(2);
            when(member.getId()).thenReturn(7L);
            when(pointType.getId()).thenReturn(17L);
            when(definitions.findForOccurrenceUpdate(39L, 8L)).thenReturn(Optional.of(definition));
            when(generation.shouldGenerate(definition, date)).thenReturn(true);
            when(taskScheduleSlots.findByTaskDefinitionIdAndSlotKeyAndEnabledTrue(39L, "morning"))
                    .thenReturn(Optional.of(slotMorning));
            when(taskScheduleSlots.findByTaskDefinitionIdAndSlotKeyAndEnabledTrue(39L, "evening"))
                    .thenReturn(Optional.of(slotEvening));
            when(slotMorning.getScheduledTime()).thenReturn(java.time.LocalTime.of(9, 0));
            when(slotEvening.getScheduledTime()).thenReturn(java.time.LocalTime.of(16, 0));
            var first = new TaskInstance(definition, member, date).withSlot("morning", java.time.LocalTime.of(9, 0));
            var second = new TaskInstance(definition, member, date).withSlot("evening", java.time.LocalTime.of(16, 0));
            org.springframework.test.util.ReflectionTestUtils.setField(first, "id", 81L);
            org.springframework.test.util.ReflectionTestUtils.setField(second, "id", 82L);
            when(instances.findByTaskDefinitionIdAndScheduledDateAndSlotKey(39L, date, "morning"))
                    .thenReturn(Optional.of(first));
            when(instances.findByTaskDefinitionIdAndScheduledDateAndSlotKey(39L, date, "evening"))
                    .thenReturn(Optional.of(second));

            assertSame(first, service.completeScheduledByManager(8L, 39L, date, "morning"));
            assertEquals(TaskInstanceStatus.COMPLETED, first.getStatus());
            assertEquals(TaskInstanceStatus.PENDING, second.getStatus());
            assertSame(first, service.completeScheduledByManager(8L, 39L, date, "morning"));
            assertSame(second, service.completeScheduledByManager(8L, 39L, date, "evening"));
            assertTrue(first.isRewardProcessed());
            assertTrue(second.isRewardProcessed());

            verify(points, times(2)).earn(eq(8L), eq(7L), eq(17L), eq(1),
                    eq(PointTransactionSourceType.TASK), anyLong(), anyString());
            verify(reputation, times(2)).add(eq(8L), eq(7L), eq(2),
                    eq(ReputationTransactionSourceType.TASK), anyLong(), anyString());
            verify(negotiations, times(2)).processApprovedOverride(eq(8L), any(TaskInstance.class));
        }
    }

    @Test
    void eachMissedSlotPenalizesOnlyOnceAndKeepsOtherSlotIndependent() throws Exception {
        try (var ignored = MockitoAnnotations.openMocks(this)) {
            var definition = mock(TaskDefinition.class);
            var member = mock(WorkspaceMember.class);
            var pointType = mock(PointType.class);
            var date = LocalDate.now().minusDays(1);
            when(definition.getAssignedMember()).thenReturn(member);
            when(definition.getTitle()).thenReturn("Clean room");
            when(definition.isMandatory()).thenReturn(true);
            when(definition.getPenaltyPointType()).thenReturn(pointType);
            when(definition.getPenaltyAmount()).thenReturn(1);
            when(definition.getPenaltyReputationAmount()).thenReturn(2);
            when(member.getId()).thenReturn(7L);
            when(exceptionPeriods.findActiveForDate(7L, date)).thenReturn(java.util.List.of());
            var morning = new TaskInstance(definition, member, date).withSlot("morning", java.time.LocalTime.of(9, 0));
            var evening = new TaskInstance(definition, member, date).withSlot("evening", java.time.LocalTime.of(16, 0));
            org.springframework.test.util.ReflectionTestUtils.setField(morning, "id", 71L);
            org.springframework.test.util.ReflectionTestUtils.setField(evening, "id", 72L);
            when(instances.findByIdAndWorkspaceIdForUpdate(71L, 8L)).thenReturn(Optional.of(morning));
            when(instances.findByIdAndWorkspaceIdForUpdate(72L, 8L)).thenReturn(Optional.of(evening));

            assertSame(morning, service.miss(8L, 71L));
            assertEquals(TaskInstanceStatus.MISSED, morning.getStatus());
            assertEquals(TaskInstanceStatus.PENDING, evening.getStatus());
            assertSame(morning, service.miss(8L, 71L));
            assertSame(evening, service.miss(8L, 72L));
            assertTrue(morning.isPenaltyProcessed());
            assertTrue(evening.isPenaltyProcessed());
            verify(points).penalty(eq(8L), eq(7L), same(pointType), eq(1),
                    eq(PointTransactionSourceType.TASK), eq(71L), anyString());
            verify(points).penalty(eq(8L), eq(7L), same(pointType), eq(1),
                    eq(PointTransactionSourceType.TASK), eq(72L), anyString());
            verify(reputation).add(eq(8L), eq(7L), eq(-2),
                    eq(ReputationTransactionSourceType.TASK), eq(71L), anyString());
            verify(reputation).add(eq(8L), eq(7L), eq(-2),
                    eq(ReputationTransactionSourceType.TASK), eq(72L), anyString());
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
