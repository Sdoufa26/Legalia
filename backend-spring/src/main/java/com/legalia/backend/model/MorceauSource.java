package com.legalia.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Fragment (chunk) d'une source juridique, tel que découpé pour la vectorisation RAG.
 * Chaque morceau est indexé individuellement dans la base vectorielle.
 */
@Entity
@Table(name = "morceaux_source")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MorceauSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Source juridique parente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_source", nullable = false)
    private SourceJuridique source;

    // Texte brut du fragment (peut être long)
    @Column(columnDefinition = "TEXT")
    private String texteExtrait;
}
