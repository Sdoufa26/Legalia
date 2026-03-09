package com.legalia.backend.repository;

import com.legalia.backend.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {

    // Recherche par email — utilisée pour l'authentification
    Optional<Utilisateur> findByEmail(String email);

    // Vérifie l'unicité de l'email avant inscription
    boolean existsByEmail(String email);
}
