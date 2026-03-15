package org.example.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public class ActivityLogRequest {
    @NotNull
    private UUID userId;
    @NotBlank
    private String activityType;
    private Instant timestamp;

    public ActivityLogRequest() {
    }

    public ActivityLogRequest(UUID userId, String activityType, Instant timestamp) {
        this.userId = userId;
        this.activityType = activityType;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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
