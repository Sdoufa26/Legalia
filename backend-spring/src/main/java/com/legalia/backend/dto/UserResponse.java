package com.legalia.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Représentation publique d'un utilisateur (jamais de mot de passe).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String email;
    private String prenom;
    private String nom;
    private LocalDateTime dateInscription;
    private String statutAbonnement;
    private String typeOffre;
    private String role;
}
