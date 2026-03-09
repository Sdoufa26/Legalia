package com.legalia.backend.repository;

import com.legalia.backend.model.SourceJuridique;
import com.legalia.backend.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SourceJuridiqueRepository extends JpaRepository<SourceJuridique, UUID> {

    // Sources juridiques associées à un utilisateur
    List<SourceJuridique> findByUtilisateur(Utilisateur utilisateur);
}
