# MedTimeFX

MedTimeFX est une application desktop développée en JavaFX et connectée à une base de données MySQL.

Le projet permet de gérer une plateforme médicale avec plusieurs espaces : patient, médecin et administrateur.

## Table des matières

- Description
- Fonctionnalités
- Stack technique
- Prérequis
- Installation
- Configuration de la base de données
- Lancement
- Structure du projet
- Compilation
- Contribution

## Description

MedTimeFX est un projet académique réalisé avec JavaFX.

L’application permet de centraliser plusieurs fonctionnalités médicales dans une seule interface desktop :

- gestion des utilisateurs
- gestion des rendez-vous
- gestion des consultations en ligne
- gestion des ordonnances
- gestion des factures
- gestion des produits pharmaceutiques
- gestion du forum médical
- gestion des notifications

## Fonctionnalités

### Authentification et comptes

- Inscription des patients et médecins.
- Connexion des utilisateurs.
- Gestion des rôles.
- Vérification des comptes.
- Gestion du profil utilisateur.

### Administration

- Gestion des utilisateurs.
- Validation des médecins.
- Consultation des statistiques.
- Suivi des comptes actifs et en attente.

### Consultations en ligne

- Création des consultations.
- Suivi des consultations par patient et médecin.
- Gestion des liens de consultation.
- Historique des consultations.

### Rendez-vous

- Création des rendez-vous.
- Gestion des disponibilités des médecins.
- Gestion de la liste d’attente.
- Consultation de l’historique.

### Ordonnances

- Création des ordonnances.
- Consultation des ordonnances.
- Génération de fichiers PDF.
- Génération de QR Code.

### Factures et paiements

- Consultation des factures.
- Suivi de l’état de paiement.
- Paiement des factures.

### Produits pharmaceutiques

- Ajout des produits.
- Modification des produits.
- Suppression des produits.
- Gestion du panier.
- Gestion des favoris.

### Forum médical

- Publication des articles.
- Gestion des commentaires.
- Consultation du contenu médical.

### Notifications

- Affichage des notifications.
- Notifications liées aux actions importantes du système.

## Stack technique

| Partie | Technologie |
|---|---|
| Interface | JavaFX |
| Langage | Java 23 |
| Base de données | MySQL |
| Gestion du projet | Maven |
| Connexion base de données | JDBC |
| Design des interfaces | Scene Builder |

## Prérequis

Avant de lancer le projet, installer :

- Java 23
- Maven
- MySQL Server
- JavaFX SDK
- Scene Builder
- IntelliJ IDEA ou un autre IDE Java

Vérifier les installations :

```bash
java --version
mvn --version
mysql --version
```

## Installation

### 1. Cloner le projet

```bash
git clone https://github.com/eyaarg/MedTimeJavaFX.git
cd MedTimeJavaFX
```

### 2. Ouvrir le projet

Ouvrir le dossier du projet dans IntelliJ IDEA.

Vérifier que le fichier suivant est bien détecté par Maven :

```text
pom.xml
```

### 3. Configurer Java

Configurer le projet avec Java 23 :

```text
File > Project Structure > Project SDK > Java 23
```

## Configuration de la base de données

Créer une base de données MySQL :

```sql
CREATE DATABASE mediplatform_test_test;
```

Importer le script SQL du projet si un dump est disponible :

```bash
mysql -u root -p mediplatform_test_test < database/mediplatform_test_test.sql
```

Vérifier le fichier de configuration :

```text
src/main/resources/config.properties
```

Exemple :

```properties
db.url=jdbc:mysql://localhost:3306/mediplatform_test_test
db.user=root
db.password=
```

Vérifier aussi la classe de connexion :

```text
src/main/java/esprit/fx/utils/MyDB.java
```

## Lancement

Installer les dépendances :

```bash
mvn clean install
```

Lancer l’application :

```bash
mvn javafx:run
```

Depuis IntelliJ IDEA :

```text
Maven > Plugins > javafx > javafx:run
```

## Structure du projet

```text
MedTimeJavaFX/
src/
  main/
    java/
      esprit/fx/
        controllers/
        entities/
        services/
        utils/
        Main.java
    resources/
      fxml/
      css/
      images/
      config.properties
pom.xml
README.md
```

## Compilation

Compiler le projet :

```bash
mvn clean compile
```

Lancer les tests :

```bash
mvn test
```

Nettoyer le projet :

```bash
mvn clean
```

## Bonnes pratiques

- Ne pas publier les clés API.
- Ne pas publier les mots de passe.
- Utiliser un fichier de configuration local pour la base de données.
- Garder le dump SQL du projet à jour.
- Faire un pull avant de commencer une modification.

## Contribution

Créer une branche :

```bash
git checkout master
git pull origin master
git checkout -b nom-de-branche
```

Ajouter les modifications :

```bash
git add .
git commit -m "Description de la modification"
git push origin nom-de-branche
```

Créer ensuite une Pull Request vers la branche principale.

## Statut du projet

Projet académique en cours de développement.
