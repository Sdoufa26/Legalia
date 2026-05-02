package com.legalia.backend.repository;

import com.legalia.backend.model.ActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActionLogRepository extends JpaRepository<ActionLog, UUID> {
    List<ActionLog> findTop50ByOrderByCreatedAtDesc();
    long countByEmailAndAction(String email, String action);
}
