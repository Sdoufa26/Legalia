package com.legalia.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Source juridique (loi, jurisprudence, CGV) indexée dans le moteur RAG.
 * Liée à un utilisateur car chaque client peut avoir ses propres CGV à analyser.
 */
@Entity
@Table(name = "sources_juridiques")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceJuridique {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Propriétaire de la source (notamment pour les CGV personnalisées)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private String titre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategorieSource categorie;

    // Date d'entrée en vigueur du texte juridique
    private LocalDate dateVigueur;

    // Indique si le contenu a été vectorisé et indexé dans la base vectorielle
    @Column(nullable = false)
    @Builder.Default
    private boolean contenuVectorise = false;

    // --- ENUMS ---

    public enum CategorieSource {
        LOI, JURISPRUDENCE, CGV
    }
}
