package esprit.fx.controllers;

import esprit.fx.entities.Doctor;
import esprit.fx.services.ServiceDoctor;
import esprit.fx.services.ServiceDoctorDocument;
import esprit.fx.entities.User;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.File;

public class DoctorRegistrationController {

    private final ServiceDoctor serviceDoctor = new ServiceDoctor();
    private final ServiceDoctorDocument serviceDoctorDocument = new ServiceDoctorDocument();

    private TextField licenseCodeField;
    private Label pdfLabel;
    private File selectedPdf;
    private User receivedUser;

    public static void showAsStage(User user) {
        if (user == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez d'abord créer un compte via le formulaire d'inscription avant de compléter votre profil médecin.");
            alert.showAndWait();
            return;
        }
        DoctorRegistrationController controller = new DoctorRegistrationController();
        controller.receivedUser = user;
        Stage stage = new Stage();
        stage.setTitle("Inscription Médecin");
        controller.initialize(stage);
        stage.show();
    }

    private void initialize(Stage stage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.CENTER);

        licenseCodeField = new TextField();
        licenseCodeField.setPromptText("Code de licence");

        pdfLabel = new Label("Aucun fichier sélectionné");
        Button choosePdfButton = new Button("Choisir un fichier PDF");
        choosePdfButton.setOnAction(event -> choosePdf());

        Button registerButton = new Button("S'inscrire");
        registerButton.setOnAction(event -> handleRegister(stage));

        root.getChildren().addAll(licenseCodeField, pdfLabel, choosePdfButton, registerButton);

        Scene scene = new Scene(root, 400, 300);
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

    private void handleRegister(Stage stage) {
        try {
            String licenseCode = licenseCodeField.getText().trim();

            if (licenseCode.isEmpty() || selectedPdf == null) {
                showAlert("Erreur", "Tous les champs sont obligatoires.");
                return;
            }

            Doctor doctor = new Doctor();
            doctor.setUserId(receivedUser.getId());
            doctor.setLicenseCode(licenseCode);
            doctor.setCertified(false);

            serviceDoctor.ajouter(doctor);
            serviceDoctorDocument.uploadDocument(doctor.getId(), selectedPdf);

            showInfo("Succès", "Inscription réussie ! Votre dossier est en attente de validation par un administrateur.");
            stage.close();

            // Retour au login
            javafx.application.Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
                    Parent root = loader.load();
                    Stage loginStage = new Stage();
                    loginStage.setTitle("MedTimeFX — Connexion");
                    loginStage.setScene(new Scene(root, 980, 720));
                    loginStage.show();
                } catch (Exception ex) {
                    System.err.println("Erreur ouverture login: " + ex.getMessage());
                }
            });
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
