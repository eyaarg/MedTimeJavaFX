package esprit.fx.controllers;

import esprit.fx.services.ServiceUser;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.sql.SQLException;

public class EmailVerificationController {

    private final ServiceUser serviceUser = new ServiceUser();

    public static void showAsStage(String email) {
        Stage stage = new Stage();
        stage.setTitle("Vérification de l'email");

        Text title = new Text("Vérification de votre compte");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Label infoLabel = new Label("Un code de vérification a été envoyé à :");
        Label emailLabel = new Label(email);
        emailLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1565C0;");

        Label tokenLabel = new Label("Entrez le code reçu par email :");
        TextField tokenField = new TextField();
        tokenField.setPromptText("Code de vérification");
        tokenField.setMaxWidth(300);

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: red;");

        Button verifyButton = new Button("Vérifier");
        verifyButton.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-weight: bold;");
        Button resendButton = new Button("Renvoyer le code");
        Button backButton = new Button("Retour au login");

        verifyButton.setOnAction(event -> {
            String token = tokenField.getText().trim();
            if (token.isEmpty()) {
                statusLabel.setText("Le code ne peut pas être vide.");
                return;
            }
            try {
                boolean success = new ServiceUser().verifyEmailToken(token);
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès",
                            "Email vérifié avec succès ! Vous pouvez maintenant vous connecter.");
                    navigateToLogin(stage);
                } else {
                    statusLabel.setText("Code invalide ou expiré. Veuillez réessayer.");
                }
            } catch (SQLException e) {
                statusLabel.setText("Erreur : " + e.getMessage());
            }
        });

        resendButton.setOnAction(event -> {
            try {
                new ServiceUser().resendVerificationEmail(email);
                showAlert(Alert.AlertType.INFORMATION, "Email renvoyé",
                        "Un nouveau code a été envoyé à " + email);
            } catch (SQLException e) {
                statusLabel.setText("Erreur lors du renvoi : " + e.getMessage());
            }
        });

        backButton.setOnAction(event -> stage.close());

        HBox buttonBox = new HBox(10, verifyButton, resendButton, backButton);
        buttonBox.setAlignment(Pos.CENTER);

        VBox root = new VBox(12, title, infoLabel, emailLabel, tokenLabel, tokenField, statusLabel, buttonBox);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #F5F5F5;");

        Scene scene = new Scene(root, 450, 320);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Closes this verification stage and opens Login.fxml in a new stage.
     * If the main login window is already open (e.g. this was opened as a modal
     * from LoginController), we just close this popup — the login window stays.
     * Otherwise we create a fresh login stage.
     */
    private static void navigateToLogin(Stage verificationStage) {
        verificationStage.close();
        // Check if a login window is already open among existing windows
        boolean loginAlreadyOpen = javafx.stage.Window.getWindows().stream()
                .anyMatch(w -> w instanceof Stage && w.isShowing()
                        && ((Stage) w).getTitle() != null
                        && ((Stage) w).getTitle().toLowerCase().contains("medtime"));
        if (loginAlreadyOpen) {
            // The login stage is still behind — nothing more to do
            return;
        }
        // No login window open: open a fresh one
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    EmailVerificationController.class.getResource("/Login.fxml"));
            javafx.scene.Parent loginRoot = loader.load();
            Stage loginStage = new Stage();
            loginStage.setTitle("MedTimeFX — Login");
            loginStage.setScene(new Scene(loginRoot));
            loginStage.setResizable(false);
            loginStage.show();
        } catch (java.io.IOException e) {
            System.err.println("[EmailVerificationController] Erreur ouverture Login: " + e.getMessage());
        }
    }

    private static void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
