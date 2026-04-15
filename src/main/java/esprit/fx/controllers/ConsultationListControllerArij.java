package esprit.fx.controllers;

import esprit.fx.entities.ConsultationsArij;
import esprit.fx.services.ServiceConsultationsArij;
import esprit.fx.utils.MyDB;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ConsultationListControllerArij {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    @FXML private Label countBadge;
    @FXML private VBox cardsContainer;
    @FXML private Button newConsultationButton;
    @FXML private TextField searchPhoneField;
    @FXML private ComboBox<String> filterStatusCombo;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label searchErrorLabel;

    private final ServiceConsultationsArij service = new ServiceConsultationsArij();

    // doctorId → [username, phone]
    private Map<Integer, String[]> doctorInfoById = new HashMap<>();
    private List<ConsultationsArij> allConsultations = new ArrayList<>();

    private int patientId = 0;

    @FXML
    private void initialize() {
        doctorInfoById = loadDoctorInfo();
        setupSearchBar();
        applyRoleUi();
        refreshFromDb();
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
        applyRoleUi();
        refreshFromDb();
    }

    private void applyRoleUi() {
        if (newConsultationButton != null) {
            boolean ready = patientId > 0;
            newConsultationButton.setVisible(ready);
            newConsultationButton.setManaged(ready);
        }
    }

    @FXML
    private void openNewConsultation() {
        openFormModal(null);
    }

    // ─── Search bar ───────────────────────────────────────────────────────────

    private void setupSearchBar() {
        if (filterStatusCombo != null) {
            filterStatusCombo.getItems().addAll("Tous", "EN_ATTENTE", "CONFIRMEE", "REFUSEE", "TERMINEE");
            filterStatusCombo.setValue("Tous");
            filterStatusCombo.setOnAction(e -> applyFilters());
        }
        if (sortCombo != null) {
            sortCombo.getItems().addAll("Date ↓ (récent)", "Date ↑ (ancien)");
            sortCombo.setValue("Date ↓ (récent)");
            sortCombo.setOnAction(e -> applyFilters());
        }
        if (searchPhoneField != null) {
            searchPhoneField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("[0-9+\\s]*")) {
                    searchPhoneField.setText(oldVal);
                    if (searchErrorLabel != null) {
                        searchErrorLabel.setText("⚠ Numéro invalide : chiffres uniquement.");
                        searchErrorLabel.setVisible(true);
                    }
                } else {
                    if (searchErrorLabel != null) searchErrorLabel.setVisible(false);
                    applyFilters();
                }
            });
        }
    }

    // ─── Data ─────────────────────────────────────────────────────────────────

    private void refreshFromDb() {
        allConsultations = patientId > 0
                ? service.getConsultationsByPatient(patientId)
                : List.of();
        applyFilters();
    }

    private void applyFilters() {
        String phone  = searchPhoneField  != null ? searchPhoneField.getText().trim()  : "";
        String status = filterStatusCombo != null ? filterStatusCombo.getValue()       : "Tous";
        String sort   = sortCombo         != null ? sortCombo.getValue()               : "Date ↓ (récent)";

        List<ConsultationsArij> filtered = new ArrayList<>(allConsultations);

        // Filtre par téléphone du médecin
        if (!phone.isEmpty()) {
            filtered = filtered.stream()
                    .filter(c -> {
                        String[] info = doctorInfoById.get(c.getDoctorId());
                        String p = info != null ? info[1] : "";
                        return p.replace(" ", "").contains(phone.replace(" ", ""));
                    })
                    .collect(Collectors.toList());
        }

        // Filtre par statut
        if (status != null && !"Tous".equals(status)) {
            filtered = filtered.stream()
                    .filter(c -> status.equalsIgnoreCase(safe(c.getStatus())))
                    .collect(Collectors.toList());
        }

        // Tri par date
        if ("Date ↑ (ancien)".equals(sort)) {
            filtered.sort(Comparator.comparing(
                    c -> c.getConsultationDate() != null ? c.getConsultationDate() : LocalDateTime.MIN));
        } else {
            filtered.sort(Comparator.comparing(
                    (ConsultationsArij c) -> c.getConsultationDate() != null ? c.getConsultationDate() : LocalDateTime.MIN
            ).reversed());
        }

        renderCards(filtered);
    }

    // ─── Cards ────────────────────────────────────────────────────────────────

    private void renderCards(List<ConsultationsArij> list) {
        cardsContainer.getChildren().clear();
        if (countBadge != null) countBadge.setText(String.valueOf(list.size()));

        if (list.isEmpty()) {
            Label empty = new Label("Aucune consultation trouvée.");
            empty.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px;");
            cardsContainer.getChildren().add(empty);
            return;
        }
        for (ConsultationsArij c : list) {
            cardsContainer.getChildren().add(buildCard(c));
        }
    }

    private VBox buildCard(ConsultationsArij c) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-border-radius:12;" +
                "-fx-border-color:#e2e8f0; -fx-border-width:1; -fx-padding:16;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,3);");

        String[] dInfo = doctorInfoById.getOrDefault(c.getDoctorId(),
                new String[]{"Médecin #" + c.getDoctorId(), "-"});

        // Ligne 1 : nom médecin + badge statut
        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        Label lblDoc = new Label("👨‍⚕️ " + dInfo[0]);
        lblDoc.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.getChildren().addAll(lblDoc, spacer, statusBadge(c.getStatus()));

        // Ligne 2 : téléphone médecin
        Label lblPhone = new Label("📞 " + dInfo[1]);
        lblPhone.setStyle("-fx-font-size:12px; -fx-text-fill:#475569;");

        // Ligne 3 : date
        Label lblDate = new Label("📅 " + fmtDate(c));
        lblDate.setStyle("-fx-font-size:12px; -fx-text-fill:#475569;");

        card.getChildren().addAll(top, lblPhone, lblDate, new Separator(), buildActionButtons(c));
        return card;
    }

    private HBox buildActionButtons(ConsultationsArij c) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);

        Button btnView = iconBtn("👁", "#eff6ff", "#1d4ed8", "#bfdbfe", "Voir détails");
        btnView.setOnAction(e -> showDetailModal(c));

        Button btnEdit = iconBtn("✏️", "#e0f2fe", "#0369a1", "#bae6fd", "Modifier");
        btnEdit.setOnAction(e -> {
            if (!isPending(c)) {
                showErrorModal("Modification impossible",
                        "Vous pouvez modifier uniquement une consultation en attente.");
                return;
            }
            openFormModal(c);
        });

        Button btnDelete = iconBtn("🗑", "#fff1f2", "#be123c", "#fecdd3", "Supprimer");
        btnDelete.setOnAction(e -> {
            if (!isPending(c)) {
                showErrorModal("Suppression impossible",
                        "Vous pouvez supprimer uniquement une consultation en attente.");
                return;
            }
            openDeleteConfirmModal(c);
        });

        box.getChildren().addAll(btnView, btnEdit, btnDelete);
        return box;
    }

    // ─── Modal : Voir détails ─────────────────────────────────────────────────

    private void showDetailModal(ConsultationsArij c) {
        String[] dInfo = doctorInfoById.getOrDefault(c.getDoctorId(),
                new String[]{"Médecin #" + c.getDoctorId(), "-"});

        VBox panel = new VBox(16);
        panel.setPadding(new Insets(28));
        panel.setStyle("-fx-background-color:white;");
        panel.setPrefWidth(480);

        panel.getChildren().addAll(
            modalTitle("📋 Détails de la consultation", "#0f172a"),
            infoSection("👨‍⚕️ Médecin",
                row("Nom",       dInfo[0]),
                row("Téléphone", dInfo[1])),
            infoSection("🗓 Consultation",
                row("Date",   fmtDate(c)),
                row("Type",   prettyType(c.getType())),
                row("Statut", prettyStatus(c.getStatus())),
                row("Frais",  c.getConsultationFee() > 0 ? String.format("%.2f TND", c.getConsultationFee()) : "-"),
                row("Meet",   safe(c.getLienMeet()).isBlank() ? "-" : c.getLienMeet()))
        );

        if (!safe(c.getRejectionReason()).isBlank())
            panel.getChildren().add(infoSection("⚠️ Motif de refus", row("Raison", c.getRejectionReason())));

        Stage modal = buildModal("Détails consultation", panel);
        Button close = modalBtn("✕ Fermer", "#f1f5f9", "#334155", "#cbd5e1");
        close.setOnAction(e -> modal.close());
        HBox foot = new HBox(close); foot.setAlignment(Pos.CENTER_RIGHT);
        panel.getChildren().add(foot);
        modal.show();
    }

    // ─── Modal : Supprimer (confirmation) ────────────────────────────────────

    private void openDeleteConfirmModal(ConsultationsArij c) {
        String[] dInfo = doctorInfoById.getOrDefault(c.getDoctorId(),
                new String[]{"Médecin #" + c.getDoctorId(), "-"});

        VBox panel = new VBox(16);
        panel.setPadding(new Insets(28));
        panel.setStyle("-fx-background-color:white;");
        panel.setPrefWidth(400);

        Label title = modalTitle("🗑 Supprimer la consultation ?", "#be123c");
        Label info = new Label("Médecin : " + dInfo[0] + "\nDate : " + fmtDate(c));
        info.setStyle("-fx-font-size:13px; -fx-text-fill:#475569; -fx-wrap-text:true;");

        Stage modal = buildModal("Confirmation suppression", panel);

        Button btnConfirm = modalBtn("Oui, supprimer", "#fff1f2", "#be123c", "#fecdd3");
        btnConfirm.setOnAction(e -> {
            service.deleteConsultation(c.getId());
            refreshFromDb();
            modal.close();
        });

        Button btnCancel = modalBtn("Annuler", "#f1f5f9", "#334155", "#cbd5e1");
        btnCancel.setOnAction(e -> modal.close());

        HBox foot = new HBox(10, btnCancel, btnConfirm);
        foot.setAlignment(Pos.CENTER_RIGHT);
        panel.getChildren().addAll(title, info, foot);
        modal.show();
    }

    // ─── Modal : Formulaire (nouvelle / modifier) ─────────────────────────────

    private void openFormModal(ConsultationsArij consultation) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/fxml/ConsultationFormArij.fxml")));
            Parent view = loader.load();
            ConsultationFormControllerArij ctrl = loader.getController();
            ctrl.setPatientId(patientId);
            if (consultation != null) ctrl.setConsultation(consultation);

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initStyle(StageStyle.DECORATED);
            modal.setTitle(consultation == null ? "Nouvelle consultation" : "Modifier consultation");
            modal.setScene(new Scene(view));
            modal.setOnHidden(e -> refreshFromDb());
            modal.show();
        } catch (IOException e) {
            System.err.println("openFormModal: " + e.getMessage());
        }
    }

    // ─── Modal : Erreur ───────────────────────────────────────────────────────

    private void showErrorModal(String title, String msg) {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(24));
        panel.setStyle("-fx-background-color:white;");
        panel.setPrefWidth(380);
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-font-size:13px; -fx-text-fill:#be123c; -fx-wrap-text:true;");
        Stage modal = buildModal(title, panel);
        Button ok = modalBtn("OK", "#fff1f2", "#be123c", "#fecdd3");
        ok.setOnAction(e -> modal.close());
        HBox foot = new HBox(ok); foot.setAlignment(Pos.CENTER_RIGHT);
        panel.getChildren().addAll(modalTitle("⚠ " + title, "#be123c"), lbl, foot);
        modal.show();
    }

    // ─── Modal builder ────────────────────────────────────────────────────────

    private Stage buildModal(String title, VBox content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:white; -fx-background:white; -fx-border-color:transparent;");
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.DECORATED);
        modal.setTitle(title);
        modal.setScene(new Scene(scroll));
        modal.setMinWidth(380);
        modal.setMaxWidth(560);
        modal.setResizable(false);
        return modal;
    }

    // ─── UI helpers ───────────────────────────────────────────────────────────

    private Label modalTitle(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:" + color + "; -fx-wrap-text:true;");
        return l;
    }

    private Button modalBtn(String text, String bg, String fg, String border) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + "; -fx-font-size:13px;" +
                "-fx-font-weight:bold; -fx-background-radius:8; -fx-border-radius:8;" +
                "-fx-border-color:" + border + "; -fx-border-width:1; -fx-padding:8 18; -fx-cursor:hand;");
        return b;
    }

    private Button iconBtn(String icon, String bg, String fg, String border, String tooltip) {
        Button b = new Button(icon);
        b.setTooltip(new Tooltip(tooltip));
        b.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + "; -fx-font-size:14px;" +
                "-fx-background-radius:8; -fx-border-radius:8;" +
                "-fx-border-color:" + border + "; -fx-border-width:1;" +
                "-fx-padding:6 10; -fx-cursor:hand; -fx-min-width:34px; -fx-min-height:34px;");
        return b;
    }

    private VBox infoSection(String sectionTitle, HBox... rows) {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color:#f8fafc; -fx-background-radius:10; -fx-border-radius:10;" +
                "-fx-border-color:#e2e8f0; -fx-border-width:1; -fx-padding:14;");
        Label t = new Label(sectionTitle);
        t.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#475569;");
        box.getChildren().add(t);
        box.getChildren().addAll(rows);
        return box;
    }

    private HBox row(String label, String value) {
        HBox h = new HBox(8);
        h.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label + " :");
        lbl.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b; -fx-min-width:90px;");
        Label val = new Label(value != null ? value : "-");
        val.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0f172a; -fx-wrap-text:true;");
        val.setMaxWidth(300);
        h.getChildren().addAll(lbl, val);
        return h;
    }

    private Label statusBadge(String status) {
        String s = safe(status).toUpperCase();
        String text; String bg; String fg;
        switch (s) {
            case "EN_ATTENTE" -> { text = "En attente"; bg = "#fff7ed"; fg = "#9a3412"; }
            case "CONFIRMEE"  -> { text = "Confirmée";  bg = "#f0fdf4"; fg = "#166534"; }
            case "REFUSEE"    -> { text = "Refusée";    bg = "#fff1f2"; fg = "#be123c"; }
            case "TERMINEE"   -> { text = "Terminée";   bg = "#eff6ff"; fg = "#1d4ed8"; }
            default           -> { text = s.isBlank() ? "-" : s; bg = "#f1f5f9"; fg = "#475569"; }
        }
        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg +
                "; -fx-font-size:11px; -fx-font-weight:bold; -fx-padding:3 10 3 10; -fx-background-radius:999;");
        return l;
    }

    // ─── Data helpers ─────────────────────────────────────────────────────────

    private Map<Integer, String[]> loadDoctorInfo() {
        Map<Integer, String[]> map = new HashMap<>();
        String sql = "SELECT d.id, u.username, u.phone_number FROM doctors d JOIN users u ON u.id = d.user_id";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name  = rs.getString("username");
                String phone = rs.getString("phone_number");
                map.put(rs.getInt("id"), new String[]{
                        name  != null && !name.isBlank()  ? name  : "Médecin #" + rs.getInt("id"),
                        phone != null && !phone.isBlank() ? phone : "-"
                });
            }
        } catch (SQLException e) {
            System.err.println("loadDoctorInfo: " + e.getMessage());
        }
        return map;
    }

    private boolean isPending(ConsultationsArij c) {
        return "EN_ATTENTE".equalsIgnoreCase(safe(c.getStatus()));
    }

    private String fmtDate(ConsultationsArij c) {
        LocalDateTime dt = c != null ? c.getConsultationDate() : null;
        return dt != null ? dt.format(FMT) : "-";
    }

    private String prettyType(String type) {
        String t = safe(type).toUpperCase();
        return switch (t) {
            case "ONLINE"    -> "🌐 Online";
            case "IN_PERSON" -> "🏥 Cabinet";
            default -> t.isBlank() ? "-" : t;
        };
    }

    private String prettyStatus(String status) {
        String s = safe(status).toUpperCase();
        return switch (s) {
            case "EN_ATTENTE" -> "⏳ En attente";
            case "CONFIRMEE"  -> "✅ Confirmée";
            case "REFUSEE"    -> "❌ Refusée";
            case "TERMINEE"   -> "✔ Terminée";
            default -> s.isBlank() ? "-" : s;
        };
    }

    private String safe(String s) { return s == null ? "" : s; }
}
