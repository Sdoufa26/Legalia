import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Guard pour les routes protégées (dashboard, upload, analysis…)
 * Redirige vers /auth/login si l'utilisateur n'est pas connecté
 */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isLoggedIn()) {
    return true;
  }
  router.navigate(['/auth/login']);
  return false;
};

/**
 * Guard pour les routes publiques d'authentification (login, register…)
 * Redirige vers /dashboard si l'utilisateur est déjà connecté
 */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isLoggedIn()) {
    return true;
  }
  router.navigate(['/dashboard']);
  return false;
};
