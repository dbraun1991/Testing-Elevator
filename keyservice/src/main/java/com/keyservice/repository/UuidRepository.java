package com.keyservice.repository;

import com.keyservice.model.UuidEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UuidRepository extends JpaRepository<UuidEntry, Long> {
}