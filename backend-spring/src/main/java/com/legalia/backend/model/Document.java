package com.legalia.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Représente un document PDF téléversé par un utilisateur.
 * Le statut évolue au fil de l'analyse par le service IA.
 */
@Entity
@Table(name = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Clé étrangère vers l'utilisateur propriétaire du document
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private String nomFichier;

    // Taille en kilo-octets
    private Long taille;

    @Column(nullable = false)
    private LocalDateTime dateTeleversement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategorieDocument categorie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutDocument statut = StatutDocument.EN_COURS;

    // Progression de l'analyse en pourcentage (0 à 100)
    @Column(nullable = false)
    @Builder.Default
    private Integer progression = 0;

    // Nombre de clauses déjà sauvegardées en BDD
    @Column(nullable = false)
    @Builder.Default
    private Integer clausesAnalysees = 0;

    // Nombre total de clauses retournées par le service IA
    @Column(nullable = false)
    @Builder.Default
    private Integer totalClauses = 0;

    // --- ENUMS ---

    public enum CategorieDocument {
        ASSURANCE, IMMO, TRAVAIL
    }

    public enum StatutDocument {
        EN_COURS, TERMINE, ERREUR
    }
}
