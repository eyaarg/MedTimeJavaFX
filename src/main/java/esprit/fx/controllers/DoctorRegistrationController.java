package esprit.fx.controllers;

import esprit.fx.entities.Doctor;
import esprit.fx.services.ServiceDoctor;
import esprit.fx.services.ServiceDoctorDocument;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

public class DoctorRegistrationController {

    private final ServiceDoctor serviceDoctor = new ServiceDoctor();
    private final ServiceDoctorDocument serviceDoctorDocument = new ServiceDoctorDocument();

    private TextField usernameField;
    private TextField emailField;
    private TextField phoneNumberField;
    private TextField licenseCodeField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private Label pdfLabel;
    private ProgressBar uploadProgressBar;
    private File selectedPdf;

    public static void showAsStage() {
        DoctorRegistrationController controller = new DoctorRegistrationController();
        Stage stage = new Stage();
        stage.setTitle("Inscription Médecin");
        controller.initialize(stage);
        stage.show();
    }

    private void initialize(Stage stage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.CENTER);

        usernameField = new TextField();
        usernameField.setPromptText("Nom d'utilisateur");

        emailField = new TextField();
        emailField.setPromptText("Email");

        phoneNumberField = new TextField();
        phoneNumberField.setPromptText("Numéro de téléphone");

        licenseCodeField = new TextField();
        licenseCodeField.setPromptText("Code de licence");

        passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");

        confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirmez le mot de passe");

        pdfLabel = new Label("Aucun fichier sélectionné");
        Button choosePdfButton = new Button("Choisir un fichier PDF");
        choosePdfButton.setOnAction(event -> choosePdf());

        uploadProgressBar = new ProgressBar();
        uploadProgressBar.setVisible(false);

        Button registerButton = new Button("S'inscrire");
        registerButton.setOnAction(event -> register(stage));

        root.getChildren().addAll(usernameField, emailField, phoneNumberField, licenseCodeField, passwordField, confirmPasswordField, pdfLabel, choosePdfButton, uploadProgressBar, registerButton);

        Scene scene = new Scene(root, 400, 500);
        stage.setScene(scene);
    }

    private void choosePdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        selectedPdf = fileChooser.showOpenDialog(null);
        if (selectedPdf != null) {
            pdfLabel.setText(selectedPdf.getName());
        } else {
            pdfLabel.setText("Aucun fichier sélectionné");
        }
    }

    private void register(Stage stage) {
        try {
            String username = usernameField.getText();
            String email = emailField.getText();
            String phoneNumber = phoneNumberField.getText();
            String licenseCode = licenseCodeField.getText();
            String password = passwordField.getText();

            // Validation des champs
            if (username.isEmpty() || email.isEmpty() || phoneNumber.isEmpty() || licenseCode.isEmpty() || password.isEmpty()) {
                showAlert("Erreur", "Tous les champs sont obligatoires.");
                return;
            }

            if (!Pattern.matches("^\\d{8}$", phoneNumber)) {
                showAlert("Erreur", "Le numéro de téléphone doit contenir exactement 8 chiffres.");
                return;
            }

            // Création de l'objet Doctor
            Doctor doctor = new Doctor(0, email, username, password, null, true, phoneNumber, false, null, null, null, null, 0, 0, 0, licenseCode, false, LocalDateTime.now(), LocalDateTime.now());

            // Ajout du docteur
            serviceDoctor.ajouter(doctor);

            // Téléchargement du document
            serviceDoctorDocument.uploadDocument(doctor.getId(), selectedPdf);

            // Affichage de la vérification par email
            EmailVerificationController.showAsStage(email);

            showInfo("Succès", "Inscription réussie.");
            stage.close();
        } catch (Exception e) {
            showAlert("Erreur", "Une erreur s'est produite : " + e.getMessage());
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
