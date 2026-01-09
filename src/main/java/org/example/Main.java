package org.example;

import org.example.config.CassandraConfig;
import org.example.model.ActivityLog;
import org.example.repository.ActivityLogRepository;
import org.example.repository.ActivityLogRepository.QueryResult;
import org.example.util.CassandraSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("=== Starting User Activity Log System Application ===");

        CassandraConfig config = new CassandraConfig();

        CassandraSessionManager sessionManager = null;
        ActivityLogRepository repository = null;

        try {
            sessionManager = new CassandraSessionManager(config);
            repository = new ActivityLogRepository(sessionManager);

            demonstrateActivityLogSystem(repository);

        } catch (Exception e) {
            logger.error("Critical error in application", e);
            System.exit(1);
        } finally {
            if (sessionManager != null) {
                sessionManager.close();
            }
        }

        logger.info("=== Application finished ===");
    }

    private static void demonstrateActivityLogSystem(ActivityLogRepository repository) {
        UUID userId = UUID.randomUUID();
        logger.info("Working with user: {}", userId);

        logger.info("\n--- 1. Inserting activity logs ---");
        insertSampleActivities(repository, userId);

        logger.info("\n--- 2. Getting all user activities ---");
        List<ActivityLog> allActivities = repository.getAllActivitiesByUser(userId);
        logger.info("Total activities: {}", allActivities.size());
        allActivities.forEach(activity -> logger.info("  - {}", activity));

        logger.info("\n--- 3. Getting last 5 activities ---");
        List<ActivityLog> recentActivities = repository.getRecentActivitiesByUser(userId, 5);
        logger.info("Last {} activities:", recentActivities.size());
        recentActivities.forEach(activity -> logger.info("  - {}", activity));

        logger.info("\n--- 4. Getting activities within time range ---");
        Instant now = Instant.now();
        Instant startTime = now.minus(1, ChronoUnit.HOURS);
        Instant endTime = now.plus(1, ChronoUnit.HOURS);
        List<ActivityLog> activitiesInRange = repository.getActivitiesByUserAndTimeRange(
                userId, startTime, endTime);
        logger.info("Activities in range {} - {}: {}", startTime, endTime, activitiesInRange.size());
        activitiesInRange.forEach(activity -> logger.info("  - {}", activity));

        logger.info("\n--- 5. Inserting activity with custom TTL (60 seconds) ---");
        ActivityLog ttlActivity = new ActivityLog(
                userId,
                UUID.randomUUID(),
                "test_ttl",
                Instant.now()
        );
        repository.insertActivity(ttlActivity, 60);
        logger.info("Inserted activity with TTL 60 seconds: {}", ttlActivity);

        logger.info("\n--- 6. Performance Comparison: Optimized vs Non-Optimized Queries ---");
        demonstratePerformanceComparison(repository, userId);
    }

    private static void insertSampleActivities(ActivityLogRepository repository, UUID userId) {
        String[] activityTypes = {"login", "view", "logout", "click", "search"};

        for (int i = 0; i < 10; i++) {
            ActivityLog activity = new ActivityLog(
                    userId,
                    UUID.randomUUID(),
                    activityTypes[i % activityTypes.length],
                    Instant.now().minus(i, ChronoUnit.MINUTES)
            );

            repository.insertActivity(activity);
            logger.info("Inserted activity: {}", activity);
        }
    }

    private static void demonstratePerformanceComparison(ActivityLogRepository repository, UUID userId) {
        logger.info("\n═══════════════════════════════════════════════════════════════════════════════");
        logger.info("PERFORMANCE COMPARISON: OPTIMIZED vs NON-OPTIMIZED QUERIES");
        logger.info("═══════════════════════════════════════════════════════════════════════════════");
        
        logger.info("\n[STEP 1] Creating large dataset (1000 activities) for performance testing...");
        String[] activityTypes = {"login", "view", "logout", "click", "search"};
        int largeDatasetSize = 1000;

        long insertStartTime = System.nanoTime();
        for (int i = 0; i < largeDatasetSize; i++) {
            ActivityLog activity = new ActivityLog(
                    userId,
                    UUID.randomUUID(),
                    activityTypes[i % activityTypes.length],
                    Instant.now().minus(i, ChronoUnit.SECONDS)
            );
            repository.insertActivity(activity);
        }
        long insertDurationMs = (System.nanoTime() - insertStartTime) / 1_000_000;
        logger.info("✓ Inserted {} activities in {} ms", largeDatasetSize, insertDurationMs);

        logger.info("\n[STEP 2] Warming up queries (to avoid cold start effects)...");
        repository.getAllActivitiesByUser(userId);
        repository.getActivitiesByType("login");
        logger.info("✓ Warm-up completed");

        logger.info("\n───────────────────────────────────────────────────────────────────────────────");
        logger.info("OPTIMIZED QUERIES (Single Partition Scan)");
        logger.info("───────────────────────────────────────────────────────────────────────────────");
        logger.info("Strategy: Using partition key (user_id) → Direct access to single partition");
        logger.info("Cassandra Operation: Single partition read (O(1) partition lookup)");
        logger.info("───────────────────────────────────────────────────────────────────────────────");

        QueryResult<List<ActivityLog>> optimizedAll = repository.getAllActivitiesByUserWithTiming(userId);
        logger.info("✓ Query 1: Get all activities for user");
        logger.info("  └─ Execution time: {} ms | Retrieved: {} activities | Partitions scanned: 1",
                optimizedAll.getExecutionTimeMs(), optimizedAll.getResult().size());

        QueryResult<List<ActivityLog>> optimizedRecent = repository.getRecentActivitiesByUserWithTiming(userId, 10);
        logger.info("✓ Query 2: Get recent 10 activities");
        logger.info("  └─ Execution time: {} ms | Retrieved: {} activities | Partitions scanned: 1",
                optimizedRecent.getExecutionTimeMs(), optimizedRecent.getResult().size());

        Instant now = Instant.now();
        Instant startTime = now.minus(30, ChronoUnit.MINUTES);
        Instant endTime = now.plus(1, ChronoUnit.MINUTES);
        QueryResult<List<ActivityLog>> optimizedRange = repository.getActivitiesByUserAndTimeRangeWithTiming(
                userId, startTime, endTime);
        logger.info("✓ Query 3: Get activities in time range (30 min)");
        logger.info("  └─ Execution time: {} ms | Retrieved: {} activities | Partitions scanned: 1",
                optimizedRange.getExecutionTimeMs(), optimizedRange.getResult().size());

        logger.info("\n───────────────────────────────────────────────────────────────────────────────");
        logger.info("NON-OPTIMIZED QUERIES (Full Table Scan)");
        logger.info("───────────────────────────────────────────────────────────────────────────────");
        logger.info("Strategy: Using ALLOW FILTERING without partition key → Scans ALL partitions");
        logger.info("Cassandra Operation: Full table scan (O(n) partition scans, where n = number of partitions)");
        logger.info("───────────────────────────────────────────────────────────────────────────────");

        QueryResult<List<ActivityLog>> nonOptimizedByType = repository.getActivitiesByTypeWithTiming("login");
        logger.info("✗ Query 4: Get activities by type 'login' (ALLOW FILTERING)");
        logger.info("  └─ Execution time: {} ms | Retrieved: {} activities | Partitions scanned: ALL",
                nonOptimizedByType.getExecutionTimeMs(), nonOptimizedByType.getResult().size());

        long avgOptimizedTime = (optimizedAll.getExecutionTimeMs() +
                optimizedRecent.getExecutionTimeMs() +
                optimizedRange.getExecutionTimeMs()) / 3;
        long nonOptimizedTime = nonOptimizedByType.getExecutionTimeMs();

        logger.info("\n═══════════════════════════════════════════════════════════════════════════════");
        logger.info("PERFORMANCE SUMMARY");
        logger.info("═══════════════════════════════════════════════════════════════════════════════");
        logger.info("Average optimized query time (single partition):   {} ms", avgOptimizedTime);
        logger.info("Non-optimized query time (full table scan):        {} ms", nonOptimizedTime);
        if (nonOptimizedTime > 0 && avgOptimizedTime > 0) {
            double speedup = (double) nonOptimizedTime / avgOptimizedTime;
            logger.info("Performance difference:                          {}x faster with optimized queries", 
                    String.format("%.2f", speedup));
        }
        
        logger.info("\n───────────────────────────────────────────────────────────────────────────────");
        logger.info("EXPLANATION");
        logger.info("───────────────────────────────────────────────────────────────────────────────");
        logger.info("✓ OPTIMIZED QUERIES:");
        logger.info("  • Use partition key (user_id) in WHERE clause");
        logger.info("  • Cassandra directly locates the partition → Single partition read");
        logger.info("  • Fast and efficient: O(1) partition lookup + O(m) rows in partition");
        logger.info("  • Scales well: Performance depends only on data in ONE partition");
        logger.info("");
        logger.info("✗ NON-OPTIMIZED QUERIES:");
        logger.info("  • Do NOT use partition key → Cannot locate specific partition");
        logger.info("  • Require ALLOW FILTERING → Cassandra scans ALL partitions");
        logger.info("  • Slow and inefficient: O(n) partition scans, where n = total partitions");
        logger.info("  • Does NOT scale: Performance degrades as number of partitions increases");
        logger.info("");
        logger.info("KEY TAKEAWAY:");
        logger.info("  With more data and more partitions, the performance difference becomes");
        logger.info("  exponentially more significant. In production with thousands of partitions,");
        logger.info("  non-optimized queries can be 100x-1000x slower!");
        logger.info("═══════════════════════════════════════════════════════════════════════════════\n");
    }
}
