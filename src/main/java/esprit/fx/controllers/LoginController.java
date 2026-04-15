package esprit.fx.controllers;

import esprit.fx.entities.Role;
import esprit.fx.entities.User;
import esprit.fx.services.ServiceUser;
import esprit.fx.utils.UserSession;
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
import java.util.List;
import java.util.Objects;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button signInButton;

    private final ServiceUser serviceUser = new ServiceUser();

    @FXML
    private void initialize() {
        signInButton.setOnAction(e -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Champs requis", "Veuillez saisir votre email et mot de passe.");
            return;
        }

        try {
            User user = serviceUser.login(username, password);

            if (user != null) {
                UserSession.setCurrentUser(user);
                UserSession.setCurrentRole(extractPrimaryRole(user));
                openMainView();
            } else {
                showAlert("Connexion échouée", "Identifiant ou mot de passe incorrect.");
            }

        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
            showAlert("Erreur", "Erreur de connexion : " + e.getMessage());
        }
    }

    @FXML
    private void loginAsPatient() {
        UserSession.setCurrentUser(null);
        UserSession.setCurrentRole("PATIENT");
        openMainView();
    }

    @FXML
    private void loginAsDoctor() {
        UserSession.setCurrentUser(null);
        UserSession.setCurrentRole("DOCTOR");
        openMainView();
    }

    @FXML
    private void loginAsAdmin() {
        UserSession.setCurrentUser(null);
        UserSession.setCurrentRole("ADMIN");
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

    @FXML
    private void openRegisterPage() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    LoginController.class.getResource("/Register.fxml")));
            Stage stage = (Stage) signInButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("MedTimeFX — Register");
            stage.setMaximized(false);
            stage.setMinWidth(560);
            stage.setMinHeight(760);
            stage.setWidth(600);
            stage.setHeight(800);
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("Erreur ouverture Register: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String extractPrimaryRole(User user) {
        if (user == null) {
            return "PATIENT";
        }
        List<Role> roles = user.getRoles();
        if (roles == null || roles.isEmpty() || roles.get(0) == null || roles.get(0).getName() == null) {
            return "PATIENT";
        }
        return roles.get(0).getName().toUpperCase();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}