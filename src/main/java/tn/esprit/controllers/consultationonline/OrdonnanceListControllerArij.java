package tn.esprit.controllers.consultationonline;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import tn.esprit.entities.consultationonline.ConsultationArij;
import tn.esprit.entities.consultationonline.LigneOrdonnanceArij;
import tn.esprit.entities.consultationonline.OrdonnanceArij;
import tn.esprit.repositories.consultationonline.LigneOrdonnanceRepositoryArij;
import tn.esprit.services.consultationonline.ConsultationServiceArij;
import tn.esprit.services.consultationonline.OrdonnanceServiceArij;
import tn.esprit.utils.consultationonline.PdfExporterArij;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OrdonnanceListControllerArij {
    private static final String CURRENT_USER_ROLE = "PATIENT";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML private FlowPane cardsContainer;
    @FXML private Button newOrdonnanceButton;

    private final ConsultationServiceArij consultationService = new ConsultationServiceArij();
    private final OrdonnanceServiceArij ordonnanceService     = new OrdonnanceServiceArij();
    private final LigneOrdonnanceRepositoryArij ligneRepo     = new LigneOrdonnanceRepositoryArij();

    @FXML
    private void initialize() {
        newOrdonnanceButton.setVisible("DOCTOR".equalsIgnoreCase(CURRENT_USER_ROLE));
        loadOrdonnances();
    }

    @FXML private void openNewOrdonnance() { /* wire to form */ }

    private void loadOrdonnances() {
        cardsContainer.getChildren().clear();
        List<OrdonnanceArij> ordonnances = new ArrayList<>();
        for (ConsultationArij c : consultationService.getMyConsultations()) {
            OrdonnanceArij o = ordonnanceService.getByConsultationId(c.getId());
            if (o != null) ordonnances.add(o);
        }

        if (ordonnances.isEmpty()) {
            cardsContainer.getChildren().add(emptyState());
            return;
        }

        for (OrdonnanceArij o : ordonnances) {
            cardsContainer.getChildren().add(buildCard(o));
        }
    }

    private VBox buildCard(OrdonnanceArij o) {
        VBox card = new VBox(0);
        card.getStyleClass().add("rx-card");
        card.setPrefWidth(280);

        // Accent bar
        Region bar = new Region();
        bar.setPrefHeight(4);
        bar.setStyle("-fx-background-color:#10b981;-fx-background-radius:12 12 0 0;");

        VBox body = new VBox(10);
        body.setPadding(new Insets(16, 18, 8, 18));

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("💊");
        icon.setStyle("-fx-font-size:20px;");
        Label num = new Label("Rx #" + (o.getNumeroOrdonnance() != null ? o.getNumeroOrdonnance() : o.getId()));
        num.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#0f172a;");
        header.getChildren().addAll(icon, num);

        Label dateLabel = new Label("📅  " + (o.getDateEmission() != null
                ? o.getDateEmission().format(FMT) : "N/A"));
        dateLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#64748b;");

        Label doctorLabel = new Label("👨‍⚕️  Médecin #" + o.getDoctorId());
        doctorLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#64748b;");

        if (o.getDiagnosis() != null && !o.getDiagnosis().isEmpty()) {
            Label diag = new Label("🩺  " + o.getDiagnosis());
            diag.setStyle("-fx-font-size:12px;-fx-text-fill:#1e293b;-fx-font-weight:bold;");
            diag.setWrapText(true);
            body.getChildren().addAll(header, dateLabel, doctorLabel, diag);
        } else {
            body.getChildren().addAll(header, dateLabel, doctorLabel);
        }

        // Actions
        HBox actions = new HBox(8);
        actions.setPadding(new Insets(4, 18, 16, 18));

        Button printBtn = new Button("⬇  Imprimer PDF");
        printBtn.getStyleClass().addAll("action-btn", "btn-action-edit");
        printBtn.setOnAction(e -> {
            List<LigneOrdonnanceArij> lignes = ligneRepo.findByOrdonnanceId(o.getId());
            String path = System.getProperty("user.home") + "/ordonnance-" + o.getId() + ".pdf";
            PdfExporterArij.exportOrdonnance(o, lignes, path);
        });

        if ("DOCTOR".equalsIgnoreCase(CURRENT_USER_ROLE)) {
            Button deleteBtn = new Button("🗑");
            deleteBtn.getStyleClass().addAll("action-btn", "btn-action-delete");
            deleteBtn.setOnAction(e -> { ordonnanceService.deleteOrdonnance(o.getId()); loadOrdonnances(); });
            actions.getChildren().addAll(printBtn, deleteBtn);
        } else {
            actions.getChildren().add(printBtn);
        }

        card.getChildren().addAll(bar, body, actions);
        return card;
    }

    private VBox emptyState() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60));
        Label icon = new Label("💊");
        icon.setStyle("-fx-font-size:48px;");
        Label msg = new Label("Aucune ordonnance trouvée");
        msg.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#64748b;");
        Label sub = new Label("Vos ordonnances apparaîtront ici après vos consultations");
        sub.setStyle("-fx-font-size:13px;-fx-text-fill:#94a3b8;");
        box.getChildren().addAll(icon, msg, sub);
        return box;
    }
}
