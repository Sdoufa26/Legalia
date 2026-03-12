"""
Point d'entrée principal de l'API FastAPI — Legalia AI Service.
Gère le démarrage de l'application et le chargement de la base de connaissances.
"""

import logging
from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import get_settings
from app.routes import analyze
from app.services import rag_service

# Configuration du logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Gestionnaire de cycle de vie de l'application.
    Charge la base de connaissances au démarrage.
    """
    settings = get_settings()

    # Chargement de la base de connaissances juridiques au démarrage
    logger.info("Démarrage du service Legalia AI...")
    try:
        rag_service.index_knowledge_base(settings.knowledge_base_path)
        logger.info("Base de connaissances juridiques chargée avec succès")
    except FileNotFoundError as e:
        logger.warning(f"Base de connaissances non trouvée : {e}. Le service démarre sans RAG.")
    except Exception as e:
        logger.error(f"Erreur lors du chargement de la base de connaissances : {e}")

    logger.info(f"Service Legalia AI prêt sur le port {settings.port}")
    yield

    # Nettoyage à l'arrêt (optionnel)
    logger.info("Arrêt du service Legalia AI")


# Initialisation de l'application FastAPI
app = FastAPI(
    title="Legalia AI Service",
    description="Service d'analyse et de vulgarisation de contrats d'assurance via IA.",
    version="1.0.0",
    lifespan=lifespan,
)

# Configuration CORS — autorise le backend Spring et le frontend Angular
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:8080",   # Backend Spring Boot
        "http://localhost:4200",   # Frontend Angular
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Enregistrement des routes
app.include_router(analyze.router)


if __name__ == "__main__":
    settings = get_settings()
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=settings.port,
        reload=True,
    )
