package com.keyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Keyservice Spring Boot application.
 *
 * <p>Bootstraps the embedded server and all Spring components: REST controller,
 * services, JPA repository, and the Actuator/Prometheus endpoint.</p>
 */
@SpringBootApplication
public class KeyserviceApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments passed to the JVM
     */
    public static void main(String[] args) {
        SpringApplication.run(KeyserviceApplication.class, args);
    }
}