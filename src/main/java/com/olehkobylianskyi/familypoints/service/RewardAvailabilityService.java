package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.dto.*;
import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.exception.ResourceNotFoundException;
import com.olehkobylianskyi.familypoints.repository.RewardDefinitionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

@Service
public class RewardAvailabilityService {
    private final RewardDefinitionRepository rewards;
    private final Clock clock;

    public RewardAvailabilityService(RewardDefinitionRepository rewards, Clock clock) {
        this.rewards = rewards;
        this.clock = clock;
    }

    public record Window(String key, LocalDate date, Instant start, Instant end) {}
    public record Evaluation(boolean open, Instant availableUntil, Instant nextOpenAt, String reason) {}

    public Evaluation evaluate(RewardDefinition reward) {
        return evaluate(reward, clock.instant());
    }

    public Evaluation evaluate(RewardDefinition reward, Instant now) {
        if ((reward.getAvailabilityMode() == null || reward.getAvailabilityMode() == RewardAvailabilityMode.ALWAYS)) {
            return new Evaluation(true, null, null, "ALWAYS");
        }
        if (reward.getAvailabilityMode() == RewardAvailabilityMode.ONE_TIME) {
            Instant start = reward.getAvailabilityStartsAt(), end = reward.getAvailabilityEndsAt();
            if (start == null || end == null) return new Evaluation(false, null, null, "NOT_CONFIGURED");
            if (!now.isBefore(start) && now.isBefore(end)) return new Evaluation(true, end, null, "OPEN");
            return new Evaluation(false, null, now.isBefore(start) ? start : null,
                    now.isBefore(start) ? "NOT_STARTED" : "ENDED");
        }
        if (reward.getAvailabilityMode() != RewardAvailabilityMode.RECURRING_WEEKLY)
            throw new IllegalStateException("Unknown reward availability mode");
        ZoneId zone = ZoneId.of(reward.getWorkspace().getTimeZone());
        LocalDate today = now.atZone(zone).toLocalDate();
        Instant next = null, until = null;
        for (int d = 0; d <= 366; d++) {
            for (Window window : windowsOn(reward, today.plusDays(d))) {
                if (!now.isBefore(window.start()) && now.isBefore(window.end())) {
                    if (until == null || window.end().isBefore(until)) until = window.end();
                } else if (window.start().isAfter(now) && (next == null || window.start().isBefore(next))) {
                    next = window.start();
                }
            }
        }
        if (until != null) return new Evaluation(true, until, null, "OPEN");
        return new Evaluation(false, null, next, next == null ? "NO_UPCOMING_WINDOW" : "OUTSIDE_WINDOW");
    }

    /**
     * Virtual windows on a workspace-local day, no database writes. Spring's ZoneId rules
     * resolve a DST gap by moving forward and choose the earlier offset in overlaps.
     */
    public List<Window> windowsOn(RewardDefinition reward, LocalDate date) {
        ZoneId zone = ZoneId.of(reward.getWorkspace().getTimeZone());
        if ((reward.getAvailabilityMode() == null || reward.getAvailabilityMode() == RewardAvailabilityMode.ALWAYS)) return List.of();
        if (reward.getAvailabilityMode() == RewardAvailabilityMode.ONE_TIME) {
            Instant from = reward.getAvailabilityStartsAt(), to = reward.getAvailabilityEndsAt();
            if (from == null || to == null) return List.of();
            Instant dayStart = date.atStartOfDay(zone).toInstant();
            Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
            return from.isBefore(dayEnd) && to.isAfter(dayStart)
                    ? List.of(new Window("one-time", date, from, to)) : List.of();
        }
        if (reward.getAvailabilityMode() != RewardAvailabilityMode.RECURRING_WEEKLY)
            throw new IllegalStateException("Unknown availability mode");
        List<Window> windows = new ArrayList<>();
        for (RewardWeeklyWindow w : reward.getWeeklyWindows()) {
            if (!w.isEnabled() || w.getDayOfWeek() != date.getDayOfWeek().getValue()
                    || w.getEffectiveFromDate() != null && date.isBefore(w.getEffectiveFromDate())
                    || w.getEffectiveToDate() != null && date.isAfter(w.getEffectiveToDate())) continue;
            Instant from = date.atTime(w.getLocalStartTime()).atZone(zone).toInstant();
            Instant to = date.atTime(w.getLocalEndTime()).atZone(zone).toInstant();
            // A forward DST gap can collapse or reverse a local window on that date.
            if (to.isAfter(from)) windows.add(new Window(w.getWindowId(), date, from, to));
        }
        windows.sort(Comparator.comparing(Window::start).thenComparing(Window::key));
        return windows;
    }

    public void requireOpenForPurchase(RewardDefinition reward) {
        if (!evaluate(reward).open()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "REWARD_OUTSIDE_AVAILABILITY_WINDOW");
        }
    }

    @Transactional(readOnly = true)
    public RewardAvailabilityResponse get(Long workspaceId, Long rewardId) {
        return response(findReward(workspaceId, rewardId));
    }

    @Transactional
    public RewardAvailabilityResponse update(Long workspaceId, Long rewardId, RewardAvailabilityRequest request) {
        RewardDefinition reward = rewards.findForPurchase(rewardId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Reward not found"));
        validate(request);
        reward.setAvailabilityMode(request.mode());
        reward.setAvailabilityStartsAt(request.mode() == RewardAvailabilityMode.ONE_TIME
                ? request.startsAt().toInstant() : null);
        reward.setAvailabilityEndsAt(request.mode() == RewardAvailabilityMode.ONE_TIME
                ? request.endsAt().toInstant() : null);
        reward.getWeeklyWindows().clear();
        if (request.mode() == RewardAvailabilityMode.RECURRING_WEEKLY) {
            for (RewardWeeklyWindowRequest w : request.weeklyWindows()) {
                reward.getWeeklyWindows().add(new RewardWeeklyWindow(
                        w.windowId(), w.dayOfWeek(), w.localStartTime(), w.localEndTime(),
                        w.effectiveFromDate(), w.effectiveToDate(), w.enabled()));
            }
        }
        rewards.saveAndFlush(reward);
        return response(reward);
    }

    private RewardDefinition findReward(Long workspaceId, Long rewardId) {
        return rewards.findByIdAndWorkspaceId(rewardId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Reward not found"));
    }

    public RewardAvailabilityResponse response(RewardDefinition reward) {
        Evaluation e = evaluate(reward);
        List<RewardWeeklyWindowRequest> windows = reward.getWeeklyWindows().stream()
                .map(w -> new RewardWeeklyWindowRequest(w.getWindowId(), w.getDayOfWeek(),
                        w.getLocalStartTime(), w.getLocalEndTime(),
                        w.getEffectiveFromDate(), w.getEffectiveToDate(), w.isEnabled()))
                .toList();
        return new RewardAvailabilityResponse(reward.getId(), reward.getAvailabilityMode(),
                reward.getWorkspace().getTimeZone(), reward.getAvailabilityStartsAt(),
                reward.getAvailabilityEndsAt(), windows, e.open(), e.availableUntil(), e.nextOpenAt(), e.reason());
    }

    private static void validate(RewardAvailabilityRequest request) {
        if (request == null || request.mode() == null)
            throw new IllegalArgumentException("Availability mode is required");
        if (request.mode() == RewardAvailabilityMode.ONE_TIME) {
            if (request.startsAt() == null || request.endsAt() == null
                    || !request.startsAt().toInstant().isBefore(request.endsAt().toInstant()))
                throw new IllegalArgumentException("ONE_TIME needs ordered startsAt/endsAt instants");
        }
        if (request.mode() != RewardAvailabilityMode.RECURRING_WEEKLY) return;
        List<RewardWeeklyWindowRequest> list = request.weeklyWindows();
        if (list == null || list.size() > 56)
            throw new IllegalArgumentException("weeklyWindows must contain at most 56 entries");
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            RewardWeeklyWindowRequest w = list.get(i);
            if (w == null || w.windowId() == null || !w.windowId().matches("[A-Za-z0-9_-]{1,64}")
                    || !ids.add(w.windowId()) || w.dayOfWeek() < 1 || w.dayOfWeek() > 7
                    || w.localStartTime() == null || w.localEndTime() == null
                    || !w.localStartTime().isBefore(w.localEndTime())
                    || w.effectiveFromDate() != null && w.effectiveToDate() != null
                    && w.effectiveFromDate().isAfter(w.effectiveToDate()))
                throw new IllegalArgumentException("Invalid recurring window at position " + i);
            for (int j = 0; j < i; j++) {
                RewardWeeklyWindowRequest other = list.get(j);
                if (other.dayOfWeek() != w.dayOfWeek()
                        || !w.localStartTime().isBefore(other.localEndTime())
                        || !other.localStartTime().isBefore(w.localEndTime())) continue;
                LocalDate aFrom = w.effectiveFromDate(), aTo = w.effectiveToDate();
                LocalDate bFrom = other.effectiveFromDate(), bTo = other.effectiveToDate();
                if ((aTo == null || bFrom == null || !aTo.isBefore(bFrom))
                        && (bTo == null || aFrom == null || !bTo.isBefore(aFrom)))
                    throw new IllegalArgumentException("Overlapping windows for the same weekday");
            }
        }
    }
}
