"""
Service d'extraction et de découpage de contrats PDF.
Utilise PyMuPDF (fitz) pour l'extraction du texte.
"""

import re
import logging
from collections import Counter
import fitz  # PyMuPDF

logger = logging.getLogger(__name__)


def extract_text(pdf_bytes: bytes) -> str:
    """
    Extrait tout le texte d'un fichier PDF fourni en bytes.

    Args:
        pdf_bytes: Contenu binaire du fichier PDF.

    Returns:
        Texte complet extrait du PDF.

    Raises:
        ValueError: Si le PDF est illisible ou vide.
    """
    try:
        doc = fitz.open(stream=pdf_bytes, filetype="pdf")
        texte_complet = []

        for num_page, page in enumerate(doc, start=1):
            texte_page = page.get_text("text")
            if texte_page.strip():
                texte_complet.append(texte_page)
            logger.debug(f"Page {num_page} extraite ({len(texte_page)} caractères)")

        doc.close()
        texte = "\n".join(texte_complet)

        if not texte.strip():
            raise ValueError("Le PDF ne contient pas de texte extractible (PDF scanné ?).")

        logger.info(f"Extraction PDF réussie : {len(texte)} caractères sur {len(texte_complet)} pages")
        return texte

    except fitz.FileDataError as e:
        raise ValueError(f"Fichier PDF invalide ou corrompu : {e}")


def _est_sommaire(contenu: str) -> bool:
    """
    Détecte si un bloc de texte est un sommaire ou une table des matières.
    Critère : plus de 30% des lignes contiennent des points de suspension
    ou des numéros de page isolés en fin de ligne.
    """
    lignes = [l for l in contenu.split("\n") if l.strip()]
    if len(lignes) < 2:
        return False
    lignes_toc = sum(
        1 for l in lignes
        if re.search(r"\.{3,}", l) or re.search(r"\s{2,}\d{1,3}\s*$", l)
    )
    return (lignes_toc / len(lignes)) > 0.3


def _est_boilerplate_juridique(contenu: str) -> bool:
    """
    Détecte les clauses contenant uniquement des informations légales
    d'identification de l'assureur (RCS, ORIAS, ACPR, siège social)
    sans contenu contractuel utile.
    """
    patterns_boilerplate = [
        r"\bRCS\b", r"\bORIAS\b", r"\bACPR\b",
        r"siège social", r"capital social",
        r"société anonyme", r"\bS\.A\.\b",
        r"immatriculée?", r"registre du commerce",
        r"autorité de contrôle", r"numéro de TVA",
    ]
    correspondances = sum(
        1 for p in patterns_boilerplate
        if re.search(p, contenu, re.IGNORECASE)
    )
    # Boilerplate si au moins 3 marqueurs présents dans un contenu court
    return correspondances >= 3 and len(contenu) < 600


def _extraire_prefixe_numerique(titre: str) -> str:
    """
    Extrait le préfixe numérique d'un titre pour détecter les sous-sections.
    Exemples : "1.2 Garanties" → "1.2", "Article 3" → "3".
    """
    match = re.match(
        r"^(?:Article\s+|Section\s+|ARTICLE\s+|SECTION\s+)?(\d+(?:\.\d+)*)",
        titre,
        re.IGNORECASE,
    )
    return match.group(1) if match else ""


def _fusionner_sous_clauses(clauses: list[dict]) -> list[dict]:
    """
    Fusionne les sous-clauses consécutives sous leur section parente.
    Exemple : "1. Garanties" + "1.1 Vol" + "1.2 Incendie" → clause unique "1. Garanties".
    """
    if not clauses:
        return clauses

    fusionnees = []
    clause_courante = clauses[0].copy()

    for clause in clauses[1:]:
        prefixe_courant = _extraire_prefixe_numerique(clause_courante["titre"])
        prefixe_entrant = _extraire_prefixe_numerique(clause["titre"])

        # La clause entrante est une sous-section de la clause courante
        if (
            prefixe_courant
            and prefixe_entrant
            and prefixe_entrant.startswith(prefixe_courant + ".")
        ):
            logger.debug(
                f"Fusion de '{clause['titre']}' dans '{clause_courante['titre']}'"
            )
            clause_courante["contenu"] += (
                f"\n\n{clause['titre']}\n{clause['contenu']}"
            )
        else:
            fusionnees.append(clause_courante)
            clause_courante = clause.copy()

    fusionnees.append(clause_courante)
    return fusionnees


def extract_clauses(text: str) -> list[dict]:
    """
    Découpe un texte contractuel en clauses individuelles avec filtrage avancé.

    Étapes :
    1. Détection des titres de clause par patterns regex
    2. Filtrage des clauses trop courtes (< 50 caractères)
    3. Filtrage des sommaires et tables des matières
    4. Filtrage des footers répétés (blocs identiques > 2 fois)
    5. Filtrage des boilerplates légaux d'identification d'assureur
    6. Fusion des sous-clauses consécutives d'un même article
    7. Limitation à 25 clauses les plus substantielles

    Args:
        text: Texte brut du contrat.

    Returns:
        Liste de dicts {"titre": str, "contenu": str}, max 25 éléments.
    """
    # Patterns de détection de début de clause
    patterns = [
        r"^(Article\s+\d+[\w\-]*[^\n]*)",
        r"^(ARTICLE\s+\d+[\w\-]*[^\n]*)",
        r"^(Section\s+\d+[\w\-]*[^\n]*)",
        r"^(SECTION\s+\d+[\w\-]*[^\n]*)",
        r"^(\d+\.\d+[\s\t]+[A-ZÀÂÉÈÊËÎÏÔÙÛÜÇ][^\n]{3,})",  # 1.1 Titre
        r"^(\d+\.\s+[A-ZÀÂÉÈÊËÎÏÔÙÛÜÇ][^\n]{3,})",          # 1. Titre
        r"^([IVX]+\.\s+[A-ZÀÂÉÈÊËÎÏÔÙÛÜÇ][^\n]{3,})",       # I. Titre
        r"^([A-Z]{3,}(?:\s+[A-Z]{2,})*\s*$)",                # TITRE EN MAJUSCULES
    ]

    pattern_global = "|".join(f"(?:{p})" for p in patterns)
    titres_trouves = list(re.finditer(pattern_global, text, re.MULTILINE))

    if not titres_trouves:
        logger.warning("Aucun pattern de clause détecté — retour du texte entier comme clause unique")
        return [{"titre": "Contrat complet", "contenu": text.strip()}]

    # --- Étape 1 : extraction brute des clauses ---
    clauses_brutes = []
    for i, match in enumerate(titres_trouves):
        # Nettoyage du titre : suppression des retours à la ligne + troncature à 100 caractères
        titre = match.group(0).strip()
        titre = " ".join(titre.split())   # normalise les espaces et supprime les \n internes
        titre = titre[:100]               # tronque à 100 caractères maximum
        debut_contenu = match.end()
        fin_contenu = titres_trouves[i + 1].start() if i + 1 < len(titres_trouves) else len(text)
        contenu = text[debut_contenu:fin_contenu].strip()
        clauses_brutes.append({"titre": titre, "contenu": contenu})

    logger.debug(f"{len(clauses_brutes)} clauses brutes détectées")

    # --- Étape 2 : détection des footers répétés ---
    # Un footer est un contenu (normalisé, tronqué à 200 chars) apparaissant plus de 2 fois
    compteur_contenus = Counter(
        " ".join(c["contenu"].split())[:200] for c in clauses_brutes
    )
    footers = {contenu for contenu, nb in compteur_contenus.items() if nb > 2}
    if footers:
        logger.debug(f"{len(footers)} footer(s) répété(s) détecté(s)")

    # --- Étape 3 : filtrage ---
    clauses_filtrees = []
    for clause in clauses_brutes:
        contenu = clause["contenu"]
        contenu_normalise = " ".join(contenu.split())[:200]

        # Contenu trop court
        if len(contenu) < 50:
            logger.debug(f"Ignoré (trop court) : '{clause['titre']}'")
            continue

        # Sommaire ou table des matières
        if _est_sommaire(contenu):
            logger.debug(f"Ignoré (sommaire) : '{clause['titre']}'")
            continue

        # Footer répété
        if contenu_normalise in footers:
            logger.debug(f"Ignoré (footer répété) : '{clause['titre']}'")
            continue

        # Boilerplate d'identification de l'assureur
        if _est_boilerplate_juridique(contenu):
            logger.debug(f"Ignoré (boilerplate légal) : '{clause['titre']}'")
            continue

        clauses_filtrees.append(clause)

    logger.debug(f"{len(clauses_filtrees)} clauses après filtrage")

    # --- Étape 4 : fusion des sous-clauses ---
    clauses_fusionnees = _fusionner_sous_clauses(clauses_filtrees)
    logger.debug(f"{len(clauses_fusionnees)} clauses après fusion des sous-sections")

    # --- Étape 5 : limitation à 25 clauses les plus substantielles ---
    MAX_CLAUSES = 25
    if len(clauses_fusionnees) > MAX_CLAUSES:
        clauses_fusionnees = sorted(
            clauses_fusionnees,
            key=lambda c: len(c["contenu"]),
            reverse=True,
        )[:MAX_CLAUSES]
        # Rétablir l'ordre d'apparition original
        titres_ordonnes = [c["titre"] for c in clauses_filtrees]
        clauses_fusionnees = sorted(
            clauses_fusionnees,
            key=lambda c: titres_ordonnes.index(c["titre"]) if c["titre"] in titres_ordonnes else 0,
        )
        logger.info(f"Limitation à {MAX_CLAUSES} clauses les plus substantielles")

    logger.info(f"{len(clauses_fusionnees)} clauses extraites du document")
    return clauses_fusionnees
