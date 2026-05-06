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
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button signInButton;

    private final ServiceUser serviceUser = new ServiceUser();
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    @FXML
    private void initialize() {
        signInButton.setOnAction(event -> handleLogin());

        Hyperlink forgotPasswordLink = new Hyperlink("Mot de passe oubli├® ?");
        forgotPasswordLink.setOnAction(event -> new ForgotPasswordController().showAsStage());

        ((VBox) signInButton.getParent()).getChildren().add(forgotPasswordLink);
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Champs requis", "Veuillez saisir votre email et mot de passe.");
            return;
        }

        try {
            User user = serviceUser.login(username, password);

            // Identifiant ou mot de passe incorrect
            if (user == null) {
                showAlert(Alert.AlertType.ERROR, "Connexion ├®chou├®e", "Identifiant ou mot de passe incorrect.");
                return;
            }

            String role = extractPrimaryRole(user);
            boolean isDoctor = role.contains("DOCTOR") || role.contains("MEDECIN");

            // CAS 1 ÔÇö Email non v├®rifi├®
            if (!user.isVerified()) {
                showAlert(Alert.AlertType.WARNING, "Compte non activ├®",
                        "Votre compte n'est pas encore activ├®. Un email de v├®rification a ├®t├® envoy├® ├á votre adresse. " +
                        "Veuillez v├®rifier votre bo├«te mail et saisir le code de confirmation.");
                EmailVerificationController.showAsStage(user.getEmail());
                return;
            }

            // CAS 2 ÔÇö M├®decin en attente de validation admin
            if (!user.isActive() && isDoctor) {
                showAlert(Alert.AlertType.WARNING, "Compte en cours de v├®rification",
                        "Votre compte m├®decin est en cours de v├®rification. Un administrateur doit examiner votre dossier " +
                        "et valider votre dipl├┤me avant que vous puissiez acc├®der ├á la plateforme. " +
                        "Vous recevrez un email d├¿s que votre compte sera approuv├®.");
                return;
            }

            // CAS 3 ÔÇö Compte bloqu├® (non m├®decin)
            if (!user.isActive()) {
                showAlert(Alert.AlertType.ERROR, "Compte suspendu",
                        "Votre compte a ├®t├® suspendu suite ├á plusieurs tentatives de connexion ├®chou├®es. " +
                        "Veuillez contacter l'administrateur pour d├®bloquer votre acc├¿s.");
                return;
            }

            // CAS 4 ÔÇö Login r├®ussi
            UserSession.setCurrentUser(user);
            UserSession.setCurrentRole(role);
            openMainView();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur inattendue lors du login", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Une erreur inattendue est survenue : " + e.getMessage());
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
            LOGGER.log(Level.SEVERE, "Erreur ouverture MainView", e);
        }
    }

    @FXML
    private void openRegisterPage() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    LoginController.class.getResource("/Register.fxml")));
            Stage stage = (Stage) signInButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("MedTimeFX ÔÇö Register");
            stage.setMaximized(false);
            stage.setMinWidth(560);
            stage.setMinHeight(760);
            stage.setWidth(600);
            stage.setHeight(800);
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("Erreur ouverture Register: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Erreur ouverture Register", e);
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
        return roles.stream().findFirst().map(role -> role.getName().toUpperCase()).orElse("PATIENT");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
