"""
Routes FastAPI pour l'analyse de contrats d'assurance.
"""

import asyncio
import logging
from fastapi import APIRouter, UploadFile, File, HTTPException

from app.models.schemas import AnalyzeResponse, ClauseResult, HealthResponse, NiveauVigilance, ProgressionResponse
from app.services import pdf_service, ai_service, rag_service

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api")

# Suivi de progression par document : { filename: { "total": N, "done": K } }
# Alimenté pendant l'analyse, nettoyé à la fin
_progressions: dict[str, dict] = {}


@router.post("/analyze", response_model=AnalyzeResponse)
async def analyze_contract(
    file: UploadFile = File(..., description="Fichier PDF du contrat d'assurance"),
):
    """
    Analyse un contrat d'assurance PDF.

    Étapes :
    1. Extraction du texte depuis le PDF.
    2. Découpage en clauses.
    3. Analyse de chaque clause via RAG + Gemini.
    4. Retour des résultats structurés et vulgarisés.
    """
    # Validation du type de fichier
    if not file.filename or not file.filename.lower().endswith(".pdf"):
        raise HTTPException(
            status_code=400,
            detail="Le fichier doit être au format PDF."
        )

    logger.info(f"Début d'analyse du document : {file.filename}")

    # Lecture du fichier en bytes
    try:
        pdf_bytes = await file.read()
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Impossible de lire le fichier : {e}")

    if len(pdf_bytes) == 0:
        raise HTTPException(status_code=400, detail="Le fichier PDF est vide.")

    # Étape 1 : Extraction du texte
    try:
        texte = pdf_service.extract_text(pdf_bytes)
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))

    # Étape 2 : Découpage en clauses
    clauses = pdf_service.extract_clauses(texte)

    if not clauses:
        raise HTTPException(
            status_code=422,
            detail="Aucune clause détectable dans ce document."
        )

    logger.info(f"{len(clauses)} clauses à analyser dans '{file.filename}'")

    # Initialisation de la progression pour ce document
    _progressions[file.filename] = {"total": len(clauses), "done": 0}

    def on_clause_done():
        """Incrémente le compteur après chaque clause analysée."""
        if file.filename in _progressions:
            _progressions[file.filename]["done"] += 1

    # Étape 3 : Analyse IA dans un thread pour ne pas bloquer l'event loop
    # (permet de servir les requêtes /progress pendant l'analyse)
    try:
        resultats_bruts = await asyncio.to_thread(
            ai_service.analyze_contract, clauses, on_clause_done
        )
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))
    finally:
        # Nettoyage de la progression une fois l'analyse terminée
        _progressions.pop(file.filename, None)

    # Étape 4 : Construction de la réponse typée
    clauses_resultat = [
        ClauseResult(
            titre_clause=r["titre_clause"],
            texte_original=r["texte_original"],
            texte_clair=r["texte_clair"],
            niveau_vigilance=NiveauVigilance(r["niveau_vigilance"]),
            source_juridique=r.get("source_juridique"),
        )
        for r in resultats_bruts
    ]

    logger.info(f"Analyse terminée pour '{file.filename}' — {len(clauses_resultat)} clauses")

    return AnalyzeResponse(
        document_name=file.filename,
        nb_clauses=len(clauses_resultat),
        clauses=clauses_resultat,
    )


@router.get("/analyze/progress/{filename}", response_model=ProgressionResponse)
async def get_progression(filename: str):
    """
    Retourne la progression en temps réel de l'analyse d'un document.
    À interroger par polling pendant que POST /api/analyze tourne.
    """
    if filename not in _progressions:
        raise HTTPException(
            status_code=404,
            detail="Aucune analyse en cours pour ce document."
        )
    prog = _progressions[filename]
    total = prog["total"]
    done = prog["done"]
    progression = round((done / total) * 100) if total > 0 else 0
    return ProgressionResponse(total_clauses=total, clauses_analysees=done, progression=progression)


@router.get("/health", response_model=HealthResponse)
async def health_check():
    """Vérifie l'état du service et de la base de connaissances."""
    return HealthResponse(
        status="ok",
        service="Legalia AI Service",
        knowledge_base_loaded=rag_service.is_knowledge_base_loaded(),
    )
