package org.example.repository;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import org.example.model.ActivityLog;
import org.example.util.CassandraSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActivityLogRepository {
    private static final Logger logger = LoggerFactory.getLogger(ActivityLogRepository.class);
    private final CassandraSessionManager sessionManager;
    private final CqlSession session;
    private PreparedStatement insertStatement;
    private PreparedStatement selectAllByUserStatement;
    private PreparedStatement selectByUserAndTimeRangeStatement;
    private PreparedStatement selectRecentByUserStatement;
    private PreparedStatement selectByActivityTypeStatement;

    private static final int DEFAULT_TTL_SECONDS = 30 * 24 * 60 * 60;

    public ActivityLogRepository(CassandraSessionManager sessionManager) {
        this.sessionManager = sessionManager;
        this.session = sessionManager.getSession();
        prepareStatements();
    }

    private void prepareStatements() {
        insertStatement = session.prepare(
                "INSERT INTO user_activities (user_id, activity_id, activity_type, timestamp) " +
                        "VALUES (?, ?, ?, ?) USING TTL ?"
        );

        selectAllByUserStatement = session.prepare(
                "SELECT user_id, activity_id, activity_type, timestamp " +
                        "FROM user_activities " +
                        "WHERE user_id = ?"
        );

        selectByUserAndTimeRangeStatement = session.prepare(
                "SELECT user_id, activity_id, activity_type, timestamp " +
                        "FROM user_activities " +
                        "WHERE user_id = ? AND timestamp >= ? AND timestamp <= ?"
        );

        selectRecentByUserStatement = session.prepare(
                "SELECT user_id, activity_id, activity_type, timestamp " +
                        "FROM user_activities " +
                        "WHERE user_id = ? " +
                        "LIMIT ?"
        );

        selectByActivityTypeStatement = session.prepare(
                "SELECT user_id, activity_id, activity_type, timestamp " +
                        "FROM user_activities " +
                        "WHERE activity_type = ? " +
                        "ALLOW FILTERING"
        );
    }

    public void insertActivity(ActivityLog activityLog, Integer ttlSeconds) {
        try {
            int ttl = (ttlSeconds != null) ? ttlSeconds : DEFAULT_TTL_SECONDS;

            BoundStatement boundStatement = insertStatement.bind(
                    activityLog.getUserId(),
                    activityLog.getActivityId(),
                    activityLog.getActivityType(),
                    activityLog.getTimestamp(),
                    ttl
            );

            session.execute(boundStatement);
            logger.debug("Activity log inserted: userId={}, activityId={}, type={}, ttl={}s",
                    activityLog.getUserId(), activityLog.getActivityId(),
                    activityLog.getActivityType(), ttl);

        } catch (Exception e) {
            logger.error("Error inserting activity log", e);
            throw new RuntimeException("Failed to insert activity log", e);
        }
    }

    public void insertActivity(ActivityLog activityLog) {
        insertActivity(activityLog, null);
    }

    public List<ActivityLog> getAllActivitiesByUser(UUID userId) {
        try {
            BoundStatement boundStatement = selectAllByUserStatement.bind(userId);
            ResultSet resultSet = session.execute(boundStatement);

            List<ActivityLog> activities = new ArrayList<>();
            for (Row row : resultSet) {
                activities.add(mapRowToActivityLog(row));
            }

            logger.debug("Retrieved {} activities for user {}", activities.size(), userId);
            return activities;

        } catch (Exception e) {
            logger.error("Error retrieving user activities", e);
            throw new RuntimeException("Failed to retrieve user activities", e);
        }
    }

    public List<ActivityLog> getActivitiesByUserAndTimeRange(UUID userId, Instant startTime, Instant endTime) {
        try {
            BoundStatement boundStatement = selectByUserAndTimeRangeStatement.bind(
                    userId,
                    startTime,
                    endTime
            );

            ResultSet resultSet = session.execute(boundStatement);

            List<ActivityLog> activities = new ArrayList<>();
            for (Row row : resultSet) {
                activities.add(mapRowToActivityLog(row));
            }

            logger.debug("Retrieved {} activities for user {} in range {} - {}",
                    activities.size(), userId, startTime, endTime);
            return activities;

        } catch (Exception e) {
            logger.error("Error retrieving activities by time range", e);
            throw new RuntimeException("Failed to retrieve activities by time range", e);
        }
    }

    public List<ActivityLog> getRecentActivitiesByUser(UUID userId, int limit) {
        try {
            BoundStatement boundStatement = selectRecentByUserStatement.bind(userId, limit);
            ResultSet resultSet = session.execute(boundStatement);

            List<ActivityLog> activities = new ArrayList<>();
            for (Row row : resultSet) {
                activities.add(mapRowToActivityLog(row));
            }

            logger.debug("Retrieved {} recent activities for user {}", activities.size(), userId);
            return activities;

        } catch (Exception e) {
            logger.error("Error retrieving recent activities", e);
            throw new RuntimeException("Failed to retrieve recent activities", e);
        }
    }

    public List<ActivityLog> getActivitiesByType(String activityType) {
        try {
            BoundStatement boundStatement = selectByActivityTypeStatement.bind(activityType);
            ResultSet resultSet = session.execute(boundStatement);

            List<ActivityLog> activities = new ArrayList<>();
            for (Row row : resultSet) {
                activities.add(mapRowToActivityLog(row));
            }

            logger.debug("Retrieved {} activities by type {} (non-optimized query)", activities.size(), activityType);
            return activities;

        } catch (Exception e) {
            logger.error("Error retrieving activities by type", e);
            throw new RuntimeException("Failed to retrieve activities by type", e);
        }
    }

    public QueryResult<List<ActivityLog>> getAllActivitiesByUserWithTiming(UUID userId) {
        long startTime = System.nanoTime();
        List<ActivityLog> activities = getAllActivitiesByUser(userId);
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        return new QueryResult<>(activities, durationMs);
    }

    public QueryResult<List<ActivityLog>> getActivitiesByUserAndTimeRangeWithTiming(
            UUID userId, Instant startTime, Instant endTime) {
        long startTimeNs = System.nanoTime();
        List<ActivityLog> activities = getActivitiesByUserAndTimeRange(userId, startTime, endTime);
        long durationMs = (System.nanoTime() - startTimeNs) / 1_000_000;
        return new QueryResult<>(activities, durationMs);
    }

    public QueryResult<List<ActivityLog>> getRecentActivitiesByUserWithTiming(UUID userId, int limit) {
        long startTime = System.nanoTime();
        List<ActivityLog> activities = getRecentActivitiesByUser(userId, limit);
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        return new QueryResult<>(activities, durationMs);
    }

    public QueryResult<List<ActivityLog>> getActivitiesByTypeWithTiming(String activityType) {
        long startTime = System.nanoTime();
        List<ActivityLog> activities = getActivitiesByType(activityType);
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        return new QueryResult<>(activities, durationMs);
    }

    private ActivityLog mapRowToActivityLog(Row row) {
        return new ActivityLog(
                row.getUuid("user_id"),
                row.getUuid("activity_id"),
                row.getString("activity_type"),
                row.getInstant("timestamp")
        );
    }

    public static class QueryResult<T> {
        private final T result;
        private final long executionTimeMs;

        public QueryResult(T result, long executionTimeMs) {
            this.result = result;
            this.executionTimeMs = executionTimeMs;
        }

        public T getResult() {
            return result;
        }

        public long getExecutionTimeMs() {
            return executionTimeMs;
        }
    }
}
