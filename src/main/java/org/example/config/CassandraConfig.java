package org.example.config;

import org.example.repository.ActivityLogRepository;
import org.example.util.CassandraSessionManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CassandraProperties.class)
public class CassandraConfig {

    @Bean(destroyMethod = "close")
    public CassandraSessionManager cassandraSessionManager(CassandraProperties properties) {
        CassandraSessionConfig config = new CassandraSessionConfig();
        config.setContactPoints(properties.getContactPoints());
        config.setPort(properties.getPort());
        config.setKeyspace(properties.getKeyspace());
        config.setDatacenter(properties.getDatacenter());
        config.setReplicationFactor(properties.getReplicationFactor());
        return new CassandraSessionManager(config);
    }

    @Bean
    public ActivityLogRepository activityLogRepository(CassandraSessionManager sessionManager) {
        return new ActivityLogRepository(sessionManager);
    }

    /**
     * Internal config DTO for CassandraSessionManager (driver-level settings).
     */
    public static class CassandraSessionConfig {
        private java.util.List<String> contactPoints = java.util.List.of("127.0.0.1");
        private String keyspace = "activity_logs";
        private String datacenter = "datacenter1";
        private int port = 9042;
        private String replicationStrategy = "NetworkTopologyStrategy";
        private int replicationFactor = 1;

        public java.util.List<String> getContactPoints() {
            return contactPoints;
        }

        public void setContactPoints(java.util.List<String> contactPoints) {
            this.contactPoints = contactPoints;
        }

        public String getKeyspace() {
            return keyspace;
        }

        public void setKeyspace(String keyspace) {
            this.keyspace = keyspace;
        }

        public String getDatacenter() {
            return datacenter;
        }

        public void setDatacenter(String datacenter) {
            this.datacenter = datacenter;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getReplicationStrategy() {
            return replicationStrategy;
        }

        public void setReplicationStrategy(String replicationStrategy) {
            this.replicationStrategy = replicationStrategy;
        }

        public int getReplicationFactor() {
            return replicationFactor;
        }

        public void setReplicationFactor(int replicationFactor) {
            this.replicationFactor = replicationFactor;
        }
    }
}
