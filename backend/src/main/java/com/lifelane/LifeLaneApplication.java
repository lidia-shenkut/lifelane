package com.lifelane;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the LifeLane Mini Spring Boot application.
 *
 * <p>Starts the embedded Tomcat server, registers all Spring-managed beans,
 * and kicks off Flyway database migrations on startup.
 */
@SpringBootApplication
public class LifeLaneApplication {

    public static void main(String[] args) {
        SpringApplication.run(LifeLaneApplication.class, args);
    }
}
