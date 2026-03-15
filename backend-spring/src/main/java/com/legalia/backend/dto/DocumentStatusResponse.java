package com.legalia.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Réponse du endpoint GET /api/documents/{id}/status.
 * Permet au front de suivre la progression de l'analyse en temps réel via polling.
 */
@Data
@Builder
public class DocumentStatusResponse {

    private UUID idDocument;

    // Statut du document : EN_COURS, TERMINE, ERREUR
    private String statut;

    // Pourcentage de progression (0 à 100)
    private Integer progression;

    // Nombre de clauses sauvegardées en BDD jusqu'ici
    private Integer clausesAnalysees;

    // Nombre total de clauses détectées par le service IA
    private Integer totalClauses;
}
