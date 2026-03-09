package com.legalia.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Facture générée lors d'un paiement d'abonnement.
 */
@Entity
@Table(name = "factures")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Clé étrangère vers l'utilisateur concerné
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private LocalDateTime dateEmission;

    // Montant avec 2 décimales (ex: 9.99)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montant;

    // Lien vers le PDF de la facture (stockage externe)
    private String lienPdf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutFacture statut;

    // --- ENUMS ---

    public enum StatutFacture {
        PAYEE, ECHEC, REMBOURSEE
    }
}
