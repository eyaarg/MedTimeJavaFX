package esprit.fx.controllers;

import esprit.fx.services.ServiceUser;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;

public class ForgotPasswordController {

    private final ServiceUser serviceUser = new ServiceUser();

    public void showAsStage() {
        Stage stage = new Stage();
        stage.setTitle("R├®initialisation du mot de passe");

        // ├ëtape 1: Entrer l'email
        VBox step1 = new VBox(10);
        step1.setPadding(new Insets(15));
        step1.setAlignment(Pos.CENTER);

        Label emailLabel = new Label("Entrez votre email :");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        Button sendButton = new Button("Envoyer");

        step1.getChildren().addAll(emailLabel, emailField, sendButton);

        // ├ëtape 2: Entrer le token et le nouveau mot de passe
        VBox step2 = new VBox(10);
        step2.setPadding(new Insets(15));
        step2.setAlignment(Pos.CENTER);
        step2.setVisible(false);

        Label tokenLabel = new Label("Entrez le token :");
        TextField tokenField = new TextField();
        tokenField.setPromptText("Token");

        Label newPasswordLabel = new Label("Nouveau mot de passe :");
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Nouveau mot de passe");

        Label confirmPasswordLabel = new Label("Confirmez le mot de passe :");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirmez le mot de passe");

        Button resetButton = new Button("R├®initialiser");

        step2.getChildren().addAll(tokenLabel, tokenField, newPasswordLabel, newPasswordField, confirmPasswordLabel, confirmPasswordField, resetButton);

        // Actions des boutons
        sendButton.setOnAction(event -> {
            String email = emailField.getText().trim();
            if (email.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "L'email ne peut pas ├¬tre vide.");
                return;
            }
            try {
                serviceUser.requestPasswordReset(email);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Succ├¿s");
                alert.setHeaderText(null);
                alert.setContentText("Un email de r├®initialisation a ├®t├® envoy├® ├á " + email);
                alert.showAndWait();
                step1.setVisible(false);
                step2.setVisible(true);
            } catch (SQLException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText(null);
                alert.setContentText("Une erreur s'est produite lors de l'envoi de l'email de r├®initialisation.");
                alert.showAndWait();
                e.printStackTrace();
            }
        });

        resetButton.setOnAction(event -> {
            String token = tokenField.getText().trim();
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            if (token.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Tous les champs doivent ├¬tre remplis.");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Les mots de passe ne correspondent pas.");
                return;
            }

            boolean success = false;
            try {
                success = serviceUser.resetPassword(token, newPassword);
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Une erreur s'est produite lors de la r├®initialisation du mot de passe.");
                return;
            }
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Succ├¿s", "Mot de passe r├®initialis├® avec succ├¿s.");
                stage.close();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "├ëchec de la r├®initialisation du mot de passe.");
            }
        });

        VBox root = new VBox(10, step1, step2);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 400, 300);
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
