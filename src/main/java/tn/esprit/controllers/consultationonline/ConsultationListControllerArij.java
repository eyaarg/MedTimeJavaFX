package tn.esprit.controllers.consultationonline;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.esprit.entities.consultationonline.ConsultationArij;
import tn.esprit.services.consultationonline.ConsultationServiceArij;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ConsultationListControllerArij {
    private static final String CURRENT_USER_ROLE = "PATIENT";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy  HH:mm");

    @FXML private ComboBox<String> filterStatusCombo;
    @FXML private ComboBox<String> filterTypeCombo;
    @FXML private TextField searchField;
    @FXML private FlowPane cardsContainer;
    @FXML private Button newConsultationButton;

    private final ConsultationServiceArij service = new ConsultationServiceArij();

    @FXML
    private void initialize() {
        filterStatusCombo.setItems(FXCollections.observableArrayList("EN_ATTENTE", "CONFIRMEE", "REFUSEE", "TERMINEE"));
        filterTypeCombo.setItems(FXCollections.observableArrayList("ONLINE", "IN_PERSON"));
        if (!"PATIENT".equalsIgnoreCase(CURRENT_USER_ROLE)) newConsultationButton.setVisible(false);
        loadConsultations(service.getMyConsultations());
    }

    @FXML private void onStatusFilterChange() { filterConsultations(); }
    @FXML private void onTypeFilterChange()   { filterConsultations(); }
    @FXML private void onSearch()             { filterConsultations(); }
    @FXML private void openNewConsultation()  { openForm(null); }

    private void filterConsultations() {
        String status = filterStatusCombo.getValue();
        String type   = filterTypeCombo.getValue();
        List<ConsultationArij> list = service.filterConsultations(
                status == null ? "" : status, type == null ? "" : type);
        String q = searchField.getText();
        if (q != null && !q.isEmpty()) {
            String lo = q.toLowerCase();
            list = list.stream().filter(c ->
                String.valueOf(c.getDoctorId()).contains(lo) ||
                (c.getStatus() != null && c.getStatus().toLowerCase().contains(lo)) ||
                (c.getLienMeet() != null && c.getLienMeet().toLowerCase().contains(lo))
            ).collect(Collectors.toList());
        }
        loadConsultations(list);
    }

    private void loadConsultations(List<ConsultationArij> list) {
        cardsContainer.getChildren().clear();

        if (list.isEmpty()) {
            cardsContainer.getChildren().add(emptyState());
            return;
        }

        for (ConsultationArij c : list) {
            cardsContainer.getChildren().add(buildCard(c));
        }
    }

    private VBox buildCard(ConsultationArij c) {
        VBox card = new VBox(0);
        card.getStyleClass().add("consult-card");
        card.setPrefWidth(280);

        // ── Color bar top ──
        Region bar = new Region();
        bar.setPrefHeight(4);
        bar.setStyle("-fx-background-color:" + statusHex(c.getStatus()) + ";"
                + "-fx-background-radius:12 12 0 0;");

        // ── Body ──
        VBox body = new VBox(10);
        body.setPadding(new Insets(16, 18, 14, 18));

        // Header row: status badge + type chip
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Label badge = statusBadge(c.getStatus());
        Label typeChip = chip(c.getType() != null ? c.getType() : "—", "#eff6ff", "#1d4ed8");
        headerRow.getChildren().addAll(badge, typeChip);

        // Date
        Label dateLabel = new Label("📅  " + (c.getConsultationDate() != null
                ? c.getConsultationDate().format(FMT) : "Date non définie"));
        dateLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#1e293b;-fx-font-weight:bold;");

        // Doctor / Patient
        String counterText = "PATIENT".equalsIgnoreCase(CURRENT_USER_ROLE)
                ? "👨‍⚕️  Médecin #" + c.getDoctorId()
                : "🧑  Patient #" + c.getPatientId();
        Label counterLabel = new Label(counterText);
        counterLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#64748b;");

        // Meet link
        if (c.getLienMeet() != null && !c.getLienMeet().isEmpty()) {
            Label link = new Label("🔗  " + c.getLienMeet());
            link.setStyle("-fx-font-size:11px;-fx-text-fill:#2563eb;");
            link.setWrapText(true);
            body.getChildren().addAll(headerRow, dateLabel, counterLabel, link);
        } else {
            body.getChildren().addAll(headerRow, dateLabel, counterLabel);
        }

        // ── Action buttons ──
        HBox actions = new HBox(8);
        actions.setPadding(new Insets(0, 18, 16, 18));
        actions.setAlignment(Pos.CENTER_LEFT);

        boolean isPending = "EN_ATTENTE".equalsIgnoreCase(c.getStatus());
        boolean isDoctor  = "DOCTOR".equalsIgnoreCase(CURRENT_USER_ROLE);

        if (isPending) {
            Button editBtn = actionBtn("✏  Modifier", "btn-action-edit");
            editBtn.setOnAction(e -> openForm(c));
            actions.getChildren().add(editBtn);
        }
        if (isDoctor) {
            Button acceptBtn = actionBtn("✓  Accepter", "btn-action-accept");
            acceptBtn.setOnAction(e -> { service.acceptConsultation(c.getId()); refreshList(); });
            Button refuseBtn = actionBtn("✕  Refuser", "btn-action-refuse");
            refuseBtn.setOnAction(e -> {
                TextInputDialog dlg = new TextInputDialog("Indisponible");
                dlg.setHeaderText("Refuser la consultation #" + c.getId());
                dlg.setContentText("Motif :");
                dlg.showAndWait().ifPresent(r ->
                    service.rejectConsultation(c.getId(), r.isEmpty() ? "Aucun motif" : r));
                refreshList();
            });
            actions.getChildren().addAll(acceptBtn, refuseBtn);
        }
        Button deleteBtn = actionBtn("🗑  Supprimer", "btn-action-delete");
        deleteBtn.setOnAction(e -> { service.deleteConsultation(c.getId()); refreshList(); });
        actions.getChildren().add(deleteBtn);

        card.getChildren().addAll(bar, body, actions);
        return card;
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Label statusBadge(String status) {
        String label = status != null ? status.replace("_", " ") : "UNKNOWN";
        String bg, fg;
        switch (status != null ? status.toUpperCase() : "") {
            case "EN_ATTENTE": bg = "#fef9c3"; fg = "#854d0e"; break;
            case "CONFIRMEE":  bg = "#dcfce7"; fg = "#166534"; break;
            case "REFUSEE":    bg = "#fee2e2"; fg = "#991b1b"; break;
            case "TERMINEE":   bg = "#e0f2fe"; fg = "#075985"; break;
            default:           bg = "#f1f5f9"; fg = "#475569";
        }
        return chip(label, bg, fg);
    }

    private Label chip(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";"
                + "-fx-font-size:10px;-fx-font-weight:bold;"
                + "-fx-padding:3 9 3 9;-fx-background-radius:20;");
        return l;
    }

    private String statusHex(String status) {
        if (status == null) return "#94a3b8";
        switch (status.toUpperCase()) {
            case "EN_ATTENTE": return "#f59e0b";
            case "CONFIRMEE":  return "#22c55e";
            case "REFUSEE":    return "#ef4444";
            case "TERMINEE":   return "#3b82f6";
            default:           return "#94a3b8";
        }
    }

    private Button actionBtn(String text, String styleClass) {
        Button b = new Button(text);
        b.getStyleClass().addAll("action-btn", styleClass);
        return b;
    }

    private VBox emptyState() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60));
        Label icon = new Label("📋");
        icon.setStyle("-fx-font-size:48px;");
        Label msg = new Label("Aucune consultation trouvée");
        msg.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#64748b;");
        Label sub = new Label("Prenez un nouveau rendez-vous pour commencer");
        sub.setStyle("-fx-font-size:13px;-fx-text-fill:#94a3b8;");
        box.getChildren().addAll(icon, msg, sub);
        return box;
    }

    private void refreshList() { loadConsultations(service.getMyConsultations()); }

    private void openForm(ConsultationArij consultation) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/fxml/consultationonline/ConsultationFormArij.fxml")));
            Parent root = loader.load();
            if (consultation != null)
                ((ConsultationFormControllerArij) loader.getController()).setConsultation(consultation);
            Stage stage = new Stage();
            stage.setTitle(consultation == null ? "Nouvelle Consultation" : "Modifier la Consultation");
            stage.setScene(new Scene(root));
            stage.setOnHidden(e -> refreshList());
            stage.show();
        } catch (IOException e) {
            System.err.println("Error opening form: " + e.getMessage());
        }
    }
}
