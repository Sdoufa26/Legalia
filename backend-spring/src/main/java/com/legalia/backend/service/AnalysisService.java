package com.legalia.backend.service;

import com.legalia.backend.dto.AIAnalysisResponse;
import com.legalia.backend.model.CarteResultat;
import com.legalia.backend.model.Document;
import com.legalia.backend.repository.CarteResultatRepository;
import com.legalia.backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Service d'orchestration de l'analyse IA d'un document.
 * Coordonne l'appel au service Python, la persistance des résultats
 * et la mise à jour du statut du document.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AIServiceClient aiServiceClient;
    private final CarteResultatRepository carteResultatRepository;
    private final DocumentRepository documentRepository;

    /**
     * Analyse un document PDF via le service IA et sauvegarde les résultats en base.
     *
     * Étapes :
     * 1. Appel au service IA FastAPI avec les bytes du PDF
     * 2. Création d'une CarteResultat par clause retournée
     * 3. Mise à jour du statut du document (TERMINE ou ERREUR)
     *
     * @param document le document déjà persisté (statut EN_COURS)
     * @param pdfBytes le contenu binaire du PDF
     * @return la liste des CarteResultat sauvegardées (vide en cas d'erreur)
     */
    @Transactional
    public List<CarteResultat> analyzeAndSave(Document document, byte[] pdfBytes) {
        log.info("Début de l'analyse IA pour le document '{}' (id={})",
                document.getNomFichier(), document.getId());

        try {
            // Étape 1 — Envoi au service IA
            AIAnalysisResponse analysisResponse = aiServiceClient.analyzeDocument(
                    pdfBytes, document.getNomFichier()
            );

            if (analysisResponse.getClauses() == null || analysisResponse.getClauses().isEmpty()) {
                log.warn("Le service IA n'a retourné aucune clause pour '{}'", document.getNomFichier());
                mettreAJourStatut(document, Document.StatutDocument.ERREUR);
                return Collections.emptyList();
            }

            // Étape 2 — Création des CarteResultat en base
            List<CarteResultat> cartes = analysisResponse.getClauses().stream()
                    .map(clause -> CarteResultat.builder()
                            .document(document)
                            .titreClause(clause.getTitreClause())
                            .texteOriginal(clause.getTexteOriginal())
                            .texteClair(clause.getTexteClair())
                            .niveauVigilance(parseNiveauVigilance(clause.getNiveauVigilance()))
                            .sourceJuridique(clause.getSourceJuridique())
                            .build())
                    .toList();

            List<CarteResultat> savedCartes = carteResultatRepository.saveAll(cartes);
            log.info("{} cartes résultat sauvegardées pour le document '{}'",
                    savedCartes.size(), document.getNomFichier());

            // Étape 3 — Mise à jour du statut
            mettreAJourStatut(document, Document.StatutDocument.TERMINE);

            return savedCartes;

        } catch (AIServiceClient.AIServiceException e) {
            log.error("Échec de l'analyse IA pour '{}' : {}", document.getNomFichier(), e.getMessage());
            mettreAJourStatut(document, Document.StatutDocument.ERREUR);
            return Collections.emptyList();
        }
    }

    /**
     * Retourne les cartes résultat associées à un document.
     *
     * @param documentId l'identifiant du document
     * @return la liste des CarteResultat
     */
    public List<CarteResultat> getResultsByDocumentId(UUID documentId) {
        return carteResultatRepository.findByDocument_Id(documentId);
    }

    // --- Méthodes privées ---

    private void mettreAJourStatut(Document document, Document.StatutDocument statut) {
        document.setStatut(statut);
        documentRepository.save(document);
        log.info("Statut du document '{}' mis à jour : {}", document.getNomFichier(), statut);
    }

    private CarteResultat.NiveauVigilance parseNiveauVigilance(String valeur) {
        try {
            return CarteResultat.NiveauVigilance.valueOf(valeur);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("Niveau de vigilance inconnu '{}', valeur MOYEN appliquée par défaut", valeur);
            return CarteResultat.NiveauVigilance.MOYEN;
        }
    }
}
