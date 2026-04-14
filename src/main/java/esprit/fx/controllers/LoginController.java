package esprit.fx.controllers;

import esprit.fx.entities.User;
import esprit.fx.services.ServiceUser;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    private ServiceUser serviceUser = new ServiceUser();

    @FXML
    private void handleLogin() {
        try {
            String email = emailField.getText();
            String password = passwordField.getText();

            // 🔐 appel du service login
            User user = serviceUser.login(email, password);

            if (user != null) {
                // ✅ LOGIN OK → aller vers Home
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Home.fxml"));
                Stage stage = (Stage) emailField.getScene().getWindow();
                stage.setScene(new Scene(loader.load()));
                stage.setTitle("Home");
            } else {
                showAlert("Erreur", "Email ou mot de passe incorrect");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Register.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }
}