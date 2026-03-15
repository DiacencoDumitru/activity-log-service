package org.example.util;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import org.example.config.CassandraConfig.CassandraSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;

public class CassandraSessionManager {
    private static final Logger logger = LoggerFactory.getLogger(CassandraSessionManager.class);
    private final CassandraSessionConfig config;
    private CqlSession session;

    public CassandraSessionManager(CassandraSessionConfig config) {
        this.config = config;
    }

    public CqlSession getSession() {
        if (session == null || session.isClosed()) {
            initializeSession();
        }
        return session;
    }

    private static final int MAX_CONNECT_ATTEMPTS = 15;
    private static final int RETRY_DELAY_SECONDS = 3;

    private void initializeSession() {
        logger.info("Initializing connection to Cassandra (contact points: {}:{})...",
                config.getContactPoints(), config.getPort());

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_CONNECT_ATTEMPTS; attempt++) {
            try {
                DriverConfigLoader configLoader = DriverConfigLoader.programmaticBuilder()
                        .withString(DefaultDriverOption.REQUEST_CONSISTENCY, "QUORUM")
                        .withString(DefaultDriverOption.REQUEST_SERIAL_CONSISTENCY, "SERIAL")
                        .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(30))
                        .withDuration(DefaultDriverOption.CONNECTION_INIT_QUERY_TIMEOUT, Duration.ofSeconds(10))
                        .build();

                CqlSessionBuilder sessionBuilder = CqlSession.builder()
                        .withConfigLoader(configLoader)
                        .withLocalDatacenter(config.getDatacenter());
                for (String contactPoint : config.getContactPoints()) {
                    sessionBuilder.addContactPoint(new InetSocketAddress(contactPoint, config.getPort()));
                }

                logger.info("Connection attempt {}/{}", attempt, MAX_CONNECT_ATTEMPTS);
                session = sessionBuilder.build();
                logger.info("Connection to Cassandra established successfully");
                int nodeCount = session.getMetadata().getNodes().size();
                logger.info("Discovered {} node(s) in cluster", nodeCount);
                session.getMetadata().getNodes().values().forEach(node ->
                        logger.info("  - Node: {} (datacenter: {})", node.getEndPoint(), node.getDatacenter()));
                initializeSchema();
                return;
            } catch (Exception e) {
                lastException = e;
                logger.warn("Attempt {}/{} failed: {}", attempt, MAX_CONNECT_ATTEMPTS, e.getMessage());
                if (session != null && !session.isClosed()) {
                    try {
                        session.close();
                    } catch (Exception ignored) {
                    }
                    session = null;
                }
                if (attempt < MAX_CONNECT_ATTEMPTS) {
                    try {
                        logger.info("Retrying in {} seconds...", RETRY_DELAY_SECONDS);
                        Thread.sleep(RETRY_DELAY_SECONDS * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while waiting to retry Cassandra connection", ie);
                    }
                }
            }
        }
        logger.error("Could not connect to Cassandra after {} attempts", MAX_CONNECT_ATTEMPTS, lastException);
        throw new RuntimeException("Failed to connect to Cassandra. Ensure Cassandra is running and ready (e.g. run 'docker-compose up -d' and wait 2-3 minutes).", lastException);
    }

    private void initializeSchema() {
        try {
            logger.info("Creating database schema...");

            String createKeyspace = String.format(
                    "CREATE KEYSPACE IF NOT EXISTS %s " +
                            "WITH replication = {" +
                            "'class': '%s'," +
                            "'%s': '%d'" +
                            "}",
                    config.getKeyspace(),
                    config.getReplicationStrategy(),
                    config.getDatacenter(),
                    config.getReplicationFactor()
            );

            session.execute(createKeyspace);
            logger.info("Keyspace '{}' created or already exists", config.getKeyspace());

            session.execute("USE " + config.getKeyspace());

            String createActivitiesTable = "CREATE TABLE IF NOT EXISTS user_activities (" +
                    "user_id UUID," +
                    "activity_id UUID," +
                    "activity_type TEXT," +
                    "timestamp TIMESTAMP," +
                    "PRIMARY KEY (user_id, timestamp, activity_id)" +
                    ") WITH CLUSTERING ORDER BY (timestamp DESC, activity_id ASC)";

            session.execute(createActivitiesTable);
            logger.info("Table 'user_activities' created or already exists");

            String createActivityTypeIndex = "CREATE INDEX IF NOT EXISTS idx_activity_type " + "ON user_activities (activity_type)";

            session.execute(createActivityTypeIndex);
            logger.info("Index 'idx_activity_type' created or already exists");

            logger.info("Database schema initialized successfully");

        } catch (Exception e) {
            logger.error("Error creating schema", e);
            throw new RuntimeException("Failed to create database schema", e);
        }
    }


    public void close() {
        if (session != null && !session.isClosed()) {
            session.close();
            logger.info("Cassandra session closed");
        }
    }
}
