package esprit.fx.controllers;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class AdminDashboardController {

    public static void showAsStage() {
        Stage stage = new Stage();
        stage.setTitle("Admin Dashboard");
        stage.setWidth(900);
        stage.setHeight(700);

        // GridPane for cards
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(20));
        gridPane.setHgap(20);
        gridPane.setVgap(20);
        gridPane.setAlignment(Pos.CENTER);

        // Add cards to the grid
        gridPane.add(createCard("Total Users", "1000", Color.web("#E3F2FD")), 0, 0);
        gridPane.add(createCard("Médecins actifs", "200", Color.web("#E8F5E9")), 1, 0);
        gridPane.add(createCard("Patients", "800", Color.web("#F3E5F5")), 0, 1);
        gridPane.add(createCard("En attente validation", "50", Color.web("#FFF3E0")), 1, 1);
        gridPane.add(createCard("Comptes bloqués", "10", Color.web("#FFEBEE")), 0, 2);
        gridPane.add(createCard("Emails non vérifiés", "30", Color.web("#FFFDE7")), 1, 2);

        // PieChart for user roles
        PieChart pieChart = new PieChart();
        pieChart.setData(getUsersPerRole());
        pieChart.setTitle("Répartition des rôles");

        // Buttons
        Button validateDoctorsButton = new Button("Valider médecins");
        Button manageUsersButton = new Button("Gérer utilisateurs");
        Button refreshButton = new Button("Rafraîchir");

        validateDoctorsButton.setOnAction(e -> System.out.println("Valider médecins clicked"));
        manageUsersButton.setOnAction(e -> System.out.println("Gérer utilisateurs clicked"));
        refreshButton.setOnAction(e -> System.out.println("Rafraîchir clicked"));

        // VBox for layout
        VBox vbox = new VBox(20);
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.CENTER);
        vbox.getChildren().addAll(gridPane, pieChart, validateDoctorsButton, manageUsersButton, refreshButton);

        // Scene setup
        Scene scene = new Scene(vbox);
        stage.setScene(scene);
        stage.show();
    }

    private static VBox createCard(String title, String value, Color color) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(10));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: " + toHexString(color) + "; -fx-background-radius: 10px;");

        Text titleLabel = new Text(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Text valueLabel = new Text(value);
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private static ObservableList<PieChart.Data> getUsersPerRole() {
        // Example data, replace with actual data retrieval logic
        return FXCollections.observableArrayList(
            new PieChart.Data("Admins", 10),
            new PieChart.Data("Doctors", 200),
            new PieChart.Data("Patients", 800)
        );
    }

    private static String toHexString(Color color) {
        int r = (int) (color.getRed() * 255);
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
