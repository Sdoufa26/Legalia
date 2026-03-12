"""
Service RAG (Retrieval-Augmented Generation).
Gère la vectorisation de la base de connaissances juridiques
et la recherche de chunks pertinents par similarité cosinus.
"""

import logging
import numpy as np
from sentence_transformers import SentenceTransformer

logger = logging.getLogger(__name__)

# Modèle léger multilingue, adapté au français
MODELE_EMBEDDING = "all-MiniLM-L6-v2"

# Stockage en mémoire des chunks vectorisés
_chunks: list[dict] = []  # [{"texte": str, "vecteur": np.ndarray}]
_modele: SentenceTransformer | None = None


def _get_modele() -> SentenceTransformer:
    """Charge le modèle d'embedding (lazy loading, instance unique)."""
    global _modele
    if _modele is None:
        logger.info(f"Chargement du modèle d'embedding : {MODELE_EMBEDDING}")
        _modele = SentenceTransformer(MODELE_EMBEDDING)
        logger.info("Modèle d'embedding chargé")
    return _modele


def _decouper_en_chunks(texte: str, taille: int = 500, chevauchement: int = 100) -> list[str]:
    """
    Découpe un texte en chunks de taille fixe avec chevauchement.

    Args:
        texte: Texte à découper.
        taille: Nombre de caractères par chunk.
        chevauchement: Nombre de caractères partagés entre chunks consécutifs.

    Returns:
        Liste de chunks textuels.
    """
    chunks = []
    debut = 0
    while debut < len(texte):
        fin = debut + taille
        chunk = texte[debut:fin].strip()
        if chunk:
            chunks.append(chunk)
        debut += taille - chevauchement

    return chunks


def index_knowledge_base(filepath: str) -> None:
    """
    Charge, découpe et vectorise la base de connaissances juridiques.
    Les vecteurs sont stockés en mémoire dans _chunks.

    Args:
        filepath: Chemin vers le fichier texte de la base de connaissances.

    Raises:
        FileNotFoundError: Si le fichier n'existe pas.
        RuntimeError: Si la vectorisation échoue.
    """
    global _chunks

    try:
        with open(filepath, "r", encoding="utf-8") as f:
            texte = f.read()
    except FileNotFoundError:
        raise FileNotFoundError(f"Base de connaissances introuvable : {filepath}")

    logger.info(f"Base de connaissances chargée : {len(texte)} caractères")

    # Découpage en chunks
    chunks_texte = _decouper_en_chunks(texte, taille=500, chevauchement=100)
    logger.info(f"{len(chunks_texte)} chunks générés")

    # Vectorisation en batch
    modele = _get_modele()
    vecteurs = modele.encode(chunks_texte, show_progress_bar=False, convert_to_numpy=True)

    _chunks = [
        {"texte": chunk, "vecteur": vecteur}
        for chunk, vecteur in zip(chunks_texte, vecteurs)
    ]

    logger.info(f"Base de connaissances indexée : {len(_chunks)} chunks vectorisés")


def search_relevant_chunks(query: str, top_k: int = 3) -> list[str]:
    """
    Recherche les chunks les plus pertinents pour une requête donnée
    via similarité cosinus.

    Args:
        query: Texte de la requête (clause à analyser).
        top_k: Nombre de chunks à retourner.

    Returns:
        Liste des top_k chunks les plus similaires.
    """
    if not _chunks:
        logger.warning("Base de connaissances vide — aucun chunk disponible")
        return []

    modele = _get_modele()
    vecteur_query = modele.encode(query, convert_to_numpy=True)

    # Calcul de la similarité cosinus pour tous les chunks
    scores = []
    for chunk in _chunks:
        vecteur_chunk = chunk["vecteur"]
        # Similarité cosinus = produit scalaire / (norme A * norme B)
        norme_query = np.linalg.norm(vecteur_query)
        norme_chunk = np.linalg.norm(vecteur_chunk)

        if norme_query == 0 or norme_chunk == 0:
            scores.append(0.0)
        else:
            score = float(np.dot(vecteur_query, vecteur_chunk) / (norme_query * norme_chunk))
            scores.append(score)

    # Tri par score décroissant
    indices_tries = sorted(range(len(scores)), key=lambda i: scores[i], reverse=True)
    top_indices = indices_tries[:top_k]

    resultats = [_chunks[i]["texte"] for i in top_indices]
    logger.debug(f"RAG : top {top_k} chunks trouvés (scores : {[round(scores[i], 3) for i in top_indices]})")

    return resultats


def is_knowledge_base_loaded() -> bool:
    """Indique si la base de connaissances a été chargée."""
    return len(_chunks) > 0
