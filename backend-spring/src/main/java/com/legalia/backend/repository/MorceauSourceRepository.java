package com.legalia.backend.repository;

import com.legalia.backend.model.MorceauSource;
import com.legalia.backend.model.SourceJuridique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MorceauSourceRepository extends JpaRepository<MorceauSource, UUID> {

    // Tous les fragments (chunks) d'une source juridique
    List<MorceauSource> findBySource(SourceJuridique source);
}
