package esprit.fx;

import esprit.fx.entities.User;
import esprit.fx.entities.Role;
import esprit.fx.utils.UserSession;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.util.List;

/**
 * Point d'entrée INDÉPENDANT pour tester uniquement le module Farah.
 * Lance directement les vues RendezVous et Disponibilité sans MainControllerArij.
 * Utilisation : Run → MainFarah
 */
public class MainFarah extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("🧪 Test Module Farah — RendezVous & Disponibilité");
        stage.setWidth(900);
        stage.setHeight(700);
        stage.setResizable(true);

        // Simuler une session utilisateur pour les tests
        simulerSession("DOCTOR"); // changer en "PATIENT" ou "ADMIN" si besoin

        afficherMenuPrincipal();
        stage.show();
    }

    // -------------------------------------------------------------------------
    // Menu principal de test
    // -------------------------------------------------------------------------

    private void afficherMenuPrincipal() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f3f4f6;");

        // Header
        VBox header = new VBox(6);
        header.setStyle(
            "-fx-background-color: linear-gradient(to right, #1e3a5f, #2563eb);" +
            "-fx-padding: 24 32;"
        );
        Label titre = new Label("🧪 Test Module Farah");
        titre.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label sous = new Label("RendezVous · Disponibilité · APIs · Métiers avancés");
        sous.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.85);");

        // Sélecteur de rôle
        HBox roleBox = new HBox(10);
        roleBox.setAlignment(Pos.CENTER_LEFT);
        roleBox.setPadding(new Insets(10, 0, 0, 0));
        Label roleLabel = new Label("Rôle actuel :");
        roleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("DOCTOR", "PATIENT", "ADMIN");
        roleCombo.setValue(UserSession.getCurrentRole());
        roleCombo.setOnAction(e -> {
            simulerSession(roleCombo.getValue());
            afficherMenuPrincipal();
        });
        roleBox.getChildren().addAll(roleLabel, roleCombo);
        header.getChildren().addAll(titre, sous, roleBox);

        // Grille de boutons
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #f3f4f6;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(20);
        content.setPadding(new Insets(24));

        // Section APIs
        content.getChildren().add(buildSection("🌐 APIs Intégrées", List.of(
            new BtnInfo("☀️ Météo (via Formulaire RDV)",
                "Ouvre le formulaire RDV → choisir une date → voir la météo",
                "#0ea5e9", () -> ouvrirFxml("/fxml/FormulaireRendezVous.fxml", "Formulaire RDV + Météo")),
            new BtnInfo("🗺️ Carte OpenStreetMap",
                "Ouvre la liste des disponibilités → cliquer 📍 Localisation",
                "#10b981", () -> ouvrirFxml("/fxml/DisponibiliteList.fxml", "Disponibilités + Carte")),
            new BtnInfo("📅 Google Calendar",
                "Ouvre la liste RDV → confirmer un RDV → bouton Google Calendar",
                "#4285f4", () -> ouvrirFxml("/fxml/RendezVousList.fxml", "RDV + Google Calendar"))
        )));

        // Section Métiers avancés
        content.getChildren().add(buildSection("⚙️ Métiers Avancés", List.of(
            new BtnInfo("📋 Historique RDV",
                "Ouvre la liste RDV → sélectionner un RDV → bouton Historique",
                "#7c3aed", () -> ouvrirFxml("/fxml/RendezVousList.fxml", "RDV + Historique")),
            new BtnInfo("💡 Suggestion de Créneaux",
                "Ouvre directement la fenêtre de suggestion",
                "#f59e0b", () -> ouvrirFxml("/fxml/SuggestionDisponibilite.fxml", "Suggestion Créneaux")),
            new BtnInfo("⏳ Liste d'Attente",
                "Ouvre directement la fenêtre liste d'attente",
                "#d97706", () -> ouvrirFxml("/fxml/ListeAttente.fxml", "Liste d'Attente"))
        )));

        // Section vues principales
        content.getChildren().add(buildSection("📋 Vues Principales", List.of(
            new BtnInfo("📅 Liste Rendez-vous",
                "Vue complète avec tous les boutons",
                "#2563eb", () -> ouvrirFxml("/fxml/RendezVousList.fxml", "Liste Rendez-vous")),
            new BtnInfo("🗓️ Liste Disponibilités",
                "Vue complète des disponibilités",
                "#059669", () -> ouvrirFxml("/fxml/DisponibiliteList.fxml", "Liste Disponibilités")),
            new BtnInfo("➕ Formulaire RDV",
                "Formulaire ajout/modification RDV",
                "#6366f1", () -> ouvrirFxml("/fxml/FormulaireRendezVous.fxml", "Formulaire RDV"))
        )));

        scroll.setContent(content);
        root.getChildren().addAll(header, scroll);

        primaryStage.setScene(new Scene(root));
    }

    // -------------------------------------------------------------------------
    // Helpers UI
    // -------------------------------------------------------------------------

    private VBox buildSection(String titre, List<BtnInfo> boutons) {
        VBox section = new VBox(12);

        Label titreLabel = new Label(titre);
        titreLabel.setStyle(
            "-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #1f2937;" +
            "-fx-border-color: #e5e7eb; -fx-border-width: 0 0 2 0; -fx-padding: 0 0 8 0;"
        );
        section.getChildren().add(titreLabel);

        for (BtnInfo info : boutons) {
            HBox card = new HBox(16);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(14, 16, 14, 16));
            card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 6, 0, 0, 2);" +
                "-fx-cursor: hand;"
            );

            VBox textBox = new VBox(4);
            HBox.setHgrow(textBox, Priority.ALWAYS);
            Label nom = new Label(info.nom);
            nom.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #1f2937;");
            Label desc = new Label(info.description);
            desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
            textBox.getChildren().addAll(nom, desc);

            Button btn = new Button("▶ Tester");
            btn.setStyle(
                "-fx-background-color: " + info.couleur + "; -fx-text-fill: white;" +
                "-fx-font-weight: 700; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;"
            );
            btn.setOnAction(e -> info.action.run());

            card.getChildren().addAll(textBox, btn);
            section.getChildren().add(card);
        }
        return section;
    }

    private void ouvrirFxml(String fxmlPath, String titre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("🧪 " + titre);
            stage.setScene(new Scene(root));
            stage.setWidth(900);
            stage.setHeight(700);
            stage.show();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir : " + fxmlPath);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            System.err.println("Erreur ouverture " + fxmlPath + " : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Session simulée
    // -------------------------------------------------------------------------

    private void simulerSession(String role) {
        User user = new User();
        user.setId(1); // ← changer selon ton ID en BDD
        user.setUsername("TestFarah");
        user.setEmail("farah@test.com");

        Role r = new Role();
        r.setName(role);
        user.setRoles(List.of(r));

        UserSession.setCurrentUser(user);
        UserSession.setCurrentRole(role);
    }

    public static void main(String[] args) {
        launch(args);
    }

    // -------------------------------------------------------------------------
    private record BtnInfo(String nom, String description, String couleur, Runnable action) {}
}
