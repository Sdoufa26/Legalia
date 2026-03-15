import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import {
  DocumentService,
  DocumentUploadResponse,
  DocumentStatusResponse
} from '../../services/document.service';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './upload.component.html',
  styleUrl: './upload.component.css'
})
export class UploadComponent implements OnDestroy {

  // ── État drag & drop ──
  isDragging = false;
  selectedFile: File | null = null;

  // ── Phase 1 : envoi du fichier au serveur ──
  uploading = false;
  uploadError = '';

  // ── Phase 2 : polling du statut d'analyse ──
  analysing = false;
  analysisProgression = 0;
  clausesAnalysees = 0;
  totalClauses = 0;
  analysisError = '';

  // ── Contraintes fichier ──
  private readonly MAX_SIZE = 10 * 1024 * 1024; // 10 Mo
  private readonly ALLOWED_TYPE = 'application/pdf';

  // ── Intervalle de polling (3 secondes) ──
  private readonly POLLING_MS = 3000;
  private pollingTimer: ReturnType<typeof setInterval> | null = null;

  // Alias pour le template (utilisé par la zone d'upload)
  get errorMessage(): string { return this.uploadError; }

  constructor(
    private documentService: DocumentService,
    private router: Router
  ) {}

  /** Arrête le polling quand le composant est détruit */
  ngOnDestroy(): void {
    this.stopPolling();
  }

  // ────────────────────────────────────────────
  // Gestion du drag & drop
  // ────────────────────────────────────────────

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleFile(files[0]);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFile(input.files[0]);
    }
  }

  /** Valide et prépare le fichier sélectionné */
  private handleFile(file: File): void {
    this.uploadError = '';

    if (file.type !== this.ALLOWED_TYPE) {
      this.uploadError = 'Seuls les fichiers PDF sont acceptés.';
      return;
    }
    if (file.size > this.MAX_SIZE) {
      this.uploadError = 'Le fichier dépasse la limite de 10 Mo.';
      return;
    }

    this.selectedFile = file;
  }

  get fileSizeFormatted(): string {
    if (!this.selectedFile) return '';
    const mb = this.selectedFile.size / (1024 * 1024);
    return mb < 1
      ? `${(this.selectedFile.size / 1024).toFixed(0)} Ko`
      : `${mb.toFixed(1)} Mo`;
  }

  removeFile(): void {
    this.selectedFile = null;
    this.uploadError = '';
  }

  // ────────────────────────────────────────────
  // Upload + lancement de l'analyse
  // ────────────────────────────────────────────

  /** Lance l'upload du document puis démarre le polling d'avancement */
  upload(): void {
    if (!this.selectedFile) return;

    this.uploading = true;
    this.uploadError = '';

    this.documentService.uploadDocument(this.selectedFile).subscribe({
      next: (response: DocumentUploadResponse) => {
        // Le fichier est reçu par le backend — on passe en phase d'analyse
        this.uploading = false;
        this.analysing = true;
        this.demarrerPolling(response.idDocument);
      },
      error: (err) => {
        this.uploading = false;
        this.uploadError = err.error?.message ?? 'Une erreur est survenue lors de l\'upload.';
      }
    });
  }

  // ────────────────────────────────────────────
  // Polling du statut d'analyse
  // ────────────────────────────────────────────

  /** Démarre le polling toutes les 3 secondes après l'upload */
  private demarrerPolling(documentId: string): void {
    // Premier appel immédiat pour ne pas attendre 3 s
    this.interrogerStatut(documentId);

    this.pollingTimer = setInterval(() => {
      this.interrogerStatut(documentId);
    }, this.POLLING_MS);
  }

  /** Interroge l'endpoint de statut et met à jour l'affichage */
  private interrogerStatut(documentId: string): void {
    this.documentService.getDocumentStatus(documentId).subscribe({
      next: (status: DocumentStatusResponse) => {
        this.analysisProgression = status.progression;
        this.clausesAnalysees = status.clausesAnalysees;
        this.totalClauses = status.totalClauses;

        if (status.statut === 'TERMINE' && status.progression >= 100) {
          // Analyse terminée — redirection vers la page de résultats
          this.stopPolling();
          this.router.navigate(['/analysis', documentId]);
        } else if (status.statut === 'ERREUR') {
          // Erreur côté serveur — on arrête et on affiche le message
          this.stopPolling();
          this.analysing = false;
          this.analysisError = 'L\'analyse a échoué. Veuillez réessayer.';
        }
      },
      error: () => {
        // Erreur réseau passagère — on laisse le polling continuer
      }
    });
  }

  /** Arrête le polling en cours */
  private stopPolling(): void {
    if (this.pollingTimer !== null) {
      clearInterval(this.pollingTimer);
      this.pollingTimer = null;
    }
  }

  // ────────────────────────────────────────────
  // Réinitialisation après erreur d'analyse
  // ────────────────────────────────────────────

  /** Réinitialise l'état pour permettre un nouvel essai */
  reessayer(): void {
    this.analysisError = '';
    this.analysisProgression = 0;
    this.clausesAnalysees = 0;
    this.totalClauses = 0;
    this.selectedFile = null;
  }
}
