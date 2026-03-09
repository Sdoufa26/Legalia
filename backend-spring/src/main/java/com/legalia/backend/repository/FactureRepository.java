package com.legalia.backend.repository;

import com.legalia.backend.model.Facture;
import com.legalia.backend.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FactureRepository extends JpaRepository<Facture, UUID> {

    // Historique de facturation d'un utilisateur
    List<Facture> findByUtilisateur(Utilisateur utilisateur);
}
