package org.example.config;

import com.datastax.oss.driver.api.core.CqlSession;
import org.example.util.CassandraSessionManager;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CassandraHealthIndicator implements HealthIndicator {

    private final CassandraSessionManager sessionManager;

    public CassandraHealthIndicator(CassandraSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public Health health() {
        try {
            CqlSession session = sessionManager.getSession();
            if (session.isClosed()) {
                return Health.down().withDetail("cassandra", "Session closed").build();
            }
            session.execute("SELECT release_version FROM system.local");
            return Health.up().withDetail("cassandra", "Connected").build();
        } catch (Exception e) {
            return Health.down().withDetail("cassandra", e.getMessage()).withException(e).build();
        }
    }
}
