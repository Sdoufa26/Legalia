package com.legalia.backend.controller;

import com.legalia.backend.config.JwtUtil;
import com.legalia.backend.dto.LoginRequest;
import com.legalia.backend.dto.RegisterRequest;
import com.legalia.backend.dto.TokenResponse;
import com.legalia.backend.dto.UserResponse;
import com.legalia.backend.service.AuthService;
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
}
