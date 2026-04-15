package esprit.fx.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

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

    // Initialisation (optionnel, car les items sont déjà dans le FXML)
    @FXML
    public void initialize() {
        System.out.println("Controller initialisé");
        System.out.println("Rôles disponibles: " + roleComboBox.getItems().size());
        System.out.println("Régions disponibles: " + regionComboBox.getItems().size());

        // Vous pouvez ajouter des valeurs par défaut si vous voulez
        // roleComboBox.setValue("Patient");
        // regionComboBox.setValue("Tunis");
    }

    // Action du bouton Create Account
    @FXML
    private void handleCreateAccount() {
        // Récupérer les valeurs
        String username = usernameField.getText();
        String email = emailAddressField.getText();
        String phone = phoneField.getText();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();
        String region = regionComboBox.getValue();
        boolean termsAccepted = termsCheckBox.isSelected();

        // Validation
        if (username.isEmpty()) {
            showAlert("Erreur", "Veuillez entrer votre nom d'utilisateur");
            return;
        }

        if (email.isEmpty()) {
            showAlert("Erreur", "Veuillez entrer votre email");
            return;
        }

        if (password.isEmpty()) {
            showAlert("Erreur", "Veuillez entrer votre mot de passe");
            return;
        }

        if (role == null) {
            showAlert("Erreur", "Veuillez sélectionner un rôle");
            return;
        }

        if (region == null) {
            showAlert("Erreur", "Veuillez sélectionner une région");
            return;
        }

        if (!termsAccepted) {
            showAlert("Erreur", "Veuillez accepter les conditions d'utilisation");
            return;
        }

        // Tout est OK - Afficher les informations
        String message = "Compte créé avec succès !\n\n"
                + "Nom: " + username + "\n"
                + "Email: " + email + "\n"
                + "Téléphone: " + phone + "\n"
                + "Rôle: " + role + "\n"
                + "Région: " + region;

        showAlert("Succès", message);

        // Ici vous pouvez ajouter votre logique pour sauvegarder dans la base de données
        // saveUserToDatabase(username, email, phone, password, role, region);
    }

    // Action du lien Sign In
    @FXML
    private void handleSignIn(MouseEvent event) {
        System.out.println("Redirection vers la page de connexion");
        // Ici vous chargez votre fichier login.fxml
        // try {
        //     Parent root = FXMLLoader.load(getClass().getResource("login.fxml"));
        //     Scene scene = new Scene(root);
        //     Stage stage = (Stage) signInLink.getScene().getWindow();
        //     stage.setScene(scene);
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }
    }


    // Méthode utilitaire pour afficher des alertes
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}