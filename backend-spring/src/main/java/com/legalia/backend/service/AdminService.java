package com.legalia.backend.service;

import com.legalia.backend.dto.ActionLogResponse;
import com.legalia.backend.dto.AdminStatsResponse;
import com.legalia.backend.dto.UserResponse;
import com.legalia.backend.model.Utilisateur;
import com.legalia.backend.repository.ActionLogRepository;
import com.legalia.backend.repository.DocumentRepository;
import com.legalia.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UtilisateurRepository utilisateurRepository;
    private final ActionLogRepository actionLogRepository;
    private final DocumentRepository documentRepository;

    /** Retourne tous les utilisateurs */
    public List<UserResponse> getAllUsers() {
        return utilisateurRepository.findAll().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    /** Retourne les statistiques globales */
    public AdminStatsResponse getStats() {
        long totalUsers = utilisateurRepository.count();
        long activeUsers = utilisateurRepository.findAll().stream()
                .filter(u -> u.getStatutAbonnement() == Utilisateur.StatutAbonnement.ACTIF)
                .count();
        long totalActions = actionLogRepository.count();

        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalDocuments(documentRepository.count())
                .totalActions(totalActions)
                .build();
    }

    /** Retourne les 50 dernières actions */
    public List<ActionLogResponse> getRecentLogs() {
        return actionLogRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(log -> ActionLogResponse.builder()
                        .email(log.getEmail())
                        .action(log.getAction())
                        .details(log.getDetails())
                        .createdAt(log.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private UserResponse toUserResponse(Utilisateur u) {
        return UserResponse.builder()
                .id(u.getIdUtilisateur())
                .email(u.getEmail())
                .prenom(u.getPrenom())
                .nom(u.getNom())
                .dateInscription(u.getDateInscription())
                .statutAbonnement(u.getStatutAbonnement().name())
                .typeOffre(u.getTypeOffre().name())
                .role(u.getRole().name())
                .build();
    }
}