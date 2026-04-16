package esprit.fx.controllers;

import esprit.fx.entities.Disponibilite;
import esprit.fx.entities.RendezVous;
import esprit.fx.entities.User;
import esprit.fx.services.ServiceDisponibilite;
import esprit.fx.services.ServiceRendezVous;
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

public class PrendreRendezVousController implements Initializable {

    @FXML private VBox containerDisponibilites;
    @FXML private VBox containerRendezVous;
    @FXML private ScrollPane scrollDisponibilites;
    @FXML private ScrollPane scrollRendezVous;
    @FXML private Button btnActualiser;

    private ServiceDisponibilite serviceDisponibilite;
    private ServiceRendezVous serviceRendezVous;
    private int currentPatientId;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("\n=== INITIALISATION PRENDRE RDV (CARTES) ===");
        
        serviceDisponibilite = new ServiceDisponibilite();
        serviceRendezVous = new ServiceRendezVous();
        
        User currentUser = UserSession.getCurrentUser();
        currentPatientId = currentUser != null ? currentUser.getId() : 0;
        
        System.out.println("Patient connecté - ID: " + currentPatientId);
        
        chargerDisponibilites();
        chargerMesRendezVous();
        
        System.out.println("===================================\n");
    }

    private void chargerDisponibilites() {
        try {
            System.out.println("\n=== CHARGEMENT DISPONIBILITÉS ===");
            
            List<Disponibilite> dispos = serviceDisponibilite.getAll();
            System.out.println("Nombre de disponibilités: " + dispos.size());
            
            containerDisponibilites.getChildren().clear();
            
            if (dispos.isEmpty()) {
                Label emptyLabel = new Label("Aucune disponibilité pour le moment");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b7280; -fx-padding: 40;");
                containerDisponibilites.getChildren().add(emptyLabel);
            } else {
                for (Disponibilite dispo : dispos) {
                    containerDisponibilites.getChildren().add(createDisponibiliteCard(dispo));
                }
            }
            
            System.out.println("=================================\n");
            
        } catch (SQLException e) {
            System.err.println("ERREUR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private VBox createDisponibiliteCard(Disponibilite dispo) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);" +
            "-fx-cursor: hand;"
        );
        
        // Header avec médecin
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
        
        // Notes
        if (dispo.getNotes() != null && !dispo.getNotes().isEmpty()) {
            Label notesLabel = new Label("📝 " + dispo.getNotes());
            notesLabel.setWrapText(true);
            notesLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");
            card.getChildren().addAll(header, datesBox, notesLabel);
        } else {
            card.getChildren().addAll(header, datesBox);
        }
        
        // Bouton prendre RDV
        if (dispo.isEstDisponible()) {
            Button btnPrendre = new Button("📅 Prendre ce rendez-vous");
            btnPrendre.setMaxWidth(Double.MAX_VALUE);
            btnPrendre.setStyle(
                "-fx-background-color: #10b981;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: 600;" +
                "-fx-padding: 12;" +
                "-fx-background-radius: 8;" +
                "-fx-font-size: 14px;" +
                "-fx-cursor: hand;"
            );
            btnPrendre.setOnAction(e -> prendreRendezVous(dispo));
            card.getChildren().add(btnPrendre);
        }
        
        // Effet hover
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4);" +
            "-fx-cursor: hand;" +
            "-fx-scale-x: 1.02;" +
            "-fx-scale-y: 1.02;"
        ));
        
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);" +
            "-fx-cursor: hand;"
        ));
        
        return card;
    }

    private void chargerMesRendezVous() {
        try {
            System.out.println("\n=== CHARGEMENT MES RENDEZ-VOUS ===");
            
            List<RendezVous> rdvs = serviceRendezVous.getRendezVousParPatient(currentPatientId);
            System.out.println("Nombre de rendez-vous: " + rdvs.size());
            
            containerRendezVous.getChildren().clear();
            
            if (rdvs.isEmpty()) {
                Label emptyLabel = new Label("Aucun rendez-vous pour le moment");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b7280; -fx-padding: 40;");
                containerRendezVous.getChildren().add(emptyLabel);
            } else {
                for (RendezVous rdv : rdvs) {
                    containerRendezVous.getChildren().add(createRendezVousCard(rdv));
                }
            }
            
            System.out.println("===================================\n");
            
        } catch (SQLException e) {
            System.err.println("ERREUR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private VBox createRendezVousCard(RendezVous rdv) {
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
        
        Label iconLabel = new Label("🩺");
        iconLabel.setStyle("-fx-font-size: 32px;");
        
        VBox infoBox = new VBox(5);
        Label nomLabel = new Label("Dr. " + rdv.getDoctorNom());
        nomLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
        
        Label idLabel = new Label("RDV #" + rdv.getId());
        idLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #9ca3af;");
        
        infoBox.getChildren().addAll(nomLabel, idLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        
        // Badge statut
        Label statutBadge = new Label();
        String statut = rdv.getStatut() != null ? rdv.getStatut().toUpperCase() : "PENDING";
        switch (statut) {
            case "CONFIRMED":
                statutBadge.setText("✓ Confirmé");
                statutBadge.setStyle(
                    "-fx-background-color: #d1fae5;" +
                    "-fx-text-fill: #065f46;" +
                    "-fx-padding: 6 12;" +
                    "-fx-background-radius: 20;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;"
                );
                break;
            case "PENDING":
                statutBadge.setText("⏳ En attente");
                statutBadge.setStyle(
                    "-fx-background-color: #fef3c7;" +
                    "-fx-text-fill: #92400e;" +
                    "-fx-padding: 6 12;" +
                    "-fx-background-radius: 20;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;"
                );
                break;
            case "CANCELLED":
                statutBadge.setText("✗ Annulé");
                statutBadge.setStyle(
                    "-fx-background-color: #fee2e2;" +
                    "-fx-text-fill: #991b1b;" +
                    "-fx-padding: 6 12;" +
                    "-fx-background-radius: 20;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;"
                );
                break;
        }
        
        header.getChildren().addAll(iconLabel, infoBox, statutBadge);
        
        // Date et motif
        VBox detailsBox = new VBox(8);
        detailsBox.setStyle("-fx-background-color: #f9fafb; -fx-padding: 12; -fx-background-radius: 8;");
        
        Label dateLabel = new Label("📅 " + (rdv.getDateHeure() != null ? 
            rdv.getDateHeure().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) : "N/A"));
        dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151; -fx-font-weight: 600;");
        
        Label motifLabel = new Label("📋 " + (rdv.getMotif() != null ? rdv.getMotif() : "Consultation"));
        motifLabel.setWrapText(true);
        motifLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");
        
        detailsBox.getChildren().addAll(dateLabel, motifLabel);
        
        // Notes
        if (rdv.getNotes() != null && !rdv.getNotes().isEmpty()) {
            Label notesLabel = new Label("📝 " + rdv.getNotes());
            notesLabel.setWrapText(true);
            notesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #9ca3af; -fx-font-style: italic;");
            detailsBox.getChildren().add(notesLabel);
        }
        
        // Boutons d'action
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
        btnModifier.setOnAction(e -> modifierRendezVous(rdv));
        
        Button btnSupprimer = new Button("🗑️ Supprimer");
        btnSupprimer.setStyle(
            "-fx-background-color: #ef4444;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: 600;" +
            "-fx-padding: 10 20;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;"
        );
        btnSupprimer.setOnAction(e -> supprimerRendezVous(rdv));
        
        actionsBox.getChildren().addAll(btnModifier, btnSupprimer);
        
        card.getChildren().addAll(header, detailsBox, actionsBox);
        
        return card;
    }

    private void prendreRendezVous(Disponibilite dispo) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Prendre Rendez-vous");
        dialog.setHeaderText("Rendez-vous avec " + dispo.getDoctorNom() + "\n" +
                            "Le " + dispo.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));

        dialog.getDialogPane().setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 20;"
        );

        ButtonType confirmButtonType = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextArea motifArea = new TextArea();
        motifArea.setPromptText("Motif de la consultation (optionnel)");
        motifArea.setPrefRowCount(4);
        motifArea.setText("Consultation générale");
        motifArea.setStyle(
            "-fx-background-color: #f9fafb;" +
            "-fx-border-color: #e5e7eb;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 10;" +
            "-fx-font-size: 13px;"
        );

        Label motifLabel = new Label("Motif:");
        motifLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #374151;");

        grid.add(motifLabel, 0, 0);
        grid.add(motifArea, 0, 1);

        dialog.getDialogPane().setContent(grid);
        
        dialog.getDialogPane().lookupButton(confirmButtonType).setStyle(
            "-fx-background-color: #10b981;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: 600;" +
            "-fx-padding: 10 20;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;"
        );
        
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle(
            "-fx-background-color: #f3f4f6;" +
            "-fx-text-fill: #374151;" +
            "-fx-font-weight: 600;" +
            "-fx-padding: 10 20;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;"
        );

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return motifArea.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(motif -> {
            try {
                RendezVous rdv = new RendezVous();
                rdv.setPatientId(currentPatientId);
                rdv.setDoctorId(dispo.getDoctorId());
                rdv.setDateHeure(dispo.getDateDebut());
                rdv.setMotif(motif != null && !motif.trim().isEmpty() ? motif : "Consultation");
                rdv.setStatut("CONFIRMED");
                rdv.setNotes("Rendez-vous pris via l'application");
                
                serviceRendezVous.ajouter(rdv);
                
                dispo.setEstDisponible(false);
                serviceDisponibilite.modifier(dispo);
                
                Stage stage = (Stage) containerDisponibilites.getScene().getWindow();
                NotificationUtil.showNotification(
                    stage,
                    "Rendez-vous créé avec " + dispo.getDoctorNom() + " le " + 
                    dispo.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")),
                    NotificationUtil.NotificationType.SUCCESS
                );
                
                chargerDisponibilites();
                chargerMesRendezVous();
                
            } catch (SQLException e) {
                Stage stage = (Stage) containerDisponibilites.getScene().getWindow();
                NotificationUtil.showNotification(
                    stage,
                    "Impossible de créer le rendez-vous: " + e.getMessage(),
                    NotificationUtil.NotificationType.ERROR
                );
            }
        });
    }

    private void modifierRendezVous(RendezVous rdv) {
        Dialog<RendezVous> dialog = new Dialog<>();
        dialog.setTitle("Modifier Rendez-vous");
        dialog.setHeaderText("Modifier le rendez-vous #" + rdv.getId());

        dialog.getDialogPane().setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 20;"
        );

        ButtonType saveButtonType = new ButtonType("Sauvegarder", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 150, 10, 10));

        String inputStyle = 
            "-fx-background-color: #f9fafb;" +
            "-fx-border-color: #e5e7eb;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 10;" +
            "-fx-font-size: 13px;";
        
        String labelStyle = "-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #374151;";

        Label motifLabel = new Label("Motif:");
        motifLabel.setStyle(labelStyle);
        TextArea motifArea = new TextArea(rdv.getMotif());
        motifArea.setPromptText("Motif");
        motifArea.setPrefRowCount(3);
        motifArea.setStyle(inputStyle);

        Label statutLabel = new Label("Statut:");
        statutLabel.setStyle(labelStyle);
        ComboBox<String> statutCombo = new ComboBox<>();
        statutCombo.getItems().addAll(
            "Confirmé (CONFIRMED)",
            "En attente (PENDING)", 
            "Annulé (CANCELLED)"
        );
        
        String currentStatut = rdv.getStatut() != null ? rdv.getStatut().toUpperCase() : "CONFIRMED";
        switch (currentStatut) {
            case "CONFIRMED":
                statutCombo.setValue("Confirmé (CONFIRMED)");
                break;
            case "PENDING":
                statutCombo.setValue("En attente (PENDING)");
                break;
            case "CANCELLED":
                statutCombo.setValue("Annulé (CANCELLED)");
                break;
            default:
                statutCombo.setValue("Confirmé (CONFIRMED)");
                break;
        }
        statutCombo.setPrefWidth(250);
        statutCombo.setStyle(inputStyle);

        Label notesLabel = new Label("Notes:");
        notesLabel.setStyle(labelStyle);
        TextArea notesArea = new TextArea(rdv.getNotes());
        notesArea.setPromptText("Notes");
        notesArea.setPrefRowCount(3);
        notesArea.setStyle(inputStyle);

        grid.add(motifLabel, 0, 0);
        grid.add(motifArea, 0, 1);
        grid.add(statutLabel, 0, 2);
        grid.add(statutCombo, 0, 3);
        grid.add(notesLabel, 0, 4);
        grid.add(notesArea, 0, 5);

        dialog.getDialogPane().setContent(grid);
        
        dialog.getDialogPane().lookupButton(saveButtonType).setStyle(
            "-fx-background-color: #3b82f6;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: 600;" +
            "-fx-padding: 10 20;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;"
        );
        
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle(
            "-fx-background-color: #f3f4f6;" +
            "-fx-text-fill: #374151;" +
            "-fx-font-weight: 600;" +
            "-fx-padding: 10 20;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;"
        );

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                rdv.setMotif(motifArea.getText());
                
                String selectedStatut = statutCombo.getValue();
                if (selectedStatut.contains("CONFIRMED")) {
                    rdv.setStatut("CONFIRMED");
                } else if (selectedStatut.contains("PENDING")) {
                    rdv.setStatut("PENDING");
                } else if (selectedStatut.contains("CANCELLED")) {
                    rdv.setStatut("CANCELLED");
                }
                
                rdv.setNotes(notesArea.getText());
                return rdv;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(modifiedRdv -> {
            try {
                serviceRendezVous.modifier(modifiedRdv);
                
                String statutFr = "";
                switch (modifiedRdv.getStatut().toUpperCase()) {
                    case "CONFIRMED": statutFr = "Confirmé"; break;
                    case "PENDING": statutFr = "En attente"; break;
                    case "CANCELLED": statutFr = "Annulé"; break;
                }
                
                Stage stage = (Stage) containerRendezVous.getScene().getWindow();
                NotificationUtil.showNotification(
                    stage,
                    "Rendez-vous modifié - Statut: " + statutFr,
                    NotificationUtil.NotificationType.SUCCESS
                );
                
                chargerMesRendezVous();
            } catch (SQLException e) {
                Stage stage = (Stage) containerRendezVous.getScene().getWindow();
                NotificationUtil.showNotification(
                    stage,
                    "Impossible de modifier le rendez-vous",
                    NotificationUtil.NotificationType.ERROR
                );
            }
        });
    }

    private void supprimerRendezVous(RendezVous rdv) {
        Stage stage = (Stage) containerRendezVous.getScene().getWindow();
        String details = "Médecin: " + rdv.getDoctorNom() + "\n" +
                        "Date: " + (rdv.getDateHeure() != null ? 
                            rdv.getDateHeure().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) : "N/A");
        
        boolean confirmed = NotificationUtil.showConfirmation(
            stage,
            "Supprimer le rendez-vous",
            "Êtes-vous sûr de vouloir supprimer ce rendez-vous?",
            details
        );
        
        if (confirmed) {
            try {
                serviceRendezVous.supprimer(rdv.getId());
                
                NotificationUtil.showNotification(
                    stage,
                    "Rendez-vous supprimé avec succès",
                    NotificationUtil.NotificationType.SUCCESS
                );
                
                chargerMesRendezVous();
                chargerDisponibilites();
            } catch (SQLException e) {
                NotificationUtil.showNotification(
                    stage,
                    "Impossible de supprimer le rendez-vous",
                    NotificationUtil.NotificationType.ERROR
                );
            }
        }
    }

    @FXML
    private void actualiser() {
        System.out.println("Actualisation des données...");
        chargerDisponibilites();
        chargerMesRendezVous();
    }
}
