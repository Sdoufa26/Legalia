package com.legalia.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "action_logs")
@Data
@NoArgsConstructor
public class ActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String action;

    @Column
    private String details;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ActionLog(String email, String action, String details) {
        this.email = email;
        this.action = action;
        this.details = details;
        this.createdAt = LocalDateTime.now();
    }
}