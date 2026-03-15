package com.legalia.backend.service;

import com.legalia.backend.dto.AIAnalysisResponse;
import com.legalia.backend.dto.AnalysisProgressResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/**
 * Client HTTP vers le service IA FastAPI (port 8000).
 * Envoie les PDF en multipart/form-data et récupère les résultats d'analyse.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.ai-service.url}")
    private String aiServiceUrl;

    /**
     * Envoie un PDF au service IA et retourne la réponse d'analyse.
     *
     * @param pdfBytes le contenu binaire du fichier PDF
     * @param filename le nom original du fichier (utilisé dans le multipart)
     * @return la réponse structurée du service IA, ou null en cas d'échec
     * @throws AIServiceException si le service est indisponible ou retourne une erreur
     */
    public AIAnalysisResponse analyzeDocument(byte[] pdfBytes, String filename) {
        String endpoint = aiServiceUrl + "/api/analyze";
        log.info("Envoi du document '{}' ({} Ko) au service IA : {}", filename, pdfBytes.length / 1024, endpoint);

        // Construction du corps multipart/form-data
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new PdfByteArrayResource(pdfBytes, filename));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<AIAnalysisResponse> response = restTemplate.postForEntity(
                    endpoint, request, AIAnalysisResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                AIAnalysisResponse result = response.getBody();
                log.info("Réponse reçue du service IA : {} clauses analysées pour '{}'",
                        result.getNbClauses(), filename);
                return result;
            }

            throw new AIServiceException("Réponse inattendue du service IA : " + response.getStatusCode());

        } catch (ResourceAccessException e) {
            // Timeout ou service inaccessible
            log.error("Service IA inaccessible ou timeout pour '{}' : {}", filename, e.getMessage());
            throw new AIServiceException("Service IA inaccessible (timeout ou connexion refusée)", e);
        } catch (RestClientException e) {
            log.error("Erreur HTTP lors de l'appel au service IA pour '{}' : {}", filename, e.getMessage());
            throw new AIServiceException("Erreur lors de l'appel au service IA : " + e.getMessage(), e);
        }
    }

    /**
     * Interroge le service IA pour obtenir la progression de l'analyse en cours.
     * Retourne null si l'endpoint est indisponible ou si le fichier n'est pas en cours d'analyse.
     *
     * @param filename le nom du fichier dont on veut la progression
     * @return la progression, ou null en cas d'erreur
     */
    public AnalysisProgressResponse getAnalysisProgress(String filename) {
        String encodedFilename = UriUtils.encodePath(filename, StandardCharsets.UTF_8);
        String endpoint = aiServiceUrl + "/api/analyze/progress/" + encodedFilename;
        try {
            return restTemplate.getForObject(endpoint, AnalysisProgressResponse.class);
        } catch (Exception e) {
            // Endpoint de progression optionnel — on ne bloque pas en cas d'erreur
            log.debug("Progression indisponible pour '{}' : {}", filename, e.getMessage());
            return null;
        }
    }

    /**
     * Exception métier lancée lorsque le service IA est en erreur.
     */
    public static class AIServiceException extends RuntimeException {
        public AIServiceException(String message) { super(message); }
        public AIServiceException(String message, Throwable cause) { super(message, cause); }
    }

    /**
     * ByteArrayResource personnalisé pour transmettre le nom de fichier
     * dans la requête multipart (requis par FastAPI).
     */
    private static class PdfByteArrayResource extends ByteArrayResource {
        private final String filename;

        public PdfByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
