================================================================================
  CORRECTION DE L'ENCODAGE UTF-8 - TOUS LES FICHIERS JAVA
================================================================================

PROBLÈME :
----------
Les fichiers Java contiennent des caractères mal encodés :
  - "Ã©" au lieu de "é"
  - "Ã¨" au lieu de "è"
  - "├®" au lieu de "é"
  - "âœ"" au lieu de "✓"
  - etc.

Cela affecte TOUS les fichiers du projet (vos fichiers + ceux de vos collègues).


SOLUTIONS DISPONIBLES :
-----------------------

┌─────────────────────────────────────────────────────────────────────────┐
│ SOLUTION 1 : Script PowerShell (RECOMMANDÉE)                            │
└─────────────────────────────────────────────────────────────────────────┘

1. Clic droit sur : Corriger-Encodage-Simple.ps1
2. Choisir : "Exécuter avec PowerShell"
3. Attendre la fin du traitement
4. Vérifier le rapport affiché

✓ Corrige automatiquement TOUS les fichiers Java
✓ Affiche un rapport détaillé
✓ Aucune installation requise


┌─────────────────────────────────────────────────────────────────────────┐
│ SOLUTION 2 : Script Python (si Python est installé)                     │
└─────────────────────────────────────────────────────────────────────────┘

1. Double-cliquer sur : CORRIGER_ENCODAGE.bat
   OU
   Exécuter dans le terminal : python fix_all_encoding.py

✓ Corrige automatiquement TOUS les fichiers Java
✓ Affiche un rapport détaillé


┌─────────────────────────────────────────────────────────────────────────┐
│ SOLUTION 3 : Correction Manuelle (VS Code / IntelliJ)                   │
└─────────────────────────────────────────────────────────────────────────┘

Consultez le fichier : GUIDE_CORRECTION_ENCODAGE.md

Pour chaque fichier :
1. Ouvrir le fichier
2. Ctrl + H (Rechercher et remplacer)
3. Remplacer les caractères mal encodés
4. Sauvegarder


FICHIERS CONCERNÉS :
--------------------

Contrôleurs (8 fichiers) :
  ✅ OrdonnanceDetailControllerArij.java (déjà corrigé)
  ⚠️  ChatbotControllerArij.java
  ⚠️  DashboardStatsControllerArij.java
  ⚠️  DisponibiliteController.java
  ⚠️  QuizSanteController.java
  ⚠️  QuizSanteControllerArij.java
  ⚠️  FormulaireRendezVousController.java
  ⚠️  RendezVousController.java

Services (3 fichiers) :
  ⚠️  ArticleService.java
  ⚠️  ServiceDisponibilite.java
  ⚠️  ServiceRendezVous.java


VÉRIFICATION APRÈS CORRECTION :
--------------------------------

1. Ouvrir un fichier corrigé
2. Rechercher (Ctrl + F) : "Ã"
3. Si aucun résultat → ✓ Correction réussie
4. Si des résultats → Relancer le script


CARACTÈRES CORRIGÉS :
---------------------

Ã© → é    (e accent aigu)
Ã¨ → è    (e accent grave)
Ãª → ê    (e accent circonflexe)
Ã§ → ç    (c cédille)
Ã  → à    (a accent grave)
├® → é    (e accent aigu alternatif)
├á → à    (a accent grave alternatif)
âœ" → ✓    (coche)
âœ— → ✗    (croix)
âš  → ⚠    (avertissement)

... et 40+ autres caractères


SUPPORT :
---------

En cas de problème :
1. Vérifiez que vous êtes dans le bon dossier du projet
2. Vérifiez que les fichiers ne sont pas en lecture seule
3. Fermez tous les fichiers ouverts dans votre éditeur
4. Relancez le script


================================================================================
  Bonne correction ! 🚀
================================================================================
