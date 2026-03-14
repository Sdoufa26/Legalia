package com.legalia.backend.service;

import com.legalia.backend.model.PasswordResetToken;
import com.legalia.backend.model.Utilisateur;
import com.legalia.backend.repository.PasswordResetTokenRepository;
import com.legalia.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.cors.frontend-url}")
    private String frontendUrl;
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Génère un token et envoie le mail via Resend.
     * Réponse toujours 200 pour ne pas révéler si l'email existe.
     */
    @Transactional
    public void sendResetEmail(String email) {
        if (!utilisateurRepository.existsByEmail(email)) {
            return;
        }

        // Supprime les anciens tokens pour cet email
        tokenRepository.deleteByEmail(email);

        // Crée un nouveau token valable 15 minutes
        String token = UUID.randomUUID().toString();
        tokenRepository.save(new PasswordResetToken(token, email));

        String resetLink = frontendUrl + "/auth/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("LEGALIA — Réinitialisation de votre mot de passe");
        message.setText(
                "Bonjour,\n\n" +
                        "Vous avez demandé à réinitialiser votre mot de passe LEGALIA.\n\n" +
                        "Cliquez sur le lien ci-dessous (valable 15 minutes) :\n" +
                        resetLink + "\n\n" +
                        "Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.\n\n" +
                        "L'équipe LEGALIA"
        );

        mailSender.send(message);
    }

    /**
     * Vérifie le token et met à jour le mot de passe.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Lien invalide ou expiré"));

        if (resetToken.isExpired()) {
            throw new IllegalArgumentException("Lien invalide ou expiré");
        }

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("Ce lien a déjà été utilisé");
        }

        Utilisateur utilisateur = utilisateurRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        utilisateur.setMotDePasseHash(passwordEncoder.encode(newPassword));
        utilisateurRepository.save(utilisateur);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
}