# Fix: Erreur "Impossible d'ouvrir le formulaire" - Bouton Nouveau RDV

## Problème
Lorsque vous cliquez sur le bouton "+ Nouveau RDV", une erreur s'affiche:
```
Impossible d'ouvrir le formulaire:
C:/Nouv/FXDVD/OneDrive/A29.%205P5817/Bureau/Pi-Deskt...
```

## Cause
Le chemin du projet contient des caractères spéciaux (`%20` pour les espaces) qui peuvent causer des problèmes lors du chargement des ressources FXML.

## Solution Appliquée

J'ai modifié la méthode `ouvrirFormulaireRendezVous()` dans `RendezVousController.java` pour:

1. **Essayer plusieurs méthodes de chargement** du fichier FXML
2. **Améliorer la gestion des erreurs** avec des messages plus détaillés
3. **Ajouter des logs** pour faciliter le débogage

### Changements dans `RendezVousController.java`

La méthode `ouvrirFormulaireRendezVous()` a été améliorée pour:

```java
private void ouvrirFormulaireRendezVous(RendezVous rdv) {
    try {
        // Essayer plusieurs méthodes pour charger le FXML
        FXMLLoader loader = new FXMLLoader();
        
        // Méthode 1: Utiliser getResource
        URL fxmlUrl = getClass().getResource("/fxml/FormulaireRendezVous.fxml");
        
        // Méthode 2: Si la méthode 1 échoue, essayer avec le ClassLoader
        if (fxmlUrl == null) {
            fxmlUrl = getClass().getClassLoader().getResource("fxml/FormulaireRendezVous.fxml");
        }
        
        // Méthode 3: Si les deux échouent, essayer un chemin absolu
        if (fxmlUrl == null) {
            fxmlUrl = FormulaireRendezVousController.class.getResource("/fxml/FormulaireRendezVous.fxml");
        }
        
        if (fxmlUrl == null) {
            showAlert("Erreur", "Fichier FXML introuvable : FormulaireRendezVous.fxml\n" +
                    "Veuillez vérifier que le fichier existe dans src/main/resources/fxml/");
            return;
        }
        
        loader.setLocation(fxmlUrl);
        Parent root = loader.load();
        
        FormulaireRendezVousController ctrl = loader.getController();
        if (ctrl == null) {
            showAlert("Erreur", "Impossible de récupérer le contrôleur du formulaire");
            return;
        }
        
        ctrl.setRendezVous(rdv);
        ctrl.setParentController(this);
        
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(rdv == null ? "Nouveau Rendez-vous" : "Modifier Rendez-vous");
        stage.setScene(new Scene(root));
        stage.showAndWait();
        
    } catch (IOException e) {
        e.printStackTrace();
        System.err.println("Erreur détaillée: " + e.getMessage());
        System.err.println("Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "Aucune"));
        showAlert("Erreur", "Impossible d'ouvrir le formulaire.\n" +
                "Erreur: " + e.getMessage() + "\n" +
                "Veuillez vérifier la console pour plus de détails.");
    } catch (Exception e) {
        e.printStackTrace();
        showAlert("Erreur", "Erreur inattendue: " + e.getMessage());
    }
}
```

## Étapes pour Appliquer le Fix

### Option 1: Recompiler le projet (Recommandé)
```bash
mvn clean compile
```

### Option 2: Rebuild dans votre IDE
1. Dans IntelliJ IDEA: `Build` → `Rebuild Project`
2. Dans Eclipse: `Project` → `Clean...` → `Build Project`

### Option 3: Si le problème persiste

Si après recompilation le problème persiste, cela peut être dû au chemin du projet. Voici les solutions:

#### Solution A: Déplacer le projet
Déplacez votre projet vers un chemin sans caractères spéciaux ni espaces:
- ❌ Mauvais: `C:/Nouv/FXDVD/OneDrive/A29.%205P5817/Bureau/Pi-Deskt...`
- ✅ Bon: `C:/Projects/MedTimeJavaFX`

#### Solution B: Vérifier les ressources
Assurez-vous que le fichier FXML existe bien:
```
src/main/resources/fxml/FormulaireRendezVous.fxml
```

#### Solution C: Nettoyer le cache Maven
```bash
mvn clean
rm -rf target/
mvn compile
```

## Vérification

Après avoir appliqué le fix:

1. **Recompilez le projet**
2. **Relancez l'application**
3. **Cliquez sur "+ Nouveau RDV"**
4. **Le formulaire devrait s'ouvrir sans erreur**

Si une erreur persiste, vérifiez la console pour voir les messages de débogage détaillés qui vous indiqueront exactement où se situe le problème.

## Messages d'Erreur Améliorés

Avec ce fix, vous obtiendrez des messages d'erreur plus précis:

- ✅ "Fichier FXML introuvable" → Le fichier n'existe pas dans les ressources
- ✅ "Impossible de récupérer le contrôleur" → Problème avec le contrôleur FXML
- ✅ "Erreur détaillée: [message]" → Détails complets de l'erreur dans la console

## Fichiers Modifiés

- ✅ `src/main/java/esprit/fx/controllers/RendezVousController.java`

## Statut

✅ **Fix appliqué** - En attente de recompilation et test

---

*Créé par: Kiro AI Assistant*
*Date: Session actuelle*
