import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService, UserResponse } from '../../core/auth/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {

  user: UserResponse | null = null;
  userMenuOpen = false;

  // Édition profil
  editingProfile = false;
  editPrenom = '';
  editNom = '';
  profileSaving = false;
  profileSuccess = '';
  profileError = '';

  // Changement mot de passe
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  passwordSaving = false;
  passwordSuccess = '';
  passwordError = '';

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

  // === Édition du profil ===

  startEditProfile(): void {
    if (!this.user) return;
    this.editPrenom = this.user.prenom;
    this.editNom = this.user.nom;
    this.editingProfile = true;
    this.profileSuccess = '';
    this.profileError = '';
  }

  cancelEditProfile(): void {
    this.editingProfile = false;
    this.profileError = '';
  }

  saveProfile(): void {
    if (!this.editPrenom.trim() || !this.editNom.trim()) {
      this.profileError = 'Le prénom et le nom sont obligatoires.';
      return;
    }
    this.profileSaving = true;
    this.profileError = '';
    this.profileSuccess = '';

    this.authService.updateProfile(this.editPrenom.trim(), this.editNom.trim()).subscribe({
      next: (u) => {
        this.user = u;
        this.editingProfile = false;
        this.profileSaving = false;
        this.profileSuccess = 'Profil mis à jour avec succès.';
        setTimeout(() => this.profileSuccess = '', 4000);
      },
      error: (err) => {
        this.profileSaving = false;
        this.profileError = err.error?.erreur || 'Erreur lors de la mise à jour.';
      }
    });
  }

  // === Changement de mot de passe ===

  savePassword(): void {
    this.passwordError = '';
    this.passwordSuccess = '';

    if (!this.currentPassword || !this.newPassword || !this.confirmPassword) {
      this.passwordError = 'Tous les champs sont obligatoires.';
      return;
    }
    if (this.newPassword.length < 6) {
      this.passwordError = 'Le nouveau mot de passe doit contenir au moins 6 caractères.';
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.passwordError = 'Les mots de passe ne correspondent pas.';
      return;
    }

    this.passwordSaving = true;

    this.authService.changePassword(this.currentPassword, this.newPassword).subscribe({
      next: () => {
        this.passwordSaving = false;
        this.passwordSuccess = 'Mot de passe modifié avec succès.';
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
        setTimeout(() => this.passwordSuccess = '', 4000);
      },
      error: (err) => {
        this.passwordSaving = false;
        this.passwordError = err.error?.erreur || 'Erreur lors du changement de mot de passe.';
      }
    });
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
