package com.gopinath_statslab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * StatsLabApplication — entry point for the JVM Query Statistics Lab.
 *
 * @SpringBootApplication is a convenience annotation that combines:
 *   - @Configuration       — marks this class as a source of bean definitions
 *   - @EnableAutoConfiguration — tells Boot to configure Spring based on
 *                               what's on the classpath (e.g., sees PostgreSQL
 *                               driver → configures DataSource automatically)
 *   - @ComponentScan       — scans com.statslab.** for @Component, @Service,
 *                            @RestController, @Repository, etc.
 *
 * When you run `mvn spring-boot:run`, Spring Boot:
 *   1. Starts an embedded Tomcat server (port 8080)
 *   2. Connects to PostgreSQL using application.yml settings
 *   3. Scans and wires all beans
 *   4. Your REST endpoints are live
 */
@SpringBootApplication
public class StatsLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(StatsLabApplication.class, args);
    }
}
