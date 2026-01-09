package org.example.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ActivityLog {
    private UUID userId;
    private UUID activityId;
    private String activityType;
    private Instant timestamp;

    public ActivityLog() {
    }

    public ActivityLog(UUID userId, UUID activityId, String activityType, Instant timestamp) {
        this.userId = userId;
        this.activityId = activityId;
        this.activityType = activityType;
        this.timestamp = timestamp;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActivityLog that = (ActivityLog) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(activityId, that.activityId) &&
                Objects.equals(activityType, that.activityType) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, activityId, activityType, timestamp);
    }

    @Override
    public String toString() {
        return "ActivityLog{" +
                "userId=" + userId +
                ", activityId=" + activityId +
                ", activityType='" + activityType + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
