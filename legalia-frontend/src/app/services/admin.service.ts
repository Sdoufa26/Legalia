import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserResponse } from '../core/auth/auth.service';

export interface AdminStats {
  totalUsers: number;
  activeUsers: number;
  totalDocuments: number;
  totalActions: number;
}

export interface ActionLog {
  email: string;
  action: string;
  details: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly API = 'http://localhost:8080/api/admin';

  constructor(private http: HttpClient) {}

  getStats(): Observable<AdminStats> {
    return this.http.get<AdminStats>(`${this.API}/stats`);
  }

  getUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.API}/users`);
  }

  getLogs(): Observable<ActionLog[]> {
    return this.http.get<ActionLog[]>(`${this.API}/logs`);
  }
}
