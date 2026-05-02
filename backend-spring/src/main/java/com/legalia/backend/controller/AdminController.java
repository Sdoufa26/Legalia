package com.legalia.backend.controller;

import com.legalia.backend.config.JwtUtil;
import com.legalia.backend.dto.ActionLogResponse;
import com.legalia.backend.dto.AdminStatsResponse;
import com.legalia.backend.dto.UserResponse;
import com.legalia.backend.model.Utilisateur;
import com.legalia.backend.repository.UtilisateurRepository;
import com.legalia.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints d'administration — accessibles uniquement aux utilisateurs ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final JwtUtil jwtUtil;
    private final UtilisateurRepository utilisateurRepository;

    /** Vérifie que l'utilisateur est bien ADMIN */
    private boolean isAdmin(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extractEmail(token);
        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
        return user.getRole() == Utilisateur.RoleUtilisateur.ADMIN;
    }

    /**
     * GET /api/admin/stats
     * Retourne les statistiques globales de l'application.
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestHeader("Authorization") String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("erreur", "Accès réservé aux administrateurs"));
        }
        AdminStatsResponse stats = adminService.getStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/admin/users
     * Retourne la liste de tous les utilisateurs.
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestHeader("Authorization") String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("erreur", "Accès réservé aux administrateurs"));
        }
        List<UserResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/admin/logs
     * Retourne les 50 dernières actions effectuées sur la plateforme.
     */
    @GetMapping("/logs")
    public ResponseEntity<?> getLogs(
            @RequestHeader("Authorization") String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("erreur", "Accès réservé aux administrateurs"));
        }
        List<ActionLogResponse> logs = adminService.getRecentLogs();
        return ResponseEntity.ok(logs);
    }
}