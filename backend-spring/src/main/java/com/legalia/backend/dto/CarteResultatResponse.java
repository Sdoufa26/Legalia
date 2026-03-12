package com.legalia.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Représentation publique d'une carte résultat d'analyse de clause.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarteResultatResponse {

    private UUID id;
    private String titreClause;
    private String texteOriginal;
    private String texteClair;
    private String niveauVigilance;
    private String sourceJuridique;
}
