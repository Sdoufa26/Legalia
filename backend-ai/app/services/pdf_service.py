"""
Service d'extraction et de découpage de contrats PDF.
Utilise PyMuPDF (fitz) pour l'extraction du texte.
"""

import re
import logging
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


def extract_clauses(text: str) -> list[dict]:
    """
    Découpe un texte contractuel en clauses individuelles.

    Détecte les séparateurs courants :
    - "Article X" / "ARTICLE X"
    - "Section X" / "SECTION X"
    - Numérotation : "1.", "1.1", "I.", "A."
    - Titres en majuscules seuls sur une ligne

    Args:
        text: Texte brut du contrat.

    Returns:
        Liste de dicts {"titre": str, "contenu": str}.
    """
    # Patterns de détection de début de clause (sans flag (?m) inline)
    patterns = [
        r"^(Article\s+\d+[\w\-]*[^\n]*)",           # Article 1, Article L113-1
        r"^(ARTICLE\s+\d+[\w\-]*[^\n]*)",
        r"^(Section\s+\d+[\w\-]*[^\n]*)",            # Section 1
        r"^(SECTION\s+\d+[\w\-]*[^\n]*)",
        r"^(\d+\.\d+[\s\t]+[A-ZÀÂÉÈÊËÎÏÔÙÛÜÇ][^\n]{3,})",  # 1.1 Titre
        r"^(\d+\.\s+[A-ZÀÂÉÈÊËÎÏÔÙÛÜÇ][^\n]{3,})",          # 1. Titre
        r"^([IVX]+\.\s+[A-ZÀÂÉÈÊËÎÏÔÙÛÜÇ][^\n]{3,})",       # I. Titre
        r"^([A-Z]{3,}(?:\s+[A-Z]{2,})*\s*$)",                # TITRE EN MAJUSCULES
    ]

    pattern_global = "|".join(f"(?:{p})" for p in patterns)

    # Trouver toutes les positions des titres de clauses (re.MULTILINE passé en flag)
    titres_trouves = list(re.finditer(pattern_global, text, re.MULTILINE))

    if not titres_trouves:
        logger.warning("Aucun pattern de clause détecté — retour du texte entier comme clause unique")
        return [{"titre": "Contrat complet", "contenu": text.strip()}]

    clauses = []
    for i, match in enumerate(titres_trouves):
        titre = match.group(0).strip()
        debut_contenu = match.end()

        # Le contenu va jusqu'au prochain titre (ou fin du texte)
        if i + 1 < len(titres_trouves):
            fin_contenu = titres_trouves[i + 1].start()
        else:
            fin_contenu = len(text)

        contenu = text[debut_contenu:fin_contenu].strip()

        # Ignorer les clauses sans contenu substantiel (< 20 caractères)
        if len(contenu) < 20:
            continue

        clauses.append({"titre": titre, "contenu": contenu})

    logger.info(f"{len(clauses)} clauses extraites du document")
    return clauses
