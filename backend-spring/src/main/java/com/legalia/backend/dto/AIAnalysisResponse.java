package com.legalia.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Réponse du service IA (FastAPI) pour l'analyse d'un contrat.
 * Correspond exactement à la structure JSON retournée par POST /api/analyze.
 */
@Data
@NoArgsConstructor
public class AIAnalysisResponse {

    @JsonProperty("document_name")
    private String documentName;

    @JsonProperty("nb_clauses")
    private int nbClauses;

    @JsonProperty("clauses")
    private List<ClauseDTO> clauses;

    /**
     * Résultat d'analyse d'une clause individuelle.
     */
    @Data
    @NoArgsConstructor
    public static class ClauseDTO {

        @JsonProperty("titre_clause")
        private String titreClause;

        @JsonProperty("texte_original")
        private String texteOriginal;

        @JsonProperty("texte_clair")
        private String texteClair;

        @JsonProperty("niveau_vigilance")
        private String niveauVigilance;

        @JsonProperty("source_juridique")
        private String sourceJuridique;
    }
}
