import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService, UserResponse } from '../../core/auth/auth.service';
import { AdminService, AdminStats, ActionLog } from '../../services/admin.service';
import { SidebarComponent } from '../../shared/components/sidebar/sidebar.component';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, SidebarComponent],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css'
})
export class AdminComponent implements OnInit {

  user: UserResponse | null = null;
  stats: AdminStats | null = null;
  users: UserResponse[] = [];
  logs: ActionLog[] = [];
  loading = true;
  userMenuOpen = false;
  activeTab: 'stats' | 'users' | 'logs' = 'stats';

  constructor(
    private authService: AuthService,
    private adminService: AdminService
  ) {}

  ngOnInit(): void {
    this.user = this.authService.getCachedUser();
    this.authService.getProfile().subscribe({ next: (u) => this.user = u });

    this.adminService.getStats().subscribe({
      next: (s) => { this.stats = s; this.loading = false; },
      error: () => { this.loading = false; }
    });

    this.adminService.getUsers().subscribe({
      next: (u) => this.users = u
    });

    this.adminService.getLogs().subscribe({
      next: (l) => this.logs = l
    });
  }

  get initials(): string {
    if (!this.user) return '?';
    return `${this.user.prenom.charAt(0)}${this.user.nom.charAt(0)}`.toUpperCase();
  }

  get fullName(): string {
    if (!this.user) return '';
    return `${this.user.prenom} ${this.user.nom}`;
  }

  toggleUserMenu(): void { this.userMenuOpen = !this.userMenuOpen; }
  logout(): void { this.authService.logout(); }

  setTab(tab: 'stats' | 'users' | 'logs'): void { this.activeTab = tab; }

  formatDate(dateStr: string): string {
    try {
      return new Date(dateStr).toLocaleDateString('fr-FR', {
        day: 'numeric', month: 'short', year: 'numeric'
      });
    } catch { return dateStr; }
  }

  formatDateTime(dateStr: string): string {
    try {
      return new Date(dateStr).toLocaleString('fr-FR', {
        day: 'numeric', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
      });
    } catch { return dateStr; }
  }

  getActionLabel(action: string): string {
    switch (action) {
      case 'LOGIN': return 'Connexion';
      case 'REGISTER': return 'Inscription';
      case 'UPLOAD': return 'Upload';
      case 'CHANGE_PASSWORD': return 'Changement de mot de passe';
      default: return action;
    }
  }

  getActionClass(action: string): string {
    switch (action) {
      case 'LOGIN': return 'badge-login';
      case 'REGISTER': return 'badge-register';
      case 'UPLOAD': return 'badge-upload';
      case 'CHANGE_PASSWORD': return 'badge-password';
      default: return '';
    }
  }

  getRoleClass(role: string): string {
    return role === 'ADMIN' ? 'badge-admin' : 'badge-client';
  }

  getStatusClass(statut: string): string {
    switch (statut) {
      case 'ACTIF': return 'badge-active';
      case 'INACTIF': return 'badge-inactive';
      case 'RESILIE': return 'badge-cancelled';
      default: return '';
    }
  }
}
