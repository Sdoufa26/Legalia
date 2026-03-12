import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

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
  private readonly TOKEN_KEY = 'legalia_token';
  private readonly USER_KEY = 'legalia_user';

  constructor(private http: HttpClient, private router: Router) {}

  login(request: LoginRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${this.API}/login`, request).pipe(
      tap(response => {
        localStorage.setItem(this.TOKEN_KEY, response.access_token);
      })
    );
  }

  register(request: RegisterRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.API}/register`, request);
  }

  getProfile(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.API}/me`).pipe(
      tap(user => {
        localStorage.setItem(this.USER_KEY, JSON.stringify(user));
      })
    );
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getCachedUser(): UserResponse | null {
    const raw = localStorage.getItem(this.USER_KEY);
    return raw ? JSON.parse(raw) : null;
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.router.navigate(['/auth/login']);
  }
}
