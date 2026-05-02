package esprit.fx.controllers;

import esprit.fx.entities.User;
import esprit.fx.entities.Doctor;
import esprit.fx.entities.Patient;
import esprit.fx.services.NotificationDAOArij;
import esprit.fx.services.ServiceDoctor;
import esprit.fx.services.ServicePatient;
import esprit.fx.utils.UserSession;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MainControllerArij {

    @FXML private StackPane contentArea;
    @FXML private Button    btnSideDashboard;
    @FXML private Button    btnModuleConsultation;
    @FXML private Button    btnModulePrendreRdv;
    @FXML private Button    btnModuleDisponibilite;
    @FXML private Button    btnModuleMarket;
    @FXML private Button    btnModuleForum;
    @FXML private Button    btnUsers;
    @FXML private Label     footerNameLabel;
    @FXML private Label     footerRoleLabel;
    @FXML private Label     avatarLabel;

    // â”€â”€ Badge notification â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    /** Label rouge affichÃ© par-dessus la cloche, contenant le compteur. */
    @FXML private Label notifBadge;

    // ------------------------------------------------------------------ //
    //  Services & Ã©tat                                                    //
    // ------------------------------------------------------------------ //
    private final NotificationDAOArij notifDAO = new NotificationDAOArij();

    /**
     * Timeline JavaFX : rafraÃ®chit le badge toutes les 30 secondes.
     *
     * On utilise Timeline (et non un Thread classique) car :
     * - ses KeyFrame s'exÃ©cutent sur le JavaFX Application Thread
     * - pas besoin de Platform.runLater() pour mettre Ã  jour le Label
     * - INDEFINITE = boucle infinie jusqu'Ã  stop() explicite
     */
    private Timeline notifTimeline;

    private int    userId    = 0;
    private int    patientId = 0;
    private int    doctorId  = 0;
    private String role      = "PATIENT";

    // ------------------------------------------------------------------ //
    //  Initialisation                                                     //
    // ------------------------------------------------------------------ //
    @FXML
    private void initialize() {
        User currentUser = UserSession.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getId();
        }
        role = normalizeRole(UserSession.getCurrentRole(), patientId, doctorId);
        resolveBusinessIds();
        role = normalizeRole(UserSession.getCurrentRole(), patientId, doctorId);

        applySessionIdentity();
        applyRoleUi();

        if (btnUsers != null) {
            boolean isAdmin = UserSession.isAdmin();
            btnUsers.setVisible(isAdmin);
            btnUsers.setManaged(isAdmin);
        }

        // DÃ©marrer le rafraÃ®chissement du badge notification
        demarrerTimeline();

        showDashboardView();
    }

    // ------------------------------------------------------------------ //
    //  Timeline â€” badge notification                                      //
    // ------------------------------------------------------------------ //

    /**
     * DÃ©marre la Timeline qui rafraÃ®chit le badge toutes les 30 secondes.
     *
     * Fonctionnement :
     *  - KeyFrame(Duration.ZERO)      â†’ mise Ã  jour immÃ©diate au dÃ©marrage
     *  - KeyFrame(Duration.seconds(30)) â†’ puis toutes les 30 secondes
     *  - setCycleCount(INDEFINITE)    â†’ boucle infinie
     *
     * La Timeline s'exÃ©cute sur le JavaFX Application Thread â†’ on peut
     * modifier notifBadge directement sans Platform.runLater().
     */
    private void demarrerTimeline() {
        notifTimeline = new Timeline(
            // Tick immÃ©diat au dÃ©marrage
            new KeyFrame(Duration.ZERO,        e -> rafraichirBadge()),
            // Tick toutes les 30 secondes
            new KeyFrame(Duration.seconds(30), e -> rafraichirBadge())
        );
        notifTimeline.setCycleCount(Timeline.INDEFINITE);
        notifTimeline.play();
    }

    /**
     * Interroge la BDD pour compter les notifications non lues
     * et met Ã  jour le badge rouge sur la cloche.
     *
     * AppelÃ© par la Timeline â†’ dÃ©jÃ  sur le JavaFX Thread.
     * Le comptage BDD est rapide (COUNT(*)) donc pas besoin de thread sÃ©parÃ©.
     * Si la BDD est lente, on pourrait l'envelopper dans un Task.
     */
    private void rafraichirBadge() {
        if (notifBadge == null || userId <= 0) return;

        long count = notifDAO.countNonLues((long) userId);

        if (count > 0) {
            // Afficher le badge avec le compteur (max "99+" pour Ã©viter dÃ©bordement)
            notifBadge.setText(count > 99 ? "99+" : String.valueOf(count));
            notifBadge.setVisible(true);
            notifBadge.setManaged(true);
        } else {
            // Masquer le badge quand tout est lu
            notifBadge.setVisible(false);
            notifBadge.setManaged(false);
        }
    }

    /**
     * ArrÃªte la Timeline proprement (appelÃ© Ã  la dÃ©connexion).
     * Ã‰vite les fuites mÃ©moire et les appels BDD aprÃ¨s logout.
     */
    private void arreterTimeline() {
        if (notifTimeline != null) {
            notifTimeline.stop();
            notifTimeline = null;
        }
    }

    // ------------------------------------------------------------------ //
    //  Handlers FXML                                                      //
    // ------------------------------------------------------------------ //

    public void setUserContext(int userId, int patientId, int doctorId, String role) {
        this.userId    = userId;
        this.patientId = patientId;
        this.doctorId  = doctorId;
        this.role      = normalizeRole(role, patientId, doctorId);
        applyRoleUi();
        showDashboardView();
    }

    @FXML
    private void showDashboardView() {
        setModuleActive(btnSideDashboard);
        loadView("/fxml/DashboardArij.fxml");
    }

    @FXML
    private void showConsultationHubs() {
        setModuleActive(btnModuleConsultation);
        showHubsView("Consultation en ligne", "Selectionnez une fonctionnalite", buildConsultationHubs());
    }

    @FXML
    private void showPrendreRdvView() {
        setModuleActive(btnModulePrendreRdv);
        try {
            loadView("/fxml/PrendreRendezVous.fxml");
        } catch (Exception e) {
            System.err.println("Erreur chargement PrendreRendezVous: " + e.getMessage());
        }
    }

    @FXML
    private void showDisponibiliteView() {
        setModuleActive(btnModuleDisponibilite);
        loadView("/fxml/DisponibiliteList.fxml");
    }

    @FXML
    private void showMarketHubs() {
        setModuleActive(btnModuleMarket);
        showHubsView("Produits Pharmaceutiques", "Selectionnez une fonctionnalite", buildMarketHubs());
    }

    @FXML
    private void showForumHubs() {
        setModuleActive(btnModuleForum);
        showHubsView("Forum Medical", "Selectionnez une fonctionnalite", buildForumHubs());
    }

    @FXML
    private void showProfile() {
        ProfileController.showAsStage();
    }

    /**
     * Ouvre le panneau de notifications ET marque toutes comme lues.
     * Le badge disparaÃ®t immÃ©diatement aprÃ¨s l'ouverture.
     */
    @FXML
    private void showNotifications() {
        // Marquer toutes les notifs comme lues dÃ¨s l'ouverture
        if (userId > 0) {
            notifDAO.marquerToutesLues((long) userId);
        }
        // RafraÃ®chir le badge immÃ©diatement (doit afficher 0 â†’ badge cachÃ©)
        rafraichirBadge();
        loadView("/fxml/NotificationListArij.fxml");
    }

    @FXML
    private void showUsers() {
        if (!UserSession.isAdmin()) {
            showDashboardView();
            return;
        }
        setModuleActive(btnUsers);
        loadUsersView();
    }

    @FXML
    private void handleLogout() {
        // ArrÃªter la Timeline avant de quitter pour Ã©viter les fuites
        arreterTimeline();
        try {
            UserSession.clear();
            Parent loginRoot = FXMLLoader.load(Objects.requireNonNull(
                MainControllerArij.class.getResource("/Login.fxml"),
                "FXML not found: /Login.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(loginRoot));
            stage.setTitle("MedTimeFX - Connexion");
            stage.setMaximized(false);
            stage.centerOnScreen();
        } catch (IOException | NullPointerException e) {
            System.err.println("Erreur lors de la deconnexion: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //
    //  IdentitÃ© utilisateur                                               //
    // ------------------------------------------------------------------ //

    private void applySessionIdentity() {
        User currentUser = UserSession.getCurrentUser();
        String displayRole = formatRole(UserSession.getCurrentRole());
        String displayName;
        if (currentUser != null && currentUser.getUsername() != null && !currentUser.getUsername().isBlank()) {
            displayName = currentUser.getUsername().trim();
        } else if (currentUser != null && currentUser.getEmail() != null && !currentUser.getEmail().isBlank()) {
            displayName = currentUser.getEmail().trim();
        } else {
            displayName = "Utilisateur";
        }
        if (footerNameLabel != null) footerNameLabel.setText(displayName);
        if (footerRoleLabel  != null) footerRoleLabel.setText(displayRole);
        if (avatarLabel      != null) avatarLabel.setText(initialOf(displayName));
    }

    private void applyRoleUi() {
        if (footerRoleLabel != null) footerRoleLabel.setText(isDoctor() ? "Medecin" : "Patient");
        if (avatarLabel != null && (avatarLabel.getText() == null || avatarLabel.getText().isBlank())) {
            avatarLabel.setText(isDoctor() ? "M" : "P");
        }
    }

    private void resolveBusinessIds() {
        User currentUser = UserSession.getCurrentUser();
        if (currentUser == null) return;
        try {
            if (isDoctor()) {
                Doctor doctor = new ServiceDoctor().afficherParId(currentUser.getId());
                if (doctor != null) doctorId = doctor.getId();
            } else {
                Patient patient = new ServicePatient().afficherParId(currentUser.getId());
                if (patient != null) patientId = patient.getId();
            }
        } catch (Exception e) {
            System.err.println("Impossible de resoudre les identifiants metier: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //
    //  Hubs                                                               //
    // ------------------------------------------------------------------ //

    private List<HubCard> buildConsultationHubs() {
        if (isDoctor()) {
            return Arrays.asList(
                new HubCard("ðŸ—“", "Consultations",  "Gerez les demandes patients",  () -> loadView("/fxml/ConsultationManagementArij.fxml")),
                new HubCard("ðŸ“‹", "Ordonnances",    "Prescriptions medicales",      () -> loadView("/fxml/OrdonnanceListArij.fxml")),
                new HubCard("ðŸ“Š", "Statistiques",   "Analytiques et graphiques",    () -> loadView("/fxml/DashboardStatsArij.fxml")),
                new HubCard("ðŸ”", "Recherche",      "Filtrage avance consultations",() -> loadView("/fxml/ConsultationFiltreArij.fxml"))
            );
        }
        return Arrays.asList(
            new HubCard("ðŸ“…", "Calendrier",        "Disponibilites et creneaux",       () -> loadView("/fxml/CalendrierArij.fxml")),
            new HubCard("ðŸ—“", "Mes consultations", "Suivez vos rendez-vous medicaux",  () -> loadView("/fxml/ConsultationListArij.fxml")),
            new HubCard("ðŸ”", "Recherche avancee", "Filtrage multi-criteres",          () -> loadView("/fxml/ConsultationFiltreArij.fxml")),
            new HubCard("ðŸ“‹", "Mes ordonnances",   "Vos prescriptions medicales",      () -> loadView("/fxml/OrdonnanceListArij.fxml")),
            new HubCard("ðŸ§¾", "Mes factures",      "Historique de vos paiements",      () -> loadView("/fxml/FactureListArij.fxml")),
            new HubCard("ðŸ¤–", "Assistante IA",     "Posez vos questions medicales",    () -> loadView("/fxml/ChatViewArij.fxml"))
        );
    }

    private List<HubCard> buildMarketHubs() {
        return Arrays.asList(
            new HubCard("ðŸ’Š", "Liste des produits", "Parcourez le catalogue medical",  () -> loadView("/fxml/ListProd.fxml")),
            new HubCard("âž•", "Ajouter un produit", "Enregistrer un nouveau produit",  () -> loadView("/fxml/AjoutProd.fxml"))
        );
    }

    private List<HubCard> buildForumHubs() {
        if (isDoctor()) {
            return Arrays.asList(
                new HubCard("ðŸ“°", "Articles medicaux", "Consultez et gerez les articles", () -> loadView("/fxml/ListerArticles.fxml")),
                new HubCard("ðŸ’¬", "Commentaires",      "Gerez tous les commentaires",     () -> loadView("/fxml/ListerCommentaires.fxml"))
            );
        }
        return List.of(new HubCard("ðŸ“°", "Articles medicaux", "Lisez et commentez les articles", () -> loadView("/fxml/ListerArticles.fxml")));
    }

    private void showHubsView(String title, String subtitle, List<HubCard> hubs) {
        VBox page = new VBox(0);
        page.setStyle("-fx-background-color:#f5f7fa;");

        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(56, 60, 32, 60));
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size:28px; -fx-font-weight:bold; -fx-text-fill:#1d4ed8;");
        Label lblSub = new Label(subtitle);
        lblSub.setStyle("-fx-font-size:13px; -fx-text-fill:#8a8a9a;");
        header.getChildren().addAll(lblTitle, lblSub);

        if (hubs.size() > 3) {
            FlowPane flow = new FlowPane();
            flow.setAlignment(Pos.CENTER);
            flow.setHgap(28); flow.setVgap(28);
            flow.setPadding(new Insets(0, 60, 60, 60));
            for (HubCard hub : hubs) flow.getChildren().add(buildHubCard(hub));
            page.getChildren().addAll(header, flow);
        } else {
            HBox grid = new HBox(28);
            grid.setAlignment(Pos.CENTER);
            grid.setPadding(new Insets(0, 60, 60, 60));
            for (HubCard hub : hubs) grid.getChildren().add(buildHubCard(hub));
            page.getChildren().addAll(header, grid);
        }
        contentArea.getChildren().setAll(page);
    }

    private VBox buildHubCard(HubCard hub) {
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(260); card.setPrefHeight(280);
        card.setPadding(new Insets(40, 32, 36, 32));
        card.setStyle("-fx-background-color:white; -fx-background-radius:14; -fx-border-radius:14; -fx-border-color:#ebebeb; -fx-border-width:1;");

        StackPane iconWrap = new StackPane();
        iconWrap.setPrefSize(80, 80);
        iconWrap.setStyle("-fx-background-color:#eff6ff; -fx-background-radius:50%;");
        Label icon = new Label(hub.icon());
        icon.setStyle("-fx-font-size:36px;");
        iconWrap.getChildren().add(icon);

        Label name = new Label(hub.name());
        name.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#1a1a2e;");
        name.setMaxWidth(200); name.setAlignment(Pos.CENTER); name.setWrapText(true);

        Label desc = new Label(hub.description());
        desc.setStyle("-fx-font-size:12px; -fx-text-fill:#8a8a9a;");
        desc.setMaxWidth(200); desc.setAlignment(Pos.CENTER); desc.setWrapText(true);

        Button btn = new Button("Acceder");
        btn.setPrefWidth(130);
        btn.setStyle("-fx-background-color:#1d4ed8; -fx-text-fill:white; -fx-font-size:13px; -fx-font-weight:bold; -fx-background-radius:8;");
        btn.setOnAction(e -> hub.action().run());

        card.getChildren().addAll(iconWrap, name, desc, btn);
        return card;
    }

    // ------------------------------------------------------------------ //
    //  Utilitaires                                                        //
    // ------------------------------------------------------------------ //

    private void setModuleActive(Button active) {
        for (Button b : Arrays.asList(btnSideDashboard, btnModuleConsultation,
                btnModulePrendreRdv, btnModuleDisponibilite,
                btnModuleMarket, btnModuleForum, btnUsers)) {
            if (b != null) b.getStyleClass().remove("nav-btn-active");
        }
        if (active != null && !active.getStyleClass().contains("nav-btn-active")) {
            active.getStyleClass().add("nav-btn-active");
        }
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                getClass().getResource(fxmlPath)));
            Node view = loader.load();
            Object ctrl = loader.getController();
            if      (ctrl instanceof DashboardControllerArij          c) c.setContext(isPatient(), patientId, doctorId);
            else if (ctrl instanceof ConsultationListControllerArij    c) c.setPatientId(patientId);
            else if (ctrl instanceof ConsultationFormControllerArij    c) c.setPatientId(patientId);
            else if (ctrl instanceof ConsultationManagementControllerArij c) c.setDoctorId(doctorId);
            else if (ctrl instanceof OrdonnanceListControllerArij      c) c.setContext(isDoctor(), patientId, doctorId);
            else if (ctrl instanceof FactureListControllerArij         c) c.setPatientId(patientId);
            else if (ctrl instanceof NotificationListControllerArij    c) c.setUserId(userId);
            else if (ctrl instanceof ArticleController                 c) c.setRole(isDoctor());
            contentArea.getChildren().setAll(view);
        } catch (IOException | NullPointerException e) {
            System.err.println("Erreur chargement vue " + fxmlPath + ": " + e.getMessage());
        }
    }

    private void loadUsersView() {
        String preferred = "/fxml/UserList.fxml";
        String legacy    = "/fxml/UserListArij.fxml";
        if (MainControllerArij.class.getResource(preferred) != null) { loadView(preferred); return; }
        if (MainControllerArij.class.getResource(legacy)    != null) { loadView(legacy);    return; }
        System.err.println("Aucune vue Users trouvee.");
    }

    private String normalizeRole(String raw, int patientId, int doctorId) {
        if (raw != null && !raw.isBlank()) {
            String r = raw.trim().toUpperCase();
            if ("ROLE_DOCTOR".equals(r) || "DOCTOR".equals(r) || "MEDECIN".equals(r)) return "DOCTOR";
            if ("ROLE_PATIENT".equals(r) || "PATIENT".equals(r))                       return "PATIENT";
        }
        return doctorId > 0 ? "DOCTOR" : "PATIENT";
    }

    private String formatRole(String role) {
        return "DOCTOR".equals(normalizeRole(role, 0, 0)) ? "Medecin" : "Patient";
    }

    private String initialOf(String text) {
        return (text == null || text.isBlank()) ? "U" : text.substring(0, 1).toUpperCase();
    }

    private boolean isDoctor()  { return "DOCTOR".equalsIgnoreCase(role); }
    private boolean isPatient() { return !isDoctor(); }

    private record HubCard(String icon, String name, String description, Runnable action) {}
}
