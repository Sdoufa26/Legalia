import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

// ── Interfaces pour l'API d'authentification ──

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  prenom: string;
  nom: string;
  password: string;
}

export interface TokenResponse {
  access_token: string;
  tokenType: string;
}

export interface UserResponse {
  id: string;
  email: string;
  prenom: string;
  nom: string;
  dateInscription: string;
  statutAbonnement: string;
  typeOffre: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly API = 'http://localhost:8080/api/auth';

  // Stockage en mémoire (PAS de localStorage) — le token est perdu au refresh
  private accessToken: string | null = null;
  private cachedUser: UserResponse | null = null;

  constructor(private http: HttpClient, private router: Router) {}

  /** Authentifie l'utilisateur et stocke le JWT en mémoire */
  login(request: LoginRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${this.API}/login`, request).pipe(
      tap(response => {
        this.accessToken = response.access_token;
      })
    );
  }

  /** Inscrit un nouvel utilisateur */
  register(request: RegisterRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.API}/register`, request);
  }

  /** Récupère le profil de l'utilisateur connecté et le met en cache */
  getProfile(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.API}/me`).pipe(
      tap(user => {
        this.cachedUser = user;
      })
    );
  }

  /** Retourne le token JWT courant (ou null) */
  getToken(): string | null {
    return this.accessToken;
  }

  /** Retourne l'utilisateur mis en cache (sans appel réseau) */
  getCachedUser(): UserResponse | null {
    return this.cachedUser;
  }

  /** Vérifie si un token existe et n'est pas expiré */
  isAuthenticated(): boolean {
    const token = this.accessToken;
    if (!token) return false;

    try {
      // Décode le payload JWT pour vérifier l'expiration
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expiresAt = payload.exp * 1000; // exp est en secondes
      return Date.now() < expiresAt;
    } catch {
      // Si le token est malformé, on considère qu'il est invalide
      return false;
    }
  }

  /** Alias pour compatibilité — vérifie si l'utilisateur est connecté */
  isLoggedIn(): boolean {
    return this.isAuthenticated();
  }

  /** Déconnecte l'utilisateur et redirige vers la page de login */
  logout(): void {
    this.accessToken = null;
    this.cachedUser = null;
    this.router.navigate(['/auth/login']);
  }
}
