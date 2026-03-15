package com.legalia.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration de l'exécution asynchrone.
 * Active @Async et définit le pool de threads dédié à l'analyse IA.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Pool de threads pour les analyses IA.
     * 2 threads de base, 4 au maximum — chaque analyse peut durer jusqu'à 5 minutes.
     */
    @Bean(name = "analysisExecutor")
    public Executor analysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("analyse-ia-");
        executor.initialize();
        return executor;
    }
}
