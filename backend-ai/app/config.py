"""
Configuration de l'application — chargement des variables d'environnement.
"""

from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    gemini_api_key: str
    spring_backend_url: str = "http://localhost:8080"
    port: int = 8000

    # Chemin vers la base de connaissances juridiques
    knowledge_base_path: str = "data/code_assurances.txt"

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    """Retourne l'instance unique des paramètres (singleton via cache)."""
    return Settings()
