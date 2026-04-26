package esprit.fx.controllers;

import esprit.fx.entities.RendezVous;
import esprit.fx.entities.User;
import esprit.fx.services.GoogleCalendarService;
import esprit.fx.services.HistoriqueService;
import esprit.fx.services.ServiceRendezVous;
import esprit.fx.services.ServiceUser;
import esprit.fx.utils.UserSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class RendezVousController implements Initializable {

    @FXML private TableView<RendezVous> tableRendezVous;
    // ✅ colId supprimé
    @FXML private TableColumn<RendezVous, String> colPatient;
    @FXML private TableColumn<RendezVous, String> colDocteur;
    @FXML private TableColumn<RendezVous, String> colDate;
    @FXML private TableColumn<RendezVous, String> colMotif;
    @FXML private TableColumn<RendezVous, String> colStatut;

    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnConfirmer;
    @FXML private Button btnAnnuler;
    @FXML private Button btnActualiser;
    @FXML private Button btnGoogleCalendar;
    @FXML private Button btnHistorique;

    @FXML private ComboBox<String> filterStatut;
    @FXML private TextField searchField;

    private ServiceRendezVous serviceRendezVous;
    private ServiceUser serviceUser;
    private GoogleCalendarService googleCalendarService;
    private HistoriqueService historiqueService;
    private ObservableList<RendezVous> rendezVousList;
    private String currentUserRole;
    private int currentUserId;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        serviceRendezVous = new ServiceRendezVous();
        serviceUser = new ServiceUser();
        googleCalendarService = new GoogleCalendarService();
        historiqueService = new HistoriqueService();
        rendezVousList = FXCollections.observableArrayList();

        User currentUser = UserSession.getCurrentUser();
        currentUserRole = UserSession.getCurrentRole();
        currentUserId = currentUser != null ? currentUser.getId() : 0;

        initializeTable();
        initializeFilters();
        configureButtonsBasedOnRole();
        chargerRendezVous();

        tableRendezVous.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> updateButtonStates(newSelection)
        );
    }

    private void initializeTable() {
        // ✅ colId.setCellValueFactory supprimé
        colPatient.setCellValueFactory(new PropertyValueFactory<>("patientNom"));
        colDocteur.setCellValueFactory(new PropertyValueFactory<>("doctorNom"));
        colMotif.setCellValueFactory(new PropertyValueFactory<>("motif"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Formater la colonne date
        colDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDateHeure() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getDateHeure().format(
                                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                );
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        // Colorer les statuts
        colStatut.setCellFactory(column -> new TableCell<RendezVous, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(statut);
                    switch (statut.toUpperCase()) {
                        case "DEMANDE":
                            setStyle("-fx-background-color: #fff3cd; -fx-text-fill: #856404;");
                            break;
                        case "CONFIRME":
                            setStyle("-fx-background-color: #d4edda; -fx-text-fill: #155724;");
                            break;
                        case "ANNULE":
                            setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #721c24;");
                            break;
                        case "TERMINE":
                            setStyle("-fx-background-color: #d1ecf1; -fx-text-fill: #0c5460;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });

        tableRendezVous.setItems(rendezVousList);
    }

    private void initializeFilters() {
        filterStatut.setItems(FXCollections.observableArrayList(
                "Tous", "DEMANDE", "CONFIRME", "ANNULE", "TERMINE"
        ));
        filterStatut.setValue("Tous");

        filterStatut.setOnAction(e -> filtrerRendezVous());
        searchField.textProperty().addListener(
                (obs, oldText, newText) -> filtrerRendezVous()
        );
    }

    private void configureButtonsBasedOnRole() {
        if ("PATIENT".equals(currentUserRole)) {
            btnConfirmer.setVisible(false);
            btnAnnuler.setText("Annuler RDV");
        } else if ("DOCTOR".equals(currentUserRole)) {
            btnAjouter.setText("Créer RDV");
        }
    }

    private void chargerRendezVous() {
        try {
            List<RendezVous> rdvs;

            if ("PATIENT".equals(currentUserRole)) {
                rdvs = serviceRendezVous.getRendezVousParPatient(currentUserId);
            } else if ("DOCTOR".equals(currentUserRole)) {
                rdvs = serviceRendezVous.getRendezVousParDocteur(currentUserId);
            } else {
                rdvs = serviceRendezVous.getAll();
            }

            rendezVousList.clear();
            rendezVousList.addAll(rdvs);

        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les rendez-vous : " + e.getMessage());
        }
    }

    private void filtrerRendezVous() {
        try {
            List<RendezVous> rdvs;

            if ("PATIENT".equals(currentUserRole)) {
                rdvs = serviceRendezVous.getRendezVousParPatient(currentUserId);
            } else if ("DOCTOR".equals(currentUserRole)) {
                rdvs = serviceRendezVous.getRendezVousParDocteur(currentUserId);
            } else {
                rdvs = serviceRendezVous.getAll();
            }

            // Filtrer par statut
            String statutFiltre = filterStatut.getValue();
            if (!"Tous".equals(statutFiltre)) {
                rdvs = rdvs.stream()
                        .filter(rdv -> statutFiltre.equals(rdv.getStatut()))
                        .toList();
            }

            // ✅ Recherche par nom patient, médecin, motif (sans ID)
            String recherche = searchField.getText().toLowerCase().trim();
            if (!recherche.isEmpty()) {
                rdvs = rdvs.stream()
                        .filter(rdv ->
                                (rdv.getPatientNom() != null &&
                                        rdv.getPatientNom().toLowerCase().contains(recherche)) ||
                                        (rdv.getDoctorNom() != null &&
                                                rdv.getDoctorNom().toLowerCase().contains(recherche)) ||
                                        (rdv.getMotif() != null &&
                                                rdv.getMotif().toLowerCase().contains(recherche))
                        )
                        .toList();
            }

            rendezVousList.clear();
            rendezVousList.addAll(rdvs);

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du filtrage : " + e.getMessage());
        }
    }

    private void updateButtonStates(RendezVous selectedRdv) {
        boolean hasSelection = selectedRdv != null;

        btnModifier.setDisable(!hasSelection);
        btnSupprimer.setDisable(!hasSelection);

        if (hasSelection) {
            String statut = selectedRdv.getStatut();
            btnConfirmer.setDisable(!"DEMANDE".equals(statut));
            btnAnnuler.setDisable(
                    "ANNULE".equals(statut) || "TERMINE".equals(statut)
            );
            // Bouton Google Calendar : visible uniquement si CONFIRME
            boolean estConfirme = "CONFIRME".equals(statut);
            btnGoogleCalendar.setDisable(!estConfirme);
            btnGoogleCalendar.setStyle(estConfirme
                ? "-fx-background-color: #4285f4; -fx-text-fill: white;" +
                  "-fx-font-weight: 700; -fx-background-radius: 6; -fx-cursor: hand;"
                : "-fx-background-color: #d1d5db; -fx-text-fill: #9ca3af;" +
                  "-fx-font-weight: 700; -fx-background-radius: 6;"
            );
            // Bouton Historique : actif dès qu'un RDV est sélectionné
            btnHistorique.setDisable(false);
            btnHistorique.setStyle(
                "-fx-background-color: #7c3aed; -fx-text-fill: white;" +
                "-fx-font-weight: 700; -fx-background-radius: 6; -fx-cursor: hand;"
            );
        } else {
            btnConfirmer.setDisable(true);
            btnAnnuler.setDisable(true);
            btnGoogleCalendar.setDisable(true);
            btnGoogleCalendar.setStyle(
                "-fx-background-color: #d1d5db; -fx-text-fill: #9ca3af;" +
                "-fx-font-weight: 700; -fx-background-radius: 6;"
            );
            btnHistorique.setDisable(true);
            btnHistorique.setStyle(
                "-fx-background-color: #d1d5db; -fx-text-fill: #9ca3af;" +
                "-fx-font-weight: 700; -fx-background-radius: 6;"
            );
        }
    }

    @FXML
    private void ajouterRendezVous() {
        ouvrirFormulaireRendezVous(null);
    }

    @FXML
    private void modifierRendezVous() {
        RendezVous selected = tableRendezVous.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ouvrirFormulaireRendezVous(selected);
        }
    }

    @FXML
    private void supprimerRendezVous() {
        RendezVous selected = tableRendezVous.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirmation");
            confirmation.setHeaderText("Supprimer le rendez-vous");
            confirmation.setContentText(
                    "Êtes-vous sûr de vouloir supprimer ce rendez-vous ?"
            );

            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    serviceRendezVous.supprimer(selected.getId());
                    chargerRendezVous();
                    showInfo("Succès", "Rendez-vous supprimé avec succès.");
                } catch (SQLException e) {
                    showAlert("Erreur",
                            "Impossible de supprimer le rendez-vous : " + e.getMessage());
                }
            }
        }
    }

    @FXML
    private void confirmerRendezVous() {
        RendezVous selected = tableRendezVous.getSelectionModel().getSelectedItem();
        if (selected != null && "DEMANDE".equals(selected.getStatut())) {
            try {
                serviceRendezVous.changerStatut(selected.getId(), "CONFIRME");
                // Enregistrer dans l'historique
                historiqueService.enregistrerChangement(
                    selected.getId(), selected.getStatut(), "CONFIRME", currentUserId
                );
                chargerRendezVous();
                showInfo("Succès", "Rendez-vous confirmé.");

                // Proposer d'ajouter à Google Calendar après confirmation
                Alert proposer = new Alert(Alert.AlertType.CONFIRMATION);
                proposer.setTitle("Google Calendar");
                proposer.setHeaderText("📅 Ajouter à Google Calendar ?");
                proposer.setContentText(
                    "Le rendez-vous est confirmé !\n" +
                    "Voulez-vous l'ajouter à votre Google Calendar ?"
                );
                proposer.getButtonTypes().setAll(
                    new ButtonType("📅 Oui, ajouter", ButtonBar.ButtonData.YES),
                    new ButtonType("Non merci",       ButtonBar.ButtonData.NO)
                );
                proposer.showAndWait().ifPresent(btn -> {
                    if (btn.getButtonData() == ButtonBar.ButtonData.YES) {
                        // Recharger le RDV avec statut CONFIRME mis à jour
                        selected.setStatut("CONFIRME");
                        ouvrirGoogleCalendar(selected);
                    }
                });

            } catch (SQLException e) {
                showAlert("Erreur",
                        "Impossible de confirmer le rendez-vous : " + e.getMessage());
            }
        }
    }

    @FXML
    private void ajouterAGoogleCalendar() {
        RendezVous selected = tableRendezVous.getSelectionModel().getSelectedItem();
        if (selected != null && "CONFIRME".equals(selected.getStatut())) {
            ouvrirGoogleCalendar(selected);
        }
    }

    /** Ouvre Google Calendar dans le navigateur avec le RDV pré-rempli. */
    private void ouvrirGoogleCalendar(RendezVous rdv) {
        boolean succes = googleCalendarService.ouvrirDansGoogleCalendar(rdv);
        if (succes) {
            showInfo("Google Calendar",
                "✅ Google Calendar ouvert dans votre navigateur.\n" +
                "Cliquez sur « Enregistrer » pour ajouter le RDV à votre calendrier.");
        } else {
            // Fallback : afficher le lien à copier
            String lien = googleCalendarService.genererLien(rdv);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Google Calendar");
            alert.setHeaderText("📅 Lien Google Calendar");
            alert.setContentText(
                "Impossible d'ouvrir le navigateur automatiquement.\n\n" +
                "Copiez ce lien dans votre navigateur :\n\n" + lien
            );
            alert.getDialogPane().setPrefWidth(600);
            alert.showAndWait();
        }
    }

    @FXML
    private void annulerRendezVous() {
        RendezVous selected = tableRendezVous.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                String ancienStatut = selected.getStatut();
                serviceRendezVous.changerStatut(selected.getId(), "ANNULE");
                // Enregistrer dans l'historique
                historiqueService.enregistrerChangement(
                    selected.getId(), ancienStatut, "ANNULE", currentUserId
                );
                chargerRendezVous();
                showInfo("Succès", "Rendez-vous annulé.");
            } catch (SQLException e) {
                showAlert("Erreur",
                        "Impossible d'annuler le rendez-vous : " + e.getMessage());
            }
        }
    }

    @FXML
    private void ouvrirHistorique() {
        RendezVous selected = tableRendezVous.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/Historique.fxml")
            );
            javafx.scene.Parent root = loader.load();
            HistoriqueController ctrl = loader.getController();
            ctrl.initHistorique(selected);

            Stage stage = new Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle("📋 Historique — RDV #" + selected.getId());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir l'historique : " + e.getMessage());
        }
    }

    @FXML
    private void actualiserListe() {
        chargerRendezVous();
    }

    private void ouvrirFormulaireRendezVous(RendezVous rendezVous) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/FormulaireRendezVous.fxml")
            );
            Parent root = loader.load();

            FormulaireRendezVousController controller = loader.getController();
            controller.setRendezVous(rendezVous);
            controller.setParentController(this);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(
                    rendezVous == null ? "Nouveau Rendez-vous" : "Modifier Rendez-vous"
            );
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    public void rafraichir() {
        chargerRendezVous();
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