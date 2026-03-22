import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  // ── Routes publiques ──
  {
    path: '',
    loadComponent: () =>
      import('./features/landing/landing.component').then(m => m.LandingComponent)
  },
  {
    path: 'auth/login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent),
    canActivate: [guestGuard]
  },
  {
    path: 'auth/register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then(m => m.RegisterComponent),
    canActivate: [guestGuard]
  },
  {
    path: 'auth/forgot-password',
    loadComponent: () =>
      import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },

  // ── Routes protégées (nécessitent une authentification) ──
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard]
  },
  {
    path: 'upload',
    loadComponent: () =>
      import('./features/upload/upload.component').then(m => m.UploadComponent),
    canActivate: [authGuard]
  },
  {
    path: 'documents',
    loadComponent: () =>
      import('./features/documents/documents.component').then(m => m.DocumentsComponent),
    canActivate: [authGuard]
  },
  {
    path: 'analysis',
    loadComponent: () =>
      import('./features/analysis-overview/analysis-overview.component').then(m => m.AnalysisOverviewComponent),
    canActivate: [authGuard]
  },
  {
    path: 'analysis/:id/progress',
    loadComponent: () =>
      import('./features/analysis-progress/analysis-progress.component').then(m => m.AnalysisProgressComponent),
    canActivate: [authGuard]
  },
  {
    path: 'analysis/:id',
    loadComponent: () =>
      import('./features/analysis/analysis.component').then(m => m.AnalysisComponent),
    canActivate: [authGuard]
  },
  {
    path: 'profile',
    loadComponent: () =>
      import('./features/profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [authGuard]
  },
  {
    path: 'auth/reset-password',
    loadComponent: () =>
      import('./features/auth/reset-password/reset-password.component')
        .then(m => m.ResetPasswordComponent)
  },
  // ── Redirection par défaut ──
  {
    path: '**',
    redirectTo: ''
  }
];
