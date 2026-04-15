package esprit.fx.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MainControllerArij {

    @FXML private StackPane contentArea;

    // Sidebar module buttons
    @FXML private Button btnSideDashboard;
    @FXML private Button btnModuleConsultation;
    @FXML private Button btnModuleMarket;
    @FXML private Button btnNotifications;

    // Footer
    @FXML private Label footerName;
    @FXML private Label footerRole;
    @FXML private Label avatarLabel;

    private int userId    = 0;
    private int patientId = 0;
    private int doctorId  = 0;
    private String role   = "PATIENT";

    @FXML
    private void initialize() {
        applyRoleUi();
        showDashboardView();
    }

    public void setUserContext(int userId, int patientId, int doctorId, String role) {
        this.userId    = userId;
        this.patientId = patientId;
        this.doctorId  = doctorId;
        this.role      = normalizeRole(role, patientId, doctorId);
        applyRoleUi();
        showDashboardView();
    }

    public int getUserId()    { return userId; }
    public int getPatientId() { return patientId; }
    public int getDoctorId()  { return doctorId; }
    public String getRole()   { return role; }

    private boolean isDoctor()  { return "DOCTOR".equalsIgnoreCase(role); }
    private boolean isPatient() { return "PATIENT".equalsIgnoreCase(role); }

    private String normalizeRole(String raw, int patientId, int doctorId) {
        if (raw != null && !raw.isBlank()) {
            String r = raw.trim().toUpperCase();
            if ("DOCTOR".equals(r) || "PATIENT".equals(r)) return r;
        }
        return doctorId > 0 ? "DOCTOR" : "PATIENT";
    }

    private void applyRoleUi() {
        if (footerRole  != null) footerRole.setText(isPatient() ? "Patient" : "Médecin");
        if (avatarLabel != null) avatarLabel.setText(isPatient() ? "P" : "M");
    }

    // ─── Sidebar actions ──────────────────────────────────────────────────────

    @FXML
    private void showDashboardView() {
        setModuleActive(btnSideDashboard);
        loadView("/fxml/DashboardArij.fxml");
    }

    @FXML
    private void showConsultationHubs() {
        setModuleActive(btnModuleConsultation);
        showHubsView("🩺  Consultation en ligne",
                "Sélectionnez une fonctionnalité",
                buildConsultationHubs());
    }

    @FXML
    private void showMarketHubs() {
        setModuleActive(btnModuleMarket);
        showHubsView("💊  Produits Pharmaceutiques",
                "Sélectionnez une fonctionnalité",
                buildMarketHubs());
    }

    @FXML
    private void showNotifications() {
        setModuleActive(btnModuleConsultation);
        loadView("/fxml/NotificationListArij.fxml");
    }

    // ─── Hub builders ─────────────────────────────────────────────────────────

    private List<HubCard> buildConsultationHubs() {
        if (isDoctor()) {
            return Arrays.asList(
                new HubCard("🗓", "Consultations",
                        "Gérez les demandes patients",
                        () -> loadView("/fxml/ConsultationManagementArij.fxml")),
                new HubCard("📋", "Ordonnances",
                        "Prescriptions médicales",
                        () -> loadView("/fxml/OrdonnanceListArij.fxml"))
            );
        } else {
            return Arrays.asList(
                new HubCard("🗓", "Mes consultations",
                        "Suivez vos rendez-vous médicaux",
                        () -> loadView("/fxml/ConsultationListArij.fxml")),
                new HubCard("📋", "Mes ordonnances",
                        "Vos prescriptions médicales",
                        () -> loadView("/fxml/OrdonnanceListArij.fxml")),
                new HubCard("🧾", "Mes factures",
                        "Historique de vos paiements",
                        () -> loadView("/fxml/FactureListArij.fxml")),
                new HubCard("🤖", "Assistante IA",
                        "Posez vos questions médicales",
                        () -> loadView("/fxml/ChatViewArij.fxml"))
            );
        }
    }

    private List<HubCard> buildMarketHubs() {
        return Arrays.asList(
            new HubCard("💊", "Liste des produits",
                    "Parcourez le catalogue médical",
                    () -> loadView("/fxml/ListProd.fxml")),
            new HubCard("➕", "Ajouter un produit",
                    "Enregistrer un nouveau produit",
                    () -> loadView("/fxml/AjoutProd.fxml"))
        );
    }

    // ─── Hub view builder ─────────────────────────────────────────────────────

    private void showHubsView(String title, String subtitle, List<HubCard> hubs) {
        // Page container
        VBox page = new VBox(0);
        page.setStyle("-fx-background-color:#f5f7fa;");

        // Header zone
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(56, 60, 32, 60));

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size:28px; -fx-font-weight:bold; -fx-text-fill:#1d4ed8;");

        Label lblSub = new Label(subtitle);
        lblSub.setStyle("-fx-font-size:13px; -fx-text-fill:#8a8a9a;");

        header.getChildren().addAll(lblTitle, lblSub);

        // Cards grid — centré horizontalement
        HBox grid = new HBox(28);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(0, 60, 60, 60));

        for (HubCard hub : hubs) {
            grid.getChildren().add(buildHubCard(hub));
        }

        // Si plus de 3 cards, utiliser FlowPane
        if (hubs.size() > 3) {
            FlowPane flow = new FlowPane();
            flow.setAlignment(Pos.CENTER);
            flow.setHgap(28);
            flow.setVgap(28);
            flow.setPadding(new Insets(0, 60, 60, 60));
            for (HubCard hub : hubs) {
                flow.getChildren().add(buildHubCard(hub));
            }
            page.getChildren().addAll(header, flow);
        } else {
            page.getChildren().addAll(header, grid);
        }

        contentArea.getChildren().setAll(page);
    }

    private VBox buildHubCard(HubCard hub) {
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(260);
        card.setPrefHeight(280);
        card.setPadding(new Insets(40, 32, 36, 32));
        card.setStyle(
            "-fx-background-color:white;" +
            "-fx-background-radius:14;" +
            "-fx-border-radius:14;" +
            "-fx-border-color:#ebebeb;" +
            "-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),12,0,0,4);"
        );

        // Icône avec fond bleu clair
        StackPane iconWrap = new StackPane();
        iconWrap.setPrefSize(80, 80);
        iconWrap.setStyle("-fx-background-color:#eff6ff; -fx-background-radius:50%;");
        Label icon = new Label(hub.icon);
        icon.setStyle("-fx-font-size:36px;");
        iconWrap.getChildren().add(icon);

        // Titre
        Label name = new Label(hub.name);
        name.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#1a1a2e; -fx-wrap-text:true;");
        name.setMaxWidth(200);
        name.setAlignment(Pos.CENTER);
        name.setWrapText(true);

        // Description
        Label desc = new Label(hub.description);
        desc.setStyle("-fx-font-size:12px; -fx-text-fill:#8a8a9a; -fx-wrap-text:true;");
        desc.setMaxWidth(200);
        desc.setAlignment(Pos.CENTER);
        desc.setWrapText(true);

        // Bouton Accéder — bleu médical
        Button btn = new Button("Accéder");
        btn.setPrefWidth(130);
        btn.setStyle(
            "-fx-background-color:#1d4ed8;" +
            "-fx-text-fill:white;" +
            "-fx-font-size:13px;" +
            "-fx-font-weight:bold;" +
            "-fx-background-radius:8;" +
            "-fx-padding:10 28;" +
            "-fx-cursor:hand;" +
            "-fx-border-color:transparent;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle()
            .replace("-fx-background-color:#1d4ed8", "-fx-background-color:#1e40af")));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle()
            .replace("-fx-background-color:#1e40af", "-fx-background-color:#1d4ed8")));
        btn.setOnAction(e -> hub.action.run());

        // Hover card — bleu médical
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle()
            .replace("-fx-border-color:#ebebeb", "-fx-border-color:#93c5fd")
            .replace("-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),12,0,0,4)",
                     "-fx-effect:dropshadow(gaussian,rgba(29,78,216,0.15),16,0,0,6)")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle()
            .replace("-fx-border-color:#93c5fd", "-fx-border-color:#ebebeb")
            .replace("-fx-effect:dropshadow(gaussian,rgba(29,78,216,0.15),16,0,0,6)",
                     "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),12,0,0,4)")));

        card.getChildren().addAll(iconWrap, name, desc, btn);
        return card;
    }

    // ─── Sidebar active state ─────────────────────────────────────────────────

    private void setModuleActive(Button active) {
        for (Button b : Arrays.asList(btnSideDashboard, btnModuleConsultation, btnModuleMarket)) {
            if (b != null) b.getStyleClass().remove("nav-btn-active");
        }
        if (active != null && !active.getStyleClass().contains("nav-btn-active"))
            active.getStyleClass().add("nav-btn-active");
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource("/Login.fxml")));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("MedTime – Login");
            stage.setMaximized(false);
            stage.setWidth(950);
            stage.setHeight(680);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ─── Load view ────────────────────────────────────────────────────────────

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            Node view = loader.load();

            Object ctrl = loader.getController();
            if (ctrl instanceof DashboardControllerArij c) {
                c.setContext(isPatient(), patientId, doctorId);
            } else if (ctrl instanceof ConsultationListControllerArij c) {
                c.setPatientId(patientId);
            } else if (ctrl instanceof ConsultationFormControllerArij c) {
                c.setPatientId(patientId);
            } else if (ctrl instanceof ConsultationManagementControllerArij c) {
                c.setDoctorId(doctorId);
            } else if (ctrl instanceof OrdonnanceListControllerArij c) {
                c.setContext(isDoctor(), patientId, doctorId);
            } else if (ctrl instanceof FactureListControllerArij c) {
                c.setPatientId(patientId);
            } else if (ctrl instanceof NotificationListControllerArij c) {
                c.setUserId(userId);
            }

            contentArea.getChildren().setAll(view);
        } catch (IOException | NullPointerException e) {
            System.err.println("Erreur chargement vue " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─── HubCard record ───────────────────────────────────────────────────────

    private record HubCard(String icon, String name, String description, Runnable action) {}
}
