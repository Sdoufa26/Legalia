package com.legalia.backend.service;

import com.legalia.backend.model.Document;
import com.legalia.backend.model.Utilisateur;
import com.legalia.backend.repository.CarteResultatRepository;
import com.legalia.backend.repository.DocumentRepository;
import com.legalia.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Service de gestion des documents PDF.
 * Gère l'upload, la validation, le stockage et la récupération des documents.
 */
@Service
@RequiredArgsConstructor
public class DocumentService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.max-size}")
    private long maxSize;

    private final DocumentRepository documentRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CarteResultatRepository carteResultatRepository;

    /**
     * Upload d'un document PDF.
     * Vérifie le format et la taille, sauvegarde le fichier sur disque,
     * puis crée une entrée en base de données.
     *
     * @param file  le fichier multipart reçu
     * @param email l'email de l'utilisateur connecté (extrait du JWT)
     * @return le Document créé en base
     */
    public Document uploadDocument(MultipartFile file, String email) {
        // Vérification du format : seuls les PDF sont acceptés
        if (!"application/pdf".equals(file.getContentType())) {
            throw new IllegalArgumentException("Seuls les fichiers PDF sont acceptés");
        }

        // Vérification de la taille : maximum 10 Mo
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Fichier trop volumineux (maximum 10 Mo)");
        }

        // Récupération de l'utilisateur connecté
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        // Génération d'un nom de fichier unique pour éviter les collisions
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String nomFichierStocke = UUID.randomUUID() + (extension != null ? "." + extension : "");

        // Sauvegarde physique du fichier dans le dossier uploads/
        try {
            Files.copy(file.getInputStream(), Paths.get(uploadDir).resolve(nomFichierStocke));
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde du fichier", e);
        }

        // Création de l'entrée en base de données
        Document document = Document.builder()
                .utilisateur(utilisateur)
                .nomFichier(file.getOriginalFilename())
                .taille(file.getSize() / 1024)  // Conversion octets → kilo-octets
                .dateTeleversement(LocalDateTime.now())
                .categorie(Document.CategorieDocument.ASSURANCE)  // Catégorie par défaut
                .statut(Document.StatutDocument.EN_COURS)
                .build();

        return documentRepository.save(document);
    }

    /**
     * Récupère tous les documents appartenant à un utilisateur.
     *
     * @param email l'email de l'utilisateur connecté
     * @return la liste de ses documents
     */
    public List<Document> getDocumentsByUser(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
        return documentRepository.findByUtilisateur(utilisateur);
    }

    /**
     * Récupère un document par son identifiant, en vérifiant qu'il appartient à l'utilisateur.
     * Lève une AccessDeniedException (403) si le document appartient à quelqu'un d'autre.
     *
     * @param documentId l'identifiant du document
     * @param email      l'email de l'utilisateur connecté
     * @return le Document si trouvé et appartenant à l'utilisateur
     */
    public Document getDocumentById(UUID documentId, String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new NoSuchElementException("Document introuvable"));

        // Vérification de la propriété du document
        if (!document.getUtilisateur().getIdUtilisateur().equals(utilisateur.getIdUtilisateur())) {
            throw new AccessDeniedException("Accès refusé à ce document");
        }

        return document;
    }

    /**
     * Supprime un document et ses résultats d'analyse.
     * Vérifie que le document appartient à l'utilisateur connecté.
     * Supprime aussi le fichier physique sur disque.
     *
     * @param documentId l'identifiant du document à supprimer
     * @param email      l'email de l'utilisateur connecté
     */
    @Transactional
    public void deleteDocument(UUID documentId, String email) {
        // Vérifie la propriété (lève 403 ou 404 si nécessaire)
        Document document = getDocumentById(documentId, email);

        // Supprime d'abord les cartes de résultat liées
        carteResultatRepository.deleteAll(carteResultatRepository.findByDocument(document));

        // Supprime l'entrée en base
        documentRepository.delete(document);

        // Supprime le fichier physique (best effort)
        try {
            Path dir = Paths.get(uploadDir);
            Files.list(dir)
                    .filter(p -> p.getFileName().toString().contains(documentId.toString()))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        } catch (IOException ignored) {}
    }
}
