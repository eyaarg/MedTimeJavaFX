package esprit.fx.controllers;

import esprit.fx.entities.RendezVous;
import esprit.fx.entities.User;
import esprit.fx.services.GoogleCalendarService;
import esprit.fx.services.HistoriqueService;
import esprit.fx.services.ListeAttenteNotificationService;
import esprit.fx.services.ServiceRendezVous;
import esprit.fx.services.ServiceUser;
import esprit.fx.utils.UserSession;
import javafx.application.Platform;
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

public class RendezVousController implements Initializable {

    // FXML — nouveau design cartes
    @FXML private VBox    containerRendezVous;
    @FXML private Button  btnAjouter;
    @FXML private Button  btnSuggestion;
    @FXML private Button  btnListeAttente;
    @FXML private Button  btnActualiser;
    @FXML private ComboBox<String> filterStatut;
    @FXML private TextField        searchField;

    private ServiceRendezVous    serviceRendezVous;
    private ServiceUser          serviceUser;
    private GoogleCalendarService googleCalendarService;
    private HistoriqueService    historiqueService;
    private ListeAttenteNotificationService listeAttenteNotifService;
    private String currentUserRole;
    private int    currentUserId;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        serviceRendezVous    = new ServiceRendezVous();
        serviceUser          = new ServiceUser();
        googleCalendarService = new GoogleCalendarService();
        historiqueService    = new HistoriqueService();
        listeAttenteNotifService = new ListeAttenteNotificationService();

        User currentUser = UserSession.getCurrentUser();
        currentUserRole  = UserSession.getCurrentRole();
        currentUserId    = currentUser != null ? currentUser.getId() : 0;

        filterStatut.getItems().addAll("Tous", "DEMANDE", "CONFIRME", "ANNULE", "TERMINE");
        filterStatut.setValue("Tous");
        filterStatut.setOnAction(e -> chargerRendezVous());
        searchField.textProperty().addListener((obs, o, n) -> chargerRendezVous());

        // Masquer "Nouveau RDV" pour les patients (ils prennent RDV via disponibilités)
        if ("PATIENT".equals(currentUserRole)) {
            btnAjouter.setVisible(false);
            btnAjouter.setManaged(false);
        }

        chargerRendezVous();
    }

    // =========================================================================
    // Chargement + affichage cartes
    // =========================================================================

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

            // Filtre statut
            String filtre = filterStatut.getValue();
            if (!"Tous".equals(filtre)) {
                rdvs = rdvs.stream().filter(r -> filtre.equals(r.getStatut())).toList();
            }

            // Recherche
            String q = searchField.getText().toLowerCase().trim();
            if (!q.isEmpty()) {
                rdvs = rdvs.stream().filter(r ->
                    (r.getPatientNom() != null && r.getPatientNom().toLowerCase().contains(q)) ||
                    (r.getDoctorNom()  != null && r.getDoctorNom().toLowerCase().contains(q))  ||
                    (r.getMotif()      != null && r.getMotif().toLowerCase().contains(q))
                ).toList();
            }

            containerRendezVous.getChildren().clear();

            if (rdvs.isEmpty()) {
                Label vide = new Label("Aucun rendez-vous trouvé");
                vide.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b7280; -fx-padding: 40;");
                containerRendezVous.getChildren().add(vide);
            } else {
                for (RendezVous rdv : rdvs) {
                    containerRendezVous.getChildren().add(buildCard(rdv));
                }
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les rendez-vous : " + e.getMessage());
        }
    }

    private VBox buildCard(RendezVous rdv) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);"
        );

        // ── Header : icône + noms + badge statut ──────────────────────────
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("🩺");
        icon.setStyle("-fx-font-size: 30px;");

        VBox names = new VBox(4);
        HBox.setHgrow(names, Priority.ALWAYS);
        Label doctorLabel = new Label("Dr. " + nvl(rdv.getDoctorNom(), "Médecin"));
        doctorLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #1f2937;");
        Label patientLabel = new Label("Patient : " + nvl(rdv.getPatientNom(), "Patient"));
        patientLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        names.getChildren().addAll(doctorLabel, patientLabel);

        Label badge = buildBadge(rdv.getStatut());
        header.getChildren().addAll(icon, names, badge);

        // ── Détails : date + motif ─────────────────────────────────────────
        HBox details = new HBox(24);
        details.setStyle("-fx-background-color: #f9fafb; -fx-padding: 10 14;" +
                         "-fx-background-radius: 8;");

        VBox dateBox = new VBox(3);
        Label dateTitle = new Label("📅 Date & Heure");
        dateTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af; -fx-font-weight: 600;");
        Label dateVal = new Label(rdv.getDateHeure() != null ?
                rdv.getDateHeure().format(FMT) : "N/A");
        dateVal.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #374151;");
        dateBox.getChildren().addAll(dateTitle, dateVal);

        VBox motifBox = new VBox(3);
        Label motifTitle = new Label("📋 Motif");
        motifTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af; -fx-font-weight: 600;");
        Label motifVal = new Label(nvl(rdv.getMotif(), "Consultation"));
        motifVal.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");
        motifVal.setWrapText(true);
        motifBox.getChildren().addAll(motifTitle, motifVal);
        HBox.setHgrow(motifBox, Priority.ALWAYS);

        details.getChildren().addAll(dateBox, motifBox);

        // ── Boutons d'action ───────────────────────────────────────────────
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        String statut = nvl(rdv.getStatut(), "");

        // Modifier (toujours visible sauf TERMINE)
        if (!"TERMINE".equals(statut)) {
            Button btnMod = btn("✏️ Modifier", "#3b82f6");
            btnMod.setOnAction(e -> ouvrirFormulaireRendezVous(rdv));
            actions.getChildren().add(btnMod);
        }

        // Confirmer (seulement si DEMANDE et rôle DOCTOR/ADMIN)
        if ("DEMANDE".equals(statut) && !"PATIENT".equals(currentUserRole)) {
            Button btnConf = btn("✅ Confirmer", "#10b981");
            btnConf.setOnAction(e -> confirmerRdv(rdv));
            actions.getChildren().add(btnConf);
        }

        // Annuler (si pas déjà annulé/terminé)
        if (!"ANNULE".equals(statut) && !"TERMINE".equals(statut)) {
            Button btnAnn = btn("❌ Annuler", "#ef4444");
            btnAnn.setOnAction(e -> annulerRdv(rdv));
            actions.getChildren().add(btnAnn);
        }

        // Google Calendar (si CONFIRME)
        if ("CONFIRME".equals(statut)) {
            Button btnGcal = btn("📅 Google Cal", "#4285f4");
            btnGcal.setOnAction(e -> ouvrirGoogleCalendar(rdv));
            actions.getChildren().add(btnGcal);
        }

        // Historique (toujours)
        Button btnHist = btn("📋 Historique", "#7c3aed");
        btnHist.setOnAction(e -> ouvrirHistoriqueRdv(rdv));
        actions.getChildren().add(btnHist);

        // Supprimer
        Button btnSuppr = btn("🗑️", "#6b7280");
        btnSuppr.setOnAction(e -> supprimerRdv(rdv));
        actions.getChildren().add(btnSuppr);

        card.getChildren().addAll(header, details, actions);
        VBox.setMargin(card, new Insets(0, 0, 4, 0));
        return card;
    }

    // =========================================================================
    // Actions
    // =========================================================================

    @FXML private void ajouterRendezVous()  { ouvrirFormulaireRendezVous(null); }
    @FXML private void actualiserListe()    { chargerRendezVous(); }

    private void confirmerRdv(RendezVous rdv) {
        try {
            serviceRendezVous.changerStatut(rdv.getId(), "CONFIRME");
            historiqueService.enregistrerChangement(rdv.getId(), rdv.getStatut(), "CONFIRME", currentUserId);
            chargerRendezVous();

            Alert ask = new Alert(Alert.AlertType.CONFIRMATION);
            ask.setTitle("Google Calendar");
            ask.setHeaderText("📅 Ajouter à Google Calendar ?");
            ask.setContentText("RDV confirmé ! Voulez-vous l'ajouter à votre calendrier ?");
            ask.getButtonTypes().setAll(
                new ButtonType("📅 Oui", ButtonBar.ButtonData.YES),
                new ButtonType("Non",    ButtonBar.ButtonData.NO)
            );
            ask.showAndWait().ifPresent(b -> {
                if (b.getButtonData() == ButtonBar.ButtonData.YES) {
                    rdv.setStatut("CONFIRME");
                    ouvrirGoogleCalendar(rdv);
                }
            });
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de confirmer : " + e.getMessage());
        }
    }

    private void annulerRdv(RendezVous rdv) {
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
        conf.setTitle("Annuler RDV");
        conf.setHeaderText("Annuler ce rendez-vous ?");
        conf.setContentText("Dr. " + rdv.getDoctorNom() + " — " +
                (rdv.getDateHeure() != null ? rdv.getDateHeure().format(FMT) : ""));
        if (conf.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                String ancien = rdv.getStatut();
                serviceRendezVous.changerStatut(rdv.getId(), "ANNULE");
                historiqueService.enregistrerChangement(rdv.getId(), ancien, "ANNULE", currentUserId);
                ListeAttenteNotificationService.NotificationResult notif =
                        listeAttenteNotifService.verifierEtNotifier(rdv);
                chargerRendezVous();
                if (notif != null) {
                    showInfo("RDV Annulé", "🔔 " + notif.message);
                }
            } catch (SQLException e) {
                showAlert("Erreur", "Impossible d'annuler : " + e.getMessage());
            }
        }
    }

    private void supprimerRdv(RendezVous rdv) {
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
        conf.setTitle("Supprimer");
        conf.setHeaderText("Supprimer ce rendez-vous ?");
        conf.setContentText("Cette action est irréversible.");
        if (conf.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                serviceRendezVous.supprimer(rdv.getId());
                chargerRendezVous();
            } catch (SQLException e) {
                showAlert("Erreur", "Impossible de supprimer : " + e.getMessage());
            }
        }
    }

    private void ouvrirGoogleCalendar(RendezVous rdv) {
        boolean ok = googleCalendarService.ouvrirDansGoogleCalendar(rdv);
        if (!ok) {
            String lien = googleCalendarService.genererLien(rdv);
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Google Calendar");
            a.setHeaderText("Lien Google Calendar");
            a.setContentText(lien);
            a.getDialogPane().setPrefWidth(600);
            a.showAndWait();
        }
    }

    private void ouvrirHistoriqueRdv(RendezVous rdv) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Historique.fxml"));
            Parent root = loader.load();
            HistoriqueController ctrl = loader.getController();
            ctrl.initHistorique(rdv);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("📋 Historique RDV");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir l'historique : " + e.getMessage());
        }
    }

    @FXML
    private void ouvrirSuggestion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SuggestionDisponibilite.fxml"));
            Parent root = loader.load();
            SuggestionController ctrl = loader.getController();
            ctrl.setParentController(this);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("💡 Suggestion de Créneaux");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir la suggestion : " + e.getMessage());
        }
    }

    @FXML
    private void ouvrirListeAttente() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ListeAttente.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("⏳ Liste d'Attente");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir la liste d'attente : " + e.getMessage());
        }
    }

    private void ouvrirFormulaireRendezVous(RendezVous rdv) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FormulaireRendezVous.fxml"));
            Parent root = loader.load();
            FormulaireRendezVousController ctrl = loader.getController();
            ctrl.setRendezVous(rdv);
            ctrl.setParentController(this);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(rdv == null ? "Nouveau Rendez-vous" : "Modifier Rendez-vous");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    public void rafraichir() { chargerRendezVous(); }

    // =========================================================================
    // Helpers visuels
    // =========================================================================

    private Label buildBadge(String statut) {
        Label badge = new Label(statut != null ? statut : "");
        String bg, fg;
        switch (nvl(statut, "")) {
            case "DEMANDE"  -> { bg = "#fef3c7"; fg = "#d97706"; }
            case "CONFIRME" -> { bg = "#d1fae5"; fg = "#059669"; }
            case "ANNULE"   -> { bg = "#fee2e2"; fg = "#dc2626"; }
            case "TERMINE"  -> { bg = "#dbeafe"; fg = "#2563eb"; }
            default         -> { bg = "#f3f4f6"; fg = "#6b7280"; }
        }
        badge.setStyle(
            "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";" +
            "-fx-padding: 5 12; -fx-background-radius: 20;" +
            "-fx-font-size: 11px; -fx-font-weight: 700;"
        );
        return badge;
    }

    private Button btn(String text, String color) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color: " + color + "; -fx-text-fill: white;" +
            "-fx-font-weight: 600; -fx-padding: 8 14;" +
            "-fx-background-radius: 7; -fx-cursor: hand; -fx-font-size: 12px;"
        );
        return b;
    }

    private String nvl(String s, String def) {
        return (s == null || s.isEmpty()) ? def : s;
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}
