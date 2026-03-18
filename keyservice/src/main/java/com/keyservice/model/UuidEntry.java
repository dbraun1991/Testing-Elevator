package com.keyservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "uuid_log")
public class UuidEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String uuid;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected UuidEntry() {}

    public UuidEntry(String uuid, LocalDateTime createdAt) {
        this.uuid = uuid;
        this.createdAt = createdAt;
    }

    public Long getId()              { return id; }
    public String getUuid()          { return uuid; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}