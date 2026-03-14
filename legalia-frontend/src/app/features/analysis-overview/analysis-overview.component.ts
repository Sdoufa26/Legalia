import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { SidebarComponent } from '../../shared/components/sidebar/sidebar.component';
import { forkJoin } from 'rxjs';
import { AuthService, UserResponse } from '../../core/auth/auth.service';
import {
  DocumentService,
  DocumentResponse,
  CarteResultatResponse
} from '../../services/document.service';

/** Résumé d'un document analysé avec ses statistiques de vigilance */
interface AnalyzedDocSummary {
  document: DocumentResponse;
  clauses: CarteResultatResponse[];
  nbClauses: number;
  nbEleve: number;
  nbMoyen: number;
  nbFaible: number;
}

@Component({
  selector: 'app-analysis-overview',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive, SidebarComponent],
  templateUrl: './analysis-overview.component.html',
  styleUrl: './analysis-overview.component.css'
})
export class AnalysisOverviewComponent implements OnInit {

  user: UserResponse | null = null;
  loading = true;
  userMenuOpen = false;

  // Données
  analyzedDocs: AnalyzedDocSummary[] = [];

  // Stats globales
  globalStats = {
    totalDocuments: 0,
    totalClauses: 0,
    totalEleve: 0,
    totalMoyen: 0,
    totalFaible: 0
  };

  // Modale de confirmation de suppression
  showDeleteModal = false;
  documentToDelete: AnalyzedDocSummary | null = null;
  deleting = false;

  constructor(
    private authService: AuthService,
    private documentService: DocumentService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Profil utilisateur
    const cached = this.authService.getCachedUser();
    if (cached) this.user = cached;

    this.authService.getProfile().subscribe({
      next: (u: UserResponse) => this.user = u,
      error: () => { if (!cached) this.authService.logout(); }
    });

    // Charge les documents puis les résultats des documents terminés
    this.documentService.getDocuments().subscribe({
      next: (docs: DocumentResponse[]) => {
        const termines = docs.filter(d => d.statut === 'TERMINE');

        if (termines.length === 0) {
          this.loading = false;
          return;
        }

        // Récupère les résultats de chaque document analysé en parallèle
        const requests = termines.map(doc =>
          this.documentService.getDocumentResults(doc.id)
        );

        forkJoin(requests).subscribe({
          next: (results: CarteResultatResponse[][]) => {
            this.analyzedDocs = termines.map((doc, i) => {
              const clauses = results[i] || [];
              return {
                document: doc,
                clauses,
                nbClauses: clauses.length,
                nbEleve: clauses.filter(c => c.niveauVigilance === 'ELEVE').length,
                nbMoyen: clauses.filter(c => c.niveauVigilance === 'MOYEN').length,
                nbFaible: clauses.filter(c => c.niveauVigilance === 'FAIBLE').length
              };
            });

            // Tri par date (plus récent en premier)
            this.analyzedDocs.sort((a, b) =>
              new Date(b.document.dateTeleversement).getTime() -
              new Date(a.document.dateTeleversement).getTime()
            );

            this.computeGlobalStats();
            this.loading = false;
          },
          error: () => this.loading = false
        });
      },
      error: () => this.loading = false
    });
  }

  private computeGlobalStats(): void {
    this.globalStats = {
      totalDocuments: this.analyzedDocs.length,
      totalClauses: this.analyzedDocs.reduce((s, d) => s + d.nbClauses, 0),
      totalEleve: this.analyzedDocs.reduce((s, d) => s + d.nbEleve, 0),
      totalMoyen: this.analyzedDocs.reduce((s, d) => s + d.nbMoyen, 0),
      totalFaible: this.analyzedDocs.reduce((s, d) => s + d.nbFaible, 0)
    };
  }

  /** Pourcentage d'un niveau de vigilance */
  getPercentage(count: number): number {
    if (this.globalStats.totalClauses === 0) return 0;
    return Math.round((count / this.globalStats.totalClauses) * 100);
  }

  /** Navigue vers le détail de l'analyse */
  viewAnalysis(docId: string): void {
    this.router.navigate(['/analysis', docId]);
  }

  /** Formate une date */
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

  /** Ouvre la modale de confirmation */
  openDeleteModal(event: Event, item: AnalyzedDocSummary): void {
    event.stopPropagation();
    this.documentToDelete = item;
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
    this.documentService.deleteDocument(this.documentToDelete.document.id).subscribe({
      next: () => {
        this.analyzedDocs = this.analyzedDocs.filter(
          d => d.document.id !== this.documentToDelete!.document.id
        );
        this.computeGlobalStats();
        this.closeDeleteModal();
      },
      error: () => {
        this.deleting = false;
      }
    });
  }
}
