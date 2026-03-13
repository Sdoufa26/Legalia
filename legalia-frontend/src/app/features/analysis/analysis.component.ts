import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { DocumentService, DocumentResponse, CarteResultatResponse } from '../../services/document.service';

@Component({
  selector: 'app-analysis',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './analysis.component.html',
  styleUrl: './analysis.component.css'
})
export class AnalysisComponent implements OnInit {

  document: DocumentResponse | null = null;
  clauses: CarteResultatResponse[] = [];
  loading = true;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private documentService: DocumentService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/dashboard']);
      return;
    }

    this.documentService.getDocument(id).subscribe({
      next: (doc) => this.document = doc,
      error: () => this.errorMessage = 'Document introuvable.'
    });

    this.documentService.getDocumentResults(id).subscribe({
      next: (clauses) => {
        this.clauses = clauses;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Impossible de charger les résultats d\'analyse.';
      }
    });
  }

  /** Classe CSS de la carte selon le niveau de vigilance */
  getCardClass(niveau: string): string {
    switch (niveau) {
      case 'ELEVE':  return 'card-alert';
      case 'MOYEN':  return 'card-risk';
      case 'FAIBLE': return 'card-opportunity';
      default:       return 'card-risk';
    }
  }

  /** Classe CSS du badge */
  getBadgeClass(niveau: string): string {
    switch (niveau) {
      case 'ELEVE':  return 'badge-alert';
      case 'MOYEN':  return 'badge-risk';
      case 'FAIBLE': return 'badge-opportunity';
      default:       return 'badge-risk';
    }
  }

  /** Label du badge (Alert / Risk / Opportunity) */
  getBadgeLabel(niveau: string): string {
    switch (niveau) {
      case 'ELEVE':  return 'Alert';
      case 'MOYEN':  return 'Risk';
      case 'FAIBLE': return 'Opportunity';
      default:       return 'Risk';
    }
  }
}
