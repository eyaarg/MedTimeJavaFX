# 🎨 Système de Notifications Moderne

## ✨ Caractéristiques

### 1. **Notifications Toast** (en bas à droite)
- 📍 Position: Bas-droite de la fenêtre
- 📏 Taille: Compacte (max 350px)
- ⏱️ Durée: Auto-fermeture après 2.5 secondes
- 🎬 Animations: Slide-up + fade-in/out
- 🎨 Design: Flat moderne avec ombres douces

#### Types de notifications:
- ✅ **SUCCESS** - Vert (#10b981) - Pour les actions réussies
- ❌ **ERROR** - Rouge (#ef4444) - Pour les erreurs
- ⚠️ **WARNING** - Orange (#f59e0b) - Pour les avertissements
- ℹ️ **INFO** - Bleu (#3b82f6) - Pour les informations

### 2. **Popup de Confirmation** (centrée)
- 🎯 Design moderne avec carte blanche
- 🔔 Icône d'avertissement orange avec fond clair
- 📝 Titre en gras + message + détails
- 🎨 Boutons stylés:
  - **Annuler**: Gris clair avec hover
  - **Confirmer**: Rouge avec hover plus foncé
- 🎬 Animation: Scale + fade-in à l'ouverture
- 📦 Ombre portée douce pour profondeur

### 3. **Dialogs de Formulaire** (Prendre RDV / Modifier)
- 🎨 Fond blanc avec coins arrondis (12px)
- 📝 Labels en gras avec couleur moderne (#374151)
- 🔲 Champs avec fond gris clair (#f9fafb)
- 🔘 Bordures subtiles (#e5e7eb)
- 🎯 Boutons colorés selon l'action:
  - **Confirmer/Créer**: Vert (#10b981)
  - **Sauvegarder**: Bleu (#3b82f6)
  - **Annuler**: Gris (#f3f4f6)
- 🖱️ Effet hover sur tous les boutons

## 🚀 Utilisation

### Notification Toast
```java
Stage stage = (Stage) node.getScene().getWindow();
NotificationUtil.showNotification(
    stage,
    "Message à afficher",
    NotificationUtil.NotificationType.SUCCESS
);
```

### Popup de Confirmation
```java
Stage stage = (Stage) node.getScene().getWindow();
boolean confirmed = NotificationUtil.showConfirmation(
    stage,
    "Titre",
    "Message principal",
    "Détails\nLigne 2\nLigne 3"
);

if (confirmed) {
    // Action confirmée
}
```

## 🎯 Avantages

1. **UX Moderne**: Design inspiré des applications web modernes
2. **Non-intrusif**: Les toasts disparaissent automatiquement
3. **Cohérent**: Même style dans toute l'application
4. **Accessible**: Couleurs contrastées et icônes claires
5. **Fluide**: Animations douces et naturelles
6. **Responsive**: S'adapte à la taille de la fenêtre

## 📋 Exemples d'utilisation dans l'app

- ✅ Rendez-vous créé avec succès
- ✅ Rendez-vous modifié
- ✅ Rendez-vous supprimé
- ❌ Erreur de connexion à la base de données
- ⚠️ Veuillez sélectionner un élément
- ℹ️ Aucune disponibilité trouvée
- ⚠️ Confirmation de suppression (popup modale)
