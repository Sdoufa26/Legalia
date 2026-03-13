package com.legalia.backend.controller;

import com.legalia.backend.dto.CarteResultatResponse;
import com.legalia.backend.dto.DocumentResponse;
import com.legalia.backend.dto.DocumentUploadResponse;
import com.legalia.backend.model.CarteResultat;
import com.legalia.backend.model.Document;
import com.legalia.backend.service.AnalysisService;
import com.legalia.backend.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Controller de gestion des documents PDF.
 * Tous les endpoints nécessitent une authentification JWT.
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final AnalysisService analysisService;

    /**
     * POST /api/documents/upload
     * Upload d'un fichier PDF, stockage sur disque, puis analyse IA synchrone.
     * Retourne les métadonnées du document et les résultats de l'analyse.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file,
                                            Authentication authentication) {
        // Lecture des bytes avant que DocumentService consomme l'InputStream
        byte[] pdfBytes;
        try {
            pdfBytes = file.getBytes();
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", "Impossible de lire le fichier"));
        }

        Document document;
        try {
            document = documentService.uploadDocument(file, authentication.getName());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erreur", "Erreur lors de l'upload du document"));
        }

        // Lancement de l'analyse IA (synchrone — peut prendre jusqu'à 120s)
        log.info("Déclenchement de l'analyse IA pour le document '{}'", document.getNomFichier());
        List<CarteResultat> cartes = analysisService.analyzeAndSave(document, pdfBytes);

        // Rechargement du document pour avoir le statut mis à jour (TERMINE ou ERREUR)
        document = documentService.getDocumentById(document.getId(), authentication.getName());

        List<CarteResultatResponse> clausesResponse = cartes.stream()
                .map(this::toCarteResponse)
                .toList();

        return ResponseEntity.status(HttpStatus.CREATED).body(toUploadResponse(document, clausesResponse));
    }

    /**
     * GET /api/documents
     * Retourne la liste des documents de l'utilisateur connecté.
     */
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getDocuments(Authentication authentication) {
        List<Document> documents = documentService.getDocumentsByUser(authentication.getName());
        List<DocumentResponse> response = documents.stream()
                .map(this::toDocumentResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/documents/{id}
     * Retourne le détail d'un document si il appartient à l'utilisateur connecté.
     * Retourne 403 si le document appartient à quelqu'un d'autre, 404 si introuvable.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDocumentById(@PathVariable UUID id,
                                             Authentication authentication) {
        try {
            Document document = documentService.getDocumentById(id, authentication.getName());
            return ResponseEntity.ok(toDocumentResponse(document));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erreur", "Document introuvable"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("erreur", "Accès refusé à ce document"));
        }
    }

    /**
     * GET /api/documents/{id}/results
     * Retourne les résultats d'analyse (CarteResultat) d'un document.
     * Vérifie que le document appartient à l'utilisateur connecté.
     */
    @GetMapping("/{id}/results")
    public ResponseEntity<?> getDocumentResults(@PathVariable UUID id,
                                                Authentication authentication) {
        try {
            // Vérification des droits d'accès (lève 403 ou 404 si nécessaire)
            documentService.getDocumentById(id, authentication.getName());

            List<CarteResultatResponse> cartes = analysisService.getResultsByDocumentId(id)
                    .stream()
                    .map(this::toCarteResponse)
                    .toList();

            return ResponseEntity.ok(cartes);

        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erreur", "Document introuvable"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("erreur", "Accès refusé à ce document"));
        }
    }

    /**
     * DELETE /api/documents/{id}
     * Supprime un document et ses résultats d'analyse.
     * Retourne 204 si succès, 403 si accès refusé, 404 si introuvable.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable UUID id,
                                            Authentication authentication) {
        try {
            documentService.deleteDocument(id, authentication.getName());
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erreur", "Document introuvable"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("erreur", "Accès refusé à ce document"));
        }
    }

    // --- Conversions entité → DTO ---

    private DocumentUploadResponse toUploadResponse(Document doc, List<CarteResultatResponse> clauses) {
        String message = doc.getStatut() == Document.StatutDocument.TERMINE
                ? "Document analysé avec succès (" + clauses.size() + " clauses)"
                : "Document uploadé — l'analyse IA a échoué";

        return DocumentUploadResponse.builder()
                .idDocument(doc.getId())
                .nomFichier(doc.getNomFichier())
                .taille(doc.getTaille())
                .dateTeleversement(doc.getDateTeleversement())
                .categorie(doc.getCategorie().name())
                .statut(doc.getStatut().name())
                .message(message)
                .clauses(clauses)
                .build();
    }

    private DocumentResponse toDocumentResponse(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .nomFichier(doc.getNomFichier())
                .taille(doc.getTaille())
                .dateTeleversement(doc.getDateTeleversement())
                .categorie(doc.getCategorie().name())
                .statut(doc.getStatut().name())
                .build();
    }

    private CarteResultatResponse toCarteResponse(CarteResultat carte) {
        return CarteResultatResponse.builder()
                .id(carte.getId())
                .titreClause(carte.getTitreClause())
                .texteOriginal(carte.getTexteOriginal())
                .texteClair(carte.getTexteClair())
                .niveauVigilance(carte.getNiveauVigilance().name())
                .sourceJuridique(carte.getSourceJuridique())
                .build();
    }
}
