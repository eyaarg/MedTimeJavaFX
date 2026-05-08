# 📝 Guide de Correction de l'Encodage UTF-8

## 🎯 Objectif
Corriger tous les caractères mal encodés dans les fichiers Java du projet (vos fichiers + ceux de vos collègues).

---

## ✅ Méthode 1 : Script Automatique (Recommandée)

### Double-cliquez sur : `CORRIGER_ENCODAGE.bat`

Le script va automatiquement :
- Scanner tous les fichiers `.java` dans `src/`
- Corriger tous les caractères mal encodés
- Afficher un rapport de correction

---

## 🔧 Méthode 2 : Correction Manuelle (VS Code / IntelliJ)

### Fichiers à corriger :

#### Contrôleurs (Controllers)
- ✅ `OrdonnanceDetailControllerArij.java` (déjà corrigé)
- ⚠️ `ChatbotControllerArij.java`
- ⚠️ `DashboardStatsControllerArij.java`
- ⚠️ `DisponibiliteController.java`
- ⚠️ `QuizSanteController.java`
- ⚠️ `QuizSanteControllerArij.java`

#### Services
- ⚠️ `ArticleService.java`
- ⚠️ `ServiceDisponibilite.java`
- ⚠️ `ServiceRendezVous.java`

### Étapes dans VS Code :

1. **Ouvrir le fichier** à corriger
2. **Ctrl + H** (Rechercher et remplacer)
3. **Cocher** "Utiliser les expressions régulières" (icône `.*`)
4. **Copier-coller** ces remplacements un par un :

```
Rechercher : Ã©
Remplacer par : é

Rechercher : Ã¨
Remplacer par : è

Rechercher : Ãª
Remplacer par : ê

Rechercher : Ã§
Remplacer par : ç

Rechercher : Ã 
Remplacer par : à

Rechercher : Ã´
Remplacer par : ô

Rechercher : Ã®
Remplacer par : î

Rechercher : â†'
Remplacer par : →

Rechercher : âœ"
Remplacer par : ✓

Rechercher : âœ—
Remplacer par : ✗

Rechercher : âš 
Remplacer par : ⚠

Rechercher : ├®
Remplacer par : é

Rechercher : ├á
Remplacer par : à

Rechercher : ├¿
Remplacer par : è

Rechercher : ├¬
Remplacer par : ê

Rechercher : ├╗
Remplacer par : û

Rechercher : Â
Remplacer par : (rien - supprimer)
```

5. **Cliquer** sur "Remplacer tout" pour chaque remplacement
6. **Sauvegarder** le fichier (Ctrl + S)
7. **Répéter** pour tous les fichiers listés ci-dessus

---

## 📋 Table de Correspondance Complète

| Mal encodé | Correct | Description |
|------------|---------|-------------|
| `Ã©` | `é` | e accent aigu |
| `Ã¨` | `è` | e accent grave |
| `Ãª` | `ê` | e accent circonflexe |
| `Ã§` | `ç` | c cédille |
| `Ã ` | `à` | a accent grave |
| `Ã´` | `ô` | o accent circonflexe |
| `Ã®` | `î` | i accent circonflexe |
| `Ã¹` | `ù` | u accent grave |
| `Ã»` | `û` | u accent circonflexe |
| `â†'` | `→` | flèche droite |
| `âœ"` | `✓` | coche |
| `âœ—` | `✗` | croix |
| `âš ` | `⚠` | avertissement |
| `├®` | `é` | e accent aigu (alt) |
| `├á` | `à` | a accent grave (alt) |
| `├¿` | `è` | e accent grave (alt) |
| `├¬` | `ê` | e accent circonflexe (alt) |
| `├╗` | `û` | u accent circonflexe (alt) |
| `Â` | `` | à supprimer |

---

## 🔍 Exemples de Corrections

### Avant :
```java
System.out.println("Date dÃ©but reÃ§ue: " + date);
throw new SQLException("Impossible de supprimer cette disponibilitÃ©");
doctorCombo.setPromptText("S├®lectionner un m├®decin");
```

### Après :
```java
System.out.println("Date début reçue: " + date);
throw new SQLException("Impossible de supprimer cette disponibilité");
doctorCombo.setPromptText("Sélectionner un médecin");
```

---

## ✅ Vérification

Après correction, recherchez dans tous les fichiers :
- `Ã` (ne devrait plus apparaître)
- `â` (ne devrait plus apparaître)
- `├` (ne devrait plus apparaître)

Si ces caractères apparaissent encore, répétez la correction.

---

## 🆘 Besoin d'aide ?

Si vous rencontrez des problèmes :
1. Vérifiez que votre éditeur est configuré en UTF-8
2. Assurez-vous de sauvegarder après chaque modification
3. Relancez le script `CORRIGER_ENCODAGE.bat`

---

## 📊 Statistiques

- **Fichiers Java totaux** : ~120 fichiers
- **Fichiers avec problèmes** : ~8 fichiers
- **Caractères à corriger** : ~50 types différents

---

**Bonne correction ! 🚀**
