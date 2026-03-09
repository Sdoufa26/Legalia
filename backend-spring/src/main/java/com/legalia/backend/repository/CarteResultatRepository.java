package com.legalia.backend.repository;

import com.legalia.backend.model.CarteResultat;
import com.legalia.backend.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarteResultatRepository extends JpaRepository<CarteResultat, UUID> {

    // Toutes les cartes d'analyse liées à un document
    List<CarteResultat> findByDocument(Document document);
}
