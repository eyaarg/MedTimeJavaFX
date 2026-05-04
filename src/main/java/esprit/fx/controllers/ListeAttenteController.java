package esprit.fx.controllers;

import esprit.fx.entities.ListeAttente;
import esprit.fx.services.ListeAttenteService;
import esprit.fx.services.ServiceRendezVous;
import esprit.fx.utils.UserSession;
import esprit.fx.entities.User;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ListeAttenteController implements Initializable {

    // --- Vue inscription (patient) ---
    @FXML private ComboBox<String> comboDocteur;
    @FXML private ComboBox<String> comboPlage;
    @FXML private DatePicker       datePickerSouhaitee;
    @FXML private Button           btnInscrire;
    @FXML private Label            labelStatusInscription;

    // --- Vue liste (médecin/admin) ---
    @FXML private VBox             listeAttenteContainer;
    @FXML private Label            labelNbAttente;

    @FXML private Button           btnFermer;
    @FXML private TabPane          tabPane;

    private ListeAttenteService listeAttenteService;
    private ServiceRendezVous   serviceRendezVous;
    private List<User>          doctorsList;
    private String              currentRole;
    private int                 currentUserId;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        listeAttenteService = new ListeAttenteService();
        serviceRendezVous   = new ServiceRendezVous();

        User currentUser = UserSession.getCurrentUser();
        currentRole      = UserSession.getCurrentRole();
        currentUserId    = currentUser != null ? currentUser.getId() : 0;

        comboPlage.getItems().addAll("Tous", "Matin", "Après-midi", "Soir");
        comboPlage.setValue("Tous");
        datePickerSouhaitee.setValue(LocalDate.now().plusDays(1));

        chargerMedecins();
        chargerListeAttente();
    }

    private void chargerMedecins() {
        try {
            doctorsList = serviceRendezVous.getAllDoctors();
            for (User d : doctorsList) {
                comboDocteur.getItems().add("Dr. " + d.getUsername());
            }
            if (!comboDocteur.getItems().isEmpty())
                comboDocteur.setValue(comboDocteur.getItems().get(0));
        } catch (SQLException e) {
            labelStatusInscription.setText("Erreur chargement médecins.");
        }
    }

    private void chargerListeAttente() {
        listeAttenteContainer.getChildren().clear();
        try {
            List<ListeAttente> liste;
            if ("DOCTOR".equals(currentRole)) {
                liste = listeAttenteService.getAttenteParDocteur(currentUserId);
                int nb = liste.size();
                labelNbAttente.setText(nb + " patient(s) en attente pour vos créneaux");
                labelNbAttente.setStyle(nb > 0
                    ? "-fx-text-fill: #d97706; -fx-font-weight: 700; -fx-font-size: 14px;"
                    : "-fx-text-fill: #059669; -fx-font-weight: 700; -fx-font-size: 14px;");
            } else {
                liste = listeAttenteService.getAttenteParPatient(currentUserId);
                labelNbAttente.setText("Vos inscriptions en liste d'attente : " + liste.size());
                labelNbAttente.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: 700; -fx-font-size: 14px;");
            }

            if (liste.isEmpty()) {
                Label vide = new Label("Aucune inscription en attente.");
                vide.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280; -fx-padding: 20;");
                listeAttenteContainer.getChildren().add(vide);
            } else {
                for (ListeAttente la : liste) {
                    listeAttenteContainer.getChildren().add(buildAttenteCard(la));
                }
            }
        } catch (SQLException e) {
            Label err = new Label("Erreur : " + e.getMessage());
            err.setStyle("-fx-text-fill: #dc2626;");
            listeAttenteContainer.getChildren().add(err);
        }
    }

    @FXML
    private void inscrire() {
        labelStatusInscription.setText("");
        if (comboDocteur.getValue() == null) {
            labelStatusInscription.setText("⚠️ Sélectionnez un médecin.");
            labelStatusInscription.setStyle("-fx-text-fill: #d97706;");
            return;
        }

        int doctorId = doctorsList.stream()
                .filter(d -> ("Dr. " + d.getUsername()).equals(comboDocteur.getValue()))
                .findFirst()
                .map(User::getId)
                .orElse(0);
        LocalDate date = datePickerSouhaitee.getValue();
        String plage   = comboPlage.getValue();

        btnInscrire.setDisable(true);
        Thread t = new Thread(() -> {
            try {
                ListeAttente result = listeAttenteService.inscrire(
                        currentUserId, doctorId, date, plage);
                Platform.runLater(() -> {
                    btnInscrire.setDisable(false);
                    if (result == null) {
                        labelStatusInscription.setText("⚠️ Vous êtes déjà inscrit en attente pour ce médecin.");
                        labelStatusInscription.setStyle("-fx-text-fill: #d97706; -fx-font-size: 13px;");
                    } else {
                        labelStatusInscription.setText(
                            "✅ Inscription confirmée ! Vous serez notifié dès qu'un créneau se libère.");
                        labelStatusInscription.setStyle("-fx-text-fill: #059669; -fx-font-weight: 700; -fx-font-size: 13px;");
                        chargerListeAttente();
                    }
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    btnInscrire.setDisable(false);
                    labelStatusInscription.setText("Erreur : " + e.getMessage());
                    labelStatusInscription.setStyle("-fx-text-fill: #dc2626;");
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private VBox buildAttenteCard(ListeAttente la) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 6, 0, 0, 2);" +
            "-fx-border-color: " + borderCouleur(la.getStatut()) + ";" +
            "-fx-border-radius: 10; -fx-border-width: 1.5;"
        );

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(iconeStatut(la.getStatut()));
        iconLabel.setStyle("-fx-font-size: 20px;");

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);

        String nomAffiche = "DOCTOR".equals(currentRole)
                ? "👤 " + (la.getPatientNom() != null ? la.getPatientNom() : "Patient " + la.getPatientId())
                : "👨‍⚕️ Dr. " + (la.getDoctorNom() != null ? la.getDoctorNom() : "Médecin " + la.getDoctorId());

        Label nomLabel = new Label(nomAffiche);
        nomLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #1f2937;");

        String dateStr = la.getDateSouhaitee() != null
                ? la.getDateSouhaitee().format(FMT) : "Flexible";
        Label detailLabel = new Label(
            "📅 " + dateStr + "  🕐 " + (la.getPlageHoraire() != null ? la.getPlageHoraire() : "Tous")
        );
        detailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        info.getChildren().addAll(nomLabel, detailLabel);

        Label statutBadge = new Label(la.getStatut());
        statutBadge.setStyle(
            "-fx-background-color: " + bgStatut(la.getStatut()) + ";" +
            "-fx-text-fill: "        + couleurStatut(la.getStatut()) + ";" +
            "-fx-padding: 4 10; -fx-background-radius: 12;" +
            "-fx-font-size: 11px; -fx-font-weight: 700;"
        );

        header.getChildren().addAll(iconLabel, info, statutBadge);

        // Date inscription
        String inscriptionStr = la.getDateInscription() != null
                ? la.getDateInscription().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
        Label inscriptionLabel = new Label("Inscrit le : " + inscriptionStr);
        inscriptionLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");

        card.getChildren().addAll(header, inscriptionLabel);

        // Bouton supprimer (patient uniquement, si EN_ATTENTE)
        if (!"DOCTOR".equals(currentRole) && "EN_ATTENTE".equals(la.getStatut())) {
            Button btnSuppr = new Button("🗑 Se désinscrire");
            btnSuppr.setStyle(
                "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;" +
                "-fx-font-weight: 600; -fx-padding: 6 14;" +
                "-fx-background-radius: 6; -fx-cursor: hand;"
            );
            btnSuppr.setOnAction(e -> {
                try {
                    listeAttenteService.supprimer(la.getId());
                    chargerListeAttente();
                } catch (SQLException ex) {
                    labelStatusInscription.setText("Erreur suppression : " + ex.getMessage());
                }
            });
            HBox btnBox = new HBox(btnSuppr);
            btnBox.setAlignment(Pos.CENTER_RIGHT);
            card.getChildren().add(btnBox);
        }

        VBox.setMargin(card, new Insets(0, 0, 8, 0));
        return card;
    }

    // -------------------------------------------------------------------------
    // Helpers visuels
    // -------------------------------------------------------------------------

    private String iconeStatut(String s) {
        if (s == null) return "⏳";
        return switch (s) {
            case "EN_ATTENTE" -> "⏳";
            case "NOTIFIE"    -> "🔔";
            case "EXPIRE"     -> "❌";
            default           -> "⏳";
        };
    }

    private String bgStatut(String s) {
        if (s == null) return "#f3f4f6";
        return switch (s) {
            case "EN_ATTENTE" -> "#fef3c7";
            case "NOTIFIE"    -> "#d1fae5";
            case "EXPIRE"     -> "#fee2e2";
            default           -> "#f3f4f6";
        };
    }

    private String couleurStatut(String s) {
        if (s == null) return "#6b7280";
        return switch (s) {
            case "EN_ATTENTE" -> "#d97706";
            case "NOTIFIE"    -> "#059669";
            case "EXPIRE"     -> "#dc2626";
            default           -> "#6b7280";
        };
    }

    private String borderCouleur(String s) {
        if (s == null) return "#e5e7eb";
        return switch (s) {
            case "EN_ATTENTE" -> "#fde68a";
            case "NOTIFIE"    -> "#6ee7b7";
            case "EXPIRE"     -> "#fca5a5";
            default           -> "#e5e7eb";
        };
    }

    @FXML
    private void fermer() {
        Stage stage = (Stage) btnFermer.getScene().getWindow();
        stage.close();
    }
}
