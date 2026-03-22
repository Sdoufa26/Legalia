import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { DocumentService, DocumentStatusResponse } from '../../services/document.service';
import { SidebarComponent } from '../../shared/components/sidebar/sidebar.component';

@Component({
  selector: 'app-analysis-progress',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, SidebarComponent],
  templateUrl: './analysis-progress.component.html',
  styleUrl: './analysis-progress.component.css'
})
export class AnalysisProgressComponent implements OnInit, OnDestroy {

  // Identifiant du document suivi
  documentId = '';

  // État de l'analyse
  progression = 0;
  clausesAnalysees = 0;
  totalClauses = 0;
  statut = 'EN_COURS';
  erreur = '';

  // Intervalle de polling (3 secondes)
  private readonly POLLING_MS = 3000;
  private pollingTimer: ReturnType<typeof setInterval> | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private documentService: DocumentService
  ) {}

  ngOnInit(): void {
    // Récupère l'id depuis l'URL /analysis/:id/progress
    this.documentId = this.route.snapshot.paramMap.get('id') ?? '';

    if (!this.documentId) {
      this.router.navigate(['/dashboard']);
      return;
    }

    // Premier appel immédiat puis polling toutes les 3 secondes
    this.interrogerStatut();
    this.pollingTimer = setInterval(() => this.interrogerStatut(), this.POLLING_MS);
  }

  /** Arrête le polling quand le composant est détruit */
  ngOnDestroy(): void {
    this.arreterPolling();
  }

  /** Interroge l'endpoint de statut et met à jour l'affichage */
  private interrogerStatut(): void {
    this.documentService.getDocumentStatus(this.documentId).subscribe({
      next: (status: DocumentStatusResponse) => {
        this.progression = status.progression;
        this.clausesAnalysees = status.clausesAnalysees;
        this.totalClauses = status.totalClauses;
        this.statut = status.statut;

        if (status.statut === 'TERMINE' && status.progression >= 100) {
          // Analyse terminée — redirection vers la page de résultats
          this.arreterPolling();
          this.router.navigate(['/analysis', this.documentId]);
        } else if (status.statut === 'ERREUR') {
          // Erreur côté serveur — on arrête et on affiche le message
          this.arreterPolling();
          this.erreur = "L'analyse a échoué. Veuillez réessayer depuis la page Documents.";
        }
      },
      error: () => {
        // Erreur réseau passagère — on laisse le polling continuer
      }
    });
  }

  /** Arrête le polling en cours */
  private arreterPolling(): void {
    if (this.pollingTimer !== null) {
      clearInterval(this.pollingTimer);
      this.pollingTimer = null;
    }
  }
}
