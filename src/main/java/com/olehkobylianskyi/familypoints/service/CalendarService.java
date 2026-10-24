package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.dto.CalendarEventResponse;
import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.exception.ResourceNotFoundException;
import com.olehkobylianskyi.familypoints.repository.*;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class CalendarService {
    private final CurrentUserService currentUser;
    private final WorkspaceRepository workspaces;
    private final WorkspaceMemberRepository members;
    private final TaskService tasks;
    private final RewardDefinitionRepository rewards;
    private final RewardAvailabilityService availability;
    private final Clock clock;

    public CalendarService(CurrentUserService currentUser, WorkspaceRepository workspaces,
                           WorkspaceMemberRepository members, TaskService tasks,
                           RewardDefinitionRepository rewards, RewardAvailabilityService availability, Clock clock) {
        this.currentUser = currentUser;
        this.workspaces = workspaces;
        this.members = members;
        this.tasks = tasks;
        this.rewards = rewards;
        this.availability = availability;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> get(Long workspaceId, LocalDate from, LocalDate to,
                                           Long requestedMemberId, String type) {
        requireWorkspace(workspaceId);
        if (from == null || to == null || from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > 61)
            throw new IllegalArgumentException("Calendar range must contain 1..62 days");
        if (type == null) type = "ALL";
        if (!Set.of("ALL", "TASK", "REWARD").contains(type))
            throw new IllegalArgumentException("type must be ALL, TASK or REWARD");
        Long memberId = requestedMemberId == null ? currentUser.getCurrentMemberId() : requestedMemberId;
        WorkspaceMember member = members.findByIdAndWorkspaceIdAndActiveTrue(memberId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found"));
        WorkspaceMember viewer = currentUser.getCurrentMember();
        if (!member.getId().equals(viewer.getId())
                && !viewer.hasPermission(WorkspacePermission.VIEW_ALL_TASKS)
                && !viewer.hasPermission(WorkspacePermission.ADMIN_OVERRIDE))
            throw new AccessDeniedException("Cannot view another member's calendar");

        Workspace workspace = workspaces.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        ZoneId zone = ZoneId.of(workspace.getTimeZone());
        Instant now = clock.instant();
        List<CalendarEventResponse> result = new ArrayList<>();
        if (!"REWARD".equals(type)) {
            for (var task : tasks.getMemberScheduleRange(workspaceId, memberId, from, to)) {
                Instant dueAt = task.scheduledTime() == null ? null
                        : task.scheduledDate().atTime(task.scheduledTime()).atZone(zone).toInstant();
                result.add(new CalendarEventResponse("TASK", task.occurrenceKey(), task.scheduledDate(),
                        task.title(), task.status(), memberId, task.taskDefinitionId(), task.instanceId(),
                        null, task.scheduledTime(), dueAt, null, null, false));
                if (result.size() > 2500) throw new IllegalArgumentException("Calendar has too many events");
            }
        }
        if (!"TASK".equals(type)) {
            for (RewardDefinition reward : rewards.findActiveWithAvailability(workspaceId)) {
                if (reward.getAvailabilityMode() == RewardAvailabilityMode.ALWAYS) continue;
                for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
                    for (var window : availability.windowsOn(reward, date)) {
                        boolean isOpen = !now.isBefore(window.start()) && now.isBefore(window.end());
                        result.add(new CalendarEventResponse("REWARD_WINDOW",
                                reward.getId() + ":" + date + ":" + window.key(),
                                date, reward.getTitle(), isOpen ? "OPEN" : "CLOSED",
                                memberId, null, null, reward.getId(), null, null,
                                window.start(), window.end(), isOpen));
                        if (result.size() > 2500) throw new IllegalArgumentException("Calendar has too many events");
                    }
                }
            }
        }
        result.sort(Comparator.comparing(CalendarEventResponse::date)
                .thenComparing(c -> c.dueAt() != null ? c.dueAt() :
                        c.startsAt() != null ? c.startsAt() : c.date().atStartOfDay(zone).toInstant())
                .thenComparing(CalendarEventResponse::key));
        return List.copyOf(result);
    }

    @Transactional(readOnly = true)
    public String getZone(Long workspaceId) {
        requireWorkspace(workspaceId);
        return workspaces.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found")).getTimeZone();
    }

    @Transactional
    public String updateZone(Long workspaceId, String timeZone) {
        requireWorkspace(workspaceId);
        if (!currentUser.getCurrentMember().hasPermission(WorkspacePermission.MANAGE_WORKSPACE)
                && !currentUser.getCurrentMember().hasPermission(WorkspacePermission.ADMIN_OVERRIDE))
            throw new AccessDeniedException("MANAGE_WORKSPACE required");
        Workspace workspace = workspaces.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        try {
            if (timeZone == null || timeZone.isBlank()) throw new IllegalArgumentException("timeZone is required");
            ZoneId zone = ZoneId.of(timeZone);
            // Disallow raw +/- offsets; IANA region identifiers are required for DST correctness.
            if (!zone.getId().contains("/")) throw new IllegalArgumentException("Expected IANA region timeZone");
            workspace.setTimeZone(zone.getId());
            return workspace.getTimeZone();
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Invalid IANA timeZone", exception);
        }
    }

    private void requireWorkspace(Long id) {
        if (!currentUser.belongsToWorkspace(id)) throw new AccessDeniedException("Workspace access denied");
    }
}
