package com.legalia.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "utilisateurs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idUtilisateur;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String motDePasseHash;

    @Column(nullable = false)
    private LocalDateTime dateInscription = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAbonnement statutAbonnement = StatutAbonnement.ACTIF;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeOffre typeOffre = TypeOffre.MENSUEL_STD;

    private LocalDate dateFinAbonnement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleUtilisateur role = RoleUtilisateur.CLIENT;

    // --- ENUMS ---

    public enum StatutAbonnement {
        ACTIF, INACTIF, RESILIE
    }

    public enum TypeOffre {
        MENSUEL_STD, ANNUEL_PREM
    }

    public enum RoleUtilisateur {
        CLIENT, ADMIN
    }
}