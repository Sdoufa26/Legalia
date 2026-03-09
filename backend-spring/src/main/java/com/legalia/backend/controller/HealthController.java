package com.legalia.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint de supervision — utilisé pour vérifier que l'API est opérationnelle.
 * Public, aucune authentification requise.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * GET /api/health
     * Retourne {"status": "ok"} si l'application tourne.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
