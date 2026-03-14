package com.legalia.backend.service;

import com.legalia.backend.config.JwtUtil;
import com.legalia.backend.dto.*;
import com.legalia.backend.model.Utilisateur;
import com.legalia.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service d'authentification.
 * Implémente UserDetailsService pour être utilisé par Spring Security
 * (chargement de l'utilisateur lors de la validation du token JWT).
 */
@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * Chargement de l'utilisateur par email — requis par Spring Security.
     * Appelé par JwtAuthFilter à chaque requête authentifiée.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouvé pour l'email : " + email));

        return User.builder()
                .username(utilisateur.getEmail())
                .password(utilisateur.getMotDePasseHash())
                .roles(utilisateur.getRole().name())  // ex: "CLIENT" → "ROLE_CLIENT"
                .build();
    }

    /**
     * Inscription d'un nouvel utilisateur.
     * Le mot de passe est hashé avec BCrypt avant stockage.
     */
    public UserResponse register(RegisterRequest request) {
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Un compte existe déjà avec cet email");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(request.getEmail());
        utilisateur.setPrenom(request.getPrenom());
        utilisateur.setNom(request.getNom());
        utilisateur.setMotDePasseHash(passwordEncoder.encode(request.getPassword()));
        utilisateur.setDateInscription(LocalDateTime.now());
        // Les valeurs par défaut (ACTIF, MENSUEL_STD, CLIENT) sont définies dans le modèle

        Utilisateur saved = utilisateurRepository.save(utilisateur);
        return toUserResponse(saved);
    }

    /**
     * Connexion — vérifie le mot de passe et retourne un token JWT si valide.
     */
    public TokenResponse login(LoginRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email ou mot de passe incorrect"));

        // Comparaison du mot de passe fourni avec le hash stocké
        if (!passwordEncoder.matches(request.getPassword(), utilisateur.getMotDePasseHash())) {
            throw new IllegalArgumentException("Email ou mot de passe incorrect");
        }

        String token = jwtUtil.generateToken(utilisateur.getEmail());
        return TokenResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .build();
    }

    /**
     * Récupération du profil de l'utilisateur connecté (à partir de son email extrait du JWT).
     */
    public UserResponse getProfile(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
        return toUserResponse(utilisateur);
    }

    /**
     * Met à jour le prénom et nom de l'utilisateur.
     */
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
        utilisateur.setPrenom(request.getPrenom());
        utilisateur.setNom(request.getNom());
        Utilisateur saved = utilisateurRepository.save(utilisateur);
        return toUserResponse(saved);
    }

    /**
     * Change le mot de passe de l'utilisateur après vérification de l'ancien.
     */
    public void changePassword(String email, ChangePasswordRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), utilisateur.getMotDePasseHash())) {
            throw new IllegalArgumentException("Mot de passe actuel incorrect");
        }
        utilisateur.setMotDePasseHash(passwordEncoder.encode(request.getNewPassword()));
        utilisateurRepository.save(utilisateur);
    }

    // Conversion entité → DTO (jamais de mot de passe dans la réponse)
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
