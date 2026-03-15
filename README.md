# Activity Log Service

A user activity logging service backed by **Apache Cassandra**. REST API, health checks, environment-based configuration, and Testcontainers-based integration tests.

---

## How to Run

The application **requires a running Cassandra instance**. If you see `Could not reach any contact point` or `Failed to connect to Cassandra`, start Cassandra first (Step 1 below). You do not need to install Cassandra on your OS—Docker is enough.

### Requirements

- **Java 17+**
- **Maven 3.8+**
- **Docker and Docker Compose** (for Cassandra and integration tests)

### 1. Start Cassandra (Docker Compose)

Three-node cluster (one node exposed on `localhost:9042`):

```bash
docker-compose up -d
```

Wait for the cluster to be ready. A **three-node cluster often needs 2–3 minutes** before the CQL port accepts connections. To verify:

```bash
docker-compose exec cassandra-node1 nodetool status
```

All nodes should show status `UN` (Up Normal). Only then start the application.

### 2. Run the Application

```bash
mvn spring-boot:run
```

The service will be available at **http://localhost:8080**

### 3. Environment Variables (optional)

| Variable | Description | Default |
|----------|-------------|---------|
| `CASSANDRA_CONTACT_POINTS` | Cassandra hosts (comma-separated) | `127.0.0.1` |
| `CASSANDRA_PORT` | CQL port | `9042` |
| `CASSANDRA_KEYSPACE` | Keyspace name | `activity_logs` |
| `CASSANDRA_DATACENTER` | Datacenter name | `datacenter1` |
| `CASSANDRA_REPLICATION_FACTOR` | Replication factor | `1` |

Example for a custom cluster:

```bash
export CASSANDRA_CONTACT_POINTS=host1,host2,host3
export CASSANDRA_PORT=9042
mvn spring-boot:run
```

### 4. Build JAR and Run

```bash
mvn clean package -DskipTests
java -jar target/activity-log-service-1.0-SNAPSHOT.jar
```

### 5. Run Integration Tests

Docker must be running (Testcontainers starts Cassandra in a container):

```bash
mvn test
```

### Troubleshooting: "STARTUP: unexpected failure" or "Could not reach any contact point"

- **Wait longer.** After `docker-compose up -d`, wait **2–3 minutes** before running the app. The app will retry connecting up to 15 times (every 3 seconds); if Cassandra is still booting, it may connect on a later attempt.
- **Confirm Cassandra is up:**  
  `docker-compose exec cassandra-node1 nodetool status`  
  All nodes should be `UN`. If you see `UJ` (Joining) or `DN` (Down), wait until they turn `UN`.
- **Check Cassandra logs:**  
  `docker-compose logs cassandra-node1`  
  Look for errors or "Starting native transport" (CQL is ready when this appears).
- **Single-node for faster startup:** To avoid waiting for a 3-node cluster, you can temporarily use one node: run only `docker run -d -p 9042:9042 cassandra:4.1` and then start the app (contact point stays `127.0.0.1:9042`).

---

## API

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/api-docs

### Main Operations

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/activities` | Record an activity (body: `userId`, `activityType`, optional `timestamp`; query: `ttlSeconds`) |
| `GET` | `/api/v1/activities/users/{userId}` | All activities for a user |
| `GET` | `/api/v1/activities/users/{userId}/recent?limit=10` | Last N activities |
| `GET` | `/api/v1/activities/users/{userId}/range?startTime=...&endTime=...` | Activities in a time range (ISO-8601) |

### Examples (curl)

```bash
# Record an activity
curl -X POST http://localhost:8080/api/v1/activities \
  -H "Content-Type: application/json" \
  -d '{"userId":"550e8400-e29b-41d4-a716-446655440000","activityType":"login"}'

# Get user activities
curl "http://localhost:8080/api/v1/activities/users/550e8400-e29b-41d4-a716-446655440000"

# Last 5 activities
curl "http://localhost:8080/api/v1/activities/users/550e8400-e29b-41d4-a716-446655440000/recent?limit=5"
```

### Health

- **http://localhost:8080/actuator/health** — overall status and Cassandra connectivity check.

---

## Architecture

### Components

```
                    ┌─────────────────────────────────────────────────────────┐
                    │                     Client (HTTP)                         │
                    └───────────────────────────┬─────────────────────────────┘
                                                │
                    ┌───────────────────────────▼─────────────────────────────┐
                    │              Spring Boot (REST API, Actuator)            │
                    │  ┌─────────────┐  ┌──────────────┐  ┌─────────────────┐  │
                    │  │ Controller  │──│   Service    │──│   Repository    │  │
                    │  └─────────────┘  └──────────────┘  └────────┬────────┘  │
                    │         │                    │                      │     │
                    │  ┌──────▼──────┐      ┌──────▼──────┐    ┌────────▼────┐ │
                    │  │ DTOs        │      │ Validation  │    │ Cassandra    │ │
                    │  │ Exception   │      │ (Bean Valid)│    │ Session Mgr  │ │
                    │  └─────────────┘      └─────────────┘    └──────┬───────┘ │
                    └─────────────────────────────────────────────────┼─────────┘
                                                                      │
                    ┌─────────────────────────────────────────────────▼─────────┐
                    │              Apache Cassandra (CQL Driver 4.x)             │
                    │  Keyspace: activity_logs                                   │
                    │  Table: user_activities (PK: user_id, CK: timestamp, id)   │
                    └───────────────────────────────────────────────────────────┘
```

### Data Layer (Cassandra)

- **Partition key:** `user_id` — queries by user hit a single partition (optimal for Cassandra).
- **Clustering:** `timestamp DESC`, `activity_id` — time-ordered and unique per event.
- **TTL** is supported on insert (default 30 days; can set `ttlSeconds` via API).
- Schema (keyspace and table) is created on first application connection.

### Responsibility Split

- **Controller** — HTTP, DTO mapping, delegating to the service.
- **Service** — business logic, DTO ↔ domain model conversion.
- **Repository** — CQL, prepared statements, Row → entity mapping.
- **CassandraSessionManager** — single CQL session, schema init, driver settings (consistency, timeout).

---

## Tech Stack

| Category | Technology | Use |
|----------|------------|-----|
| **Language / runtime** | Java 17 | Application core |
| **Framework** | Spring Boot 3.2 | REST, configuration, Actuator |
| **Database** | Apache Cassandra 4.1 | Activity log storage |
| **DB driver** | DataStax Java Driver 4.1 | Cassandra connection, prepared statements |
| **Validation** | Bean Validation (Jakarta) | Input DTOs |
| **API docs** | Springdoc OpenAPI 2.x | Swagger UI, OpenAPI 3 |
| **Observability** | Spring Boot Actuator | Health (including custom Cassandra indicator) |
| **Tests** | JUnit 5, Testcontainers, Spring Boot Test | Integration tests with real Cassandra in Docker |
| **Build** | Maven | Build, run, tests |

---

## Project Structure

```
src/main/java/org/example/
├── ActivityLogApplication.java      # Spring Boot entry point
├── config/
│   ├── CassandraConfig.java         # Beans: session manager, repository
│   ├── CassandraProperties.java     # application.yml → cassandra.*
│   └── CassandraHealthIndicator.java # /actuator/health — Cassandra check
├── model/
│   └── ActivityLog.java            # Domain entity
├── repository/
│   └── ActivityLogRepository.java  # CQL operations (insert, select by user/time)
├── service/
│   └── ActivityLogService.java     # Service layer
├── web/
│   ├── ActivityLogController.java  # REST API
│   ├── GlobalExceptionHandler.java # Error and validation handling
│   └── dto/
│       ├── ActivityLogRequest.java
│       └── ActivityLogResponse.java
└── util/
    └── CassandraSessionManager.java # Session and schema setup
```

---

## License
MIT
