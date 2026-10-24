package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.WorkspaceMember;
import com.olehkobylianskyi.familypoints.exception.ResourceNotFoundException;
import com.olehkobylianskyi.familypoints.repository.*;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CalendarServiceSecurityTest {
    private final CurrentUserService user = mock(CurrentUserService.class);
    private final WorkspaceRepository workspaces = mock(WorkspaceRepository.class);
    private final WorkspaceMemberRepository members = mock(WorkspaceMemberRepository.class);
    private final TaskService tasks = mock(TaskService.class);
    private final RewardDefinitionRepository rewards = mock(RewardDefinitionRepository.class);
    private final RewardAvailabilityService availability = mock(RewardAvailabilityService.class);
    private final CalendarService calendar = new CalendarService(user,workspaces,members,tasks,rewards,
            availability,Clock.fixed(Instant.parse("2026-10-22T12:00:00Z"),ZoneOffset.UTC));

    private final LocalDate from = LocalDate.of(2026,10,22);

    @Test void foreignWorkspaceIsRejectedBeforeQueryingAnySchedule() {
        when(user.belongsToWorkspace(99L)).thenReturn(false);
        assertThrows(AccessDeniedException.class,
                () -> calendar.get(99L,from,from,null,"ALL"));
        verifyNoInteractions(workspaces,members,tasks,rewards,availability);
    }

    @Test void foreignMemberIdentifierDoesNotLeakOtherFamilysEvents() {
        when(user.belongsToWorkspace(8L)).thenReturn(true);
        when(members.findByIdAndWorkspaceIdAndActiveTrue(44L,8L))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> calendar.get(8L,from,from,44L,"ALL"));
        verifyNoInteractions(workspaces,tasks,rewards,availability);
    }

    @Test void viewerCannotOpenAnotherMembersCalendarWithoutPermission() {
        var member = mock(WorkspaceMember.class);
        var viewer = mock(WorkspaceMember.class);
        when(user.belongsToWorkspace(8L)).thenReturn(true);
        when(members.findByIdAndWorkspaceIdAndActiveTrue(12L,8L)).thenReturn(Optional.of(member));
        when(member.getId()).thenReturn(12L);
        when(viewer.getId()).thenReturn(3L);
        when(user.getCurrentMember()).thenReturn(viewer);
        assertThrows(AccessDeniedException.class,
                () -> calendar.get(8L,from,from,12L,"ALL"));
        verifyNoInteractions(workspaces,tasks,rewards,availability);
    }

    @Test void rejectsUnboundedRequestsBeforeRunningQueries() {
        when(user.belongsToWorkspace(8L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> calendar.get(8L,from,from.plusDays(62),null,"ALL"));
        verifyNoInteractions(workspaces,members,tasks,rewards,availability);
    }
}
