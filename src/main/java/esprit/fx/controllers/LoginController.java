package esprit.fx.controllers;

import esprit.fx.entities.User;
import esprit.fx.services.ServiceUser;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class LoginController {

    @FXML private TextField     usernameField;  // contient l'email
    @FXML private PasswordField passwordField;
    @FXML private Button        signInButton;

    private final ServiceUser serviceUser = new ServiceUser();

    @FXML
    private void initialize() {
        // Bouton Sign In
        signInButton.setOnAction(e -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();  // c'est l'email
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Champs requis", "Veuillez saisir votre email et mot de passe.");
            return;
        }

        try {
            User user = serviceUser.login(username, password);  // login(email, password)
            if (user != null) {
                openMainView();
            } else {
                showAlert("Connexion échouée", "Identifiant ou mot de passe incorrect.");
            }
        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
            showAlert("Erreur", "Erreur de connexion : " + e.getMessage());
        }
    }

    /** Raccourci développement — connexion rapide en tant que Patient */
    @FXML
    private void loginAsPatient() {
        openMainView();
    }

    /** Raccourci développement — connexion rapide en tant que Médecin */
    @FXML
    private void loginAsDoctor() {
        openMainView();
    }

    /** Raccourci développement — connexion rapide en tant qu'Admin */
    @FXML
    private void loginAsAdmin() {
        openMainView();
    }

    private void openMainView() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    LoginController.class.getResource("/fxml/MainViewArij.fxml")));
            Stage stage = (Stage) signInButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("MedTimeFX");
            stage.setMaximized(true);
        } catch (IOException e) {
            System.err.println("Erreur ouverture MainView: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
