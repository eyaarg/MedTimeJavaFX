package esprit.fx.controllers;

import esprit.fx.entities.ConsultationsArij;
import esprit.fx.entities.FactureArij;
import esprit.fx.entities.LigneOrdonnanceArij;
import esprit.fx.entities.OrdonnanceArij;
import esprit.fx.services.ServiceConsultationsArij;
import esprit.fx.services.ServiceFactureArij;
import esprit.fx.services.ServiceOrdonnanceArij;
import esprit.fx.services.QrCodeServiceArij;
import esprit.fx.utils.MyDB;
import esprit.fx.utils.PdfExporterArij;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class OrdonnanceListControllerArij {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @FXML private VBox cardsContainer;
    @FXML private Button newOrdonnanceButton;
    @FXML private Label countBadge;
    @FXML private TextField searchPhoneField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label searchErrorLabel;

    private final ServiceConsultationsArij consultationService = new ServiceConsultationsArij();
    private final ServiceOrdonnanceArij ordonnanceService = new ServiceOrdonnanceArij();
    private final ServiceFactureArij factureService = new ServiceFactureArij();
    private final QrCodeServiceArij qrCodeService = new QrCodeServiceArij();

    private Map<Integer, String[]> patientInfoById = new HashMap<>();
    private Map<Integer, String[]> doctorInfoById  = new HashMap<>();
    private List<OrdonnanceArij> allOrdonnances = new ArrayList<>();
    private Map<Integer, Integer> ordonnancePatientId = new HashMap<>(); // ordonnanceId → patientId

    private boolean isDoctor = false;
    private int patientId = 0;
    private int doctorId  = 0;

    @FXML
    private void initialize() {
        patientInfoById = loadPatientInfo();
        doctorInfoById  = loadDoctorInfo();
        applyRoleUi();
        setupSearchBar();
        loadOrdonnances();
    }

    public void setContext(boolean isDoctor, int patientId, int doctorId) {
        this.isDoctor  = isDoctor;
        this.patientId = patientId;
        this.doctorId  = doctorId;
        applyRoleUi();
        loadOrdonnances();
    }

    private void applyRoleUi() {
        if (newOrdonnanceButton != null) {
            newOrdonnanceButton.setVisible(isDoctor);
            newOrdonnanceButton.setManaged(isDoctor);
        }
    }

    // ─── Search bar setup ─────────────────────────────────────────────────────

    private void setupSearchBar() {
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

    // ─── Apply filters + sort ─────────────────────────────────────────────────

    private void applyFilters() {
        String phone = searchPhoneField != null ? searchPhoneField.getText().trim() : "";
        String sort  = sortCombo        != null ? sortCombo.getValue()              : "Date ↓ (récent)";

        List<OrdonnanceArij> filtered = new ArrayList<>(allOrdonnances);

        // Filtre par téléphone du patient
        if (!phone.isEmpty()) {
            filtered = filtered.stream()
                    .filter(o -> {
                        int pid = ordonnancePatientId.getOrDefault(o.getId(), 0);
                        String[] info = patientInfoById.get(pid);
                        String p = info != null ? info[1] : "";
                        return p.replace(" ", "").contains(phone.replace(" ", ""));
                    })
                    .collect(java.util.stream.Collectors.toList());
        }

        // Tri par date d'émission
        if ("Date ↑ (ancien)".equals(sort)) {
            filtered.sort(java.util.Comparator.comparing(
                    o -> o.getDateEmission() != null ? o.getDateEmission() : java.time.LocalDateTime.MIN));
        } else {
            filtered.sort(java.util.Comparator.comparing(
                    (OrdonnanceArij o) -> o.getDateEmission() != null ? o.getDateEmission() : java.time.LocalDateTime.MIN
            ).reversed());
        }

        renderCards(filtered);
    }

    @FXML
    private void openNewOrdonnance() {
        if (!isDoctor || doctorId <= 0) return;

        List<ConsultationsArij> consultations = consultationService.getConsultationsByDoctor(doctorId);
        List<ConsultationsArij> eligibles = new ArrayList<>();
        for (ConsultationsArij c : consultations) {
            if ("TERMINEE".equalsIgnoreCase(c.getStatus())
                    && ordonnanceService.getByConsultationId(c.getId()) == null) {
                eligibles.add(c);
            }
        }

        if (eligibles.isEmpty()) {
            showInfoModal("Aucune consultation terminée sans ordonnance disponible.");
            return;
        }

        openOrdonnanceFormModal(null, eligibles.get(0).getId(), eligibles.get(0).getPatientId());
    }

    // ─── Load list ────────────────────────────────────────────────────────────

    private void loadOrdonnances() {
        allOrdonnances = new ArrayList<>();
        ordonnancePatientId = new HashMap<>();

        List<ConsultationsArij> consultations = isDoctor
                ? consultationService.getConsultationsByDoctor(doctorId)
                : consultationService.getConsultationsByPatient(patientId);

        for (ConsultationsArij c : consultations) {
            OrdonnanceArij o = ordonnanceService.getByConsultationId(c.getId());
            if (o != null) {
                allOrdonnances.add(o);
                ordonnancePatientId.put(o.getId(), c.getPatientId());
            }
        }

        applyFilters();
    }

    private void renderCards(List<OrdonnanceArij> list) {
        cardsContainer.getChildren().clear();
        if (countBadge != null) countBadge.setText(String.valueOf(list.size()));

        if (list.isEmpty()) {
            Label empty = new Label("Aucune ordonnance trouvée.");
            empty.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px;");
            cardsContainer.getChildren().add(empty);
            return;
        }

        for (OrdonnanceArij o : list) {
            cardsContainer.getChildren().add(buildSummaryCard(o));
        }
    }

    // ─── Summary card ─────────────────────────────────────────────────────────

    private VBox buildSummaryCard(OrdonnanceArij o) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-border-radius:12;" +
                "-fx-border-color:#e2e8f0; -fx-border-width:1; -fx-padding:16;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,3);");

        int pid = ordonnancePatientId.getOrDefault(o.getId(), findPatientIdByConsultation(o.getConsultationId()));
        String[] pInfo = patientInfoById.getOrDefault(pid, new String[]{"Patient #" + pid, "-"});

        // Ligne 1 : nom
        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        Label lblName = new Label("👤 " + pInfo[0]);
        lblName.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.getChildren().addAll(lblName, spacer);

        // Ligne 2 : téléphone
        Label lblPhone = new Label("📞 " + pInfo[1]);
        lblPhone.setStyle("-fx-font-size:12px; -fx-text-fill:#475569;");

        // Ligne 3 : date
        String dateStr = o.getDateEmission() != null ? o.getDateEmission().format(FMT) : "N/A";
        Label lblDate = new Label("📅 " + dateStr);
        lblDate.setStyle("-fx-font-size:12px; -fx-text-fill:#475569;");

        // Actions
        HBox actions = buildActionButtons(o, pid, pInfo);

        card.getChildren().addAll(top, lblPhone, lblDate, new Separator(), actions);
        return card;
    }

    // ─── Action buttons (icônes seulement) ───────────────────────────────────

    private HBox buildActionButtons(OrdonnanceArij o, int pid, String[] pInfo) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);

        Button btnView = iconBtn("👁", "#eff6ff", "#1d4ed8", "#bfdbfe", "Voir détails");
        btnView.setOnAction(e -> showDetailModal(o, pid, pInfo));

        Button btnDelete = iconBtn("🗑", "#fff1f2", "#be123c", "#fecdd3", "Supprimer");
        btnDelete.setOnAction(e -> {
            ordonnanceService.deleteOrdonnance(o.getId());
            loadOrdonnances();
        });

        box.getChildren().add(btnView);

        if (isDoctor) {
            Button btnEdit = iconBtn("✏️", "#e0f2fe", "#0369a1", "#bae6fd", "Modifier");
            btnEdit.setOnAction(e -> openEditModal(o, pid, pInfo));
            box.getChildren().add(btnEdit);
        } else {
            // Patient : télécharger PDF ordonnance + facture
            Button btnPdfOrd = iconBtn("🖨", "#eff6ff", "#1d4ed8", "#bfdbfe", "Télécharger ordonnance PDF");
            btnPdfOrd.setOnAction(e -> {
                List<LigneOrdonnanceArij> lignes = ordonnanceService.getLignesByOrdonnanceId(o.getId());
                PdfExporterArij.exportOrdonnance(o, lignes,
                        System.getProperty("user.home") + "/ordonnance-" + o.getId() + ".pdf");
                showInfoModal("✅ Ordonnance exportée dans votre dossier.");
            });
            Button btnPdfFac = iconBtn("🧾", "#f0fdf4", "#166534", "#bbf7d0", "Télécharger facture PDF");
            btnPdfFac.setOnAction(e -> {
                FactureArij facture = factureService.findByOrdonnanceId(o.getId());
                if (facture != null) {
                    PdfExporterArij.exportFacture(facture,
                            System.getProperty("user.home") + "/facture-" + facture.getId() + ".pdf");
                    showInfoModal("✅ Facture exportée dans votre dossier.");
                } else {
                    showInfoModal("⚠ Aucune facture associée.");
                }
            });
            box.getChildren().addAll(btnPdfOrd, btnPdfFac);
        }

        box.getChildren().add(btnDelete);
        return box;
    }

    // ─── Detail modal ─────────────────────────────────────────────────────────

    private void showDetailModal(OrdonnanceArij o, int pid, String[] pInfo) {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(28));
        panel.setStyle("-fx-background-color:white;");
        panel.setPrefWidth(520);

        String dateStr  = o.getDateEmission()  != null ? o.getDateEmission().format(FMT)  : "N/A";
        String validStr = o.getDateValidite()   != null ? o.getDateValidite().format(FMT)  : "-";

        panel.getChildren().addAll(
            modalTitle("💊 Détails de l'ordonnance", "#6d28d9"),
            infoSection("👤 Patient", row("Nom complet", pInfo[0]), row("Téléphone", pInfo[1])),
            infoSection("📋 Ordonnance",
                row("Numéro",        o.getNumeroOrdonnance() != null ? o.getNumeroOrdonnance() : "#" + o.getId()),
                row("Date émission", dateStr),
                row("Date validité", validStr),
                row("Diagnostic",    o.getDiagnosis()    != null ? o.getDiagnosis()    : "-"),
                row("Instructions",  o.getInstructions() != null ? o.getInstructions() : "-"))
        );

        if (o.getContent() != null && !o.getContent().isBlank()) {
            VBox sec = infoSection("📝 Contenu");
            TextArea ta = new TextArea(o.getContent());
            ta.setEditable(false); ta.setWrapText(true); ta.setPrefRowCount(4);
            ta.setStyle("-fx-background-color:#f5f3ff; -fx-border-color:#ddd6fe; -fx-border-radius:8; -fx-background-radius:8;");
            sec.getChildren().add(ta);
            panel.getChildren().add(sec);
        }

        // ── AJOUTER LA SECTION QR CODE ──
        panel.getChildren().add(buildQRCodeSection(o));

        Stage modal = buildModal("Détails ordonnance", panel);
        Button close = modalBtn("✕ Fermer", "#f1f5f9", "#334155", "#cbd5e1");
        close.setOnAction(e -> modal.close());
        HBox foot = new HBox(close); foot.setAlignment(Pos.CENTER_RIGHT);
        panel.getChildren().add(foot);
        modal.show();
    }

    // ─── Edit modal ───────────────────────────────────────────────────────────

    private void openEditModal(OrdonnanceArij o, int pid, String[] pInfo) {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(28));
        panel.setStyle("-fx-background-color:white;");
        panel.setPrefWidth(460);

        Label patient = new Label("Patient : " + pInfo[0] + "  📞 " + pInfo[1]);
        patient.setStyle("-fx-font-size:13px; -fx-text-fill:#475569;");

        TextField tfDiag    = editField(o.getDiagnosis(),    "Ex: Grippe saisonnière");
        TextArea  taInstr   = editArea(o.getInstructions(),  "Instructions pour le patient...");
        TextArea  taContent = editArea(o.getContent(),       "Contenu de l'ordonnance...");

        javafx.scene.control.DatePicker dpValid = new javafx.scene.control.DatePicker();
        if (o.getDateValidite() != null) dpValid.setValue(o.getDateValidite().toLocalDate());
        dpValid.setMaxWidth(Double.MAX_VALUE);

        Label lblErr = errorLabel();
        Stage modal = buildModal("Modifier ordonnance", panel);

        Button btnSave = modalBtn("💾 Enregistrer", "#fefce8", "#854d0e", "#fde68a");
        btnSave.setOnAction(e -> {
            if (tfDiag.getText().trim().isEmpty()) { lblErr.setText("⚠ Le diagnostic est obligatoire."); return; }
            o.setDiagnosis(tfDiag.getText().trim());
            o.setInstructions(taInstr.getText().trim());
            o.setContent(taContent.getText().trim());
            if (dpValid.getValue() != null) o.setDateValidite(dpValid.getValue().atStartOfDay());
            List<LigneOrdonnanceArij> lignes = ordonnanceService.getLignesByOrdonnanceId(o.getId());
            ordonnanceService.updateOrdonnance(o, lignes);
            loadOrdonnances();
            modal.close();
            showDetailModal(ordonnanceService.findById(o.getId()), pid, pInfo);
        });

        Button btnCancel = modalBtn("✕ Annuler", "#f1f5f9", "#334155", "#cbd5e1");
        btnCancel.setOnAction(e -> modal.close());
        HBox foot = new HBox(10, btnCancel, btnSave); foot.setAlignment(Pos.CENTER_RIGHT);

        panel.getChildren().addAll(
            modalTitle("✏️ Modifier l'ordonnance", "#854d0e"), patient,
            fieldLabel("Diagnostic"),     tfDiag,
            fieldLabel("Instructions"),   taInstr,
            fieldLabel("Contenu"),        taContent,
            fieldLabel("Date validité"),  dpValid,
            lblErr, foot);
        modal.show();
    }

    // ─── Ordonnance form modal (nouvelle) ────────────────────────────────────

    private void openOrdonnanceFormModal(OrdonnanceArij ordonnance, int consultationId, int patientId) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/fxml/OrdonnanceFormArij.fxml")));
            Parent view = loader.load();
            OrdonnanceFormControllerArij ctrl = loader.getController();
            ctrl.setContext(consultationId, patientId, doctorId);
            if (ordonnance != null) ctrl.setOrdonnance(ordonnance);
            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initStyle(StageStyle.DECORATED);
            modal.setTitle(ordonnance == null ? "Nouvelle ordonnance" : "Modifier l'ordonnance");
            modal.setScene(new Scene(view));
            modal.setOnHidden(e -> loadOrdonnances());
            modal.show();
        } catch (IOException e) {
            System.err.println("openOrdonnanceFormModal: " + e.getMessage());
        }
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
        TextField tf = new TextField(value != null ? value : "");
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color:#fefce8; -fx-border-color:#fde68a; -fx-border-radius:8;" +
                "-fx-background-radius:8; -fx-padding:8 12; -fx-font-size:13px;");
        return tf;
    }

    private TextArea editArea(String value, String prompt) {
        TextArea ta = new TextArea(value != null ? value : "");
        ta.setPromptText(prompt);
        ta.setPrefRowCount(3);
        ta.setWrapText(true);
        ta.setStyle("-fx-background-color:#fefce8; -fx-border-color:#fde68a; -fx-border-radius:8;" +
                "-fx-background-radius:8; -fx-font-size:13px;");
        return ta;
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
        lbl.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b; -fx-min-width:100px;");
        Label val = new Label(value != null ? value : "-");
        val.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0f172a; -fx-wrap-text:true;");
        val.setMaxWidth(300);
        h.getChildren().addAll(lbl, val);
        return h;
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

    private int findPatientIdByConsultation(int consultationId) {
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(
                "SELECT patient_id FROM consultations WHERE id = ?")) {
            ps.setInt(1, consultationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("findPatientIdByConsultation: " + e.getMessage());
        }
        return 0;
    }

    // ─── QR Code Section ──────────────────────────────────────────────────────

    /**
     * Construit la section QR Code pour la modal d'ordonnance.
     * 
     * Affiche :
     *   - Image du QR Code (160x160)
     *   - URL de scan
     *   - Token d'accès
     *   - Bouton pour sauvegarder le QR Code
     */
    private VBox buildQRCodeSection(OrdonnanceArij o) {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color:#f8fafc; -fx-background-radius:10;" +
                "-fx-border-radius:10; -fx-border-color:#e2e8f0; -fx-border-width:1; -fx-padding:14;");

        Label titre = new Label("🔐 QR Code de vérification");
        titre.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#475569;");

        // Générer le QR Code (160x160)
        javafx.scene.image.Image qrFxImage = qrCodeService.genererQRCodeOrdonnance(o, 160);

        HBox qrRow = new HBox(16);
        qrRow.setAlignment(Pos.CENTER_LEFT);

        if (qrFxImage != null) {
            // ImageView du QR Code
            ImageView qrView = new ImageView(qrFxImage);
            qrView.setFitWidth(160);
            qrView.setFitHeight(160);
            qrView.setPreserveRatio(true);
            qrView.setSmooth(true);
            qrView.setStyle("-fx-border-color:#e2e8f0; -fx-border-width:1; -fx-border-radius:6;");

            // Infos à droite du QR
            VBox infoQr = new VBox(8);
            infoQr.setAlignment(Pos.TOP_LEFT);

            String qrContent = qrCodeService.buildOrdonnanceQrContent(o);
            Label lblUrl = new Label(qrContent);
            lblUrl.setStyle("-fx-font-size:10px; -fx-text-fill:#64748b; -fx-wrap-text:true;");
            lblUrl.setMaxWidth(260);
            lblUrl.setWrapText(true);

            Label lblToken = new Label("Token : " + o.getAccessToken());
            lblToken.setStyle("-fx-font-size:9px; -fx-text-fill:#94a3b8; -fx-font-family:'Courier New';");
            lblToken.setWrapText(true);
            lblToken.setMaxWidth(260);

            Label lblInstructions = new Label("Scannez ce code avec votre téléphone pour accéder aux données de l'ordonnance.");
            lblInstructions.setStyle("-fx-font-size:10px; -fx-text-fill:#64748b; -fx-wrap-text:true;");
            lblInstructions.setMaxWidth(260);

            // Bouton "Sauvegarder QR Code"
            Button btnSauvegarder = new Button("💾 Sauvegarder QR Code");
            btnSauvegarder.setStyle(
                "-fx-background-color:#eff6ff; -fx-text-fill:#1d4ed8;" +
                "-fx-font-size:12px; -fx-font-weight:bold;" +
                "-fx-background-radius:8; -fx-border-radius:8;" +
                "-fx-border-color:#bfdbfe; -fx-border-width:1;" +
                "-fx-padding:7 14; -fx-cursor:hand;"
            );
            btnSauvegarder.setOnAction(e -> sauvegarderQRCode(o));

            infoQr.getChildren().addAll(
                new Label("Contenu du QR :") {{ setStyle("-fx-font-size:12px; -fx-text-fill:#6d28d9; -fx-font-weight:bold;"); }},
                lblUrl,
                new Label("Token d'accès :") {{ setStyle("-fx-font-size:12px; -fx-text-fill:#6d28d9; -fx-font-weight:bold;"); }},
                lblToken,
                new Label("Instructions :") {{ setStyle("-fx-font-size:12px; -fx-text-fill:#6d28d9; -fx-font-weight:bold;"); }},
                lblInstructions,
                btnSauvegarder
            );

            qrRow.getChildren().addAll(qrView, infoQr);

        } else {
            Label erreur = new Label("⚠ QR Code non disponible (token manquant).");
            erreur.setStyle("-fx-text-fill:#dc2626; -fx-font-size:12px;");
            qrRow.getChildren().add(erreur);
        }

        section.getChildren().addAll(titre, qrRow);
        return section;
    }

    /**
     * Ouvre un FileChooser pour sauvegarder le QR Code en PNG.
     */
    private void sauvegarderQRCode(OrdonnanceArij o) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Sauvegarder le QR Code");
        chooser.setInitialFileName("qr-ordonnance-" + o.getId() + ".png");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image PNG", "*.png")
        );

        // Dossier par défaut : bureau de l'utilisateur
        File bureau = new File(System.getProperty("user.home") + "/Desktop");
        if (bureau.exists()) chooser.setInitialDirectory(bureau);

        // Récupérer la fenêtre parente
        Stage stage = (Stage) cardsContainer.getScene().getWindow();
        File fichier = chooser.showSaveDialog(stage);

        if (fichier != null) {
            boolean ok = qrCodeService.genererQRCodeFichier(
                qrCodeService.buildOrdonnanceQrContent(o),
                fichier.getAbsolutePath(),
                300
            );
            if (ok) {
                showInfoModal("✅ QR Code sauvegardé :\n" + fichier.getAbsolutePath());
            } else {
                showInfoModal("✗ Erreur lors de la sauvegarde du QR Code.");
            }
        }
    }
}
