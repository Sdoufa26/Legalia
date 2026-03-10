package com.legalia.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Réponse renvoyée après un upload de document.
 * Contient les métadonnées du document créé et un message de confirmation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponse {

    private UUID idDocument;
    private String nomFichier;
    private Long taille;
    private LocalDateTime dateTeleversement;
    private String categorie;
    private String statut;
    private String message;
}
