package esprit.fx.controllers;

import esprit.fx.services.UserStatsService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.Map;

public class AdminDashboardController {

    private static final UserStatsService statsService = new UserStatsService();

    public static void showAsStage() {
        Stage stage = new Stage();
        stage.setTitle("Admin Dashboard — Statistiques");
        stage.setWidth(950);
        stage.setHeight(750);

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(20));
        gridPane.setHgap(20);
        gridPane.setVgap(20);
        gridPane.setAlignment(Pos.CENTER);

        // Charger les stats réelles
        int totalUsers = 0, activeDoctors = 0, totalPatients = 0,
                pendingDoctors = 0, lockedAccounts = 0, unverifiedEmails = 0;
        try {
            totalUsers     = statsService.getTotalUsers();
            activeDoctors  = statsService.getActiveUsers(); // médecins actifs via getActiveDoctors si dispo
            totalPatients  = statsService.getTotalPatients();
            pendingDoctors = statsService.getPendingDoctors();
            lockedAccounts = statsService.getLockedAccounts();
            unverifiedEmails = statsService.getUnverifiedEmails();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les statistiques : " + e.getMessage());
        }

        gridPane.add(createCard("Total Utilisateurs",   String.valueOf(totalUsers),     Color.web("#E3F2FD")), 0, 0);
        gridPane.add(createCard("Médecins actifs",      String.valueOf(activeDoctors),  Color.web("#E8F5E9")), 1, 0);
        gridPane.add(createCard("Patients",             String.valueOf(totalPatients),  Color.web("#F3E5F5")), 0, 1);
        gridPane.add(createCard("En attente validation",String.valueOf(pendingDoctors), Color.web("#FFF3E0")), 1, 1);
        gridPane.add(createCard("Comptes bloqués",      String.valueOf(lockedAccounts), Color.web("#FFEBEE")), 0, 2);
        gridPane.add(createCard("Emails non vérifiés",  String.valueOf(unverifiedEmails),Color.web("#FFFDE7")),1, 2);

        // PieChart répartition des rôles
        PieChart pieChart = new PieChart();
        pieChart.setData(loadRoleData());
        pieChart.setTitle("Répartition des rôles");
        pieChart.setLegendVisible(true);
        pieChart.setPrefHeight(280);

        // Boutons d'action
        Button validateDoctorsButton = new Button("Valider médecins");
        validateDoctorsButton.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-weight: bold;");
        validateDoctorsButton.setOnAction(e -> AdminDoctorValidationController.showAsStage());

        Button manageUsersButton = new Button("Gérer utilisateurs");
        manageUsersButton.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold;");
        manageUsersButton.setOnAction(e -> showAlert("Info", "Ouvrez la vue Gestion Utilisateurs depuis le menu principal."));

        Button refreshButton = new Button("Rafraîchir");
        refreshButton.setStyle("-fx-background-color: #F57F17; -fx-text-fill: white; -fx-font-weight: bold;");
        refreshButton.setOnAction(e -> {
            stage.close();
            showAsStage();
        });

        HBox buttonBox = new HBox(15, validateDoctorsButton, manageUsersButton, refreshButton);
        buttonBox.setAlignment(Pos.CENTER);

        VBox vbox = new VBox(20);
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.CENTER);
        vbox.setStyle("-fx-background-color: #FAFAFA;");
        vbox.getChildren().addAll(gridPane, pieChart, buttonBox);

        Scene scene = new Scene(vbox);
        stage.setScene(scene);
        stage.show();
    }

    private static VBox createCard(String title, String value, Color color) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(200);
        card.setPrefHeight(100);
        card.setStyle("-fx-background-color: " + toHexString(color) + "; " +
                "-fx-background-radius: 12px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2);");

        Text titleLabel = new Text(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        titleLabel.setFill(Color.web("#424242"));

        Text valueLabel = new Text(value);
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        valueLabel.setFill(Color.web("#212121"));

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private static ObservableList<PieChart.Data> loadRoleData() {
        try {
            Map<String, Integer> usersPerRole = statsService.getUsersPerRole();
            ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
            usersPerRole.forEach((role, count) -> data.add(new PieChart.Data(role + " (" + count + ")", count)));
            if (data.isEmpty()) {
                data.add(new PieChart.Data("Aucune donnée", 1));
            }
            return data;
        } catch (SQLException e) {
            return FXCollections.observableArrayList(new PieChart.Data("Erreur", 1));
        }
    }

    private static String toHexString(Color color) {
        int r = (int) (color.getRed() * 255);
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
