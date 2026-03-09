package com.legalia.backend.repository;

import com.legalia.backend.model.SourceJuridique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SourceJuridiqueRepository extends JpaRepository<SourceJuridique, UUID> {
}
