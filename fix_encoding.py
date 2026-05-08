#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script pour corriger l'encodage UTF-8 des fichiers Java
"""

import os
import sys

# Liste des fichiers à corriger
files = [
    "src/main/java/esprit/fx/controllers/DashboardStatsControllerArij.java",
    "src/main/java/esprit/fx/controllers/DisponibiliteController.java",
    "src/main/java/esprit/fx/controllers/QuizSanteController.java",
    "src/main/java/esprit/fx/controllers/QuizSanteControllerArij.java",
    "src/main/java/esprit/fx/services/ArticleService.java",
    "src/main/java/esprit/fx/services/ServiceDisponibilite.java",
    "src/main/java/esprit/fx/services/ServiceRendezVous.java"
]

# Table de correspondance des caractères mal encodés
replacements = {
    # Caractères accentués
    'Ã©': 'é',
    'Ã¨': 'è',
    'Ãª': 'ê',
    'Ã§': 'ç',
    'Ã ': 'à',
    'Ã´': 'ô',
    'Ã®': 'î',
    'Ã¹': 'ù',
    'Ã»': 'û',
    'Ã¯': 'ï',
    'Ã«': 'ë',
    
    # Majuscules accentuées
    'Ã‰': 'É',
    'Ãˆ': 'È',
    'ÃŠ': 'Ê',
    'Ã‡': 'Ç',
    'Ã€': 'À',
    'Ã"': 'Ô',
    'ÃŽ': 'Î',
    'Ã™': 'Ù',
    'Ã›': 'Û',
    
    # Symboles
    'â†'': '→',
    'âœ"': '✓',
    'âœ—': '✗',
    'âš ': '⚠',
    'â€¢': '•',
    'aEUR¢': '•',
    'Â': '',
    
    # Patterns complexes
    'Ã(c)': 'é',
    'dÃ©': 'dé',
    'reÃ§': 'reç',
}

def fix_file(filepath):
    """Corrige l'encodage d'un fichier"""
    try:
        # Lire le fichier
        with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
        
        # Appliquer les remplacements
        for old, new in replacements.items():
            content = content.replace(old, new)
        
        # Sauvegarder avec UTF-8 sans BOM
        with open(filepath, 'w', encoding='utf-8', newline='\n') as f:
            f.write(content)
        
        print(f"✓ {filepath} corrigé")
        return True
    except Exception as e:
        print(f"✗ Erreur avec {filepath}: {e}")
        return False

def main():
    print("Correction de l'encodage des fichiers Java...\n")
    
    success_count = 0
    for filepath in files:
        if os.path.exists(filepath):
            if fix_file(filepath):
                success_count += 1
        else:
            print(f"✗ Fichier introuvable: {filepath}")
    
    print(f"\n✓ Correction terminée! {success_count}/{len(files)} fichiers corrigés")

if __name__ == "__main__":
    main()
