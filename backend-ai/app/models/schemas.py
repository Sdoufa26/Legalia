"""
Modèles Pydantic pour les requêtes et réponses de l'API.
"""

from pydantic import BaseModel
from typing import Optional
from enum import Enum


class NiveauVigilance(str, Enum):
    """Niveau d'attention requis pour une clause."""
    FAIBLE = "FAIBLE"
    MOYEN = "MOYEN"
    ELEVE = "ELEVE"


class ClauseResult(BaseModel):
    """Résultat de l'analyse d'une clause contractuelle."""
    titre_clause: str
    texte_original: str
    texte_clair: str
    niveau_vigilance: NiveauVigilance
    source_juridique: Optional[str] = None


class AnalyzeResponse(BaseModel):
    """Réponse complète de l'analyse d'un contrat."""
    document_name: str
    nb_clauses: int
    clauses: list[ClauseResult]


class HealthResponse(BaseModel):
    """Réponse du health check."""
    status: str
    service: str
    knowledge_base_loaded: bool


class ProgressionResponse(BaseModel):
    """Progression en temps réel de l'analyse d'un document.
    Les noms de champs correspondent aux @JsonProperty du DTO Spring (AnalysisProgressResponse).
    """
    total_clauses: int
    clauses_analysees: int
    progression: int  # pourcentage 0-100
