package org.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActivityLogApiIntegrationTest {

    private static final DockerImageName CASSANDRA_IMAGE = DockerImageName.parse("cassandra:4.1");

    @Container
    static CassandraContainer<?> cassandra = new CassandraContainer<>(CASSANDRA_IMAGE);

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void cassandraProperties(DynamicPropertyRegistry registry) {
        InetSocketAddress contactPoint = cassandra.getContactPoint();
        registry.add("cassandra.contact-points", () -> contactPoint.getHostString());
        registry.add("cassandra.port", () -> contactPoint.getPort());
        registry.add("cassandra.replication-factor", () -> 1);
    }

    @Test
    void healthEndpointReportsCassandraUp() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }

    @Test
    void recordActivityAndRetrieveByUser() {
        UUID userId = UUID.randomUUID();
        String activityType = "login";

        Map<String, Object> request = Map.of(
                "userId", userId.toString(),
                "activityType", activityType,
                "timestamp", Instant.now().toString()
        );

        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/activities",
                request,
                Map.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().get("userId")).isEqualTo(userId.toString());
        assertThat(createResponse.getBody().get("activityType")).isEqualTo(activityType);
        assertThat(createResponse.getBody().get("activityId")).isNotNull();

        ResponseEntity<List> listResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/activities/users/" + userId,
                List.class);

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).isNotNull();
        assertThat(listResponse.getBody()).hasSize(1);
    }

    @Test
    void getRecentActivitiesRespectsLimit() {
        UUID userId = UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            restTemplate.postForEntity(
                    "http://localhost:" + port + "/api/v1/activities",
                    Map.of(
                            "userId", userId.toString(),
                            "activityType", "view",
                            "timestamp", Instant.now().toString()),
                    Map.class);
        }

        ResponseEntity<List> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/activities/users/" + userId + "/recent?limit=2",
                List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSizeLessThanOrEqualTo(2);
    }
}
