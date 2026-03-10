package com.legalia.backend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Configuration du stockage des fichiers uploadés.
 * Crée le dossier d'upload au démarrage de l'application s'il n'existe pas.
 */
@Configuration
public class FileStorageConfig {

    @Value("${app.upload.dir}")
    private String uploadDir;

    /**
     * Initialisation du dossier d'upload au démarrage.
     * Lance une exception si la création est impossible (permissions, espace disque...).
     */
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le dossier d'upload : " + uploadDir, e);
        }
    }
}
