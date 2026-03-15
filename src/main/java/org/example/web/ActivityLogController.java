package org.example.web;

import org.example.service.ActivityLogService;
import org.example.web.dto.ActivityLogRequest;
import org.example.web.dto.ActivityLogResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @PostMapping
    public ResponseEntity<ActivityLogResponse> recordActivity(
            @Valid @RequestBody ActivityLogRequest request,
            @RequestParam(required = false) Integer ttlSeconds) {
        ActivityLogResponse response = ttlSeconds != null
                ? activityLogService.recordActivity(request, ttlSeconds)
                : activityLogService.recordActivity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/users/{userId}")
    public List<ActivityLogResponse> getActivitiesByUser(@PathVariable UUID userId) {
        return activityLogService.getActivitiesByUser(userId);
    }

    @GetMapping("/users/{userId}/recent")
    public List<ActivityLogResponse> getRecentActivities(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "10") int limit) {
        return activityLogService.getRecentActivitiesByUser(userId, limit);
    }

    @GetMapping("/users/{userId}/range")
    public List<ActivityLogResponse> getActivitiesByTimeRange(
            @PathVariable UUID userId,
            @RequestParam Instant startTime,
            @RequestParam Instant endTime) {
        return activityLogService.getActivitiesByUserAndTimeRange(userId, startTime, endTime);
    }
}
