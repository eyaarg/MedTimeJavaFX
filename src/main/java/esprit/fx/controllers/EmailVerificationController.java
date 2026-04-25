package esprit.fx.controllers;

import esprit.fx.services.ServiceUser;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;

public class EmailVerificationController {

    private final ServiceUser serviceUser = new ServiceUser();

    public static void showAsStage(String email) {
        Stage stage = new Stage();
        stage.setTitle("Vérification de l'email");

        Label emailLabel = new Label("Email envoyé à " + email);
        TextField tokenField = new TextField();
        tokenField.setPromptText("Entrez le token");

        Button verifyButton = new Button("Vérifier");
        Button resendButton = new Button("Renvoyer");
        Button backButton = new Button("Retour login");

        verifyButton.setOnAction(event -> {
            String token = tokenField.getText().trim();
            if (token.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Le token ne peut pas être vide.");
                return;
            }
            boolean success = false;
            try {
                success = new EmailVerificationController().serviceUser.verifyEmailToken(token);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Email vérifié avec succès.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la vérification de l'email.");
            }
        });

        resendButton.setOnAction(event -> {
            // Logic to resend the email (not implemented here)
            showAlert(Alert.AlertType.INFORMATION, "Info", "Email de vérification renvoyé.");
        });

        backButton.setOnAction(event -> stage.close());

        HBox buttonBox = new HBox(10, verifyButton, resendButton, backButton);
        buttonBox.setAlignment(Pos.CENTER);

        VBox root = new VBox(10, emailLabel, tokenField, buttonBox);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 400, 200);
        stage.setScene(scene);
        stage.show();
    }

    private static void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
