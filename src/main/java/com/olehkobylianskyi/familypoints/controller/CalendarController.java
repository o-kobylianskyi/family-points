package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.CalendarEventResponse;
import com.olehkobylianskyi.familypoints.service.CalendarService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workspaces/{workspaceId}/calendar")
public class CalendarController {
    private final CalendarService calendar;

    public CalendarController(CalendarService calendar) { this.calendar = calendar; }

    @GetMapping
    public List<CalendarEventResponse> get(@PathVariable Long workspaceId,
            @RequestParam LocalDate from, @RequestParam LocalDate to,
            @RequestParam(required = false) Long memberId,
            @RequestParam(defaultValue = "ALL") String type) {
        return calendar.get(workspaceId, from, to, memberId, type);
    }

    @GetMapping("/time-zone")
    public Map<String, String> timeZone(@PathVariable Long workspaceId) {
        return Map.of("timeZone", calendar.getZone(workspaceId));
    }

    public record TimeZoneRequest(@NotBlank String timeZone) {}
    @PutMapping("/time-zone")
    public Map<String, String> changeTimeZone(@PathVariable Long workspaceId,
                                              @Valid @RequestBody TimeZoneRequest request) {
        return Map.of("timeZone", calendar.updateZone(workspaceId, request.timeZone()));
    }
}
