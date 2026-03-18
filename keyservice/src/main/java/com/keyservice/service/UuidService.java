package com.keyservice.service;

import com.keyservice.model.UuidEntry;
import com.keyservice.repository.UuidRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UuidService {

    private static final Logger log = LoggerFactory.getLogger(UuidService.class);

    private final UuidRepository uuidRepository;

    public UuidService(UuidRepository uuidRepository) {
        this.uuidRepository = uuidRepository;
    }

    public String generateAndPersist() {
        String uuid = UUID.randomUUID().toString();
        log.info("[UUID] generated={}", uuid);
        uuidRepository.save(new UuidEntry(uuid, LocalDateTime.now()));
        log.info("[UUID] persisted={}", uuid);
        return uuid;
    }
}
