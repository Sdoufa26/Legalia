import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { DocumentService, DocumentUploadResponse } from '../../services/document.service';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './upload.component.html',
  styleUrl: './upload.component.css'
})
export class UploadComponent {

  // État du drag & drop
  isDragging = false;
  selectedFile: File | null = null;

  // État de l'upload
  uploading = false;
  uploadProgress = 0;
  errorMessage = '';

  // Contraintes
  private readonly MAX_SIZE = 10 * 1024 * 1024; // 10 Mo
  private readonly ALLOWED_TYPE = 'application/pdf';

  constructor(
    private documentService: DocumentService,
    private router: Router
  ) {}

  /** Gère l'événement dragover */
  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  /** Gère l'événement dragleave */
  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  /** Gère le drop d'un fichier */
  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleFile(files[0]);
    }
  }

  /** Gère la sélection via le bouton "Choisir un fichier" */
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFile(input.files[0]);
    }
  }

  /** Valide et prépare le fichier sélectionné */
  private handleFile(file: File): void {
    this.errorMessage = '';

    if (file.type !== this.ALLOWED_TYPE) {
      this.errorMessage = 'Seuls les fichiers PDF sont acceptés.';
      return;
    }

    if (file.size > this.MAX_SIZE) {
      this.errorMessage = 'Le fichier dépasse la limite de 10 Mo.';
      return;
    }

    this.selectedFile = file;
  }

  /** Retourne la taille formatée du fichier */
  get fileSizeFormatted(): string {
    if (!this.selectedFile) return '';
    const mb = this.selectedFile.size / (1024 * 1024);
    return mb < 1
      ? `${(this.selectedFile.size / 1024).toFixed(0)} Ko`
      : `${mb.toFixed(1)} Mo`;
  }

  /** Retire le fichier sélectionné */
  removeFile(): void {
    this.selectedFile = null;
    this.errorMessage = '';
  }

  /** Lance l'upload et l'analyse du document */
  upload(): void {
    if (!this.selectedFile) return;

    this.uploading = true;
    this.errorMessage = '';
    this.uploadProgress = 0;

    // Simule une progression pendant l'upload + analyse (ne dépasse jamais 99%)
    const progressInterval = setInterval(() => {
      const remaining = 99 - this.uploadProgress;
      if (remaining > 1) {
        this.uploadProgress += remaining * 0.08;
        this.uploadProgress = Math.min(this.uploadProgress, 99);
      }
    }, 400);

    this.documentService.uploadDocument(this.selectedFile).subscribe({
      next: (response: DocumentUploadResponse) => {
        clearInterval(progressInterval);
        this.uploadProgress = 100;

        // Redirige vers la page de résultats après un court délai
        setTimeout(() => {
          this.router.navigate(['/analysis', response.idDocument]);
        }, 600);
      },
      error: (err) => {
        clearInterval(progressInterval);
        this.uploading = false;
        this.uploadProgress = 0;
        this.errorMessage = err.error?.message ?? 'Une erreur est survenue lors de l\'upload.';
      }
    });
  }
}
