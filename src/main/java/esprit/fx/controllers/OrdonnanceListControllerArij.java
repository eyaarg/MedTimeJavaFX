package esprit.fx.controllers;

import esprit.fx.entities.*;
import esprit.fx.services.*;
import esprit.fx.utils.PdfExporterArij;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class OrdonnanceListControllerArij {

    // ── Rôle courant ──────────────────────────────────────────────────────
    private static final String ROLE      = "PATIENT";  // "PATIENT" ou "DOCTOR"
    private static final int    USER_ID   = 1;
    private static final int    PATIENT_ID = 1; // id du patient courant
    // ─────────────────────────────────────────────────────────────────────

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @FXML private FlowPane cardsContainer;
    @FXML private Button   newOrdonnanceButton;

    private final ServiceConsultationsArij consultationService = new ServiceConsultationsArij();
    private final ServiceOrdonnanceArij    ordonnanceService   = new ServiceOrdonnanceArij();
    private final ServiceFactureArij       factureService      = new ServiceFactureArij();

    @FXML
    private void initialize() {
        newOrdonnanceButton.setVisible("DOCTOR".equalsIgnoreCase(ROLE));
        newOrdonnanceButton.setManaged("DOCTOR".equalsIgnoreCase(ROLE));
        try {
            loadOrdonnances();
        } catch (Exception e) {
            System.err.println("Erreur initialize OrdonnanceList: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void openNewOrdonnance() { openForm(null, 0); }

    private void loadOrdonnances() {
        cardsContainer.getChildren().clear();
        List<OrdonnanceArij> ordonnances = new ArrayList<>();
        for (ConsultationsArij c : consultationService.getMyConsultations()) {
            OrdonnanceArij o = ordonnanceService.getByConsultationId(c.getId());
            if (o != null) ordonnances.add(o);
        }
        if (ordonnances.isEmpty()) { cardsContainer.getChildren().add(emptyState()); return; }
        for (OrdonnanceArij o : ordonnances) cardsContainer.getChildren().add(buildCard(o));
    }

    // ── Card builder ──────────────────────────────────────────────────────

    private VBox buildCard(OrdonnanceArij o) {
        VBox card = new VBox(0);
        card.getStyleClass().add("rx-card");
        card.setPrefWidth(290);

        // Barre verte
        Region bar = new Region();
        bar.setPrefHeight(5);
        bar.setStyle("-fx-background-color:#10b981;-fx-background-radius:12 12 0 0;");

        // Corps
        VBox body = new VBox(10);
        body.setPadding(new Insets(16, 18, 12, 18));

        // En-tête
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("💊"); icon.setStyle("-fx-font-size:22px;");
        VBox titleBlock = new VBox(2);
        Label num = new Label("Rx #" + (o.getNumeroOrdonnance() != null ? o.getNumeroOrdonnance() : o.getId()));
        num.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#0f172a;");
        Label dateLabel = new Label("📅  " + (o.getDateEmission() != null ? o.getDateEmission().format(FMT) : "N/A"));
        dateLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#64748b;");
        titleBlock.getChildren().addAll(num, dateLabel);
        header.getChildren().addAll(icon, titleBlock);

        Label doctorLabel = new Label("👨‍⚕️  Médecin #" + o.getDoctorId());
        doctorLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#64748b;");

        body.getChildren().addAll(header, doctorLabel);

        if (o.getDiagnosis() != null && !o.getDiagnosis().isEmpty()) {
            Label diag = new Label("🩺  " + o.getDiagnosis());
            diag.setStyle("-fx-font-size:12px;-fx-text-fill:#1e293b;-fx-font-weight:bold;");
            diag.setWrapText(true);
            body.getChildren().add(diag);
        }

        // Facture associée (si patient)
        if ("PATIENT".equalsIgnoreCase(ROLE)) {
            FactureArij facture = factureService.findByOrdonnanceId(o.getId());
            if (facture != null) {
                Label feeLabel = new Label("💰  " + String.format("%.2f TND", facture.getMontant()));
                feeLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#2563eb;-fx-font-weight:bold;");
                body.getChildren().add(feeLabel);
            }
        }

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#f1f5f9;");

        HBox actions = buildActions(o);
        card.getChildren().addAll(bar, body, sep, actions);
        return card;
    }

    private HBox buildActions(OrdonnanceArij o) {
        HBox actions = new HBox(6);
        actions.setPadding(new Insets(10, 14, 12, 14));
        actions.setAlignment(Pos.CENTER_LEFT);

        if ("PATIENT".equalsIgnoreCase(ROLE)) {
            // Patient : imprimer ordonnance + imprimer facture
            Button printOrd = iconBtn("🖨", "btn-icon-primary", "Imprimer l'ordonnance");
            printOrd.setOnAction(e -> {
                List<LigneOrdonnanceArij> lignes = ordonnanceService.getLignesByOrdonnanceId(o.getId());
                PdfExporterArij.exportOrdonnance(o, lignes,
                        System.getProperty("user.home") + "/ordonnance-" + o.getId() + ".pdf");
                showInfo("Ordonnance exportée dans votre dossier personnel.");
            });

            Button printFac = iconBtn("🧾", "btn-icon-secondary", "Imprimer la facture");
            printFac.setOnAction(e -> {
                FactureArij facture = factureService.findByOrdonnanceId(o.getId());
                if (facture != null) {
                    PdfExporterArij.exportFacture(facture,
                            System.getProperty("user.home") + "/facture-" + facture.getId() + ".pdf");
                    showInfo("Facture exportée dans votre dossier personnel.");
                } else {
                    showInfo("Aucune facture associée à cette ordonnance.");
                }
            });
            actions.getChildren().addAll(printOrd, printFac);

        } else {
            // Médecin : supprimer ordonnance
            Button del = iconBtn("🗑", "btn-icon-danger", "Supprimer l'ordonnance");
            del.setOnAction(e -> {
                if (confirm("Supprimer cette ordonnance ?")) {
                    ordonnanceService.deleteOrdonnance(o.getId());
                    loadOrdonnances();
                }
            });
            actions.getChildren().add(del);
        }

        return actions;
    }

    private void openForm(OrdonnanceArij ordonnance, int consultationId) {
        // Pour le médecin : choisir la consultation d'abord si nouvelle ordonnance
        if (ordonnance == null) {
            // Sélectionner une consultation confirmée sans ordonnance
            List<ConsultationsArij> consultations = consultationService.getMyConsultations();
            List<ConsultationsArij> eligibles = new ArrayList<>();
            for (ConsultationsArij c : consultations) {
                if ("CONFIRMEE".equalsIgnoreCase(c.getStatus())
                        && ordonnanceService.getByConsultationId(c.getId()) == null) {
                    eligibles.add(c);
                }
            }
            if (eligibles.isEmpty()) {
                showInfo("Aucune consultation confirmée sans ordonnance disponible.");
                return;
            }
            // Dialogue de sélection
            ChoiceDialog<String> dlg = new ChoiceDialog<>();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            Map<String, ConsultationsArij> map = new LinkedHashMap<>();
            for (ConsultationsArij c : eligibles) {
                String key = "Consultation #" + c.getId() + " — Patient #" + c.getPatientId()
                        + (c.getConsultationDate() != null ? " — " + c.getConsultationDate().format(fmt) : "");
                map.put(key, c);
                dlg.getItems().add(key);
            }
            dlg.setSelectedItem(dlg.getItems().get(0));
            dlg.setTitle("Sélectionner une consultation");
            dlg.setHeaderText("Choisissez la consultation pour cette ordonnance");
            dlg.setContentText("Consultation :");
            Optional<String> result = dlg.showAndWait();
            if (result.isEmpty()) return;
            ConsultationsArij selected = map.get(result.get());
            openOrdonnanceForm(null, selected.getId(), selected.getPatientId());
        } else {
            openOrdonnanceForm(ordonnance, ordonnance.getConsultationId(), 0);
        }
    }

    private void openOrdonnanceForm(OrdonnanceArij ordonnance, int consultationId, int patientId) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    OrdonnanceListControllerArij.class.getResource("/fxml/OrdonnanceFormArij.fxml")));
            Parent root = loader.load();
            OrdonnanceFormControllerArij ctrl = loader.getController();
            ctrl.setContext(consultationId, patientId);
            if (ordonnance != null) ctrl.setOrdonnance(ordonnance);
            Stage stage = new Stage();
            stage.setTitle(ordonnance == null ? "Nouvelle Ordonnance" : "Modifier l'Ordonnance");
            stage.setScene(new Scene(root));
            stage.setOnHidden(e -> loadOrdonnances());
            stage.show();
        } catch (IOException e) { System.err.println("Erreur: " + e.getMessage()); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Button iconBtn(String icon, String styleClass, String tooltip) {
        Button b = new Button(icon);
        b.getStyleClass().addAll("icon-btn", styleClass);
        b.setTooltip(new Tooltip(tooltip));
        return b;
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }

    private VBox emptyState() {
        VBox box = new VBox(12); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(80));
        Label icon = new Label("💊"); icon.setStyle("-fx-font-size:52px;");
        Label msg  = new Label("Aucune ordonnance trouvée");
        msg.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#64748b;");
        Label sub  = new Label("DOCTOR".equalsIgnoreCase(ROLE)
                ? "Créez une ordonnance depuis une consultation confirmée"
                : "Vos ordonnances apparaîtront ici après vos consultations");
        sub.setStyle("-fx-font-size:12px;-fx-text-fill:#94a3b8;");
        box.getChildren().addAll(icon, msg, sub);
        return box;
    }
}
