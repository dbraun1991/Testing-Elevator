package com.keyservice.service;

import com.keyservice.model.UuidEntry;
import com.keyservice.repository.UuidRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UuidService {

    private final UuidRepository uuidRepository;

    public UuidService(UuidRepository uuidRepository) {
        this.uuidRepository = uuidRepository;
    }

    public String generateAndPersist() {
        String uuid = UUID.randomUUID().toString();
        uuidRepository.save(new UuidEntry(uuid, LocalDateTime.now()));
        return uuid;
    }
}