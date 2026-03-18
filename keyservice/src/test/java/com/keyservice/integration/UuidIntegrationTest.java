package com.keyservice.integration;

import com.keyservice.repository.UuidRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P3 — Integrationstest (Testcontainers)
 *
 * Startet eine echte PostgreSQL-Instanz aus dem JUnit-Test heraus.
 * Ruft /uuid auf und prüft, ob der Eintrag korrekt in der DB persistiert wurde.
 * Teardown erfolgt automatisch — kein manuelles Aufräumen nötig.
 *
 * Ausführen:
 *   mvn test -Dtest=UuidIntegrationTest
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class UuidIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("keyservice")
            .withUsername("keyservice")
            .withPassword("VerylongKeyserviceDatabasepassword");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UuidRepository uuidRepository;

    @Test
    void uuid_isPersisted_inDatabase() {
        String responseUuid = restTemplate.getForObject("/uuid", String.class);

        assertThat(responseUuid)
                .as("Response sollte eine gültige UUID sein")
                .isNotBlank()
                .matches("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");

        assertThat(uuidRepository.findAll())
                .as("UUID muss in der Datenbank persistiert worden sein")
                .anyMatch(entry -> entry.getUuid().equals(responseUuid));
    }
}
