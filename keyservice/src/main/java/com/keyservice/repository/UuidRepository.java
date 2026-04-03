package com.keyservice.repository;

import com.keyservice.model.UuidEntry;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link com.keyservice.model.UuidEntry}.
 *
 * <p>Inherits full CRUD and pagination support from {@link JpaRepository}.
 * No custom queries are required — Spring Data generates all needed operations
 * at runtime.</p>
 */
public interface UuidRepository extends JpaRepository<UuidEntry, Long> {
}