package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;

@SpringBootApplication(exclude = CassandraAutoConfiguration.class)
public class ActivityLogApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivityLogApplication.class, args);
    }
}
