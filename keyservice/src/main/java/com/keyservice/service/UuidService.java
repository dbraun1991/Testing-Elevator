package com.keyservice.service;

import com.keyservice.model.UuidEntry;
import com.keyservice.repository.UuidRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service backing the {@code GET /uuid} endpoint.
 *
 * <p>Generates a random UUID and persists it to the {@code uuid_log} table via
 * {@link com.keyservice.repository.UuidRepository}. If no database is available
 * the save will throw, which propagates as HTTP 500 — this is intentional and
 * demonstrates the need for proper containerization (P2 stage).</p>
 */
@Service
public class UuidService {

    private static final Logger log = LoggerFactory.getLogger(UuidService.class);

    private final UuidRepository uuidRepository;

    /**
     * Creates the service with its required repository dependency.
     *
     * @param uuidRepository Spring Data repository used to persist UUID entries
     */
    public UuidService(UuidRepository uuidRepository) {
        this.uuidRepository = uuidRepository;
    }

    /**
     * Generates a random UUID, saves it to the database, and returns it.
     *
     * @return the newly generated UUID as a canonical string
     *         (e.g. {@code "550e8400-e29b-41d4-a716-446655440000"})
     */
    public String generateAndPersist() {
        String uuid = UUID.randomUUID().toString();
        log.info("[UUID] generated={}", uuid);
        uuidRepository.save(new UuidEntry(uuid, LocalDateTime.now()));
        log.info("[UUID] persisted={}", uuid);
        return uuid;
    }
}
