package esprit.fx.controllers;

import esprit.fx.entities.Role;
import esprit.fx.entities.User;
import esprit.fx.services.ServiceUser;
import esprit.fx.utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailAddressField;

    @FXML
    private TextField phoneField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private ComboBox<String> regionComboBox;

    @FXML
    private CheckBox termsCheckBox;

    @FXML
    private Button createAccountBtn;

    @FXML
    private Text signInLink;

    private final ServiceUser serviceUser = new ServiceUser();

    // Initialisation (optionnel, car les items sont déjà dans le FXML)
    @FXML
    public void initialize() {
        roleComboBox.setValue("Patient");
        regionComboBox.setValue("Tunis");
    }

    // Action du bouton Create Account
    @FXML
    private void handleCreateAccount() {
        String username = usernameField.getText();
        String email = emailAddressField.getText();
        String phone = phoneField.getText();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();
        boolean termsAccepted = termsCheckBox.isSelected();

        if (username == null || username.isBlank()) {
            showAlert("Erreur", "Veuillez entrer votre nom d'utilisateur");
            return;
        }

        if (email == null || email.isBlank()) {
            showAlert("Erreur", "Veuillez entrer votre email");
            return;
        }

        if (password == null || password.isBlank()) {
            showAlert("Erreur", "Veuillez entrer votre mot de passe");
            return;
        }

        if (role == null) {
            showAlert("Erreur", "Veuillez sélectionner un rôle");
            return;
        }

        if (!termsAccepted) {
            showAlert("Erreur", "Veuillez accepter les conditions d'utilisation");
            return;
        }

        try {
            User userToCreate = new User();
            userToCreate.setUsername(username.trim());
            userToCreate.setEmail(email.trim());
            userToCreate.setPhoneNumber(phone == null ? null : phone.trim());
            userToCreate.setPassword(password);
            userToCreate.setRequestedRole(role);
            userToCreate.setCreatedAt(LocalDateTime.now());
            userToCreate.setActive(true);
            userToCreate.setVerified(true);
            userToCreate.setFailedAttempts(0);

            User createdUser = serviceUser.registerUser(userToCreate, role);

            UserSession.setCurrentUser(createdUser);
            UserSession.setCurrentRole(extractPrimaryRole(createdUser));

            showInfo("Succès", "Compte créé avec succès. Bienvenue " + createdUser.getUsername() + " !");
            openMainView();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de créer le compte : " + e.getMessage());
        } catch (Exception e) {
            showAlert("Erreur", "Une erreur inattendue est survenue : " + e.getMessage());
        }
    }

    @FXML
    private void handleSignIn(MouseEvent event) {
        openLoginPage();
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

    private void openLoginPage() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    RegisterController.class.getResource("/Login.fxml")));
            Stage stage = (Stage) signInLink.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("MedTimeFX — Login");
            stage.setMaximized(false);
            stage.setMinWidth(900);
            stage.setMinHeight(680);
            stage.setWidth(980);
            stage.setHeight(720);
            stage.centerOnScreen();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir la page Login : " + e.getMessage());
        }
    }

    private void openMainView() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    RegisterController.class.getResource("/fxml/MainViewArij.fxml")));
            Stage stage = (Stage) createAccountBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("MedTimeFX");
            stage.setMaximized(true);
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir la page principale : " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}