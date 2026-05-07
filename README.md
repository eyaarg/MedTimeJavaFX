# 🏷️ MedTimeFX

![Java](https://img.shields.io/badge/Java-23-blue?style=for-the-badge&logo=openjdk)
![JavaFX](https://img.shields.io/badge/JavaFX-23.0.2-0ea5e9?style=for-the-badge)
![Maven](https://img.shields.io/badge/Maven-Build-c71a36?style=for-the-badge&logo=apachemaven)
![MySQL](https://img.shields.io/badge/MySQL-Database-4479a1?style=for-the-badge&logo=mysql&logoColor=white)
![JDBC](https://img.shields.io/badge/JDBC-Connexion-2563eb?style=for-the-badge)
![Projet](https://img.shields.io/badge/Projet-Acad%C3%A9mique-16a34a?style=for-the-badge)

**MedTimeFX** est une application desktop médicale développée avec **JavaFX**, connectée à une base de données **MySQL** via **JDBC**.  
Elle centralise la gestion des utilisateurs, des rendez-vous, des consultations en ligne, des ordonnances, des factures, du marketplace pharmaceutique et du forum médical.

## 📸 Aperçu

> Ajouter ici les captures d’écran principales du projet.

| Connexion | Tableau de bord | Consultations |
|---|---|---|
| `docs/screenshots/login.png` | `docs/screenshots/dashboard.png` | `docs/screenshots/consultations.png` |

---

## 📋 Table des matières

- [🎯 Fonctionnalités](#-fonctionnalités)
- [🛠️ Stack technique](#️-stack-technique)
- [⚙️ Prérequis](#️-prérequis)
- [🚀 Installation et lancement](#-installation-et-lancement)
- [🗄️ Configuration de la base de données](#️-configuration-de-la-base-de-données)
- [📁 Structure du projet](#-structure-du-projet)
- [🧪 Compilation](#-compilation)
- [🤝 Contribution](#-contribution)

---

## 🎯 Fonctionnalités

### 👤 Authentification et gestion des comptes
- Inscription et connexion des utilisateurs.
- Gestion des rôles : patient, médecin et administrateur.
- Vérification par email pour les comptes patients.
- Validation administrative des comptes médecins.
- Gestion du profil utilisateur.

### 🛡️ Espace administrateur
- Tableau de bord administrateur.
- Gestion des utilisateurs et des rôles.
- Validation ou refus des médecins.
- Consultation des statistiques globales.

### 🩺 Consultations en ligne
- Création et suivi des consultations.
- Gestion des consultations côté patient et côté médecin.
- Génération de liens de consultation en ligne.
- Notifications liées aux consultations.
- Historique des consultations.

### 📅 Rendez-vous et disponibilités
- Gestion des rendez-vous médicaux.
- Consultation des disponibilités des médecins.
- Liste d’attente.
- Suggestions de créneaux.
- Historique des rendez-vous.

### 💊 Ordonnances
- Création et consultation des ordonnances.
- Génération de PDF.
- Génération de QR Code pour accéder à l’ordonnance.
- Suppression et visualisation des ordonnances.

### 🧾 Factures et paiements
- Génération et consultation des factures.
- Gestion de l’état de paiement.
- Paiement en ligne.
- Export PDF selon les besoins du module.

### 🛒 Marketplace pharmaceutique
- Gestion des produits pharmaceutiques.
- Ajout, modification, suppression et consultation des produits.
- Gestion du panier.
- Gestion des favoris.
- Paiement de commandes.

### 💬 Forum médical
- Publication d’articles.
- Ajout et gestion des commentaires.
- Modération du contenu.
- Consultation des articles médicaux.

### 🔔 Notifications
- Notifications internes.
- Support WebSocket pour les notifications en temps réel.
- Notifications liées aux consultations, rendez-vous et actions importantes.

### 📊 Tableaux de bord et statistiques
- Indicateurs de suivi.
- Statistiques des consultations.
- Statistiques administrateur.
- Visualisation des informations importantes par rôle.

---

## 🛠️ Stack technique

| Couche | Technologie |
|---|---|
| Frontend | JavaFX |
| Backend | Java 23 |
| Base de données | MySQL |
| Build Tool | Maven |
| ORM / Connexion | JDBC |
| UI Design | Scene Builder |

---

## ⚙️ Prérequis

Avant de lancer le projet, installer :

- **Java 23**
- **Maven**
- **MySQL Server**
- **JavaFX SDK**
- **Scene Builder**
- Un IDE Java recommandé : **IntelliJ IDEA**

Vérifier l’installation :

```bash
java --version
mvn --version
mysql --version
```

---

## 🚀 Installation et lancement

### 1. Cloner le projet

```bash
git clone https://github.com/eyaarg/MedTimeJavaFX.git
cd MedTimeJavaFX
```

### 2. Ouvrir le projet

Ouvrir le dossier du projet dans **IntelliJ IDEA**.

Vérifier que Maven détecte correctement le fichier :

```text
pom.xml
```

### 3. Configurer Java 23

Dans IntelliJ IDEA :

```text
File > Project Structure > Project SDK > Java 23
```

### 4. Configurer la base de données MySQL

Créer une base de données MySQL :

```sql
CREATE DATABASE mediplatform_test_test;
```

Importer ensuite le script SQL du projet si un dump est fourni par l’équipe :

```bash
mysql -u root -p mediplatform_test_test < database/mediplatform_test_test.sql
```

### 5. Configurer la connexion JDBC

Vérifier le fichier :

```text
src/main/resources/config.properties
```

Exemple de configuration :

```properties
db.url=jdbc:mysql://localhost:3306/mediplatform_test_test
db.user=root
db.password=
```

Vérifier aussi la classe de connexion si nécessaire :

```text
src/main/java/esprit/fx/utils/MyDB.java
```

### 6. Installer les dépendances Maven

```bash
mvn clean install
```

### 7. Lancer l’application

```bash
mvn javafx:run
```

Ou depuis IntelliJ IDEA :

```text
Maven > Plugins > javafx > javafx:run
```

---

## 🗄️ Configuration de la base de données

Le projet utilise une base MySQL avec plusieurs tables fonctionnelles, notamment :

- `users`
- `roles`
- `user_roles`
- `patients`
- `doctors`
- `doctor_documents`
- `consultations`
- `disponibilite_medecin`
- `rendez_vous`
- `ordonnances`
- `factures`
- `product`
- `panier`
- `article`
- `commentaire`
- `notifications`

Certaines tables ou colonnes peuvent être initialisées automatiquement au lancement, mais il est recommandé d’utiliser le dump SQL commun du projet pour garantir un environnement complet.

---

## 📁 Structure du projet

```text
MedTimeJavaFX/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── esprit/fx/
│   │   │       ├── controllers/
│   │   │       ├── entities/
│   │   │       ├── services/
│   │   │       ├── utils/
│   │   │       └── Main.java
│   │   └── resources/
│   │       ├── fxml/
│   │       ├── css/
│   │       ├── images/
│   │       └── config.properties
│   └── test/
├── uploads/
├── pom.xml
└── README.md
```

---

## 🧪 Compilation

Compiler le projet sans lancer l’interface :

```bash
mvn clean compile
```

Lancer les tests si disponibles :

```bash
mvn test
```

Nettoyer le projet :

```bash
mvn clean
```

---

## 🔐 Sécurité et bonnes pratiques

- Ne pas publier les clés API dans GitHub.
- Ne pas versionner les mots de passe réels.
- Utiliser `config.properties` pour la configuration locale.
- Ajouter les fichiers sensibles dans `.gitignore`.
- Garder un dump SQL partagé et à jour pour toute l’équipe.

---

## 🤝 Contribution

Workflow recommandé :

```bash
git checkout master
git pull origin master
git checkout -b nom-de-branche
```

Après modification :

```bash
git add .
git commit -m "Description claire de la modification"
git push origin nom-de-branche
```

Créer ensuite une Pull Request vers `master`.

---

## 👥 Équipe

Projet académique réalisé dans le cadre d’un module Java / JavaFX.

---

## 📌 Statut

Projet en développement académique.  
Les fonctionnalités peuvent évoluer selon les besoins de l’équipe et les corrections d’intégration.
