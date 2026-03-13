import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService, UserResponse } from '../../core/auth/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {

  user: UserResponse | null = null;
  userMenuOpen = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.user = this.authService.getCachedUser();

    this.authService.getProfile().subscribe({
      next: (u) => { this.user = u; },
      error: () => { if (!this.user) this.authService.logout(); }
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

  toggleUserMenu(): void {
    this.userMenuOpen = !this.userMenuOpen;
  }

  logout(): void {
    this.authService.logout();
  }

  formatDate(dateStr: string): string {
    try {
      return new Date(dateStr).toLocaleDateString('fr-FR', {
        day: 'numeric', month: 'long', year: 'numeric'
      });
    } catch { return dateStr; }
  }

  getSubscriptionLabel(type: string): string {
    switch (type) {
      case 'MENSUEL_STD': return 'Mensuel Standard';
      case 'ANNUEL_PREM': return 'Annuel Premium';
      default: return type;
    }
  }

  getStatusClass(statut: string): string {
    switch (statut) {
      case 'ACTIF': return 'badge-active';
      case 'INACTIF': return 'badge-inactive';
      case 'RESILIE': return 'badge-cancelled';
      default: return '';
    }
  }

  getStatusLabel(statut: string): string {
    switch (statut) {
      case 'ACTIF': return 'Actif';
      case 'INACTIF': return 'Inactif';
      case 'RESILIE': return 'Résilié';
      default: return statut;
    }
  }
}
