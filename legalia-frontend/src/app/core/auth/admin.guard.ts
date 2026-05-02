import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const user = auth.getCachedUser();
  if (user?.role === 'ADMIN') return true;

  router.navigate(['/dashboard']);
  return false;
};
