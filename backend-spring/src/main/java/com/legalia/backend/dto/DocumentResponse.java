package com.legalia.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Représentation publique d'un document (sans la relation utilisateur).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    private UUID id;
    private String nomFichier;
    private Long taille;            // en Ko
    private LocalDateTime dateTeleversement;
    private String categorie;
    private String statut;
}
