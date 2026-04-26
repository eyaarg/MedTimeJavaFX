package esprit.fx.controllers;

import esprit.fx.entities.ConsultationsArij;
import esprit.fx.entities.OrdonnanceArij;
import esprit.fx.services.ServiceConsultationsArij;
import esprit.fx.services.ServiceOrdonnanceArij;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConsultationManagementControllerArij {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    @FXML private Label countBadge;
    @FXML private VBox cardsContainer;
    @FXML private TextField searchPhoneField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> filterStatusCombo;
    @FXML private Label searchErrorLabel;

    private final ServiceConsultationsArij service = new ServiceConsultationsArij();
    private final ServiceOrdonnanceArij ordonnanceService = new ServiceOrdonnanceArij();

    private Map<Integer, String[]> patientInfoById = new HashMap<>();
    private List<ConsultationsArij> allConsultations = new ArrayList<>();
    private int doctorId = 0;

    @FXML
    private void initialize() {
        patientInfoById = loadPatientInfo();
        setupSearchBar();
        refresh();
    }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
        refresh();
    }

    private void setupSearchBar() {
        if (sortCombo != null) {
            sortCombo.getItems().addAll("Date ↓ (récent)", "Date ↑ (ancien)");
            sortCombo.setValue("Date ↓ (récent)");
            sortCombo.setOnAction(e -> applyFilters());
        }
        if (filterStatusCombo != null) {
            filterStatusCombo.getItems().addAll("Tous", "EN_ATTENTE", "CONFIRMEE", "REFUSEE", "TERMINEE");
            filterStatusCombo.setValue("Tous");
            filterStatusCombo.setOnAction(e -> applyFilters());
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

    private void applyFilters() {
        String phone  = searchPhoneField  != null ? searchPhoneField.getText().trim()  : "";
        String status = filterStatusCombo != null ? filterStatusCombo.getValue()       : "Tous";
        String sort   = sortCombo         != null ? sortCombo.getValue()               : "Date ↓ (récent)";

        List<ConsultationsArij> filtered = new ArrayList<>(allConsultations);

        if (!phone.isEmpty()) {
            filtered = filtered.stream()
                    .filter(c -> {
                        String[] info = patientInfoById.get(c.getPatientId());
                        String p = info != null ? info[1] : "";
                        return p.replace(" ", "").contains(phone.replace(" ", ""));
                    })
                    .collect(java.util.stream.Collectors.toList());
        }

        if (status != null && !"Tous".equals(status)) {
            filtered = filtered.stream()
                    .filter(c -> status.equalsIgnoreCase(safe(c.getStatus())))
                    .collect(java.util.stream.Collectors.toList());
        }

        if ("Date ↑ (ancien)".equals(sort)) {
            filtered.sort(java.util.Comparator.comparing(
                    c -> c.getConsultationDate() != null ? c.getConsultationDate() : LocalDateTime.MIN));
        } else {
            filtered.sort(java.util.Comparator.comparing(
                    (ConsultationsArij c) -> c.getConsultationDate() != null ? c.getConsultationDate() : LocalDateTime.MIN
            ).reversed());
        }

        renderCards(filtered);
    }

    private void refresh() {
        allConsultations = doctorId > 0
                ? service.getConsultationsByDoctor(doctorId)
                : List.of();
        applyFilters();
    }

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
            cardsContainer.getChildren().add(buildSummaryCard(c));
        }
    }

    private VBox buildSummaryCard(ConsultationsArij c) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-border-radius:12;" +
                "-fx-border-color:#e2e8f0; -fx-border-width:1; -fx-padding:16;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,3); -fx-cursor:default;");

        String[] info = patientInfoById.getOrDefault(c.getPatientId(), new String[]{"Patient #" + c.getPatientId(), "-"});

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        Label lblName = new Label("👤 " + info[0]);
        lblName.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.getChildren().addAll(lblName, spacer, statusBadge(c.getStatus()));

        Label lblPhone = new Label("📞 " + info[1]);
        lblPhone.setStyle("-fx-font-size:12px; -fx-text-fill:#475569;");

        Label lblDate = new Label("📅 " + fmtDate(c));
        lblDate.setStyle("-fx-font-size:12px; -fx-text-fill:#475569;");

        card.getChildren().addAll(top, lblPhone, lblDate, new Separator(), buildActionButtons(c));
        return card;
    }

    private HBox buildActionButtons(ConsultationsArij c) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);

        Button btnDetails = iconBtn("👁", "#eff6ff", "#1d4ed8", "#bfdbfe", "Voir détails");
        btnDetails.setOnAction(e -> showDetailModal(c));

        Button btnEdit = iconBtn("✏️", "#e0f2fe", "#0369a1", "#bae6fd", "Modifier");
        btnEdit.setOnAction(e -> openEditModal(c));

        Button btnDelete = iconBtn("🗑", "#fff1f2", "#be123c", "#fecdd3", "Supprimer");
        btnDelete.setOnAction(e -> openDeleteConfirmModal(c));

        box.getChildren().addAll(btnDetails, btnEdit);

        String status = safe(c.getStatus()).toUpperCase();
        if ("EN_ATTENTE".equals(status)) {
            Button approve = iconBtn("✅", "#f0fdf4", "#166534", "#bbf7d0", "Approuver");
            approve.setOnAction(e -> { service.acceptConsultation(c.getId()); refresh(); showDetailModal(service.findById(c.getId())); });
            Button reject = iconBtn("❌", "#fff7ed", "#9a3412", "#fed7aa", "Refuser");
            reject.setOnAction(e -> openRejectModal(c));
            box.getChildren().addAll(approve, reject);
        } else if ("CONFIRMEE".equals(status)) {
            Button complete = iconBtn("✔", "#eff6ff", "#1d4ed8", "#bfdbfe", "Terminer");
            complete.setOnAction(e -> ouvrirFormulaireCloturer(c));
            box.getChildren().add(complete);
        } else if ("TERMINEE".equals(status)) {
            boolean hasRx = ordonnanceService.getByConsultationId(c.getId()) != null;
            if (!hasRx) {
                Button createRx = iconBtn("💊", "#f5f3ff", "#6d28d9", "#ddd6fe", "Créer ordonnance");
                createRx.setOnAction(e -> openOrdonnanceForm(c));
                box.getChildren().add(createRx);
            } else {
                Button viewRx = iconBtn("💊", "#f5f3ff", "#6d28d9", "#ddd6fe", "Voir ordonnance");
                viewRx.setOnAction(e -> showOrdonnanceModal(c.getId()));
                box.getChildren().add(viewRx);
            }
        }

        box.getChildren().add(btnDelete);
        return box;
    }

    // ─── Modals ───────────────────────────────────────────────────────────────

    private void openDeleteConfirmModal(ConsultationsArij c) {
        String[] info = patientInfoById.getOrDefault(c.getPatientId(),
                new String[]{"Patient #" + c.getPatientId(), "-"});

        VBox panel = new VBox(16);
        panel.setPadding(new Insets(28));
        panel.setStyle("-fx-background-color:white;");
        panel.setPrefWidth(400);

        Label title = new Label("🗑 Confirmer la suppression");
        title.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#be123c;");

        Label msg = new Label("Voulez-vous vraiment supprimer la consultation de :");
        msg.setStyle("-fx-font-size:13px; -fx-text-fill:#475569; -fx-wrap-text:true;");

        Label patient = new Label("👤 " + info[0] + "  —  📅 " + fmtDate(c));
        patient.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0f172a; -fx-wrap-text:true;");

        Label warn = new Label("⚠ Cette action est irréversible.");
        warn.setStyle("-fx-font-size:12px; -fx-text-fill:#9a3412;");

        Stage modal = buildModal("Confirmation suppression", panel);

        Button btnConfirm = modalBtn("Oui, supprimer", "#fff1f2", "#be123c", "#fecdd3");
        btnConfirm.setOnAction(e -> {
            service.deleteConsultation(c.getId());
            refresh();
            modal.close();
        });

        Button btnCancel = modalBtn("Annuler", "#f1f5f9", "#334155", "#cbd5e1");
        btnCancel.setOnAction(e -> modal.close());

        HBox foot = new HBox(10, btnCancel, btnConfirm);
        foot.setAlignment(Pos.CENTER_RIGHT);
        panel.getChildren().addAll(title, msg, patient, warn, foot);
        modal.show();
    }

    private void showDetailModal(ConsultationsArij c) {
        if (c == null) return;
        String[] info = patientInfoById.getOrDefault(c.getPatientId(), new String[]{"Patient #" + c.getPatientId(), "-"});
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(28));
        panel.setStyle("-fx-background-color:white;");
        panel.setPrefWidth(480);
        panel.getChildren().addAll(
            modalTitle("📋 Détails de la consultation", "#0f172a"),
            infoSection("👤 Patient", row("Nom", info[0]), row("Téléphone", info[1])),
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

    private void openEditModal(ConsultationsArij c) {
        String[] info = patientInfoById.getOrDefault(c.getPatientId(), new String[]{"Patient #" + c.getPatientId(), "-"});
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(28));
        panel.setStyle("-fx-background-color:white;");
        panel.setPrefWidth(460);

        Label patient = new Label("Patient : " + info[0] + "  📞 " + info[1]);
        patient.setStyle("-fx-font-size:13px; -fx-text-fill:#475569;");

        javafx.scene.control.DatePicker datePicker = new javafx.scene.control.DatePicker();
        if (c.getConsultationDate() != null) datePicker.setValue(c.getConsultationDate().toLocalDate());
        datePicker.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> cmbType = new ComboBox<>();
        cmbType.getItems().addAll("ONLINE", "IN_PERSON");
        cmbType.setValue(safe(c.getType()).toUpperCase().isBlank() ? "ONLINE" : safe(c.getType()).toUpperCase());
        cmbType.setMaxWidth(Double.MAX_VALUE);

        TextField tfFee  = editField(c.getConsultationFee() > 0 ? String.valueOf(c.getConsultationFee()) : "", "Ex: 50.00");
        TextField tfMeet = editField(safe(c.getLienMeet()), "https://meet.google.com/...");
        Label lblErr = errorLabel();

        Stage modal = buildModal("Modifier consultation", panel);
        Button btnSave = modalBtn("💾 Enregistrer", "#fefce8", "#854d0e", "#fde68a");
        btnSave.setOnAction(e -> {
            if (datePicker.getValue() == null) { lblErr.setText("⚠ Date obligatoire."); return; }
            double fee = 0;
            if (!tfFee.getText().isBlank()) {
                try { fee = Double.parseDouble(tfFee.getText().trim()); }
                catch (NumberFormatException ex) { lblErr.setText("⚠ Frais invalide (nombre attendu)."); return; }
                if (fee < 0) { lblErr.setText("⚠ Frais ne peut pas être négatif."); return; }
            }
            c.setConsultationDate(datePicker.getValue().atTime(
                    c.getConsultationDate() != null ? c.getConsultationDate().toLocalTime() : java.time.LocalTime.of(9, 0)));
            c.setType(cmbType.getValue());
            c.setLienMeet(tfMeet.getText().trim());
            c.setConsultationFee(fee);
            service.updateConsultation(c);
            patientInfoById = loadPatientInfo();
            refresh();
            modal.close();
            showDetailModal(service.findById(c.getId()));
        });
        Button btnCancel = modalBtn("✕ Annuler", "#f1f5f9", "#334155", "#cbd5e1");
        btnCancel.setOnAction(e -> modal.close());
        HBox foot = new HBox(10, btnCancel, btnSave); foot.setAlignment(Pos.CENTER_RIGHT);

        panel.getChildren().addAll(modalTitle("✏️ Modifier la consultation", "#854d0e"), patient,
                fieldLabel("Date de consultation"), datePicker,
                fieldLabel("Type"), cmbType,
                fieldLabel("Frais (TND)"), tfFee,
                fieldLabel("Lien Meet"), tfMeet,
                lblErr, foot);
        modal.show();
    }

    private void openRejectModal(ConsultationsArij c) {
        String[] info = patientInfoById.getOrDefault(c.getPatientId(), new String[]{"Patient #" + c.getPatientId(), "-"});
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(28));
        panel.setStyle("-fx-background-color:white;");
        panel.setPrefWidth(440);

        Label patient = new Label("Patient : " + info[0]);
        patient.setStyle("-fx-font-size:13px; -fx-text-fill:#475569;");

        TextArea taMotif = new TextArea();
        taMotif.setPromptText("Expliquez la raison du refus...");
        taMotif.setPrefRowCount(4);
        taMotif.setWrapText(true);
        taMotif.setStyle("-fx-background-color:#fff1f2; -fx-border-color:#fecdd3; -fx-border-radius:8; -fx-background-radius:8; -fx-font-size:13px;");

        Label lblErr = errorLabel();
        Stage modal = buildModal("Refus consultation", panel);
        Button btnConfirm = modalBtn("Confirmer le refus", "#fff1f2", "#be123c", "#fecdd3");
        btnConfirm.setOnAction(e -> {
            String motif = taMotif.getText().trim();
            if (motif.isEmpty())    { lblErr.setText("⚠ Le motif est obligatoire."); return; }
            if (motif.length() < 5) { lblErr.setText("⚠ Motif trop court (min 5 caractères)."); return; }
            service.rejectConsultation(c.getId(), motif);
            refresh();
            modal.close();
        });
        Button btnCancel = modalBtn("Annuler", "#f1f5f9", "#334155", "#cbd5e1");
        btnCancel.setOnAction(e -> modal.close());
        HBox foot = new HBox(10, btnCancel, btnConfirm); foot.setAlignment(Pos.CENTER_RIGHT);
        panel.getChildren().addAll(modalTitle("❌ Refuser la consultation", "#be123c"),
                patient, fieldLabel("Motif du refus"), taMotif, lblErr, foot);
        modal.show();
    }

    private void showOrdonnanceModal(int consultationId) {
        OrdonnanceArij o = ordonnanceService.getByConsultationId(consultationId);
        if (o == null) { showInfoModal("Ordonnance introuvable."); return; }
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(28));
        panel.setStyle("-fx-background-color:white;");
        panel.setPrefWidth(460);
        TextArea content = new TextArea(o.getContent() != null ? o.getContent() : "");
        content.setEditable(false); content.setWrapText(true); content.setPrefRowCount(6);
        content.setStyle("-fx-background-color:#f5f3ff; -fx-border-color:#ddd6fe; -fx-border-radius:8; -fx-background-radius:8;");
        Stage modal = buildModal("Ordonnance", panel);
        Button close = modalBtn("✕ Fermer", "#f1f5f9", "#334155", "#cbd5e1");
        close.setOnAction(e -> modal.close());
        HBox foot = new HBox(close); foot.setAlignment(Pos.CENTER_RIGHT);
        panel.getChildren().addAll(
            modalTitle("💊 Rx #" + (o.getNumeroOrdonnance() != null ? o.getNumeroOrdonnance() : o.getId()), "#6d28d9"),
            content, foot);
        modal.show();
    }

    private void openOrdonnanceForm(ConsultationsArij c) {
        if (c == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/fxml/OrdonnanceFormArij.fxml")));
            Parent view = loader.load();
            OrdonnanceFormControllerArij ctrl = loader.getController();
            ctrl.setContext(c.getId(), c.getPatientId(), doctorId);
            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Nouvelle ordonnance");
            modal.setScene(new Scene(view));
            modal.setOnHidden(e -> refresh());
            modal.show();
        } catch (IOException e) {
            System.err.println("openOrdonnanceForm: " + e.getMessage());
        }
    }

    /**
     * Ouvre le formulaire de clôture de consultation.
     * Remplace le simple bouton "Terminer" par un formulaire complet :
     * médicaments, posologie, recommandations, prix, lien Meet.
     */
    private void ouvrirFormulaireCloturer(ConsultationsArij c) {
        if (c == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                getClass().getResource("/fxml/CloturerConsultationArij.fxml")));
            Parent view = loader.load();

            CloturerConsultationControllerArij ctrl = loader.getController();
            ctrl.setContext(c, doctorId, this::refresh);

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Clôturer la consultation #" + c.getId());
            modal.setScene(new Scene(view));
            modal.setResizable(false);
            modal.show();
        } catch (IOException e) {
            System.err.println("ouvrirFormulaireCloturer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Stage buildModal(String title, VBox content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:white; -fx-background:white; -fx-border-color:transparent;");
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.DECORATED);
        modal.setTitle(title);
        modal.setScene(new Scene(scroll));
        modal.setMinWidth(420);
        modal.setMaxWidth(580);
        modal.setResizable(false);
        return modal;
    }

    private void showInfoModal(String msg) {
        VBox panel = new VBox(16); panel.setPadding(new Insets(24)); panel.setPrefWidth(340);
        panel.setStyle("-fx-background-color:white;");
        Label lbl = new Label(msg); lbl.setStyle("-fx-font-size:13px; -fx-text-fill:#475569; -fx-wrap-text:true;");
        Stage modal = buildModal("Information", panel);
        Button ok = modalBtn("OK", "#eff6ff", "#1d4ed8", "#bfdbfe");
        ok.setOnAction(e -> modal.close());
        HBox foot = new HBox(ok); foot.setAlignment(Pos.CENTER_RIGHT);
        panel.getChildren().addAll(lbl, foot);
        modal.show();
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

    private TextField editField(String value, String prompt) {
        TextField tf = new TextField(value);
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color:#fefce8; -fx-border-color:#fde68a; -fx-border-radius:8;" +
                "-fx-background-radius:8; -fx-padding:8 12; -fx-font-size:13px;");
        return tf;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#374151;");
        return l;
    }

    private Label errorLabel() {
        Label l = new Label("");
        l.setStyle("-fx-text-fill:#be123c; -fx-font-size:12px;");
        l.setWrapText(true);
        return l;
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

    private Map<Integer, String[]> loadPatientInfo() {
        Map<Integer, String[]> map = new HashMap<>();
        String sql = "SELECT p.id, u.username, u.phone_number FROM patients p JOIN users u ON u.id = p.user_id";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name  = rs.getString("username");
                String phone = rs.getString("phone_number");
                map.put(rs.getInt("id"), new String[]{
                        name  != null && !name.isBlank()  ? name  : "Patient #" + rs.getInt("id"),
                        phone != null && !phone.isBlank() ? phone : "-"
                });
            }
        } catch (SQLException e) {
            System.err.println("loadPatientInfo: " + e.getMessage());
        }
        return map;
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
