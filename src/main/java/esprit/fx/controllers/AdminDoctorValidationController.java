package esprit.fx.controllers;

import esprit.fx.entities.Doctor;
import esprit.fx.entities.Doctor_documents;
import esprit.fx.services.ServiceDoctor;
import esprit.fx.services.ServiceDoctorDocument;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class AdminDoctorValidationController {

    private final ServiceDoctor serviceDoctor = new ServiceDoctor();
    private final ServiceDoctorDocument serviceDoctorDocument = new ServiceDoctorDocument();
    private TableView<Doctor> doctorTable;
    private Label pendingDoctorsLabel;

    public static void showAsStage() {
        AdminDoctorValidationController controller = new AdminDoctorValidationController();
        Stage stage = new Stage();
        stage.setTitle("Validation des M├®decins");
        controller.initialize(stage);
        stage.show();
    }

    private void initialize(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        // Label M├®decins en attente
        pendingDoctorsLabel = new Label();
        updatePendingDoctorsLabel();

        // Bouton Rafra├«chir
        Button refreshButton = new Button("Rafra├«chir");
        refreshButton.setOnAction(e -> refreshTable());

        HBox topBox = new HBox(10, pendingDoctorsLabel, refreshButton);
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.setPadding(new Insets(10));

        // TableView
        doctorTable = new TableView<>();
        setupTableColumns();
        refreshTable();

        root.setTop(topBox);
        root.setCenter(doctorTable);

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
    }

    private void setupTableColumns() {
        TableColumn<Doctor, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Doctor, String> usernameColumn = new TableColumn<>("Username");
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<Doctor, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<Doctor, String> phoneColumn = new TableColumn<>("Phone");
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));

        TableColumn<Doctor, String> licenseCodeColumn = new TableColumn<>("License Code");
        licenseCodeColumn.setCellValueFactory(new PropertyValueFactory<>("licenseCode"));

        TableColumn<Doctor, String> registrationDateColumn = new TableColumn<>("Date inscription");
        registrationDateColumn.setCellValueFactory(new PropertyValueFactory<>("registrationDate"));

        TableColumn<Doctor, Void> actionsColumn = new TableColumn<>("Actions");
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewPdfButton = new Button("Voir PDF");
            private final Button approveButton = new Button("Approuver");
            private final Button rejectButton = new Button("Refuser");

            {
                viewPdfButton.setOnAction(e -> viewPdf(getTableView().getItems().get(getIndex())));
                approveButton.setOnAction(e -> approveDoctor(getTableView().getItems().get(getIndex())));
                rejectButton.setOnAction(e -> rejectDoctor(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, viewPdfButton, approveButton, rejectButton);
                    setGraphic(buttons);
                }
            }
        });

        doctorTable.getColumns().addAll(idColumn, usernameColumn, emailColumn, phoneColumn, licenseCodeColumn, registrationDateColumn, actionsColumn);
    }

    private void refreshTable() {
        try {
            List<Doctor> pendingDoctors = serviceDoctor.getDoctorsPendingVerification();
            ObservableList<Doctor> doctorList = FXCollections.observableArrayList(pendingDoctors);
            doctorTable.setItems(doctorList);
            updatePendingDoctorsLabel();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de rafra├«chir la table : " + e.getMessage());
        }
    }

    private void updatePendingDoctorsLabel() {
        try {
            int count = serviceDoctor.getPendingDoctorsCount();
            pendingDoctorsLabel.setText("M├®decins en attente : " + count);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de mettre ├á jour le label : " + e.getMessage());
        }
    }

    private void viewPdf(Doctor doctor) {
        // doctor.getId() = doctor_id dans la table doctors
        Doctor_documents doc;
        try {
            doc = serviceDoctorDocument.getLatestDocumentByDoctorId(doctor.getId());
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la r├®cup├®ration du document : " + e.getMessage());
            return;
        }

        if (doc == null) {
            showAlert("Fichier PDF introuvable", "Le m├®decin n'a pas encore upload├® son document.");
            return;
        }

        File pdfFile = new File(doc.getFolder_name() + "/" + doc.getStored_name());
        if (!pdfFile.exists()) {
            showAlert("Fichier PDF introuvable", "Le m├®decin n'a pas encore upload├® son document.");
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            showAlert("Erreur", "Impossible d'ouvrir le PDF sur ce syst├¿me.");
            return;
        }

        try {
            Desktop.getDesktop().open(pdfFile);
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le fichier PDF : " + e.getMessage());
        }
    }

    private void approveDoctor(Doctor doctor) {
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirmation");
        confirmationAlert.setHeaderText(null);
        confirmationAlert.setContentText("├ètes-vous s├╗r de vouloir approuver ce m├®decin ?");

        if (confirmationAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                serviceDoctor.approveDoctorCertification(doctor.getUserId());
                refreshTable();
                showInfo("Succ├¿s", "M├®decin approuv├®. Un email de confirmation a ├®t├® envoy├®.");
            } catch (SQLException e) {
                showAlert("Erreur", "Impossible d'approuver le m├®decin : " + e.getMessage());
            }
        }
    }

    private void rejectDoctor(Doctor doctor) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Refuser le m├®decin");
        dialog.setHeaderText("Veuillez fournir un motif de refus.");

        TextArea reasonField = new TextArea();
        reasonField.setPromptText("Motif du refus");

        dialog.getDialogPane().setContent(reasonField);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return reasonField.getText();
            }
            return null;
        });

        String reason = dialog.showAndWait().orElse(null);
        if (reason != null && !reason.trim().isEmpty()) {
            try {
                serviceDoctor.rejectDoctorCertification(doctor.getUserId(), reason.trim());
                refreshTable();
                showInfo("Refus enregistr├®", "Le m├®decin a ├®t├® notifi├® par email.");
            } catch (SQLException e) {
                showAlert("Erreur", "Impossible de refuser le m├®decin : " + e.getMessage());
            }
        } else if (reason != null) {
            showAlert("Erreur", "Le motif de refus est obligatoire.");
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
