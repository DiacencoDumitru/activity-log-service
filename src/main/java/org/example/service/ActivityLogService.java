package org.example.service;

import org.example.model.ActivityLog;
import org.example.repository.ActivityLogRepository;
import org.example.web.dto.ActivityLogRequest;
import org.example.web.dto.ActivityLogResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ActivityLogService {

    private final ActivityLogRepository repository;

    public ActivityLogService(ActivityLogRepository repository) {
        this.repository = repository;
    }

    public ActivityLogResponse recordActivity(ActivityLogRequest request) {
        return recordActivity(request, null);
    }

    public ActivityLogResponse recordActivity(ActivityLogRequest request, Integer ttlSeconds) {
        Instant ts = request.getTimestamp() != null ? request.getTimestamp() : Instant.now();
        ActivityLog log = new ActivityLog(
                request.getUserId(),
                UUID.randomUUID(),
                request.getActivityType(),
                ts
        );
        repository.insertActivity(log, ttlSeconds);
        return ActivityLogResponse.from(log);
    }

    public List<ActivityLogResponse> getActivitiesByUser(UUID userId) {
        return repository.getAllActivitiesByUser(userId).stream()
                .map(ActivityLogResponse::from)
                .collect(Collectors.toList());
    }

    public List<ActivityLogResponse> getRecentActivitiesByUser(UUID userId, int limit) {
        return repository.getRecentActivitiesByUser(userId, limit).stream()
                .map(ActivityLogResponse::from)
                .collect(Collectors.toList());
    }

    public List<ActivityLogResponse> getActivitiesByUserAndTimeRange(
            UUID userId, Instant startTime, Instant endTime) {
        return repository.getActivitiesByUserAndTimeRange(userId, startTime, endTime).stream()
                .map(ActivityLogResponse::from)
                .collect(Collectors.toList());
    }
}
