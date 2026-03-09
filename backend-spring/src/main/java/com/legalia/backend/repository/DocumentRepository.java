package com.legalia.backend.repository;

import com.legalia.backend.model.Document;
import com.legalia.backend.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    // Récupère tous les documents d'un utilisateur
    List<Document> findByUtilisateur(Utilisateur utilisateur);
}
