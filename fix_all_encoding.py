#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script pour corriger l'encodage UTF-8 de TOUS les fichiers Java du projet
"""

import os
import glob

# Table de correspondance complète des caractères mal encodés
replacements = {
    # Caractères accentués minuscules
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
    
    # Symboles et caractères spéciaux
    'â†'': '→',
    'âœ"': '✓',
    'âœ—': '✗',
    'âš ': '⚠',
    'â€¢': '•',
    'aEUR¢': '•',
    '®': '',
    'œ': 'œ',
    'Â': '',
    
    # Patterns spécifiques trouvés dans le code
    'Ã(c)': 'é',
    'dÃ©': 'dé',
    'reÃ§': 'reç',
    'Ã©t': 'ét',
    'Ã©n': 'én',
    'Ã©r': 'ér',
    'Ã©l': 'él',
    'Ã©m': 'ém',
    'Ã©v': 'év',
    'Ã©c': 'éc',
    'Ã©d': 'éd',
    'Ã©p': 'ép',
    'Ã©s': 'és',
    'Ã©g': 'ég',
    
    # Caractères spéciaux Unicode mal encodés
    'Ô×ò': '🆕',
    'Ô£Å': '✏️',
    'ÔÜá': '⚠️',
    'Ô£à': '🔍',
    'ÔÇö': '—',
    '├®': 'é',
    '├á': 'à',
    '├¿': 'è',
    '├¬': 'ê',
    '├╗': 'û',
    '├®': 'é',
    
    # Patterns complexes
    'publiÃ©': 'publié',
    'dÃ©but': 'début',
    'reÃ§ue': 'reçue',
    'Ã©tÃ©': 'été',
    'crÃ©Ã©': 'créé',
    'modifiÃ©': 'modifié',
    'supprimÃ©': 'supprimé',
    'gÃ©nÃ©rÃ©': 'généré',
    'dÃ©tectÃ©': 'détecté',
    'rÃ©cupÃ©rer': 'récupérer',
    'mÃ©decin': 'médecin',
    'MÃ©decin': 'Médecin',
    'disponibilitÃ©': 'disponibilité',
    'DisponibilitÃ©': 'Disponibilité',
    'RequÃªte': 'Requête',
    'Ã©trangÃ¨re': 'étrangère',
    'Ãªtre': 'être',
    'supÃ©rieure': 'supérieure',
    'ajoutÃ©e': 'ajoutée',
    'modifiÃ©e': 'modifiée',
    'supprimÃ©e': 'supprimée',
    'liÃ©e': 'liée',
    'liÃ©s': 'liés',
    'utilisÃ©e': 'utilisée',
    'Ã©lÃ©ments': 'éléments',
    'S├®lectionner': 'Sélectionner',
    'caract├¿res': 'caractères',
    'suppl├®mentaires': 'supplémentaires',
    'succ├¿s': 'succès',
    'trouv├®e': 'trouvée',
}

def fix_file(filepath):
    """Corrige l'encodage d'un fichier"""
    try:
        # Lire le fichier
        with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
        
        original_content = content
        
        # Appliquer les remplacements
        for old, new in replacements.items():
            content = content.replace(old, new)
        
        # Seulement sauvegarder si des changements ont été faits
        if content != original_content:
            # Sauvegarder avec UTF-8 sans BOM
            with open(filepath, 'w', encoding='utf-8', newline='\n') as f:
                f.write(content)
            
            print(f"✓ {filepath}")
            return True
        else:
            return False
    except Exception as e:
        print(f"✗ Erreur avec {filepath}: {e}")
        return False

def main():
    print("=" * 70)
    print("Correction de l'encodage de TOUS les fichiers Java du projet")
    print("=" * 70)
    print()
    
    # Trouver tous les fichiers Java
    java_files = []
    for root, dirs, files in os.walk('src'):
        for file in files:
            if file.endswith('.java'):
                java_files.append(os.path.join(root, file))
    
    print(f"📁 {len(java_files)} fichiers Java trouvés\n")
    
    success_count = 0
    for filepath in sorted(java_files):
        if fix_file(filepath):
            success_count += 1
    
    print()
    print("=" * 70)
    print(f"✓ Correction terminée!")
    print(f"📊 {success_count} fichiers corrigés sur {len(java_files)} fichiers")
    print("=" * 70)

if __name__ == "__main__":
    main()
