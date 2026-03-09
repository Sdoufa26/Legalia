package com.legalia.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse renvoyée après une connexion réussie.
 * Contient le token JWT à inclure dans les requêtes suivantes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    // Sérialisé en "access_token" dans le JSON (convention OAuth2)
    @JsonProperty("access_token")
    private String accessToken;

    // Toujours "Bearer" pour notre implémentation
    @JsonProperty("token_type")
    private String tokenType;
}
