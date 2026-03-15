package com.legalia.backend.service;

import com.legalia.backend.dto.AIAnalysisResponse;
import com.legalia.backend.dto.AnalysisProgressResponse;
import com.legalia.backend.model.CarteResultat;
import com.legalia.backend.model.Document;
import com.legalia.backend.repository.CarteResultatRepository;
import com.legalia.backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Service d'orchestration de l'analyse IA d'un document.
 * L'analyse est exécutée de manière asynchrone.
 * Pendant que FastAPI traite le PDF, on poll son endpoint de progression
 * toutes les 3 secondes pour mettre à jour la BDD en temps réel.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AIServiceClient aiServiceClient;
    private final CarteResultatRepository carteResultatRepository;
    private final DocumentRepository documentRepository;

    /** Intervalle de polling de la progression (en millisecondes) */
    private static final int POLLING_INTERVAL_MS = 3_000;

    /**
     * Lance l'analyse IA du document en arrière-plan.
     * Deux actions s'exécutent en parallèle :
     * - L'appel bloquant POST /api/analyze (peut durer jusqu'à 5 minutes)
     * - Un polling GET /api/analyze/progress/{filename} toutes les 3 secondes
     *
     * Pas de @Transactional sur cette méthode : chaque documentRepository.save()
     * ouvre sa propre transaction, ce qui rend les mises à jour immédiatement
     * visibles pour le front qui poll /status.
     *
     * @param documentId l'identifiant du document
     * @param pdfBytes   le contenu binaire du PDF
     */
    @Async("analysisExecutor")
    public void analyzeAndSaveAsync(UUID documentId, byte[] pdfBytes) {
        // Rechargement du document dans le thread asynchrone
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new NoSuchElementException("Document introuvable : " + documentId));

        log.info("Début de l'analyse asynchrone pour '{}' (id={})", document.getNomFichier(), documentId);

        // Lance l'appel HTTP bloquant vers FastAPI dans un thread séparé (ForkJoinPool)
        CompletableFuture<AIAnalysisResponse> futureResponse = CompletableFuture.supplyAsync(() ->
                aiServiceClient.analyzeDocument(pdfBytes, document.getNomFichier())
        );

        // Polling de la progression pendant que FastAPI analyse
        pollerProgression(document, futureResponse);

        // Récupération et traitement du résultat final
        try {
            AIAnalysisResponse analysisResponse = futureResponse.get();

            if (analysisResponse.getClauses() == null || analysisResponse.getClauses().isEmpty()) {
                log.warn("Le service IA n'a retourné aucune clause pour '{}'", document.getNomFichier());
                mettreAJourStatut(document, Document.StatutDocument.ERREUR);
                return;
            }

            // Sauvegarde de toutes les clauses en base
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

            carteResultatRepository.saveAll(cartes);
            log.info("{} cartes résultat sauvegardées pour '{}'", cartes.size(), document.getNomFichier());

            // Progression finale à 100%
            document.setProgression(100);
            document.setClausesAnalysees(cartes.size());
            document.setTotalClauses(cartes.size());
            documentRepository.save(document);

            mettreAJourStatut(document, Document.StatutDocument.TERMINE);

        } catch (ExecutionException e) {
            log.error("Échec de l'analyse IA pour '{}' : {}", document.getNomFichier(), e.getCause().getMessage());
            mettreAJourStatut(document, Document.StatutDocument.ERREUR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread d'analyse interrompu pour '{}'", document.getNomFichier());
            mettreAJourStatut(document, Document.StatutDocument.ERREUR);
        }
    }

    /**
     * Poll GET /api/analyze/progress/{filename} toutes les 3 secondes
     * jusqu'à ce que l'appel principal à FastAPI soit terminé.
     * Met à jour progression, clausesAnalysees et totalClauses en BDD à chaque réponse.
     */
    private void pollerProgression(Document document, CompletableFuture<?> futureResponse) {
        while (!futureResponse.isDone()) {
            try {
                Thread.sleep(POLLING_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            // Appel à l'endpoint de progression (non bloquant en cas d'erreur)
            AnalysisProgressResponse progress = aiServiceClient.getAnalysisProgress(document.getNomFichier());

            if (progress != null
                    && progress.getTotalClauses() != null
                    && progress.getTotalClauses() > 0) {

                document.setProgression(progress.getProgression() != null ? progress.getProgression() : 0);
                document.setClausesAnalysees(progress.getClausesAnalysees() != null ? progress.getClausesAnalysees() : 0);
                document.setTotalClauses(progress.getTotalClauses());

                // Chaque save() est sa propre transaction → commit immédiat
                documentRepository.save(document);

                log.info("Progression '{}' : {}% ({}/{})",
                        document.getNomFichier(),
                        document.getProgression(),
                        document.getClausesAnalysees(),
                        document.getTotalClauses());
            }
        }
    }

    /**
     * Retourne les cartes résultat associées à un document.
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
