package org.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "cassandra")
public class CassandraProperties {
    private List<String> contactPoints = List.of("127.0.0.1");
    private int port = 9042;
    private String keyspace = "activity_logs";
    private String datacenter = "datacenter1";
    private int replicationFactor = 1;

    public List<String> getContactPoints() {
        return contactPoints;
    }

    public void setContactPoints(List<String> contactPoints) {
        this.contactPoints = contactPoints;
    }

    public void setContactPoints(String contactPoints) {
        this.contactPoints = contactPoints == null || contactPoints.isBlank()
                ? List.of("127.0.0.1")
                : List.of(contactPoints.trim().split("\\s*,\\s*"));
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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

    public int getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(int replicationFactor) {
        this.replicationFactor = replicationFactor;
    }
}
