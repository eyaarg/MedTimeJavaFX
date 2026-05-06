package esprit.fx.controllers;

import esprit.fx.entities.Disponibilite;
import esprit.fx.entities.User;
import esprit.fx.services.OpenStreetMapService;
import esprit.fx.services.ServiceDisponibilite;
import esprit.fx.services.ServiceDoctor;
import esprit.fx.services.ServiceUser;
import esprit.fx.entities.Doctor;
import esprit.fx.utils.NotificationUtil;
import esprit.fx.utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class DisponibiliteController implements Initializable {

    @FXML private VBox containerDisponibilites;
    @FXML private Button btnAjouter;
    @FXML private Button btnActualiser;
    @FXML private CheckBox filterDisponible;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;

    private ServiceDisponibilite serviceDisponibilite;
    private ServiceUser serviceUser;
    private ServiceDoctor serviceDoctor;
    private String currentUserRole;
    private int currentUserId;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        serviceDisponibilite = new ServiceDisponibilite();
        serviceUser = new ServiceUser();
        serviceDoctor = new ServiceDoctor();

        User currentUser = UserSession.getCurrentUser();
        currentUserRole = UserSession.getCurrentRole();
        currentUserId = currentUser != null ? currentUser.getId() : 0;

        initializeFilters();
        configureButtonsBasedOnRole();
        chargerDisponibilites();
    }

    private void initializeFilters() {
        filterDisponible.setSelected(true);

        sortCombo.getItems().addAll(
                "Date (Croissant)",
                "Date (Decroissant)",
                "Medecin (A-Z)",
                "Medecin (Z-A)"
        );
        sortCombo.setValue("Date (Croissant)");

        filterDisponible.setOnAction(e -> chargerDisponibilites());
        searchField.textProperty().addListener(
                (obs, oldText, newText) -> chargerDisponibilites()
        );
        sortCombo.setOnAction(e -> chargerDisponibilites());
    }

    private void configureButtonsBasedOnRole() {
        if ("PATIENT".equals(currentUserRole)) {
            btnAjouter.setVisible(false);
        }
    }

    private void chargerDisponibilites() {
        try {
            List<Disponibilite> dispos;

            if ("DOCTOR".equals(currentUserRole)) {
                dispos = serviceDisponibilite.getDisponibilitesParDocteur(currentUserId);
            } else {
                dispos = serviceDisponibilite.getAll();
            }

            // Filtrer par disponibilit├®
            if (filterDisponible.isSelected()) {
                dispos = dispos.stream()
                        .filter(Disponibilite::isEstDisponible)
                        .toList();
            }

            // Ô£à Recherche par m├®decin, date, notes (sans ID)
            String recherche = searchField.getText().toLowerCase().trim();
            if (!recherche.isEmpty()) {
                dispos = dispos.stream()
                        .filter(dispo -> {
                            // Recherche par nom de m├®decin
                            if (dispo.getDoctorNom() != null &&
                                    dispo.getDoctorNom().toLowerCase().contains(recherche)) {
                                return true;
                            }
                            // Recherche par date d├®but
                            if (dispo.getDateDebut() != null &&
                                    dispo.getDateDebut()
                                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                            .contains(recherche)) {
                                return true;
                            }
                            // Recherche par notes
                            if (dispo.getNotes() != null &&
                                    dispo.getNotes().toLowerCase().contains(recherche)) {
                                return true;
                            }
                            return false;
                        })
                        .toList();
            }

            String sortOption = sortCombo.getValue();
            if (sortOption != null) {
                switch (sortOption) {
                    case "Date (Croissant)":
                        dispos = dispos.stream()
                                .sorted((d1, d2) -> {
                                    if (d1.getDateDebut() == null) return 1;
                                    if (d2.getDateDebut() == null) return -1;
                                    return d1.getDateDebut().compareTo(d2.getDateDebut());
                                }).toList();
                        break;
                    case "Date (Decroissant)":
                        dispos = dispos.stream()
                                .sorted((d1, d2) -> {
                                    if (d1.getDateDebut() == null) return 1;
                                    if (d2.getDateDebut() == null) return -1;
                                    return d2.getDateDebut().compareTo(d1.getDateDebut());
                                }).toList();
                        break;
                    case "Medecin (A-Z)":
                        dispos = dispos.stream()
                                .sorted((d1, d2) -> {
                                    if (d1.getDoctorNom() == null) return 1;
                                    if (d2.getDoctorNom() == null) return -1;
                                    return d1.getDoctorNom().compareToIgnoreCase(d2.getDoctorNom());
                                }).toList();
                        break;
                    case "Medecin (Z-A)":
                        dispos = dispos.stream()
                                .sorted((d1, d2) -> {
                                    if (d1.getDoctorNom() == null) return 1;
                                    if (d2.getDoctorNom() == null) return -1;
                                    return d2.getDoctorNom().compareToIgnoreCase(d1.getDoctorNom());
                                }).toList();
                        break;
                }
            }

            containerDisponibilites.getChildren().clear();

            if (dispos.isEmpty()) {
                Label emptyLabel = new Label("Aucune disponibilit├® trouv├®e");
                emptyLabel.setStyle(
                        "-fx-font-size: 16px; -fx-text-fill: #6b7280; -fx-padding: 40;"
                );
                containerDisponibilites.getChildren().add(emptyLabel);
            } else {
                for (Disponibilite dispo : dispos) {
                    containerDisponibilites.getChildren()
                            .add(createDisponibiliteCard(dispo));
                }
            }

        } catch (SQLException e) {
            Stage stage = (Stage) containerDisponibilites.getScene().getWindow();
            if (stage != null) {
                NotificationUtil.showNotification(
                        stage,
                        "Impossible de charger les disponibilit├®s: " + e.getMessage(),
                        NotificationUtil.NotificationType.ERROR
                );
            }
        }
    }

    @FXML
    private void ajouterDisponibilite() {
        ouvrirFormulaireDisponibilite(null);
    }

    private VBox createDisponibiliteCard(Disponibilite dispo) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);"
        );

        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox infoBox = new VBox(5);
        Label nomLabel = new Label("Dr. " + dispo.getDoctorNom());
        nomLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
        infoBox.getChildren().add(nomLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Badge statut
        Label statutBadge = new Label();
        if (dispo.isEstDisponible()) {
            statutBadge.setText("Disponible");
            statutBadge.setStyle(
                    "-fx-background-color: #d1fae5; -fx-text-fill: #065f46;" +
                    "-fx-padding: 6 12; -fx-background-radius: 20;" +
                    "-fx-font-size: 12px; -fx-font-weight: bold;"
            );
        } else {
            statutBadge.setText("Occupe");
            statutBadge.setStyle(
                    "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;" +
                    "-fx-padding: 6 12; -fx-background-radius: 20;" +
                    "-fx-font-size: 12px; -fx-font-weight: bold;"
            );
        }

        header.getChildren().addAll(infoBox, statutBadge);

        // Dates
        HBox datesBox = new HBox(20);
        datesBox.setStyle("-fx-background-color: #f9fafb; -fx-padding: 12; -fx-background-radius: 8;");

        VBox debutBox = new VBox(4);
        Label debutTitle = new Label("Debut");
        debutTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280; -fx-font-weight: 600;");
        Label debutValue = new Label(dispo.getDateDebut() != null ?
                dispo.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A");
        debutValue.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151; -fx-font-weight: 600;");
        debutBox.getChildren().addAll(debutTitle, debutValue);

        VBox finBox = new VBox(4);
        Label finTitle = new Label("Fin");
        finTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280; -fx-font-weight: 600;");
        Label finValue = new Label(dispo.getDateFin() != null ?
                dispo.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A");
        finValue.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151; -fx-font-weight: 600;");
        finBox.getChildren().addAll(finTitle, finValue);

        datesBox.getChildren().addAll(debutBox, finBox);
        card.getChildren().addAll(header, datesBox);

        // Notes
        if (dispo.getNotes() != null && !dispo.getNotes().isEmpty()) {
            Label notesLabel = new Label(dispo.getNotes());
            notesLabel.setWrapText(true);
            notesLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");
            card.getChildren().add(notesLabel);
        }

        // Boutons
        if (!"PATIENT".equals(currentUserRole)) {
            HBox actionsBox = new HBox(10);
            actionsBox.setAlignment(Pos.CENTER_RIGHT);

            Button btnLocalisation = new Button("Localisation");
            btnLocalisation.setStyle(
                    "-fx-background-color: #10b981; -fx-text-fill: white;" +
                    "-fx-font-weight: 600; -fx-padding: 10 20;" +
                    "-fx-background-radius: 8; -fx-cursor: hand;"
            );
            btnLocalisation.setOnAction(e -> ouvrirCarte(dispo));

            Button btnModifier = new Button("Modifier");
            btnModifier.setStyle(
                    "-fx-background-color: #3b82f6; -fx-text-fill: white;" +
                    "-fx-font-weight: 600; -fx-padding: 10 20;" +
                    "-fx-background-radius: 8; -fx-cursor: hand;"
            );
            btnModifier.setOnAction(e -> modifierDisponibilite(dispo));

            Button btnSupprimer = new Button("Supprimer");
            btnSupprimer.setStyle(
                    "-fx-background-color: #ef4444; -fx-text-fill: white;" +
                    "-fx-font-weight: 600; -fx-padding: 10 20;" +
                    "-fx-background-radius: 8; -fx-cursor: hand;"
            );
            btnSupprimer.setOnAction(e -> supprimerDisponibilite(dispo));

            actionsBox.getChildren().addAll(btnLocalisation, btnModifier, btnSupprimer);
            card.getChildren().add(actionsBox);
        } else {
            HBox patientActionsBox = new HBox(10);
            patientActionsBox.setAlignment(Pos.CENTER_RIGHT);

            Button btnLocalisationPatient = new Button("Voir le cabinet");
            btnLocalisationPatient.setStyle(
                    "-fx-background-color: #10b981; -fx-text-fill: white;" +
                    "-fx-font-weight: 600; -fx-padding: 10 20;" +
                    "-fx-background-radius: 8; -fx-cursor: hand;"
            );
            btnLocalisationPatient.setOnAction(e -> ouvrirCarte(dispo));
            patientActionsBox.getChildren().add(btnLocalisationPatient);
            card.getChildren().add(patientActionsBox);
        }

        return card;
    }

    @FXML
    private void modifierDisponibilite() {
        // Non utilis├®e ÔÇö boutons dans les cartes
    }

    private void modifierDisponibilite(Disponibilite dispo) {
        try {
            Disponibilite fullDispo = serviceDisponibilite.afficherParId(dispo.getId());
            if (fullDispo != null) {
                ouvrirFormulaireDisponibilite(fullDispo);
            }
        } catch (SQLException e) {
            Stage stage = (Stage) containerDisponibilites.getScene().getWindow();
            NotificationUtil.showNotification(
                    stage,
                    "Erreur lors du chargement: " + e.getMessage(),
                    NotificationUtil.NotificationType.ERROR
            );
        }
    }

    @FXML
    private void supprimerDisponibilite() {
        // Non utilis├®e ÔÇö boutons dans les cartes
    }

    private void supprimerDisponibilite(Disponibilite dispo) {
        Stage stage = (Stage) containerDisponibilites.getScene().getWindow();
        String details = "M├®decin: " + dispo.getDoctorNom() + "\n" +
                "Date: " + (dispo.getDateDebut() != null ?
                dispo.getDateDebut().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy ├á HH:mm")) : "N/A");

        boolean confirmed = NotificationUtil.showConfirmation(
                stage,
                "Supprimer la disponibilit├®",
                "├ètes-vous s├╗r de vouloir supprimer cette disponibilit├®?",
                details
        );

        if (confirmed) {
            try {
                serviceDisponibilite.supprimer(dispo.getId());
                NotificationUtil.showNotification(
                        stage,
                        "Disponibilit├® supprim├®e avec succ├¿s",
                        NotificationUtil.NotificationType.SUCCESS
                );
                chargerDisponibilites();
            } catch (SQLException e) {
                String message = e.getMessage();
                if (message.contains("foreign key constraint") ||
                        message.contains("rendez-vous")) {
                    NotificationUtil.showNotification(
                            stage,
                            "Cette disponibilit├® est li├®e ├á des rendez-vous existants",
                            NotificationUtil.NotificationType.ERROR
                    );
                } else {
                    NotificationUtil.showNotification(
                            stage,
                            "Impossible de supprimer: " + message,
                            NotificationUtil.NotificationType.ERROR
                    );
                }
            }
        }
    }

    @FXML
    private void actualiserListe() {
        chargerDisponibilites();
    }

    /** Ouvre la fen├¬tre carte OpenStreetMap pour localiser le cabinet du m├®decin. */
    private void ouvrirCarte(Disponibilite dispo) {
        try {
            // R├®cup├®rer l'adresse du m├®decin depuis la table doctors
            String adresse = "";
            try {
                Doctor doctor = serviceDoctor.afficherParId(dispo.getDoctorId());
                if (doctor != null && doctor.getAdresse() != null) {
                    adresse = doctor.getAdresse();
                }
            } catch (SQLException e) {
                System.err.println("Adresse m├®decin non trouv├®e: " + e.getMessage());
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/Carte.fxml")
            );
            Parent root = loader.load();

            CarteController carteController = loader.getController();
            carteController.initCarte(
                    dispo.getDoctorNom() != null ? dispo.getDoctorNom() : "M├®decin",
                    adresse
            );

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("­ƒôì Localisation ÔÇö Cabinet Dr. " + dispo.getDoctorNom());
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.show();

        } catch (IOException e) {
            Stage stage = (Stage) containerDisponibilites.getScene().getWindow();
            NotificationUtil.showNotification(
                    stage,
                    "Impossible d'ouvrir la carte: " + e.getMessage(),
                    NotificationUtil.NotificationType.ERROR
            );
        }
    }

    private void ouvrirFormulaireDisponibilite(Disponibilite disponibilite) {
        try {
            creerFormulaireSimple(disponibilite);
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    private void creerFormulaireSimple(Disponibilite disponibilite) {
        Dialog<Disponibilite> dialog = new Dialog<>();
        dialog.setTitle(disponibilite == null ?
                "Ô×ò Nouvelle Disponibilit├®" : "Ô£Å´©Å Modifier Disponibilit├®");

        dialog.getDialogPane().setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-padding: 0;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 5);"
        );

        ButtonType saveButtonType = new ButtonType(
                "­ƒÆ¥ Sauvegarder", ButtonBar.ButtonData.OK_DONE
        );
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        VBox mainContainer = new VBox(0);
        mainContainer.setStyle("-fx-background-color: white; -fx-background-radius: 16;");

        VBox header = new VBox(8);
        header.setStyle(
                "-fx-background-color: linear-gradient(to right, #667eea 0%, #764ba2 100%);" +
                        "-fx-padding: 25;" +
                        "-fx-background-radius: 16 16 0 0;"
        );
        Label titleLabel = new Label(disponibilite == null ?
                "Ô×ò Nouvelle Disponibilit├®" : "Ô£Å´©Å Modifier Disponibilit├®");
        titleLabel.setStyle(
                "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;"
        );
        Label subtitleLabel = new Label("Remplissez tous les champs obligatoires");
        subtitleLabel.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.9);"
        );
        header.getChildren().addAll(titleLabel, subtitleLabel);

        VBox formContainer = new VBox(20);
        formContainer.setPadding(new Insets(30));
        formContainer.setStyle("-fx-background-color: white;");

        String inputStyle =
                "-fx-background-color: #f9fafb;" +
                        "-fx-border-color: #e5e7eb;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 12;" +
                        "-fx-font-size: 14px;";

        String labelStyle =
                "-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #374151;";
        String errorStyle =
                "-fx-font-size: 12px; -fx-text-fill: #ef4444; -fx-padding: 4 0 0 0;";

        // Champ M├®decin
        VBox doctorBox = new VBox(6);
        Label doctorLabel = new Label("M├®decin *");
        doctorLabel.setStyle(labelStyle);
        ComboBox<String> doctorCombo = new ComboBox<>();
        doctorCombo.setPromptText("S├®lectionner un m├®decin");
        doctorCombo.setPrefWidth(400);
        doctorCombo.setStyle(inputStyle);
        Label doctorError = new Label();
        doctorError.setStyle(errorStyle);
        doctorError.setVisible(false);
        doctorBox.getChildren().addAll(doctorLabel, doctorCombo, doctorError);

        try {
            List<User> doctors = serviceUser.getAllDoctors();
            if (!doctors.isEmpty()) {
                for (User doctor : doctors) {
                    doctorCombo.getItems().add(
                            doctor.getId() + " - " + doctor.getUsername()
                    );
                }
            } else {
                List<Disponibilite> existingDispos = serviceDisponibilite.getAll();
                java.util.Set<Integer> doctorIds = new java.util.HashSet<>();
                for (Disponibilite d : existingDispos) {
                    doctorIds.add(d.getDoctorId());
                }
                for (Integer id : doctorIds) {
                    doctorCombo.getItems().add(id + " - M├®decin " + id);
                }
            }
        } catch (Exception e) {
            doctorCombo.getItems().addAll("1 - M├®decin 1", "2 - M├®decin 2");
        }

        doctorCombo.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused &&
                    (doctorCombo.getValue() == null || doctorCombo.getValue().isEmpty())) {
                doctorError.setText("ÔÜá Le m├®decin est obligatoire");
                doctorError.setVisible(true);
                doctorCombo.setStyle(
                        inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;"
                );
            } else {
                doctorError.setVisible(false);
                doctorCombo.setStyle(inputStyle);
            }
        });

        // Date d├®but
        VBox dateDebutBox = new VBox(6);
        Label dateDebutLabel = new Label("Date et heure de d├®but *");
        dateDebutLabel.setStyle(labelStyle);
        HBox dateDebutInputs = new HBox(10);
        DatePicker dateDebutPicker = new DatePicker();
        dateDebutPicker.setPromptText("Date");
        dateDebutPicker.setPrefWidth(200);
        dateDebutPicker.setStyle(inputStyle);
        TextField heureDebutField = new TextField("09:00");
        heureDebutField.setPromptText("HH:mm");
        heureDebutField.setPrefWidth(100);
        heureDebutField.setStyle(inputStyle);
        dateDebutInputs.getChildren().addAll(dateDebutPicker, heureDebutField);
        Label dateDebutError = new Label();
        dateDebutError.setStyle(errorStyle);
        dateDebutError.setVisible(false);
        dateDebutBox.getChildren().addAll(dateDebutLabel, dateDebutInputs, dateDebutError);

        // Date fin
        VBox dateFinBox = new VBox(6);
        Label dateFinLabel = new Label("Date et heure de fin *");
        dateFinLabel.setStyle(labelStyle);
        HBox dateFinInputs = new HBox(10);
        DatePicker dateFinPicker = new DatePicker();
        dateFinPicker.setPromptText("Date");
        dateFinPicker.setPrefWidth(200);
        dateFinPicker.setStyle(inputStyle);
        TextField heureFinField = new TextField("17:00");
        heureFinField.setPromptText("HH:mm");
        heureFinField.setPrefWidth(100);
        heureFinField.setStyle(inputStyle);
        dateFinInputs.getChildren().addAll(dateFinPicker, heureFinField);
        Label dateFinError = new Label();
        dateFinError.setStyle(errorStyle);
        dateFinError.setVisible(false);
        dateFinBox.getChildren().addAll(dateFinLabel, dateFinInputs, dateFinError);

        // Statut
        VBox statutBox = new VBox(6);
        Label statutLabel = new Label("Statut");
        statutLabel.setStyle(labelStyle);
        CheckBox disponibleCheck = new CheckBox("Ô£ô Disponible");
        disponibleCheck.setSelected(true);
        disponibleCheck.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151;");
        statutBox.getChildren().addAll(statutLabel, disponibleCheck);

        // Notes
        VBox notesBox = new VBox(6);
        Label notesLabel = new Label("Notes");
        notesLabel.setStyle(labelStyle);
        TextArea notesArea = new TextArea();
        notesArea.setPromptText(
                "Ajoutez des notes suppl├®mentaires (minimum 4 caract├¿res)..."
        );
        notesArea.setPrefRowCount(3);
        notesArea.setStyle(inputStyle);
        Label notesError = new Label();
        notesError.setStyle(errorStyle);
        notesError.setVisible(false);
        notesBox.getChildren().addAll(notesLabel, notesArea, notesError);

        notesArea.textProperty().addListener((obs, oldVal, newVal) -> {
            String text = newVal.trim();
            if (!text.isEmpty() && text.length() < 4) {
                notesError.setText("ÔÜá Les notes doivent contenir au moins 4 caract├¿res");
                notesError.setVisible(true);
                notesArea.setStyle(
                        inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;"
                );
            } else {
                notesError.setVisible(false);
                notesArea.setStyle(inputStyle);
            }
        });

        // Remplir si modification
        if (disponibilite != null) {
            for (String item : doctorCombo.getItems()) {
                if (item.startsWith(disponibilite.getDoctorId() + " ")) {
                    doctorCombo.setValue(item);
                    break;
                }
            }
            if (disponibilite.getDateDebut() != null) {
                dateDebutPicker.setValue(disponibilite.getDateDebut().toLocalDate());
                heureDebutField.setText(disponibilite.getDateDebut()
                        .format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            if (disponibilite.getDateFin() != null) {
                dateFinPicker.setValue(disponibilite.getDateFin().toLocalDate());
                heureFinField.setText(disponibilite.getDateFin()
                        .format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            disponibleCheck.setSelected(disponibilite.isEstDisponible());
            notesArea.setText(
                    disponibilite.getNotes() != null ? disponibilite.getNotes() : ""
            );
        } else {
            dateDebutPicker.setValue(java.time.LocalDate.now());
            dateFinPicker.setValue(java.time.LocalDate.now());
        }

        formContainer.getChildren().addAll(
                doctorBox, dateDebutBox, dateFinBox, statutBox, notesBox
        );
        mainContainer.getChildren().addAll(header, formContainer);
        dialog.getDialogPane().setContent(mainContainer);

        dialog.getDialogPane().lookupButton(saveButtonType).setStyle(
                "-fx-background-color: #10b981;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: 600;" +
                        "-fx-padding: 12 24;" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-size: 14px;" +
                        "-fx-cursor: hand;"
        );

        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle(
                "-fx-background-color: #f3f4f6;" +
                        "-fx-text-fill: #374151;" +
                        "-fx-font-weight: 600;" +
                        "-fx-padding: 12 24;" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-size: 14px;" +
                        "-fx-cursor: hand;"
        );

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    boolean isValid = true;

                    if (doctorCombo.getValue() == null ||
                            doctorCombo.getValue().isEmpty()) {
                        doctorError.setText("ÔÜá Le m├®decin est obligatoire");
                        doctorError.setVisible(true);
                        isValid = false;
                    }

                    if (dateDebutPicker.getValue() == null) {
                        dateDebutError.setText("ÔÜá La date de d├®but est obligatoire");
                        dateDebutError.setVisible(true);
                        isValid = false;
                    }

                    if (!heureDebutField.getText()
                            .matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                        dateDebutError.setText(
                                "ÔÜá Format d'heure invalide (HH:mm, ex: 09:00)"
                        );
                        dateDebutError.setVisible(true);
                        isValid = false;
                    }

                    if (dateFinPicker.getValue() == null) {
                        dateFinError.setText("ÔÜá La date de fin est obligatoire");
                        dateFinError.setVisible(true);
                        isValid = false;
                    }

                    if (!heureFinField.getText()
                            .matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                        dateFinError.setText(
                                "ÔÜá Format d'heure invalide (HH:mm, ex: 17:00)"
                        );
                        dateFinError.setVisible(true);
                        isValid = false;
                    }

                    String notesText = notesArea.getText().trim();
                    if (!notesText.isEmpty() && notesText.length() < 4) {
                        notesError.setText(
                                "ÔÜá Les notes doivent contenir au moins 4 caract├¿res"
                        );
                        notesError.setVisible(true);
                        isValid = false;
                    }

                    if (!isValid) return null;

                    int doctorId = Integer.parseInt(
                            doctorCombo.getValue().split(" - ")[0]
                    );

                    String[] heureDebutParts = heureDebutField.getText().split(":");
                    String[] heureFinParts = heureFinField.getText().split(":");

                    java.time.LocalDateTime dateDebut = dateDebutPicker.getValue()
                            .atTime(
                                    Integer.parseInt(heureDebutParts[0]),
                                    Integer.parseInt(heureDebutParts[1])
                            );
                    java.time.LocalDateTime dateFin = dateFinPicker.getValue()
                            .atTime(
                                    Integer.parseInt(heureFinParts[0]),
                                    Integer.parseInt(heureFinParts[1])
                            );

                    if (dateFin.isBefore(dateDebut) || dateFin.isEqual(dateDebut)) {
                        dateFinError.setText(
                                "ÔÜá La date de fin doit ├¬tre sup├®rieure ├á la date de d├®but"
                        );
                        dateFinError.setVisible(true);
                        return null;
                    }

                    Disponibilite result = disponibilite != null ?
                            disponibilite : new Disponibilite();
                    result.setDoctorId(doctorId);
                    result.setDateDebut(dateDebut);
                    result.setDateFin(dateFin);
                    result.setEstDisponible(disponibleCheck.isSelected());
                    result.setNotes(notesText.isEmpty() ? null : notesText);

                    return result;

                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                Stage stage = (Stage) containerDisponibilites.getScene().getWindow();
                if (disponibilite == null) {
                    serviceDisponibilite.ajouter(result);
                    NotificationUtil.showNotification(
                            stage,
                            "Disponibilit├® ajout├®e avec succ├¿s",
                            NotificationUtil.NotificationType.SUCCESS
                    );
                } else {
                    serviceDisponibilite.modifier(result);
                    NotificationUtil.showNotification(
                            stage,
                            "Disponibilit├® modifi├®e avec succ├¿s",
                            NotificationUtil.NotificationType.SUCCESS
                    );
                }
                chargerDisponibilites();
            } catch (SQLException e) {
                Stage stage = (Stage) containerDisponibilites.getScene().getWindow();
                NotificationUtil.showNotification(
                        stage,
                        "Erreur: " + e.getMessage(),
                        NotificationUtil.NotificationType.ERROR
                );
            }
        });
    }

    public void rafraichir() {
        chargerDisponibilites();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
