package com.legalia.backend.controller;

import com.legalia.backend.dto.DocumentResponse;
import com.legalia.backend.dto.DocumentUploadResponse;
import com.legalia.backend.model.Document;
import com.legalia.backend.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Controller de gestion des documents PDF.
 * Tous les endpoints nécessitent une authentification JWT.
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * POST /api/documents/upload
     * Upload d'un fichier PDF (multipart/form-data).
     * Retourne les métadonnées du document créé.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file,
                                            Authentication authentication) {
        try {
            Document document = documentService.uploadDocument(file, authentication.getName());
            DocumentUploadResponse response = toUploadResponse(document, "Document téléversé avec succès");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            // Format invalide ou fichier trop volumineux
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erreur", "Erreur lors de l'upload du document"));
        }
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

    // --- Conversions entité → DTO ---

    private DocumentUploadResponse toUploadResponse(Document doc, String message) {
        return DocumentUploadResponse.builder()
                .idDocument(doc.getId())
                .nomFichier(doc.getNomFichier())
                .taille(doc.getTaille())
                .dateTeleversement(doc.getDateTeleversement())
                .categorie(doc.getCategorie().name())
                .statut(doc.getStatut().name())
                .message(message)
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
}
