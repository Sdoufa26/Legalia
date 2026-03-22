import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService, UserResponse } from '../../core/auth/auth.service';
import { DocumentService, DocumentResponse } from '../../services/document.service';
import { SidebarComponent } from '../../shared/components/sidebar/sidebar.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, SidebarComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {

  user: UserResponse | null = null;
  loading = true;
  userMenuOpen = false;

  // KPIs calculés à partir des vrais documents
  stats = {
    progress: 0,
    analyzed: 0,
    total: 0,
    pending: 0,
    errors: 0
  };

  // Documents chargés depuis l'API
  documents: DocumentResponse[] = [];

  // Modale de confirmation de suppression
  showDeleteModal = false;
  documentToDelete: DocumentResponse | null = null;
  deleting = false;

  constructor(
    private authService: AuthService,
    private documentService: DocumentService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Charge le profil utilisateur
    const cached = this.authService.getCachedUser();
    if (cached) {
      this.user = cached;
    }

    this.authService.getProfile().subscribe({
      next: (user: UserResponse) => {
        this.user = user;
      },
      error: () => {
        if (!cached) {
          this.authService.logout();
        }
      }
    });

    // Charge les documents depuis le back-end
    this.documentService.getDocuments().subscribe({
      next: (docs: DocumentResponse[]) => {
        this.documents = docs;
        this.computeStats();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  /** Calcule les KPIs à partir de la liste de documents */
  private computeStats(): void {
    const total = this.documents.length;
    const analyzed = this.documents.filter(d => d.statut === 'TERMINE').length;
    const pending = this.documents.filter(d => d.statut === 'EN_COURS').length;
    const errors = this.documents.filter(d => d.statut === 'ERREUR').length;
    const progress = total > 0 ? Math.round((analyzed / total) * 100) : 0;

    this.stats = { progress, analyzed, total, pending, errors };
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

  /** Retourne la classe CSS selon le statut du document */
  getStatusClass(statut: string): string {
    switch (statut) {
      case 'TERMINE':  return 'validated';
      case 'EN_COURS': return 'pending';
      case 'ERREUR':   return 'error';
      default:         return '';
    }
  }

  /** Retourne le label lisible du statut */
  getStatusLabel(statut: string): string {
    switch (statut) {
      case 'TERMINE':  return 'Terminé';
      case 'EN_COURS': return 'En cours';
      case 'ERREUR':   return 'Erreur';
      default:         return statut;
    }
  }

  /** Retourne le label du bouton d'action selon le statut */
  getActionLabel(statut: string): string {
    switch (statut) {
      case 'TERMINE':  return 'Voir l\'analyse';
      case 'EN_COURS': return 'Analyse en cours…';
      case 'ERREUR':   return 'Réessayer';
      default:         return 'Voir';
    }
  }

  /** Retourne la classe du bouton d'action */
  getActionClass(statut: string): string {
    switch (statut) {
      case 'TERMINE':  return 'btn-action primary';
      case 'EN_COURS': return 'btn-action progress';
      case 'ERREUR':   return 'btn-action danger';
      default:         return 'btn-action';
    }
  }

  /** Action sur clic du bouton (ouvre l'analyse, la progression ou redirige vers upload) */
  onDocumentAction(doc: DocumentResponse): void {
    if (doc.statut === 'TERMINE') {
      this.router.navigate(['/analysis', doc.id]);
    } else if (doc.statut === 'EN_COURS') {
      // Redirige vers la page de suivi de progression
      this.router.navigate(['/analysis', doc.id, 'progress']);
    } else if (doc.statut === 'ERREUR') {
      this.router.navigate(['/upload']);
    }
  }

  /** Formate la date depuis le back-end */
  formatDate(dateStr: string): string {
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString('fr-FR', {
        day: 'numeric',
        month: 'short',
        year: 'numeric'
      });
    } catch {
      return dateStr;
    }
  }

  /** Ouvre la modale de confirmation */
  openDeleteModal(event: Event, doc: DocumentResponse): void {
    event.stopPropagation();
    this.documentToDelete = doc;
    this.showDeleteModal = true;
  }

  /** Ferme la modale */
  closeDeleteModal(): void {
    this.showDeleteModal = false;
    this.documentToDelete = null;
    this.deleting = false;
  }

  /** Confirme la suppression */
  confirmDelete(): void {
    if (!this.documentToDelete) return;
    this.deleting = true;
    this.documentService.deleteDocument(this.documentToDelete.id).subscribe({
      next: () => {
        this.documents = this.documents.filter(d => d.id !== this.documentToDelete!.id);
        this.computeStats();
        this.closeDeleteModal();
      },
      error: () => {
        this.deleting = false;
      }
    });
  }
}
