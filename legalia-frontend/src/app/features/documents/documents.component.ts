import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { DocumentService, DocumentResponse } from '../../services/document.service';

@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './documents.component.html',
  styleUrl: './documents.component.css'
})
export class DocumentsComponent implements OnInit {

  documents: DocumentResponse[] = [];
  loading = true;

  // Modale de confirmation de suppression
  showDeleteModal = false;
  documentToDelete: DocumentResponse | null = null;
  deleting = false;

  constructor(
    private documentService: DocumentService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.documentService.getDocuments().subscribe({
      next: (docs) => {
        this.documents = docs;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  getStatusClass(statut: string): string {
    switch (statut) {
      case 'TERMINE':  return 'validated';
      case 'EN_COURS': return 'pending';
      case 'ERREUR':   return 'error';
      default:         return '';
    }
  }

  getStatusLabel(statut: string): string {
    switch (statut) {
      case 'TERMINE':  return 'Terminé';
      case 'EN_COURS': return 'En cours';
      case 'ERREUR':   return 'Erreur';
      default:         return statut;
    }
  }

  getActionLabel(statut: string): string {
    switch (statut) {
      case 'TERMINE':  return 'Voir l\'analyse';
      case 'EN_COURS': return 'En cours…';
      case 'ERREUR':   return 'Réessayer';
      default:         return 'Voir';
    }
  }

  getActionClass(statut: string): string {
    switch (statut) {
      case 'TERMINE':  return 'btn-action primary';
      case 'EN_COURS': return 'btn-action secondary';
      case 'ERREUR':   return 'btn-action danger';
      default:         return 'btn-action';
    }
  }

  onDocumentAction(doc: DocumentResponse): void {
    if (doc.statut === 'TERMINE') {
      this.router.navigate(['/analysis', doc.id]);
    } else if (doc.statut === 'ERREUR') {
      this.router.navigate(['/upload']);
    }
  }

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

  formatSize(sizeKb: number): string {
    if (sizeKb < 1024) return `${sizeKb} Ko`;
    return `${(sizeKb / 1024).toFixed(1)} Mo`;
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
        this.closeDeleteModal();
      },
      error: () => {
        this.deleting = false;
      }
    });
  }
}
