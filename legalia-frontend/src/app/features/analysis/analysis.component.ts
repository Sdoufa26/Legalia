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
  exporting = false;

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

  /** Label du badge */
  getBadgeLabel(niveau: string): string {
    switch (niveau) {
      case 'ELEVE':  return 'Alerte';
      case 'MOYEN':  return 'Risque';
      case 'FAIBLE': return 'Opportunité';
      default:       return 'Risque';
    }
  }

  /** Label du niveau en français */
  getVigilanceLabel(niveau: string): string {
    switch (niveau) {
      case 'ELEVE':  return 'ÉLEVÉ';
      case 'MOYEN':  return 'MOYEN';
      case 'FAIBLE': return 'FAIBLE';
      default:       return niveau;
    }
  }

  /** Exporte les résultats en PDF */
  async exportPDF(): Promise<void> {
    if (!this.document || this.clauses.length === 0) return;
    this.exporting = true;

    try {
      const { jsPDF } = await import('jspdf');
      const pdf = new jsPDF('p', 'mm', 'a4');
      const pageWidth = pdf.internal.pageSize.getWidth();
      const pageHeight = pdf.internal.pageSize.getHeight();
      const margin = 20;
      const contentWidth = pageWidth - margin * 2;
      let y = margin;

      // ── Couleurs
      const colors: Record<string, { r: number; g: number; b: number }> = {
        ELEVE:  { r: 230, g: 81, b: 0 },
        MOYEN:  { r: 249, g: 168, b: 37 },
        FAIBLE: { r: 46, g: 125, b: 50 }
      };

      const checkNewPage = (needed: number): void => {
        if (y + needed > pageHeight - margin) {
          pdf.addPage();
          y = margin;
        }
      };

      // ── EN-TÊTE
      pdf.setFillColor(26, 46, 90);
      pdf.rect(0, 0, pageWidth, 45, 'F');

      pdf.setTextColor(255, 255, 255);
      pdf.setFontSize(22);
      pdf.setFont('helvetica', 'bold');
      pdf.text('LEGALIA', margin, 18);

      pdf.setFontSize(10);
      pdf.setFont('helvetica', 'normal');
      pdf.text('Rapport d\'analyse contractuelle', margin, 26);

      pdf.setFontSize(9);
      pdf.text(`Document : ${this.document.nomFichier}`, margin, 34);
      const dateStr = new Date().toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
      pdf.text(`Date : ${dateStr}`, pageWidth - margin - pdf.getTextWidth(`Date : ${dateStr}`), 34);

      y = 55;

      // ── STATISTIQUES RÉSUMÉ
      const nbEleve = this.clauses.filter(c => c.niveauVigilance === 'ELEVE').length;
      const nbMoyen = this.clauses.filter(c => c.niveauVigilance === 'MOYEN').length;
      const nbFaible = this.clauses.filter(c => c.niveauVigilance === 'FAIBLE').length;

      pdf.setFillColor(248, 249, 252);
      pdf.roundedRect(margin, y, contentWidth, 28, 3, 3, 'F');

      pdf.setTextColor(26, 46, 90);
      pdf.setFontSize(10);
      pdf.setFont('helvetica', 'bold');
      pdf.text('Synthese', margin + 6, y + 8);

      pdf.setFont('helvetica', 'normal');
      pdf.setFontSize(9);
      pdf.setTextColor(80, 80, 80);
      pdf.text(`${this.clauses.length} clauses analysees`, margin + 6, y + 16);

      // Badges de stats
      const statsX = margin + 6;
      const statsY = y + 22;
      pdf.setFontSize(8);

      // Élevé
      const col = colors['ELEVE'];
      pdf.setFillColor(col.r, col.g, col.b);
      pdf.roundedRect(statsX, statsY - 3.5, 4, 4, 1, 1, 'F');
      pdf.setTextColor(col.r, col.g, col.b);
      pdf.text(`${nbEleve} Eleve`, statsX + 6, statsY);

      // Moyen
      const col2 = colors['MOYEN'];
      pdf.setFillColor(col2.r, col2.g, col2.b);
      pdf.roundedRect(statsX + 30, statsY - 3.5, 4, 4, 1, 1, 'F');
      pdf.setTextColor(col2.r, col2.g, col2.b);
      pdf.text(`${nbMoyen} Moyen`, statsX + 36, statsY);

      // Faible
      const col3 = colors['FAIBLE'];
      pdf.setFillColor(col3.r, col3.g, col3.b);
      pdf.roundedRect(statsX + 64, statsY - 3.5, 4, 4, 1, 1, 'F');
      pdf.setTextColor(col3.r, col3.g, col3.b);
      pdf.text(`${nbFaible} Faible`, statsX + 70, statsY);

      y += 36;

      // ── CLAUSES
      for (const clause of this.clauses) {
        const titleLines = pdf.splitTextToSize(clause.titreClause || '', contentWidth - 12);
        const explLines = pdf.splitTextToSize(clause.texteClair || '', contentWidth - 12);
        const sourceLines = clause.sourceJuridique ? pdf.splitTextToSize(`Source : ${clause.sourceJuridique}`, contentWidth - 12) : [];
        const blockHeight = 12 + (titleLines.length * 5) + (explLines.length * 4.5) + (sourceLines.length * 4) + 10;

        checkNewPage(blockHeight);

        // Barre latérale colorée
        const color = colors[clause.niveauVigilance] || colors['MOYEN'];
        pdf.setFillColor(color.r, color.g, color.b);
        pdf.rect(margin, y, 3, blockHeight - 4, 'F');

        // Badge de vigilance
        pdf.setFillColor(color.r, color.g, color.b);
        pdf.roundedRect(margin + 6, y, 18, 5, 1.5, 1.5, 'F');
        pdf.setTextColor(255, 255, 255);
        pdf.setFontSize(7);
        pdf.setFont('helvetica', 'bold');
        pdf.text(this.getVigilanceLabel(clause.niveauVigilance), margin + 7, y + 3.7);

        y += 8;

        // Titre
        pdf.setTextColor(26, 46, 90);
        pdf.setFontSize(11);
        pdf.setFont('helvetica', 'bold');
        pdf.text(titleLines, margin + 6, y);
        y += titleLines.length * 5 + 2;

        // Explication
        pdf.setTextColor(70, 70, 70);
        pdf.setFontSize(9);
        pdf.setFont('helvetica', 'normal');
        pdf.text(explLines, margin + 6, y);
        y += explLines.length * 4.5;

        // Source juridique
        if (sourceLines.length > 0) {
          y += 2;
          pdf.setTextColor(120, 120, 120);
          pdf.setFontSize(8);
          pdf.setFont('helvetica', 'italic');
          pdf.text(sourceLines, margin + 6, y);
          y += sourceLines.length * 4;
        }

        y += 8;
      }

      // ── PIED DE PAGE sur chaque page
      const totalPages = pdf.getNumberOfPages();
      for (let i = 1; i <= totalPages; i++) {
        pdf.setPage(i);
        pdf.setFontSize(8);
        pdf.setTextColor(170, 170, 170);
        pdf.setFont('helvetica', 'normal');
        pdf.text(`Legalia — Rapport genere automatiquement`, margin, pageHeight - 10);
        pdf.text(`Page ${i}/${totalPages}`, pageWidth - margin - 20, pageHeight - 10);
      }

      // ── TÉLÉCHARGEMENT
      const fileName = this.document.nomFichier.replace(/\.pdf$/i, '');
      pdf.save(`Legalia_Rapport_${fileName}.pdf`);

    } catch (err) {
      console.error('Erreur export PDF:', err);
    } finally {
      this.exporting = false;
    }
  }
}
