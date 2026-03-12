"""
Service IA — Appel à l'API Gemini pour la vulgarisation des clauses.
Utilise le modèle gemini-2.0-flash pour des réponses rapides.
"""

import json
import logging
import re
import time
from google import genai
from google.genai import types

from app.config import get_settings
from app.services import rag_service

logger = logging.getLogger(__name__)

# Client Gemini (instance unique)
_client: genai.Client | None = None


def _get_client() -> genai.Client:
    """Initialise et retourne le client Gemini (singleton)."""
    global _client
    if _client is None:
        settings = get_settings()
        _client = genai.Client(api_key=settings.gemini_api_key)
        logger.info("Client Gemini initialisé (modèle : gemini-2.0-flash)")
    return _client


def _construire_prompt(
    clause_titre: str,
    clause_contenu: str,
    articles_pertinents: list[str],
) -> str:
    """
    Construit le prompt envoyé à Gemini pour l'analyse d'une clause.

    Args:
        clause_titre: Titre de la clause à analyser.
        clause_contenu: Texte brut de la clause.
        articles_pertinents: Extraits de loi issus du RAG.

    Returns:
        Prompt formaté en français.
    """
    articles_formates = "\n\n---\n\n".join(articles_pertinents) if articles_pertinents else "Aucun article pertinent trouvé."

    prompt = f"""Tu es un expert juridique spécialisé dans les contrats d'assurance français.
Ta mission est d'analyser une clause contractuelle et de la vulgariser pour un non-juriste.

IMPORTANT : Réponds UNIQUEMENT en te basant sur les articles de loi fournis ci-dessous.
Ne pas inventer de références juridiques. Si aucune loi ne s'applique directement, indique "Non spécifié".

=== ARTICLES DE LOI PERTINENTS (Code des Assurances) ===
{articles_formates}

=== CLAUSE À ANALYSER ===
Titre : {clause_titre}
Contenu :
{clause_contenu}

=== INSTRUCTIONS ===
Analyse cette clause et réponds UNIQUEMENT au format JSON suivant, sans aucun texte avant ou après :

{{
  "texte_clair": "Explication en langage simple et accessible, compréhensible par un particulier non-juriste (2-4 phrases).",
  "niveau_vigilance": "FAIBLE ou MOYEN ou ELEVE",
  "source_juridique": "Référence exacte à l'article de loi cité dans le contexte, ou 'Non spécifié'",
  "justification_vigilance": "Courte explication du niveau de vigilance choisi (1 phrase)."
}}

Critères du niveau de vigilance :
- FAIBLE : Clause standard, sans risque particulier pour l'assuré.
- MOYEN : Clause contenant des conditions ou restrictions à lire attentivement.
- ELEVE : Clause potentiellement défavorable, limitation importante ou exclusion de garantie.
"""
    return prompt


def analyze_clause(
    clause_titre: str,
    clause_contenu: str,
    articles_pertinents: list[str],
) -> dict:
    """
    Analyse une clause contractuelle via Gemini et retourne un résultat structuré.

    Args:
        clause_titre: Titre de la clause.
        clause_contenu: Texte original de la clause.
        articles_pertinents: Extraits de loi pour le contexte RAG.

    Returns:
        Dict avec clés : titre_clause, texte_original, texte_clair,
                        niveau_vigilance, source_juridique.
    """
    client = _get_client()
    prompt = _construire_prompt(clause_titre, clause_contenu, articles_pertinents)

    logger.debug(f"Envoi de la clause '{clause_titre}' à Gemini")

    # Retry avec backoff exponentiel en cas de rate limit (429)
    delais_retry = [5, 15, 30]
    for tentative, delai in enumerate(delais_retry + [None], start=1):
        try:
            reponse = client.models.generate_content(
                model="gemini-2.0-flash",
                contents=prompt,
            )
            texte_reponse = reponse.text.strip()

            # Nettoyage des balises markdown éventuelles (```json ... ```)
            texte_reponse = re.sub(r"^```(?:json)?\s*", "", texte_reponse)
            texte_reponse = re.sub(r"\s*```$", "", texte_reponse)

            donnees = json.loads(texte_reponse)

            # Validation et valeurs par défaut
            niveau = donnees.get("niveau_vigilance", "MOYEN").upper()
            if niveau not in ("FAIBLE", "MOYEN", "ELEVE"):
                niveau = "MOYEN"

            return {
                "titre_clause": clause_titre,
                "texte_original": clause_contenu,
                "texte_clair": donnees.get("texte_clair", "Analyse non disponible."),
                "niveau_vigilance": niveau,
                "source_juridique": donnees.get("source_juridique", "Non spécifié"),
            }

        except json.JSONDecodeError as e:
            logger.error(f"Réponse Gemini non parseable pour '{clause_titre}' : {e}")
            return {
                "titre_clause": clause_titre,
                "texte_original": clause_contenu,
                "texte_clair": "L'analyse automatique a échoué. Veuillez consulter un juriste.",
                "niveau_vigilance": "MOYEN",
                "source_juridique": "Non spécifié",
            }

        except Exception as e:
            if "429" in str(e) and delai is not None:
                logger.warning(f"Rate limit Gemini (tentative {tentative}/3) — attente {delai}s avant retry...")
                time.sleep(delai)
                continue
            logger.error(f"Erreur Gemini pour '{clause_titre}' : {e}")
            raise RuntimeError(f"Erreur lors de l'appel à Gemini : {e}")


def analyze_contract(clauses: list[dict]) -> list[dict]:
    """
    Analyse toutes les clauses d'un contrat.

    Pour chaque clause :
    1. Recherche les articles de loi pertinents via le RAG.
    2. Envoie la clause à Gemini pour vulgarisation.

    Args:
        clauses: Liste de dicts {"titre": str, "contenu": str}.

    Returns:
        Liste de résultats d'analyse structurés.
    """
    resultats = []
    total = len(clauses)

    for i, clause in enumerate(clauses, start=1):
        titre = clause.get("titre", f"Clause {i}")
        contenu = clause.get("contenu", "")

        logger.info(f"Analyse clause {i}/{total} : {titre[:60]}...")

        # Étape RAG : récupération des articles de loi pertinents
        query_rag = f"{titre} {contenu[:200]}"
        articles_pertinents = rag_service.search_relevant_chunks(query_rag, top_k=3)

        # Étape IA : vulgarisation par Gemini
        resultat = analyze_clause(titre, contenu, articles_pertinents)
        resultats.append(resultat)

        # Pause entre les appels pour respecter le rate limit Gemini (tier gratuit ~15 req/min)
        if i < total:
            time.sleep(4)

    logger.info(f"Analyse complète : {len(resultats)}/{total} clauses traitées")
    return resultats
