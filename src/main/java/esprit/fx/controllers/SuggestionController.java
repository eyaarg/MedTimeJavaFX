package esprit.fx.controllers;

import esprit.fx.entities.RendezVous;
import esprit.fx.entities.User;
import esprit.fx.services.ServiceRendezVous;
import esprit.fx.services.SuggestionService;
import esprit.fx.utils.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class SuggestionController implements Initializable {

    @FXML private ComboBox<String> comboDocteur;
    @FXML private ComboBox<String> comboPlage;
    @FXML private DatePicker       datePickerSemaine;
    @FXML private Button           btnSuggerer;
    @FXML private Button           btnFermer;
    @FXML private VBox             suggestionsContainer;
    @FXML private Label            labelStatus;

    private SuggestionService    suggestionService;
    private ServiceRendezVous    serviceRendezVous;
    private RendezVousController parentController;

    // Map doctorId par index du combo
    private List<User> doctorsList;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy", java.util.Locale.FRENCH);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        suggestionService = new SuggestionService();
        serviceRendezVous = new ServiceRendezVous();

        comboPlage.getItems().addAll("Tous", "Matin", "Après-midi", "Soir");
        comboPlage.setValue("Tous");

        datePickerSemaine.setValue(LocalDate.now());

        chargerMedecins();
    }

    public void setParentController(RendezVousController ctrl) {
        this.parentController = ctrl;
    }

    private void chargerMedecins() {
        try {
            doctorsList = serviceRendezVous.getAllDoctors();
            for (User d : doctorsList) {
                comboDocteur.getItems().add(d.getId() + " — Dr. " + d.getUsername());
            }
            if (!comboDocteur.getItems().isEmpty()) {
                comboDocteur.setValue(comboDocteur.getItems().get(0));
            }
        } catch (SQLException e) {
            labelStatus.setText("Erreur chargement médecins : " + e.getMessage());
        }
    }

    @FXML
    private void lancerSuggestion() {
        suggestionsContainer.getChildren().clear();
        labelStatus.setText("⏳ Recherche des créneaux libres...");
        labelStatus.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 13px;");
        btnSuggerer.setDisable(true);

        if (comboDocteur.getValue() == null) {
            labelStatus.setText("⚠️ Veuillez sélectionner un médecin.");
            labelStatus.setStyle("-fx-text-fill: #d97706;");
            btnSuggerer.setDisable(false);
            return;
        }

        int doctorId = Integer.parseInt(comboDocteur.getValue().split(" — ")[0]);
        String plage = comboPlage.getValue();
        LocalDate semaine = datePickerSemaine.getValue() != null
                ? SuggestionService.getLundiDeSemaine(datePickerSemaine.getValue())
                : SuggestionService.getLundiDeSemaine(LocalDate.now());

        // Algorithme en thread séparé
        Thread t = new Thread(() -> {
            try {
                List<SuggestionService.Creneau> suggestions =
                        suggestionService.suggerer(doctorId, plage, semaine, 3);
                Platform.runLater(() -> afficherSuggestions(suggestions, doctorId));
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    labelStatus.setText("Erreur : " + e.getMessage());
                    labelStatus.setStyle("-fx-text-fill: #dc2626;");
                    btnSuggerer.setDisable(false);
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void afficherSuggestions(List<SuggestionService.Creneau> suggestions,
                                     int doctorId) {
        btnSuggerer.setDisable(false);
        suggestionsContainer.getChildren().clear();

        if (suggestions.isEmpty()) {
            labelStatus.setText("😔 Aucun créneau libre trouvé pour cette semaine et plage horaire.");
            labelStatus.setStyle("-fx-text-fill: #d97706; -fx-font-size: 13px;");
            return;
        }

        labelStatus.setText("✅ " + suggestions.size() + " créneau(x) disponible(s) trouvé(s) !");
        labelStatus.setStyle("-fx-text-fill: #059669; -fx-font-size: 13px; -fx-font-weight: 700;");

        int[] index = {1};
        for (SuggestionService.Creneau c : suggestions) {
            suggestionsContainer.getChildren().add(
                    buildCreneauCard(c, index[0]++, doctorId)
            );
        }
    }

    private VBox buildCreneauCard(SuggestionService.Creneau creneau, int num, int doctorId) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);" +
            "-fx-border-color: #e0e7ff; -fx-border-radius: 12; -fx-border-width: 1;"
        );

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        // Numéro
        Label numLabel = new Label("✅ Créneau " + num);
        numLabel.setStyle(
            "-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #059669;"
        );

        // Plage badge
        String plageLabel = getPlageLabel(creneau.debut.toLocalTime());
        Label plageBadge = new Label(plageLabel);
        plageBadge.setStyle(
            "-fx-background-color: #ede9fe; -fx-text-fill: #7c3aed;" +
            "-fx-padding: 3 10; -fx-background-radius: 12;" +
            "-fx-font-size: 11px; -fx-font-weight: 700;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(numLabel, spacer, plageBadge);

        // Date/heure
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy", java.util.Locale.FRENCH);
        DateTimeFormatter hFmt = DateTimeFormatter.ofPattern("HH:mm");
        Label dateLabel = new Label(
            "📅 " + creneau.debut.format(fmt) +
            "  🕐 " + creneau.debut.format(hFmt) + " — " + creneau.fin.format(hFmt)
        );
        dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #1f2937; -fx-font-weight: 600;");

        // Bouton réserver
        Button btnReserver = new Button("📋 Réserver ce créneau");
        btnReserver.setStyle(
            "-fx-background-color: #4f46e5; -fx-text-fill: white;" +
            "-fx-font-weight: 700; -fx-padding: 10 20;" +
            "-fx-background-radius: 8; -fx-cursor: hand;"
        );
        btnReserver.setOnAction(e -> reserverCreneau(creneau, doctorId));

        card.getChildren().addAll(header, dateLabel, btnReserver);
        VBox.setMargin(card, new Insets(0, 0, 8, 0));
        return card;
    }

    private void reserverCreneau(SuggestionService.Creneau creneau, int doctorId) {
        try {
            User currentUser = UserSession.getCurrentUser();
            if (currentUser == null) {
                labelStatus.setText("⚠️ Vous devez être connecté pour réserver.");
                return;
            }

            RendezVous rdv = new RendezVous();
            rdv.setPatientId(currentUser.getId());
            rdv.setDoctorId(doctorId);
            rdv.setDateHeure(creneau.debut);
            rdv.setMotif("Réservation via suggestion automatique");
            rdv.setStatut("DEMANDE");

            serviceRendezVous.ajouter(rdv);

            labelStatus.setText("🎉 RDV créé avec succès pour le " +
                creneau.debut.format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
            labelStatus.setStyle("-fx-text-fill: #059669; -fx-font-weight: 700;");

            if (parentController != null) parentController.rafraichir();

            // Fermer après 2 secondes
            Thread closer = new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                Platform.runLater(this::fermer);
            });
            closer.setDaemon(true);
            closer.start();

        } catch (SQLException e) {
            labelStatus.setText("Erreur création RDV : " + e.getMessage());
            labelStatus.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    private String getPlageLabel(LocalTime t) {
        if (!t.isBefore(SuggestionService.MATIN_DEBUT) && t.isBefore(SuggestionService.MATIN_FIN))
            return "🌅 Matin";
        if (!t.isBefore(SuggestionService.APREM_DEBUT) && t.isBefore(SuggestionService.APREM_FIN))
            return "☀️ Après-midi";
        if (!t.isBefore(SuggestionService.SOIR_DEBUT) && t.isBefore(SuggestionService.SOIR_FIN))
            return "🌆 Soir";
        return "🕐 Autre";
    }

    @FXML
    private void fermer() {
        Stage stage = (Stage) btnFermer.getScene().getWindow();
        stage.close();
    }
}
