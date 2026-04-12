package esprit.fx.controllers;

import esprit.fx.entities.ConsultationsArij;
import esprit.fx.services.ServiceConsultationsArij;
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

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ConsultationListControllerArij {

    // ── Rôle courant : changer ici pour tester DOCTOR / PATIENT ──────────
    private static final String ROLE = "PATIENT";   // "PATIENT" ou "DOCTOR"
    private static final int    USER_ID = 1;
    // ─────────────────────────────────────────────────────────────────────

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    @FXML private ComboBox<String> filterStatusCombo;
    @FXML private ComboBox<String> filterTypeCombo;
    @FXML private TextField        searchField;
    @FXML private FlowPane         cardsContainer;
    @FXML private Button           newConsultationButton;

    private final ServiceConsultationsArij service = new ServiceConsultationsArij();

    @FXML
    private void initialize() {
        filterStatusCombo.setItems(FXCollections.observableArrayList("EN_ATTENTE","CONFIRMEE","REFUSEE","TERMINEE"));
        filterTypeCombo.setItems(FXCollections.observableArrayList("ONLINE","IN_PERSON"));
        // Seul le patient peut créer
        newConsultationButton.setVisible("PATIENT".equalsIgnoreCase(ROLE));
        newConsultationButton.setManaged("PATIENT".equalsIgnoreCase(ROLE));
        loadConsultations(service.getMyConsultations());
    }

    @FXML private void onStatusFilterChange() { filterConsultations(); }
    @FXML private void onTypeFilterChange()   { filterConsultations(); }
    @FXML private void onSearch()             { filterConsultations(); }
    @FXML private void openNewConsultation()  { openForm(null); }

    private void filterConsultations() {
        String status = filterStatusCombo.getValue();
        String type   = filterTypeCombo.getValue();
        List<ConsultationsArij> list = service.filterConsultations(
                status == null ? "" : status, type == null ? "" : type);
        String q = searchField.getText();
        if (q != null && !q.isEmpty()) {
            String lo = q.toLowerCase();
            list = list.stream().filter(c ->
                String.valueOf(c.getDoctorId()).contains(lo) ||
                (c.getStatus() != null && c.getStatus().toLowerCase().contains(lo))
            ).collect(Collectors.toList());
        }
        loadConsultations(list);
    }

    private void loadConsultations(List<ConsultationsArij> list) {
        cardsContainer.getChildren().clear();
        if (list.isEmpty()) { cardsContainer.getChildren().add(emptyState()); return; }
        for (ConsultationsArij c : list) cardsContainer.getChildren().add(buildCard(c));
    }

    // ── Card builder ──────────────────────────────────────────────────────

    private VBox buildCard(ConsultationsArij c) {
        VBox card = new VBox(0);
        card.getStyleClass().add("consult-card");
        card.setPrefWidth(290);

        // Barre colorée selon statut
        Region bar = new Region();
        bar.setPrefHeight(5);
        bar.setStyle("-fx-background-color:" + statusHex(c.getStatus()) + ";-fx-background-radius:12 12 0 0;");

        // Corps
        VBox body = new VBox(10);
        body.setPadding(new Insets(16, 18, 12, 18));

        // Ligne 1 : badges
        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.getChildren().addAll(
            statusBadge(c.getStatus()),
            chip(c.getType() != null ? c.getType() : "—", "#eff6ff", "#1d4ed8")
        );

        // Ligne 2 : date
        Label dateLabel = new Label("📅  " + (c.getConsultationDate() != null
                ? c.getConsultationDate().format(FMT) : "Date non définie"));
        dateLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#1e293b;-fx-font-weight:bold;");

        // Ligne 3 : interlocuteur
        String who = "PATIENT".equalsIgnoreCase(ROLE)
                ? "👨‍⚕️  Médecin #" + c.getDoctorId()
                : "🧑  Patient #" + c.getPatientId();
        Label whoLabel = new Label(who);
        whoLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#64748b;");

        // Ligne 4 : frais (si définis)
        if (c.getConsultationFee() > 0) {
            Label feeLabel = new Label("💰  " + String.format("%.2f TND", c.getConsultationFee()));
            feeLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#2563eb;-fx-font-weight:bold;");
            body.getChildren().addAll(badges, dateLabel, whoLabel, feeLabel);
        } else {
            body.getChildren().addAll(badges, dateLabel, whoLabel);
        }

        // Séparateur
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#f1f5f9;");

        // Boutons d'action (icônes uniquement)
        HBox actions = buildActions(c);

        card.getChildren().addAll(bar, body, sep, actions);
        return card;
    }

    private HBox buildActions(ConsultationsArij c) {
        HBox actions = new HBox(6);
        actions.setPadding(new Insets(10, 14, 12, 14));
        actions.setAlignment(Pos.CENTER_LEFT);

        boolean isPending = "EN_ATTENTE".equalsIgnoreCase(c.getStatus());

        if ("PATIENT".equalsIgnoreCase(ROLE)) {
            // Patient : supprimer seulement si EN_ATTENTE
            if (isPending) {
                Button del = iconBtn("🗑", "btn-icon-danger", "Supprimer");
                del.setOnAction(e -> {
                    if (confirm("Supprimer cette consultation ?")) {
                        service.deleteConsultation(c.getId());
                        refreshList();
                    }
                });
                actions.getChildren().add(del);
            }
        } else {
            // Médecin : voir (pas d'action spéciale) + supprimer
            Button del = iconBtn("🗑", "btn-icon-danger", "Supprimer");
            del.setOnAction(e -> {
                if (confirm("Supprimer cette consultation ?")) {
                    service.deleteConsultation(c.getId());
                    refreshList();
                }
            });

            // Accepter / Refuser si EN_ATTENTE
            if (isPending) {
                Button accept = iconBtn("✅", "btn-icon-success", "Accepter");
                accept.setOnAction(e -> { service.acceptConsultation(c.getId()); refreshList(); });

                Button refuse = iconBtn("❌", "btn-icon-warning", "Refuser");
                refuse.setOnAction(e -> {
                    TextInputDialog dlg = new TextInputDialog();
                    dlg.setTitle("Refus"); dlg.setHeaderText("Motif du refus");
                    dlg.setContentText("Raison :");
                    dlg.showAndWait().ifPresent(r ->
                        service.rejectConsultation(c.getId(), r.isEmpty() ? "Aucun motif" : r));
                    refreshList();
                });
                actions.getChildren().addAll(accept, refuse);
            }
            actions.getChildren().add(del);
        }

        return actions;
    }

    // ── Helpers UI ────────────────────────────────────────────────────────

    /** Bouton icône avec tooltip */
    private Button iconBtn(String icon, String styleClass, String tooltip) {
        Button b = new Button(icon);
        b.getStyleClass().addAll("icon-btn", styleClass);
        b.setTooltip(new Tooltip(tooltip));
        return b;
    }

    private Label statusBadge(String status) {
        String label = status != null ? status.replace("_", " ") : "INCONNU";
        String bg, fg;
        switch (status != null ? status.toUpperCase() : "") {
            case "EN_ATTENTE": bg="#fef9c3"; fg="#854d0e"; break;
            case "CONFIRMEE":  bg="#dcfce7"; fg="#166534"; break;
            case "REFUSEE":    bg="#fee2e2"; fg="#991b1b"; break;
            case "TERMINEE":   bg="#e0f2fe"; fg="#075985"; break;
            default:           bg="#f1f5f9"; fg="#475569";
        }
        return chip(label, bg, fg);
    }

    private Label chip(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+fg+
                ";-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:3 9 3 9;-fx-background-radius:20;");
        return l;
    }

    private String statusHex(String s) {
        if (s == null) return "#94a3b8";
        switch (s.toUpperCase()) {
            case "EN_ATTENTE": return "#f59e0b";
            case "CONFIRMEE":  return "#22c55e";
            case "REFUSEE":    return "#ef4444";
            case "TERMINEE":   return "#3b82f6";
            default:           return "#94a3b8";
        }
    }

    private boolean confirm(String msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private VBox emptyState() {
        VBox box = new VBox(12); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(80));
        Label icon = new Label("📋"); icon.setStyle("-fx-font-size:52px;");
        Label msg  = new Label("Aucune consultation trouvée");
        msg.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#64748b;");
        Label sub  = new Label("PATIENT".equalsIgnoreCase(ROLE)
                ? "Cliquez sur + pour prendre un rendez-vous"
                : "Aucune consultation assignée pour le moment");
        sub.setStyle("-fx-font-size:12px;-fx-text-fill:#94a3b8;");
        box.getChildren().addAll(icon, msg, sub);
        return box;
    }

    private void refreshList() { loadConsultations(service.getMyConsultations()); }

    private void openForm(ConsultationsArij consultation) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/fxml/ConsultationFormArij.fxml")));
            Parent root = loader.load();
            if (consultation != null)
                ((ConsultationFormControllerArij) loader.getController()).setConsultation(consultation);
            Stage stage = new Stage();
            stage.setTitle(consultation == null ? "Nouvelle Consultation" : "Modifier");
            stage.setScene(new Scene(root));
            stage.setOnHidden(e -> refreshList());
            stage.show();
        } catch (IOException e) { System.err.println("Erreur: " + e.getMessage()); }
    }
}
