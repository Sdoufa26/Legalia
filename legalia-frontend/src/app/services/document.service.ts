import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// ── Interfaces correspondant aux DTOs du back-end ──

export interface DocumentResponse {
  id: string;
  nomFichier: string;
  taille: number;          // en Ko
  dateTeleversement: string;
  categorie: string;       // ASSURANCE, IMMO, TRAVAIL
  statut: string;          // EN_COURS, TERMINE, ERREUR
}

export interface CarteResultatResponse {
  id: string;
  titreClause: string;
  texteOriginal: string;
  texteClair: string;
  niveauVigilance: string; // FAIBLE, MOYEN, ELEVE
  sourceJuridique: string;
}

export interface DocumentUploadResponse {
  idDocument: string;
  nomFichier: string;
  taille: number;
  dateTeleversement: string;
  categorie: string;
  statut: string;
  message: string;
  clauses: CarteResultatResponse[];
}

@Injectable({ providedIn: 'root' })
export class DocumentService {

  private readonly API = 'http://localhost:8080/api/documents';

  constructor(private http: HttpClient) {}

  /** Upload un fichier PDF (multipart/form-data) */
  uploadDocument(file: File): Observable<DocumentUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<DocumentUploadResponse>(`${this.API}/upload`, formData);
  }

  /** Récupère la liste de tous les documents de l'utilisateur connecté */
  getDocuments(): Observable<DocumentResponse[]> {
    return this.http.get<DocumentResponse[]>(this.API);
  }

  /** Récupère les détails d'un document par son ID */
  getDocument(id: string): Observable<DocumentResponse> {
    return this.http.get<DocumentResponse>(`${this.API}/${id}`);
  }

  /** Récupère les résultats d'analyse (cartes de résultat) d'un document */
  getDocumentResults(id: string): Observable<CarteResultatResponse[]> {
    return this.http.get<CarteResultatResponse[]>(`${this.API}/${id}/results`);
  }
}
