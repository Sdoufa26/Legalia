import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService, UserResponse } from '../../core/auth/auth.service';

interface Document {
  name: string;
  date: string;
  status: 'Validated' | 'Pending' | 'Error';
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {

  user: UserResponse | null = null;
  loading = true;
  userMenuOpen = false;

  stats = {
    progress: 50,
    analyzed: 3,
    total: 6,
    pending: 2
  };

  documents: Document[] = [
    { name: 'Contrat assurance habitation - Client Martin', date: '24 Jan 2026', status: 'Validated' },
    { name: 'Police automobile entreprise - SAS Durand',   date: '23 Jan 2026', status: 'Validated' },
    { name: 'Convention RC professionnelle - Cabinet Avocat', date: '22 Jan 2026', status: 'Pending'   },
    { name: 'Contrat prévoyance collective - PME Solutions',  date: '21 Jan 2026', status: 'Validated' },
    { name: 'Assurance cyber-risques - Tech Startup Inc',     date: '20 Jan 2026', status: 'Error'     },
  ];

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    // Essaie d'abord le cache, puis appelle le backend
    const cached = this.authService.getCachedUser();
    if (cached) {
      this.user = cached;
      this.loading = false;
    }

    this.authService.getProfile().subscribe({
      next: (user : UserResponse) => {
        this.user = user;
        this.loading = false;
      },
      error: () => {
        if (!cached) {
          this.authService.logout();
        }
        this.loading = false;
      }
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

  getStatusClass(status: string): string {
    return status.toLowerCase();
  }

  getActionLabel(status: string): string {
    switch (status) {
      case 'Validated': return 'View Analysis';
      case 'Pending':   return 'Analyze';
      case 'Error':     return 'Retry';
      default:          return 'View';
    }
  }

  getActionClass(status: string): string {
    switch (status) {
      case 'Validated': return 'btn-action primary';
      case 'Pending':   return 'btn-action secondary';
      case 'Error':     return 'btn-action danger';
      default:          return 'btn-action';
    }
  }
}
