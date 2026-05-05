package esprit.fx.controllers;

import esprit.fx.entities.HistoriqueRendezVous;
import esprit.fx.entities.RendezVous;
import esprit.fx.services.HistoriqueService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class HistoriqueController implements Initializable {

    @FXML private VBox  timelineContainer;
    @FXML private Label labelRdvInfo;
    @FXML private Label labelNbChangements;
    @FXML private Label labelDuree;
    @FXML private Button btnFermer;

    private HistoriqueService historiqueService;
    private RendezVous rdv;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        historiqueService = new HistoriqueService();
    }

    /** Appelé depuis RendezVousController pour initialiser avec le RDV sélectionné. */
    public void initHistorique(RendezVous rdv) {
        this.rdv = rdv;
        afficherInfoRdv();
        chargerTimeline();
    }

    private void afficherInfoRdv() {
        String date = rdv.getDateHeure() != null
                ? rdv.getDateHeure().format(FMT) : "N/A";
        labelRdvInfo.setText(
            "👤 " + (rdv.getPatientNom() != null ? rdv.getPatientNom() : "Patient") +
            "  ·  👨‍⚕️ Dr. " + (rdv.getDoctorNom() != null ? rdv.getDoctorNom() : "Médecin") +
            "  ·  📅 " + date
        );
    }

    private void chargerTimeline() {
        timelineContainer.getChildren().clear();
        try {
            List<HistoriqueRendezVous> historique =
                    historiqueService.getHistoriqueParRdv(rdv.getId());

            if (historique.isEmpty()) {
                Label vide = new Label("Aucun historique disponible pour ce rendez-vous.");
                vide.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280; -fx-padding: 20;");
                timelineContainer.getChildren().add(vide);
                labelNbChangements.setText("0 changement");
                labelDuree.setText("—");
                return;
            }

            // Stats
            labelNbChangements.setText(historique.size() + " changement(s)");
            calculerDuree(historique);

            // Timeline
            for (int i = 0; i < historique.size(); i++) {
                HistoriqueRendezVous h = historique.get(i);
                timelineContainer.getChildren().add(buildTimelineItem(h, i == 0, i == historique.size() - 1));
            }

        } catch (SQLException e) {
            Label err = new Label("Erreur chargement : " + e.getMessage());
            err.setStyle("-fx-text-fill: #ef4444;");
            timelineContainer.getChildren().add(err);
        }
    }

    private VBox buildTimelineItem(HistoriqueRendezVous h, boolean isFirst, boolean isLast) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.TOP_LEFT);

        // Colonne gauche : ligne + point
        VBox lineCol = new VBox(0);
        lineCol.setAlignment(Pos.TOP_CENTER);
        lineCol.setPrefWidth(40);

        // Ligne du haut (cachée pour le 1er)
        Region lineTop = new Region();
        lineTop.setPrefWidth(2);
        lineTop.setPrefHeight(16);
        lineTop.setStyle(isFirst
            ? "-fx-background-color: transparent;"
            : "-fx-background-color: #c7d2fe;");

        // Point coloré selon le statut
        Label point = new Label("●");
        point.setStyle("-fx-font-size: 18px; -fx-text-fill: " + couleurStatut(h.getNouveauStatut()) + ";");

        // Ligne du bas (cachée pour le dernier)
        Region lineBottom = new Region();
        lineBottom.setPrefWidth(2);
        lineBottom.setPrefHeight(30);
        VBox.setVgrow(lineBottom, Priority.ALWAYS);
        lineBottom.setStyle(isLast
            ? "-fx-background-color: transparent;"
            : "-fx-background-color: #c7d2fe;");

        lineCol.getChildren().addAll(lineTop, point, lineBottom);

        // Colonne droite : contenu
        VBox content = new VBox(6);
        content.setPadding(new Insets(8, 0, 20, 12));
        HBox.setHgrow(content, Priority.ALWAYS);

        // Date
        String dateStr = h.getDateChangement() != null
                ? h.getDateChangement().format(FMT) : "N/A";
        Label dateLabel = new Label("🕐 " + dateStr);
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");

        // Titre changement
        String titre = buildTitreChangement(h);
        Label titreLabel = new Label(titre);
        titreLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #1f2937;");

        // Badge nouveau statut
        Label badge = new Label(h.getNouveauStatut());
        badge.setStyle(
            "-fx-background-color: " + bgStatut(h.getNouveauStatut()) + ";" +
            "-fx-text-fill: "        + couleurStatut(h.getNouveauStatut()) + ";" +
            "-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: 700;"
        );

        // Modifié par
        String parNom = h.getModifieParNom() != null ? h.getModifieParNom() : "Système";
        Label parLabel = new Label("👤 Par : " + parNom);
        parLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        HBox badgeRow = new HBox(10, badge, parLabel);
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().addAll(dateLabel, titreLabel, badgeRow);

        // Commentaire si présent
        if (h.getCommentaire() != null && !h.getCommentaire().isEmpty()) {
            Label comm = new Label("💬 " + h.getCommentaire());
            comm.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280; -fx-font-style: italic;");
            comm.setWrapText(true);
            content.getChildren().add(comm);
        }

        row.getChildren().addAll(lineCol, content);

        VBox wrapper = new VBox(row);
        wrapper.setStyle(
            "-fx-background-color: white; -fx-background-radius: 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);" +
            "-fx-padding: 4 12;"
        );
        VBox.setMargin(wrapper, new Insets(0, 0, 8, 0));
        return wrapper;
    }

    private String buildTitreChangement(HistoriqueRendezVous h) {
        if (h.getAncienStatut() == null || h.getAncienStatut().isEmpty()) {
            return "📋 RDV créé — statut initial : " + h.getNouveauStatut();
        }
        return "🔄 Statut changé : " + h.getAncienStatut() + " → " + h.getNouveauStatut();
    }

    private void calculerDuree(List<HistoriqueRendezVous> historique) {
        LocalDateTime debut = historique.get(0).getDateChangement();
        LocalDateTime fin   = historique.get(historique.size() - 1).getDateChangement();
        if (debut != null && fin != null) {
            Duration d = Duration.between(debut, fin);
            long jours  = d.toDays();
            long heures = d.toHoursPart();
            long mins   = d.toMinutesPart();
            if (jours > 0)
                labelDuree.setText(jours + "j " + heures + "h " + mins + "min");
            else if (heures > 0)
                labelDuree.setText(heures + "h " + mins + "min");
            else
                labelDuree.setText(mins + " min");
        } else {
            labelDuree.setText("—");
        }
    }

    private String couleurStatut(String statut) {
        if (statut == null) return "#6b7280";
        return switch (statut.toUpperCase()) {
            case "DEMANDE"  -> "#d97706";
            case "CONFIRME" -> "#059669";
            case "ANNULE"   -> "#dc2626";
            case "TERMINE"  -> "#2563eb";
            default         -> "#6b7280";
        };
    }

    private String bgStatut(String statut) {
        if (statut == null) return "#f3f4f6";
        return switch (statut.toUpperCase()) {
            case "DEMANDE"  -> "#fef3c7";
            case "CONFIRME" -> "#d1fae5";
            case "ANNULE"   -> "#fee2e2";
            case "TERMINE"  -> "#dbeafe";
            default         -> "#f3f4f6";
        };
    }

    @FXML
    private void fermer() {
        Stage stage = (Stage) btnFermer.getScene().getWindow();
        stage.close();
    }
}
