package com.legalia.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Résultat de l'analyse IA pour une clause du document.
 * Chaque carte vulgarise un extrait du contrat avec son niveau de vigilance.
 */
@Entity
@Table(name = "cartes_resultat")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarteResultat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Document analysé auquel appartient cette carte
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_document", nullable = false)
    private Document document;

    @Column(nullable = false)
    private String titreClause;

    // Texte brut extrait du contrat (peut être long)
    @Column(columnDefinition = "TEXT")
    private String texteOriginal;

    // Version vulgarisée générée par l'IA
    @Column(columnDefinition = "TEXT")
    private String texteClair;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NiveauVigilance niveauVigilance;

    // Référence juridique associée à cette clause (ex: "Art. L.113-1 Code des assurances")
    private String sourceJuridique;

    // --- ENUMS ---

    public enum NiveauVigilance {
        FAIBLE, MOYEN, ELEVE
    }
}
