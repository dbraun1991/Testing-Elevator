package com.keyservice.integration;

import com.keyservice.repository.UuidRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
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

    private static final Logger log = LoggerFactory.getLogger(UuidIntegrationTest.class);

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
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
        log.info("");
        log.info("=======   Preparation   =======");
        log.info("[DB] Vor dem Test: Es befinden sich {} Einträge in der Datenbank", uuidRepository.count());

        log.info("");
        log.info("=======   Execution   =======");
        String responseUuid = restTemplate.getForObject("/uuid", String.class);

        log.info("");
        log.info("=======   Recheck   =======");
        assertThat(responseUuid)
                .as("Response sollte eine gültige UUID sein")
                .isNotBlank()
                .matches("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");

        uuidRepository.findAll().stream()
                .filter(entry -> entry.getUuid().equals(responseUuid))
                .findFirst()
                .ifPresent(entry -> log.info("First found entry={}", entry.getUuid()));

        assertThat(uuidRepository.findAll())
                .as("UUID muss in der Datenbank persistiert worden sein")
                .anyMatch(entry -> entry.getUuid().equals(responseUuid));

        log.info("[DB] Nach dem Test: Es befinden sich {} Eintrag in der Datenbank", uuidRepository.count());
    }
}
