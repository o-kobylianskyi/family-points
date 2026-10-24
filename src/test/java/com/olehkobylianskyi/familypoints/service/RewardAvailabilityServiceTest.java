package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.dto.*;
import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.RewardDefinitionRepository;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RewardAvailabilityServiceTest {
    private final RewardDefinitionRepository repository = mock(RewardDefinitionRepository.class);
    private final RewardAvailabilityService service = new RewardAvailabilityService(
            repository, Clock.fixed(Instant.parse("2026-10-26T15:00:00Z"), ZoneOffset.UTC));

    private RewardDefinition weeklyReward() {
        Workspace family = new Workspace("Example family");
        family.setTimeZone("Europe/Berlin");
        RewardDefinition reward = mock(RewardDefinition.class);
        when(reward.getWorkspace()).thenReturn(family);
        when(reward.getAvailabilityMode()).thenReturn(RewardAvailabilityMode.RECURRING_WEEKLY);
        when(reward.getWeeklyWindows()).thenReturn(List.of(new RewardWeeklyWindow(
                "mon-afternoon", 1, LocalTime.of(16,0), LocalTime.of(18,0), null, null, true)));
        return reward;
    }

    @Test void berlinMondayHasExactHalfOpenTimeBoundaries() {
        var reward = weeklyReward();
        assertFalse(service.evaluate(reward, Instant.parse("2026-10-26T14:59:59Z")).open());
        assertTrue(service.evaluate(reward, Instant.parse("2026-10-26T15:00:00Z")).open());
        assertTrue(service.evaluate(reward, Instant.parse("2026-10-26T16:59:59Z")).open());
        assertFalse(service.evaluate(reward, Instant.parse("2026-10-26T17:00:00Z")).open());
        assertEquals(Instant.parse("2026-10-26T17:00:00Z"),
                service.evaluate(reward, Instant.parse("2026-10-26T15:00:00Z")).availableUntil());
    }

    @Test void legacyRewardAlwaysAvailable() {
        var reward = mock(RewardDefinition.class);
        when(reward.getAvailabilityMode()).thenReturn(RewardAvailabilityMode.ALWAYS);
        assertTrue(service.evaluate(reward).open());
    }

    @Test void oneTimeWindowHasExactInstantBoundaries() {
        var reward = mock(RewardDefinition.class);
        when(reward.getAvailabilityMode()).thenReturn(RewardAvailabilityMode.ONE_TIME);
        when(reward.getAvailabilityStartsAt()).thenReturn(Instant.parse("2026-11-05T17:00:00Z"));
        when(reward.getAvailabilityEndsAt()).thenReturn(Instant.parse("2026-11-05T19:00:00Z"));
        assertFalse(service.evaluate(reward, Instant.parse("2026-11-05T16:59:59Z")).open());
        assertTrue(service.evaluate(reward, Instant.parse("2026-11-05T17:00:00Z")).open());
        assertFalse(service.evaluate(reward, Instant.parse("2026-11-05T19:00:00Z")).open());
    }

    @Test void dstGapCannotProduceReversedWindows() {
        var family = new Workspace("Example"); family.setTimeZone("Europe/Berlin");
        var reward = mock(RewardDefinition.class);
        when(reward.getWorkspace()).thenReturn(family);
        when(reward.getAvailabilityMode()).thenReturn(RewardAvailabilityMode.RECURRING_WEEKLY);
        when(reward.getWeeklyWindows()).thenReturn(List.of(new RewardWeeklyWindow(
                "spring-gap", 7, LocalTime.of(2,30), LocalTime.of(3,0), null, null, true)));
        assertTrue(service.windowsOn(reward, LocalDate.of(2026,3,29)).isEmpty());
    }

    @Test void overlapRejectedBeforeAnySave() {
        var reward = mock(RewardDefinition.class);
        when(repository.findForPurchase(7L,8L)).thenReturn(Optional.of(reward));
        var rules = List.of(new RewardWeeklyWindowRequest("first",1,LocalTime.of(16,0),
                    LocalTime.of(18,0),null,null,true),
                new RewardWeeklyWindowRequest("second",1,LocalTime.of(17,0),
                    LocalTime.of(19,0),null,null,true));
        assertThrows(IllegalArgumentException.class, () -> service.update(8L,7L,
                new RewardAvailabilityRequest(RewardAvailabilityMode.RECURRING_WEEKLY,
                        null,null,rules)));
        verify(reward, never()).setAvailabilityMode(any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test void inactiveRewardAvailabilityIsNotExposedToOrdinaryMembers() {
        var reward = mock(RewardDefinition.class);
        when(repository.findByIdAndWorkspaceId(7L, 8L)).thenReturn(Optional.of(reward));
        when(reward.isActive()).thenReturn(false);
        assertThrows(com.olehkobylianskyi.familypoints.exception.ResourceNotFoundException.class,
                () -> service.get(8L, 7L, false));
        verify(reward, never()).getWeeklyWindows();
    }
}
