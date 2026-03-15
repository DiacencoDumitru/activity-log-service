package org.example.web.dto;

import org.example.model.ActivityLog;

import java.time.Instant;
import java.util.UUID;

public class ActivityLogResponse {
    private UUID userId;
    private UUID activityId;
    private String activityType;
    private Instant timestamp;

    public ActivityLogResponse() {
    }

    public ActivityLogResponse(UUID userId, UUID activityId, String activityType, Instant timestamp) {
        this.userId = userId;
        this.activityId = activityId;
        this.activityType = activityType;
        this.timestamp = timestamp;
    }

    public static ActivityLogResponse from(ActivityLog log) {
        return new ActivityLogResponse(
                log.getUserId(),
                log.getActivityId(),
                log.getActivityType(),
                log.getTimestamp()
        );
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getActivityId() {
        return activityId;
    }

    public void setActivityId(UUID activityId) {
        this.activityId = activityId;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
