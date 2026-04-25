package esprit.fx.controllers;

import esprit.fx.entities.Disponibilite;
import esprit.fx.entities.User;
import esprit.fx.services.ServiceDisponibilite;
import esprit.fx.services.ServiceUser;
import esprit.fx.utils.NotificationUtil;
import esprit.fx.utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

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
    private String currentUserRole;
    private int currentUserId;
    private Disponibilite selectedDisponibilite;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        serviceDisponibilite = new ServiceDisponibilite();
        serviceUser = new ServiceUser();
        
        // Récupérer les informations de l'utilisateur connecté
        User currentUser = UserSession.getCurrentUser();
        currentUserRole = UserSession.getCurrentRole();
        currentUserId = currentUser != null ? currentUser.getId() : 0;
        
        initializeFilters();
        configureButtonsBasedOnRole();
        chargerDisponibilites();
    }

    private void initializeFilters() {
        filterDisponible.setSelected(true);
        
        // Initialiser le ComboBox de tri
        sortCombo.getItems().addAll(
            "ID (Croissant)",
            "ID (Décroissant)"
        );
        sortCombo.setValue("ID (Croissant)");
        
        filterDisponible.setOnAction(e -> chargerDisponibilites());
        searchField.textProperty().addListener((obs, oldText, newText) -> chargerDisponibilites());
        sortCombo.setOnAction(e -> chargerDisponibilites());
    }

    private void configureButtonsBasedOnRole() {
        if ("PATIENT".equals(currentUserRole)) {
            // Les patients peuvent seulement voir les disponibilités
            btnAjouter.setVisible(false);
        }
        // Les médecins et admins peuvent tout gérer
    }

    private void chargerDisponibilites() {
        try {
            List<Disponibilite> dispos;
            
            if ("DOCTOR".equals(currentUserRole)) {
                dispos = serviceDisponibilite.getDisponibilitesParDocteur(currentUserId);
            } else {
                dispos = serviceDisponibilite.getAll();
            }
            
            // Filtrer par disponibilité
            if (filterDisponible.isSelected()) {
                dispos = dispos.stream()
                    .filter(Disponibilite::isEstDisponible)
                    .toList();
            }
            
            // Filtrer par recherche (ID ou médecin)
            String recherche = searchField.getText().toLowerCase().trim();
            if (!recherche.isEmpty()) {
                dispos = dispos.stream()
                    .filter(dispo -> {
                        // Recherche par ID
                        if (String.valueOf(dispo.getId()).contains(recherche)) {
                            return true;
                        }
                        // Recherche par nom de médecin
                        if (dispo.getDoctorNom() != null && dispo.getDoctorNom().toLowerCase().contains(recherche)) {
                            return true;
                        }
                        // Recherche par notes
                        if (dispo.getNotes() != null && dispo.getNotes().toLowerCase().contains(recherche)) {
                            return true;
                        }
                        return false;
                    })
                    .toList();
            }
            
            // Trier selon le choix
            String sortOption = sortCombo.getValue();
            if (sortOption != null) {
                if (sortOption.equals("ID (Croissant)")) {
                    dispos = dispos.stream()
                        .sorted((d1, d2) -> Integer.compare(d1.getId(), d2.getId()))
                        .toList();
                } else if (sortOption.equals("ID (Décroissant)")) {
                    dispos = dispos.stream()
                        .sorted((d1, d2) -> Integer.compare(d2.getId(), d1.getId()))
                        .toList();
                }
            }
            
            containerDisponibilites.getChildren().clear();
            
            if (dispos.isEmpty()) {
                Label emptyLabel = new Label("Aucune disponibilité trouvée");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b7280; -fx-padding: 40;");
                containerDisponibilites.getChildren().add(emptyLabel);
            } else {
                for (Disponibilite dispo : dispos) {
                    containerDisponibilites.getChildren().add(createDisponibiliteCard(dispo));
                }
            }
            
        } catch (SQLException e) {
            Stage stage = (Stage) containerDisponibilites.getScene().getWindow();
            if (stage != null) {
                NotificationUtil.showNotification(
                    stage,
                    "Impossible de charger les disponibilités: " + e.getMessage(),
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
        
        Label iconLabel = new Label("👨‍⚕️");
        iconLabel.setStyle("-fx-font-size: 32px;");
        
        VBox infoBox = new VBox(5);
        Label nomLabel = new Label("Dr. " + dispo.getDoctorNom());
        nomLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
        
        Label idLabel = new Label("ID: " + dispo.getId());
        idLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #9ca3af;");
        
        infoBox.getChildren().addAll(nomLabel, idLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        
        // Badge statut
        Label statutBadge = new Label();
        if (dispo.isEstDisponible()) {
            statutBadge.setText("✓ Disponible");
            statutBadge.setStyle(
                "-fx-background-color: #d1fae5;" +
                "-fx-text-fill: #065f46;" +
                "-fx-padding: 6 12;" +
                "-fx-background-radius: 20;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;"
            );
        } else {
            statutBadge.setText("✗ Occupé");
            statutBadge.setStyle(
                "-fx-background-color: #fee2e2;" +
                "-fx-text-fill: #991b1b;" +
                "-fx-padding: 6 12;" +
                "-fx-background-radius: 20;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;"
            );
        }
        
        header.getChildren().addAll(iconLabel, infoBox, statutBadge);
        
        // Dates
        HBox datesBox = new HBox(20);
        datesBox.setStyle("-fx-background-color: #f9fafb; -fx-padding: 12; -fx-background-radius: 8;");
        
        VBox debutBox = new VBox(4);
        Label debutTitle = new Label("📅 Début");
        debutTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280; -fx-font-weight: 600;");
        Label debutValue = new Label(dispo.getDateDebut() != null ? 
            dispo.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A");
        debutValue.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151; -fx-font-weight: 600;");
        debutBox.getChildren().addAll(debutTitle, debutValue);
        
        VBox finBox = new VBox(4);
        Label finTitle = new Label("🕐 Fin");
        finTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280; -fx-font-weight: 600;");
        Label finValue = new Label(dispo.getDateFin() != null ? 
            dispo.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A");
        finValue.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151; -fx-font-weight: 600;");
        finBox.getChildren().addAll(finTitle, finValue);
        
        datesBox.getChildren().addAll(debutBox, finBox);
        
        card.getChildren().addAll(header, datesBox);
        
        // Notes
        if (dispo.getNotes() != null && !dispo.getNotes().isEmpty()) {
            Label notesLabel = new Label("📝 " + dispo.getNotes());
            notesLabel.setWrapText(true);
            notesLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");
            card.getChildren().add(notesLabel);
        }
        
        // Boutons d'action (seulement pour médecins/admins)
        if (!"PATIENT".equals(currentUserRole)) {
            HBox actionsBox = new HBox(10);
            actionsBox.setAlignment(Pos.CENTER_RIGHT);
            
            Button btnModifier = new Button("✏️ Modifier");
            btnModifier.setStyle(
                "-fx-background-color: #3b82f6;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: 600;" +
                "-fx-padding: 10 20;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;"
            );
            btnModifier.setOnAction(e -> modifierDisponibilite(dispo));
            
            Button btnSupprimer = new Button("🗑️ Supprimer");
            btnSupprimer.setStyle(
                "-fx-background-color: #ef4444;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: 600;" +
                "-fx-padding: 10 20;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;"
            );
            btnSupprimer.setOnAction(e -> supprimerDisponibilite(dispo));
            
            actionsBox.getChildren().addAll(btnModifier, btnSupprimer);
            card.getChildren().add(actionsBox);
        }
        
        return card;
    }

    @FXML
    private void modifierDisponibilite() {
        // Cette méthode n'est plus utilisée car les boutons sont dans les cartes
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
        // Cette méthode n'est plus utilisée car les boutons sont dans les cartes
    }
    
    private void supprimerDisponibilite(Disponibilite dispo) {
        Stage stage = (Stage) containerDisponibilites.getScene().getWindow();
        String details = "Médecin: " + dispo.getDoctorNom() + "\n" +
                        "Date: " + (dispo.getDateDebut() != null ? 
                            dispo.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) : "N/A");
        
        boolean confirmed = NotificationUtil.showConfirmation(
            stage,
            "Supprimer la disponibilité",
            "Êtes-vous sûr de vouloir supprimer cette disponibilité?",
            details
        );
        
        if (confirmed) {
            try {
                serviceDisponibilite.supprimer(dispo.getId());
                
                NotificationUtil.showNotification(
                    stage,
                    "Disponibilité supprimée avec succès",
                    NotificationUtil.NotificationType.SUCCESS
                );
                
                chargerDisponibilites();
            } catch (SQLException e) {
                String message = e.getMessage();
                if (message.contains("foreign key constraint") || message.contains("rendez-vous")) {
                    NotificationUtil.showNotification(
                        stage,
                        "Cette disponibilité est liée à des rendez-vous existants",
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

    private void ouvrirFormulaireDisponibilite(Disponibilite disponibilite) {
        try {
            // Créer un formulaire simple en code au lieu d'utiliser FXML
            creerFormulaireSimple(disponibilite);
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    private void creerFormulaireSimple(Disponibilite disponibilite) {
        Dialog<Disponibilite> dialog = new Dialog<>();
        dialog.setTitle(disponibilite == null ? "➕ Nouvelle Disponibilité" : "✏️ Modifier Disponibilité");
        
        // Style moderne pour le dialog
        dialog.getDialogPane().setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 16;" +
            "-fx-padding: 0;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 5);"
        );

        ButtonType saveButtonType = new ButtonType("💾 Sauvegarder", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Container principal avec header
        VBox mainContainer = new VBox(0);
        mainContainer.setStyle("-fx-background-color: white; -fx-background-radius: 16;");
        
        // Header avec gradient
        VBox header = new VBox(8);
        header.setStyle(
            "-fx-background-color: linear-gradient(to right, #667eea 0%, #764ba2 100%);" +
            "-fx-padding: 25;" +
            "-fx-background-radius: 16 16 0 0;"
        );
        Label titleLabel = new Label(disponibilite == null ? "➕ Nouvelle Disponibilité" : "✏️ Modifier Disponibilité");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subtitleLabel = new Label("Remplissez tous les champs obligatoires");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.9);");
        header.getChildren().addAll(titleLabel, subtitleLabel);
        
        // Formulaire
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
        
        String labelStyle = "-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #374151;";
        String errorStyle = "-fx-font-size: 12px; -fx-text-fill: #ef4444; -fx-padding: 4 0 0 0;";
        
        // Champ Médecin
        VBox doctorBox = new VBox(6);
        Label doctorLabel = new Label("Médecin *");
        doctorLabel.setStyle(labelStyle);
        ComboBox<String> doctorCombo = new ComboBox<>();
        doctorCombo.setPromptText("Sélectionner un médecin");
        doctorCombo.setPrefWidth(400);
        doctorCombo.setStyle(inputStyle);
        Label doctorError = new Label();
        doctorError.setStyle(errorStyle);
        doctorError.setVisible(false);
        doctorBox.getChildren().addAll(doctorLabel, doctorCombo, doctorError);
        
        // Charger les médecins
        try {
            ServiceUser serviceUser = new ServiceUser();
            List<User> doctors = serviceUser.getAllDoctors();
            
            if (!doctors.isEmpty()) {
                for (User doctor : doctors) {
                    doctorCombo.getItems().add(doctor.getId() + " - " + doctor.getUsername());
                }
            } else {
                List<Disponibilite> existingDispos = serviceDisponibilite.getAll();
                java.util.Set<Integer> doctorIds = new java.util.HashSet<>();
                for (Disponibilite dispo : existingDispos) {
                    doctorIds.add(dispo.getDoctorId());
                }
                for (Integer id : doctorIds) {
                    doctorCombo.getItems().add(id + " - Médecin " + id);
                }
            }
        } catch (Exception e) {
            doctorCombo.getItems().addAll("1 - Médecin 1", "2 - Médecin 2", "3 - Médecin 3");
        }
        
        // Validation médecin (obligatoire)
        doctorCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                doctorError.setText("⚠ Le médecin est obligatoire");
                doctorError.setVisible(true);
                doctorCombo.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
            } else {
                doctorError.setVisible(false);
                doctorCombo.setStyle(inputStyle);
            }
        });
        
        // Validation au focus perdu
        doctorCombo.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && (doctorCombo.getValue() == null || doctorCombo.getValue().isEmpty())) {
                doctorError.setText("⚠ Le médecin est obligatoire");
                doctorError.setVisible(true);
                doctorCombo.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
            }
        });
        
        // Date et heure début
        VBox dateDebutBox = new VBox(6);
        Label dateDebutLabel = new Label("Date et heure de début *");
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
        
        // Validation date début
        dateDebutPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                dateDebutError.setText("⚠ La date de début est obligatoire");
                dateDebutError.setVisible(true);
                dateDebutPicker.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
            } else {
                dateDebutError.setVisible(false);
                dateDebutPicker.setStyle(inputStyle);
            }
        });
        
        dateDebutPicker.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && dateDebutPicker.getValue() == null) {
                dateDebutError.setText("⚠ La date de début est obligatoire");
                dateDebutError.setVisible(true);
                dateDebutPicker.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
            }
        });
        
        // Validation heure début
        heureDebutField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                dateDebutError.setText("⚠ Format d'heure invalide (HH:mm, ex: 09:00)");
                dateDebutError.setVisible(true);
                heureDebutField.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
            } else {
                dateDebutError.setVisible(false);
                heureDebutField.setStyle(inputStyle);
            }
        });
        
        heureDebutField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && !heureDebutField.getText().matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                dateDebutError.setText("⚠ Format d'heure invalide (HH:mm, ex: 09:00)");
                dateDebutError.setVisible(true);
                heureDebutField.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
            }
        });
        
        // Date et heure fin
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
        
        // Validation date fin
        dateFinPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                dateFinError.setText("⚠ La date de fin est obligatoire");
                dateFinError.setVisible(true);
                dateFinPicker.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
            } else {
                // Vérifier que date fin > date début
                if (dateDebutPicker.getValue() != null && heureDebutField.getText().matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$") 
                    && heureFinField.getText().matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                    try {
                        String[] heureDebutParts = heureDebutField.getText().split(":");
                        String[] heureFinParts = heureFinField.getText().split(":");
                        
                        int heureDebut = Integer.parseInt(heureDebutParts[0]);
                        int minuteDebut = Integer.parseInt(heureDebutParts[1]);
                        int heureFin = Integer.parseInt(heureFinParts[0]);
                        int minuteFin = Integer.parseInt(heureFinParts[1]);
                        
                        java.time.LocalDateTime dateDebut = dateDebutPicker.getValue().atTime(heureDebut, minuteDebut);
                        java.time.LocalDateTime dateFin = newVal.atTime(heureFin, minuteFin);
                        
                        if (dateFin.isBefore(dateDebut) || dateFin.isEqual(dateDebut)) {
                            dateFinError.setText("⚠ La date de fin doit être supérieure à la date de début");
                            dateFinError.setVisible(true);
                            dateFinPicker.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
                            return;
                        }
                    } catch (Exception e) {
                        // Ignorer les erreurs de parsing
                    }
                }
                dateFinError.setVisible(false);
                dateFinPicker.setStyle(inputStyle);
            }
        });
        
        dateFinPicker.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && dateFinPicker.getValue() == null) {
                dateFinError.setText("⚠ La date de fin est obligatoire");
                dateFinError.setVisible(true);
                dateFinPicker.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
            }
        });
        
        // Validation heure fin
        heureFinField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                dateFinError.setText("⚠ Format d'heure invalide (HH:mm, ex: 17:00)");
                dateFinError.setVisible(true);
                heureFinField.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
            } else {
                // Vérifier que date fin > date début
                if (dateDebutPicker.getValue() != null && dateFinPicker.getValue() != null 
                    && heureDebutField.getText().matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                    try {
                        String[] heureDebutParts = heureDebutField.getText().split(":");
                        String[] heureFinParts = newVal.split(":");
                        
                        int heureDebut = Integer.parseInt(heureDebutParts[0]);
                        int minuteDebut = Integer.parseInt(heureDebutParts[1]);
                        int heureFin = Integer.parseInt(heureFinParts[0]);
                        int minuteFin = Integer.parseInt(heureFinParts[1]);
                        
                        java.time.LocalDateTime dateDebut = dateDebutPicker.getValue().atTime(heureDebut, minuteDebut);
                        java.time.LocalDateTime dateFin = dateFinPicker.getValue().atTime(heureFin, minuteFin);
                        
                        if (dateFin.isBefore(dateDebut) || dateFin.isEqual(dateDebut)) {
                            dateFinError.setText("⚠ La date de fin doit être supérieure à la date de début");
                            dateFinError.setVisible(true);
                            heureFinField.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
                            return;
                        }
                    } catch (Exception e) {
                        // Ignorer les erreurs de parsing
                    }
                }
                dateFinError.setVisible(false);
                heureFinField.setStyle(inputStyle);
            }
        });
        
        heureFinField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && !heureFinField.getText().matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                dateFinError.setText("⚠ Format d'heure invalide (HH:mm, ex: 17:00)");
                dateFinError.setVisible(true);
                heureFinField.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
            }
        });
        
        // Statut
        VBox statutBox = new VBox(6);
        Label statutLabel = new Label("Statut");
        statutLabel.setStyle(labelStyle);
        CheckBox disponibleCheck = new CheckBox("✓ Disponible");
        disponibleCheck.setSelected(true);
        disponibleCheck.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151;");
        statutBox.getChildren().addAll(statutLabel, disponibleCheck);
        
        // Notes
        VBox notesBox = new VBox(6);
        Label notesLabel = new Label("Notes");
        notesLabel.setStyle(labelStyle);
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Ajoutez des notes supplémentaires (minimum 4 caractères)...");
        notesArea.setPrefRowCount(3);
        notesArea.setStyle(inputStyle);
        Label notesError = new Label();
        notesError.setStyle(errorStyle);
        notesError.setVisible(false);
        notesBox.getChildren().addAll(notesLabel, notesArea, notesError);
        
        // Validation notes (minimum 4 caractères si rempli)
        notesArea.textProperty().addListener((obs, oldVal, newVal) -> {
            String text = newVal.trim();
            if (!text.isEmpty() && text.length() < 4) {
                notesError.setText("⚠ Les notes doivent contenir au moins 4 caractères");
                notesError.setVisible(true);
                notesArea.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
            } else {
                notesError.setVisible(false);
                notesArea.setStyle(inputStyle);
            }
        });
        
        notesArea.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                String text = notesArea.getText().trim();
                if (!text.isEmpty() && text.length() < 4) {
                    notesError.setText("⚠ Les notes doivent contenir au moins 4 caractères");
                    notesError.setVisible(true);
                    notesArea.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
                }
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
                heureDebutField.setText(disponibilite.getDateDebut().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            }
            
            if (disponibilite.getDateFin() != null) {
                dateFinPicker.setValue(disponibilite.getDateFin().toLocalDate());
                heureFinField.setText(disponibilite.getDateFin().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            }
            
            disponibleCheck.setSelected(disponibilite.isEstDisponible());
            notesArea.setText(disponibilite.getNotes() != null ? disponibilite.getNotes() : "");
        } else {
            dateDebutPicker.setValue(java.time.LocalDate.now());
            dateFinPicker.setValue(java.time.LocalDate.now());
        }
        
        // Sauvegarder les valeurs initiales pour détecter les modifications
        final String initialDoctor = doctorCombo.getValue();
        final java.time.LocalDate initialDateDebut = dateDebutPicker.getValue();
        final String initialHeureDebut = heureDebutField.getText();
        final java.time.LocalDate initialDateFin = dateFinPicker.getValue();
        final String initialHeureFin = heureFinField.getText();
        final boolean initialDisponible = disponibleCheck.isSelected();
        final String initialNotes = notesArea.getText();
        
        // Désactiver le bouton de sauvegarde si pas de modification
        javafx.beans.property.BooleanProperty hasChanges = new javafx.beans.property.SimpleBooleanProperty(false);
        
        doctorCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            hasChanges.set(!java.util.Objects.equals(newVal, initialDoctor) ||
                          !java.util.Objects.equals(dateDebutPicker.getValue(), initialDateDebut) ||
                          !java.util.Objects.equals(heureDebutField.getText(), initialHeureDebut) ||
                          !java.util.Objects.equals(dateFinPicker.getValue(), initialDateFin) ||
                          !java.util.Objects.equals(heureFinField.getText(), initialHeureFin) ||
                          disponibleCheck.isSelected() != initialDisponible ||
                          !java.util.Objects.equals(notesArea.getText(), initialNotes));
        });
        
        dateDebutPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            hasChanges.set(!java.util.Objects.equals(doctorCombo.getValue(), initialDoctor) ||
                          !java.util.Objects.equals(newVal, initialDateDebut) ||
                          !java.util.Objects.equals(heureDebutField.getText(), initialHeureDebut) ||
                          !java.util.Objects.equals(dateFinPicker.getValue(), initialDateFin) ||
                          !java.util.Objects.equals(heureFinField.getText(), initialHeureFin) ||
                          disponibleCheck.isSelected() != initialDisponible ||
                          !java.util.Objects.equals(notesArea.getText(), initialNotes));
        });
        
        heureDebutField.textProperty().addListener((obs, oldVal, newVal) -> {
            hasChanges.set(!java.util.Objects.equals(doctorCombo.getValue(), initialDoctor) ||
                          !java.util.Objects.equals(dateDebutPicker.getValue(), initialDateDebut) ||
                          !java.util.Objects.equals(newVal, initialHeureDebut) ||
                          !java.util.Objects.equals(dateFinPicker.getValue(), initialDateFin) ||
                          !java.util.Objects.equals(heureFinField.getText(), initialHeureFin) ||
                          disponibleCheck.isSelected() != initialDisponible ||
                          !java.util.Objects.equals(notesArea.getText(), initialNotes));
        });
        
        dateFinPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            hasChanges.set(!java.util.Objects.equals(doctorCombo.getValue(), initialDoctor) ||
                          !java.util.Objects.equals(dateDebutPicker.getValue(), initialDateDebut) ||
                          !java.util.Objects.equals(heureDebutField.getText(), initialHeureDebut) ||
                          !java.util.Objects.equals(newVal, initialDateFin) ||
                          !java.util.Objects.equals(heureFinField.getText(), initialHeureFin) ||
                          disponibleCheck.isSelected() != initialDisponible ||
                          !java.util.Objects.equals(notesArea.getText(), initialNotes));
        });
        
        heureFinField.textProperty().addListener((obs, oldVal, newVal) -> {
            hasChanges.set(!java.util.Objects.equals(doctorCombo.getValue(), initialDoctor) ||
                          !java.util.Objects.equals(dateDebutPicker.getValue(), initialDateDebut) ||
                          !java.util.Objects.equals(heureDebutField.getText(), initialHeureDebut) ||
                          !java.util.Objects.equals(dateFinPicker.getValue(), initialDateFin) ||
                          !java.util.Objects.equals(newVal, initialHeureFin) ||
                          disponibleCheck.isSelected() != initialDisponible ||
                          !java.util.Objects.equals(notesArea.getText(), initialNotes));
        });
        
        disponibleCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            hasChanges.set(!java.util.Objects.equals(doctorCombo.getValue(), initialDoctor) ||
                          !java.util.Objects.equals(dateDebutPicker.getValue(), initialDateDebut) ||
                          !java.util.Objects.equals(heureDebutField.getText(), initialHeureDebut) ||
                          !java.util.Objects.equals(dateFinPicker.getValue(), initialDateFin) ||
                          !java.util.Objects.equals(heureFinField.getText(), initialHeureFin) ||
                          newVal != initialDisponible ||
                          !java.util.Objects.equals(notesArea.getText(), initialNotes));
        });
        
        notesArea.textProperty().addListener((obs, oldVal, newVal) -> {
            hasChanges.set(!java.util.Objects.equals(doctorCombo.getValue(), initialDoctor) ||
                          !java.util.Objects.equals(dateDebutPicker.getValue(), initialDateDebut) ||
                          !java.util.Objects.equals(heureDebutField.getText(), initialHeureDebut) ||
                          !java.util.Objects.equals(dateFinPicker.getValue(), initialDateFin) ||
                          !java.util.Objects.equals(heureFinField.getText(), initialHeureFin) ||
                          disponibleCheck.isSelected() != initialDisponible ||
                          !java.util.Objects.equals(newVal, initialNotes));
        });
        
        // Désactiver le bouton si pas de changement
        javafx.scene.Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.disableProperty().bind(hasChanges.not());

        formContainer.getChildren().addAll(doctorBox, dateDebutBox, dateFinBox, statutBox, notesBox);
        mainContainer.getChildren().addAll(header, formContainer);
        dialog.getDialogPane().setContent(mainContainer);
        
        // Style des boutons
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
                    // Valider tous les champs avant de sauvegarder
                    boolean isValid = true;
                    
                    // Valider médecin
                    String selectedDoctor = doctorCombo.getValue();
                    if (selectedDoctor == null || selectedDoctor.isEmpty()) {
                        doctorError.setText("⚠ Le médecin est obligatoire");
                        doctorError.setVisible(true);
                        doctorCombo.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
                        isValid = false;
                    }
                    
                    // Valider date début
                    if (dateDebutPicker.getValue() == null) {
                        dateDebutError.setText("⚠ La date de début est obligatoire");
                        dateDebutError.setVisible(true);
                        dateDebutPicker.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
                        isValid = false;
                    }
                    
                    // Valider heure début
                    if (!heureDebutField.getText().matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                        dateDebutError.setText("⚠ Format d'heure invalide (HH:mm, ex: 09:00)");
                        dateDebutError.setVisible(true);
                        heureDebutField.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
                        isValid = false;
                    }
                    
                    // Valider date fin
                    if (dateFinPicker.getValue() == null) {
                        dateFinError.setText("⚠ La date de fin est obligatoire");
                        dateFinError.setVisible(true);
                        dateFinPicker.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
                        isValid = false;
                    }
                    
                    // Valider heure fin
                    if (!heureFinField.getText().matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                        dateFinError.setText("⚠ Format d'heure invalide (HH:mm, ex: 17:00)");
                        dateFinError.setVisible(true);
                        heureFinField.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
                        isValid = false;
                    }
                    
                    // Valider notes (minimum 4 caractères si rempli)
                    String notesText = notesArea.getText().trim();
                    if (!notesText.isEmpty() && notesText.length() < 4) {
                        notesError.setText("⚠ Les notes doivent contenir au moins 4 caractères");
                        notesError.setVisible(true);
                        notesArea.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
                        isValid = false;
                    }
                    
                    if (!isValid) {
                        return null;
                    }
                    
                    int doctorId = Integer.parseInt(selectedDoctor.split(" - ")[0]);
                    
                    String[] heureDebutParts = heureDebutField.getText().split(":");
                    String[] heureFinParts = heureFinField.getText().split(":");
                    
                    int heureDebut = Integer.parseInt(heureDebutParts[0]);
                    int minuteDebut = Integer.parseInt(heureDebutParts[1]);
                    int heureFin = Integer.parseInt(heureFinParts[0]);
                    int minuteFin = Integer.parseInt(heureFinParts[1]);
                    
                    java.time.LocalDateTime dateDebut = dateDebutPicker.getValue().atTime(heureDebut, minuteDebut);
                    java.time.LocalDateTime dateFin = dateFinPicker.getValue().atTime(heureFin, minuteFin);
                    
                    // Valider que date fin > date début
                    if (dateFin.isBefore(dateDebut) || dateFin.isEqual(dateDebut)) {
                        dateFinError.setText("⚠ La date de fin doit être supérieure à la date de début");
                        dateFinError.setVisible(true);
                        dateFinPicker.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
                        heureFinField.setStyle(inputStyle + "-fx-border-color: #ef4444; -fx-border-width: 2;");
                        return null;
                    }
                    
                    Disponibilite result = disponibilite != null ? disponibilite : new Disponibilite();
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
                        "Disponibilité ajoutée avec succès",
                        NotificationUtil.NotificationType.SUCCESS
                    );
                } else {
                    serviceDisponibilite.modifier(result);
                    NotificationUtil.showNotification(
                        stage,
                        "Disponibilité modifiée avec succès",
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

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}