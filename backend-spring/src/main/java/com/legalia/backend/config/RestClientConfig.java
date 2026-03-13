package com.legalia.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration du client HTTP pour les appels vers le service IA (FastAPI).
 */
@Configuration
public class RestClientConfig {

    /**
     * RestTemplate avec un timeout de 120 secondes en lecture,
     * pour tolérer les analyses longues du service IA.
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);   // 10s pour établir la connexion
        factory.setReadTimeout(120_000);     // 120s pour recevoir la réponse complète
        return new RestTemplate(factory);
    }
}
