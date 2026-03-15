package com.legalia.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse du endpoint GET /api/analyze/progress/{filename} du service IA FastAPI.
 * Permet de suivre l'avancement de l'analyse en temps réel.
 */
@Data
@NoArgsConstructor
public class AnalysisProgressResponse {

    // Pourcentage de progression (0 à 100)
    @JsonProperty("progression")
    private Integer progression;

    // Nombre de clauses déjà traitées par le service IA
    @JsonProperty("clauses_analysees")
    private Integer clausesAnalysees;

    // Nombre total de clauses détectées dans le document
    @JsonProperty("total_clauses")
    private Integer totalClauses;
}
