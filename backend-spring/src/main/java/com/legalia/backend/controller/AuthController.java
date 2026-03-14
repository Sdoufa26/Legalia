package com.legalia.backend.controller;

import com.legalia.backend.config.JwtUtil;
import com.legalia.backend.dto.*;
import com.legalia.backend.service.AuthService;
import com.legalia.backend.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints d'authentification publics (/api/auth/**) et profil protégé.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final PasswordResetService passwordResetService;

    /**
     * POST /api/auth/register
     * Crée un nouveau compte utilisateur.
     * Retourne 201 Created avec le profil, ou 400 si l'email est déjà pris.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            UserResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/login
     * Authentifie l'utilisateur et retourne un token JWT.
     * Retourne 401 si les identifiants sont incorrects.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            TokenResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erreur", e.getMessage()));
        }
    }

    /**
     * GET /api/auth/me  [PROTÉGÉ]
     * Retourne le profil de l'utilisateur connecté.
     * L'email est extrait depuis le SecurityContext (lui-même alimenté par JwtAuthFilter).
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(
            @RequestHeader("Authorization") String authHeader) {

        // Extrait l'email directement depuis le token
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extractEmail(token);

        UserResponse response = authService.getProfile(email);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/auth/profile  [PROTÉGÉ]
     * Met à jour le prénom et le nom de l'utilisateur connecté.
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateProfileRequest request) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extractEmail(token);
        UserResponse response = authService.updateProfile(email, request);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/auth/password  [PROTÉGÉ]
     * Change le mot de passe de l'utilisateur connecté.
     */
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extractEmail(token);
        try {
            authService.changePassword(email, request);
            return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/forgot-password  [PUBLIC]
     * Envoie un email de réinitialisation si l'adresse existe en base.
     * Retourne toujours 200 pour ne pas révéler si l'email est enregistré.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.sendResetEmail(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Si cet email existe, un lien a été envoyé."));
    }

    /**
     * POST /api/auth/reset-password  [PUBLIC]
     * Vérifie le token de réinitialisation et met à jour le mot de passe.
     * Retourne 400 si le token est invalide, expiré ou déjà utilisé.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Mot de passe mis à jour avec succès."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        }
    }
}
