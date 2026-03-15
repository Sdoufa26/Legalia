"""
Service IA — Appel à l'API Gemini pour la vulgarisation des clauses.
Système de fallback automatique : gemini-2.5-flash → gemini-2.0-flash.
"""

import json
import logging
import re
import time
from collections.abc import Callable
from google import genai
from google.genai import types

from app.config import get_settings
from app.services import rag_service

logger = logging.getLogger(__name__)

# Modèles Gemini : principal et fallback
MODELE_PRINCIPAL = "gemini-2.5-flash"
MODELE_FALLBACK = "gemini-2.0-flash"

# Client Gemini (instance unique)
_client: genai.Client | None = None

# Modèle actif pour l'analyse en cours (réinitialisé à chaque nouveau document)
_modele_actif: str = MODELE_PRINCIPAL


class FallbackRequis(Exception):
    """Levée quand le modèle principal est indisponible après 2 tentatives."""
    pass


def _get_client() -> genai.Client:
    """Initialise et retourne le client Gemini (singleton)."""
    global _client
    if _client is None:
        settings = get_settings()
        _client = genai.Client(api_key=settings.gemini_api_key)
        logger.info(f"Client Gemini initialisé (modèle principal : {MODELE_PRINCIPAL})")
    return _client


def get_modele_actif() -> str:
    """Retourne le modèle Gemini actuellement utilisé pour les analyses."""
    return _modele_actif


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
        Prompt formaté en français, orienté vulgarisation et accessibilité.
    """
    articles_formates = "\n\n---\n\n".join(articles_pertinents) if articles_pertinents else "Aucun article pertinent trouvé."

    prompt = f"""Tu es un expert juridique spécialisé en droit des assurances français. Tu travailles pour Legalia, une plateforme qui aide les particuliers à comprendre leurs contrats d'assurance.

CONTEXTE JURIDIQUE (Articles du Code des Assurances pertinents) :
{articles_formates}

CLAUSE DU CONTRAT À ANALYSER :
Titre : {clause_titre}
Contenu : {clause_contenu}

INSTRUCTIONS :
1. Explique cette clause en langage simple et accessible, comme si tu parlais à quelqu'un qui n'a aucune formation juridique. Utilise des phrases courtes et des exemples concrets du quotidien.
2. Évalue le niveau de vigilance selon les règles précises ci-dessous.
3. Cite la source juridique la plus spécifique parmi celles fournies dans le contexte.

RÈGLES PRÉCISES POUR LE NIVEAU DE VIGILANCE :
- FAIBLE : informations générales, descriptions du contrat, définitions, garanties sans restriction, clauses favorables à l'assuré
- MOYEN : garanties avec franchises standards, obligations classiques (délais de déclaration), clauses de renouvellement
- ELEVE : exclusions de garantie, délais de carence, déchéances, plafonds bas, clauses restrictives, franchises élevées, conditions qui peuvent piéger l'assuré

ATTENTION : ne mets pas ELEVE partout. Une clause qui décrit simplement ce qui est couvert sans restriction est FAIBLE. Réserve ELEVE aux clauses qui limitent, excluent ou imposent des contraintes à l'assuré.

EXEMPLES DE CALIBRAGE :
- 'Le contrat couvre les dommages causés par l'incendie sans franchise' → FAIBLE (garantie favorable)
- 'Le capital mobilier assuré est plafonné à 70 000 euros' → MOYEN (plafond à vérifier)
- 'La franchise applicable est de 150 euros par sinistre' → MOYEN (franchise standard)
- 'Sont exclus les dommages résultant d'un défaut d'entretien' → ELEVE (exclusion)
- 'Un délai de carence de 30 jours s'applique' → ELEVE (restriction importante)
- 'Le non-respect de ces délais peut entraîner la déchéance' → ELEVE (risque de perte de droit)
- 'Le contrat est résiliable à l'échéance avec préavis de 2 mois' → MOYEN (obligation classique)
- 'La prime annuelle est de 480 euros, payable mensuellement' → FAIBLE (information factuelle)

Sur un contrat typique, la répartition devrait être environ : 30% FAIBLE, 40% MOYEN, 30% ELEVE. Si tu mets plus de 50% en ELEVE, c'est que tu es trop sévère.

RÈGLES POUR LA SOURCE JURIDIQUE :
Pour la source juridique, choisis l'article le PLUS SPÉCIFIQUE parmi ceux fournis :
- Garanties et étendue de la couverture → Article L113-1
- Obligations de déclaration de l'assuré → Article L113-2
- Paiement de la prime → Article L113-3
- Fausse déclaration intentionnelle → Article L113-8
- Fausse déclaration non intentionnelle → Article L113-9
- Résiliation annuelle → Article L113-12
- Résiliation infra-annuelle → Article L113-12-2
- Franchise → Article L122-1
- Principe indemnitaire → Article L121-1
- Catastrophes naturelles → Article L125-1
- Prescription → Article L114-1
Ne cite PAS toujours L113-1 par défaut. Choisis l'article qui correspond VRAIMENT au sujet de la clause.

RÉPONDS UNIQUEMENT en JSON valide avec cette structure exacte, sans texte avant ou après :
{{
  "texte_clair": "Explication vulgarisée ici",
  "niveau_vigilance": "FAIBLE ou MOYEN ou ELEVE",
  "source_juridique": "Article L113-1 ou Non applicable"
}}

RÈGLES STRICTES :
- Base-toi UNIQUEMENT sur les articles de loi fournis dans le contexte
- Ne mentionne JAMAIS "cette clause" ou "ce paragraphe", parle directement du contenu
- Utilise le "vous" pour t'adresser à l'assuré
- Donne des exemples concrets : "Par exemple, si un dégât des eaux survient chez vous..."
"""
    return prompt


def analyze_clause(
    clause_titre: str,
    clause_contenu: str,
    articles_pertinents: list[str],
    modele: str,
) -> dict:
    """
    Analyse une clause contractuelle via Gemini et retourne un résultat structuré.

    Args:
        clause_titre: Titre de la clause.
        clause_contenu: Texte original de la clause.
        articles_pertinents: Extraits de loi pour le contexte RAG.
        modele: Identifiant du modèle Gemini à utiliser.

    Returns:
        Dict avec clés : titre_clause, texte_original, texte_clair,
                        niveau_vigilance, source_juridique.

    Raises:
        FallbackRequis: Si le modèle répond 503/429 après 2 tentatives.
        RuntimeError: Pour toute autre erreur Gemini.
    """
    client = _get_client()
    prompt = _construire_prompt(clause_titre, clause_contenu, articles_pertinents)

    logger.info(f"Analyse avec {modele} — clause '{clause_titre[:50]}'")

    # 2 tentatives sur le modèle courant avant de déclencher le fallback
    MAX_TENTATIVES = 2
    DELAI_RETRY = 3  # secondes entre les deux tentatives

    for tentative in range(1, MAX_TENTATIVES + 1):
        try:
            reponse = client.models.generate_content(
                model=modele,
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
                "source_juridique": donnees.get("source_juridique", "Non applicable"),
            }

        except json.JSONDecodeError as e:
            # Réponse non parseable : retour dégradé immédiat, pas de retry
            logger.error(f"Réponse Gemini non parseable pour '{clause_titre}' : {e}")
            return {
                "titre_clause": clause_titre,
                "texte_original": clause_contenu,
                "texte_clair": "L'analyse automatique a échoué. Veuillez consulter un juriste.",
                "niveau_vigilance": "MOYEN",
                "source_juridique": "Non applicable",
            }

        except Exception as e:
            erreur = str(e)
            est_indisponible = (
                "503" in erreur or "UNAVAILABLE" in erreur
                or "429" in erreur or "RESOURCE_EXHAUSTED" in erreur
            )

            if est_indisponible:
                if tentative < MAX_TENTATIVES:
                    logger.warning(
                        f"Modèle {modele} indisponible (tentative {tentative}/{MAX_TENTATIVES})"
                        f" — retry dans {DELAI_RETRY}s..."
                    )
                    time.sleep(DELAI_RETRY)
                    continue
                else:
                    # 2 tentatives épuisées → signaler le besoin de fallback
                    logger.warning(
                        f"{MAX_TENTATIVES} tentatives échouées sur {modele} — fallback requis"
                    )
                    raise FallbackRequis(
                        f"Modèle {modele} indisponible après {MAX_TENTATIVES} tentatives : {erreur}"
                    )

            # Erreur inattendue : remontée directe
            logger.error(f"Erreur Gemini pour '{clause_titre}' : {e}")
            raise RuntimeError(f"Erreur lors de l'appel à Gemini : {e}")


def analyze_contract(
    clauses: list[dict],
    on_clause_done: Callable | None = None,
) -> list[dict]:
    """
    Analyse toutes les clauses d'un contrat avec fallback automatique de modèle.

    Pour chaque clause :
    1. Recherche les articles de loi pertinents via le RAG.
    2. Envoie la clause à Gemini pour vulgarisation.
    3. En cas d'indisponibilité du modèle principal après 2 tentatives,
       bascule sur le modèle fallback pour toute la suite du document.

    Args:
        clauses: Liste de dicts {"titre": str, "contenu": str}.
        on_clause_done: Callback optionnel appelé après chaque clause analysée,
                        utilisé pour le suivi de progression en temps réel.

    Returns:
        Liste de résultats d'analyse structurés.
    """
    global _modele_actif

    # Réinitialisation au modèle principal à chaque nouveau document
    _modele_actif = MODELE_PRINCIPAL
    logger.info(f"Début d'analyse — modèle : {_modele_actif}")

    resultats = []
    total = len(clauses)

    for i, clause in enumerate(clauses, start=1):
        titre = clause.get("titre", f"Clause {i}")
        contenu = clause.get("contenu", "")

        logger.info(f"Analyse clause {i}/{total} : {titre[:60]}...")

        # Étape RAG : récupération des articles de loi pertinents
        query_rag = f"{titre} {contenu[:200]}"
        articles_pertinents = rag_service.search_relevant_chunks(query_rag, top_k=3)

        # Étape IA : vulgarisation par Gemini avec gestion du fallback
        try:
            resultat = analyze_clause(titre, contenu, articles_pertinents, _modele_actif)
        except FallbackRequis:
            # Bascule définitive sur le modèle fallback pour la suite du document
            _modele_actif = MODELE_FALLBACK
            logger.warning(f"Fallback sur {MODELE_FALLBACK} pour la suite du document")
            resultat = analyze_clause(titre, contenu, articles_pertinents, _modele_actif)

        resultats.append(resultat)

        # Notification de progression après chaque clause traitée
        if on_clause_done is not None:
            on_clause_done()

        # Pause entre les appels pour respecter le rate limit Gemini
        if i < total:
            time.sleep(2)

    logger.info(
        f"Analyse complète : {len(resultats)}/{total} clauses traitées"
        f" (modèle final : {_modele_actif})"
    )
    return resultats
