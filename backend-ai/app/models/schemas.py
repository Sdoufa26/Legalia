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
