package org.example.config;

import java.util.List;

public class CassandraConfig {
    private List<String> contactPoints;
    private String keyspace;
    private String datacenter;
    private int port;
    private String replicationStrategy;
    private int replicationFactor;

    public CassandraConfig() {
        this.contactPoints = List.of("127.0.0.1");
        this.keyspace = "activity_logs";
        this.datacenter = "datacenter1";
        this.port = 9042;
        this.replicationStrategy = "NetworkTopologyStrategy";
        this.replicationFactor = 3;
    }

    public List<String> getContactPoints() {
        return contactPoints;
    }

    public void setContactPoints(List<String> contactPoints) {
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
