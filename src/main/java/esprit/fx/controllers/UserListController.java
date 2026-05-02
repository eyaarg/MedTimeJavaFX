package esprit.fx.controllers;

import esprit.fx.entities.Doctor;
import esprit.fx.entities.Doctor_documents;
import esprit.fx.entities.Role;
import esprit.fx.entities.User;
import esprit.fx.services.ServiceDoctor;
import esprit.fx.services.ServiceDoctorDocument;
import esprit.fx.services.ServiceUser;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.awt.Desktop;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UserListController {

    // ── Patterns ──────────────────────────────────────────────────────────────
    private static final Pattern EMAIL_PATTERN    = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN    = Pattern.compile("^(\\d{8}|\\+[1-9]\\d{6,14})$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).+$");

    // ── FXML bindings (barre du haut — définis dans UserList.fxml) ────────────
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> roleFilterComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private Label            resultCountLabel;
    @FXML private VBox             cardsContainer;   // remplace usersTable

    // ── Services ──────────────────────────────────────────────────────────────
    private final ServiceUser          serviceUser          = new ServiceUser();
    private final ServiceDoctor        serviceDoctor        = new ServiceDoctor();
    private final ServiceDoctorDocument serviceDoctorDoc    = new ServiceDoctorDocument();

    // ── Données maîtres ───────────────────────────────────────────────────────
    private List<User>   masterData   = new ArrayList<>();
    private List<Doctor> doctorData   = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void initialize() {
        roleFilterComboBox.getItems().setAll("Tous", "ADMIN", "DOCTOR", "PATIENT");
        roleFilterComboBox.setValue("Tous");

        statusFilterComboBox.getItems().setAll("Tous", "Actif", "En attente", "Bloqué");
        statusFilterComboBox.setValue("Tous");

        searchField.textProperty().addListener((o, ov, nv) -> rebuildCards());
        roleFilterComboBox.valueProperty().addListener((o, ov, nv) -> rebuildCards());
        statusFilterComboBox.valueProperty().addListener((o, ov, nv) -> rebuildCards());

        refreshUsers();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHARGEMENT DONNÉES
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void refreshUsers() {
        try {
            masterData = serviceUser.getAll();
            doctorData = serviceDoctor.getAll();
        } catch (SQLException e) {
            masterData = new ArrayList<>();
            doctorData = new ArrayList<>();
            showError("Erreur chargement", e.getMessage());
        }
        rebuildCards();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REBUILD CARTES
    // ─────────────────────────────────────────────────────────────────────────
    private void rebuildCards() {
        cardsContainer.getChildren().clear();

        List<User> filtered = applyFilters(masterData);
        resultCountLabel.setText(filtered.size() + " utilisateur(s)");

        // Séparer en 3 groupes
        List<User> pending  = new ArrayList<>();
        List<User> active   = new ArrayList<>();
        List<User> blocked  = new ArrayList<>();

        for (User u : filtered) {
            if (isBlocked(u))       blocked.add(u);
            else if (isPending(u))  pending.add(u);
            else                    active.add(u);
        }

        if (!pending.isEmpty()) {
            cardsContainer.getChildren().add(sectionLabel("⏳  Médecins en attente de validation", "#E65100"));
            for (User u : pending) cardsContainer.getChildren().add(buildCard(u));
        }
        if (!active.isEmpty()) {
            cardsContainer.getChildren().add(sectionLabel("✅  Utilisateurs actifs", "#2E7D32"));
            for (User u : active) cardsContainer.getChildren().add(buildCard(u));
        }
        if (!blocked.isEmpty()) {
            cardsContainer.getChildren().add(sectionLabel("🔒  Comptes bloqués", "#C62828"));
            for (User u : blocked) cardsContainer.getChildren().add(buildCard(u));
        }

        if (filtered.isEmpty()) {
            Label empty = new Label("Aucun utilisateur trouvé.");
            empty.setStyle("-fx-text-fill:#9E9E9E; -fx-font-size:14px;");
            empty.setPadding(new Insets(30));
            cardsContainer.getChildren().add(empty);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONSTRUCTION D'UNE CARTE
    // ─────────────────────────────────────────────────────────────────────────
    private HBox buildCard(User user) {
        HBox card = new HBox(16);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-color: #EEEEEE;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);"
        );

        // Avatar
        card.getChildren().add(buildAvatar(user));

        // Infos
        VBox info = new VBox(4);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(safe(user.getUsername()));
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        nameLabel.setStyle("-fx-text-fill: #212121;");

        Label emailLabel = new Label(safe(user.getEmail()));
        emailLabel.setStyle("-fx-text-fill: #757575; -fx-font-size: 12px;");

        String phone = user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()
                ? user.getPhoneNumber() : "—";
        Label phoneLabel = new Label("📞 " + phone);
        phoneLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 11px;");

        HBox badges = new HBox(6);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.getChildren().add(roleBadge(user));
        badges.getChildren().add(statusBadge(user));

        info.getChildren().addAll(nameLabel, emailLabel, phoneLabel, badges);

        // Boutons
        HBox actions = buildActions(user);
        actions.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(info, actions);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AVATAR
    // ─────────────────────────────────────────────────────────────────────────
    private StackPane buildAvatar(User user) {
        String initials = initials(user.getUsername());
        String roleKey  = primaryRoleKey(user);

        String bg = switch (roleKey) {
            case "DOCTOR" -> "#0C447C";
            case "ADMIN"  -> "#3C3489";
            default       -> "#085041";
        };

        Circle circle = new Circle(26);
        circle.setFill(Color.web(bg));

        Label lbl = new Label(initials);
        lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        StackPane sp = new StackPane(circle, lbl);
        sp.setMinSize(52, 52);
        sp.setMaxSize(52, 52);
        return sp;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BADGES
    // ─────────────────────────────────────────────────────────────────────────
    private Label roleBadge(User user) {
        String roleKey = primaryRoleKey(user);
        String text; String bg; String fg;
        switch (roleKey) {
            case "DOCTOR" -> { text = "MÉDECIN"; bg = "#E6F1FB"; fg = "#0C447C"; }
            case "ADMIN"  -> { text = "ADMIN";   bg = "#EEEDFE"; fg = "#3C3489"; }
            default       -> { text = "PATIENT"; bg = "#E1F5EE"; fg = "#085041"; }
        }
        return badge(text, bg, fg);
    }

    private Label statusBadge(User user) {
        if (isBlocked(user))      return badge("Bloqué",     "#FCEBEB", "#791F1F");
        if (isPending(user))      return badge("En attente", "#FAEEDA", "#633806");
        if (!user.isVerified())   return badge("Non vérifié","#FFF8E1", "#E65100");
        return badge("Actif", "#EAF3DE", "#27500A");
    }

    private Label badge(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + fg + ";" +
            "-fx-font-size: 10px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 2 8 2 8;" +
            "-fx-background-radius: 20;"
        );
        return l;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BOUTONS D'ACTION
    // ─────────────────────────────────────────────────────────────────────────
    private HBox buildActions(User user) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_RIGHT);

        String roleKey    = primaryRoleKey(user);
        boolean isDoctor  = "DOCTOR".equals(roleKey);
        boolean isPending = isPending(user);
        boolean isBlocked = isBlocked(user);
        boolean isCertified = isDoctor && isDoctorCertified(user);

        if (isDoctor && isPending) {
            // Médecin en attente
            box.getChildren().add(actionBtn("Approuver", "#EAF3DE", "#27500A", e -> approveDoctor(user)));
            box.getChildren().add(actionBtn("Refuser",   "#FCEBEB", "#791F1F", e -> rejectDoctor(user)));
        } else if (isDoctor && isCertified) {
            // Médecin actif certifié
            box.getChildren().add(actionBtn("Révoquer",  "#FAEEDA", "#633806", e -> revokeDoctor(user)));
        } else if (isBlocked) {
            // Compte bloqué
            box.getChildren().add(actionBtn("Débloquer", "#E6F1FB", "#0C447C", e -> unlockUser(user)));
        }

        // Voir PDF pour TOUS les médecins (actifs ET en attente)
        if (isDoctor) {
            box.getChildren().add(pdfBtn(user));
        }

        // Modifier + Supprimer pour tous
        box.getChildren().add(actionBtn("Modifier",  "#F5F5F5", "#424242", e -> openEditUserDialog(user)));
        box.getChildren().add(actionBtn("Supprimer", "#FCEBEB", "#791F1F", e -> deleteUser(user)));

        return box;
    }

    /** Bouton "Voir PDF" avec le style demandé */
    private Button pdfBtn(User user) {
        Button b = new Button("Voir PDF");
        b.setStyle(
            "-fx-background-color: #FAEEDA;" +
            "-fx-text-fill: #633806;" +
            "-fx-border-color: #FAC775;" +
            "-fx-border-width: 1;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 4 10 4 10;" +
            "-fx-background-radius: 6;" +
            "-fx-border-radius: 6;" +
            "-fx-cursor: hand;"
        );
        b.setOnAction(e -> viewPdf(user));
        return b;
    }

    private Button actionBtn(String text, String bg, String fg,
                             javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + fg + ";" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 4 10 4 10;" +
            "-fx-background-radius: 6;" +
            "-fx-cursor: hand;"
        );
        b.setOnAction(handler);
        return b;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION LABEL
    // ─────────────────────────────────────────────────────────────────────────
    private Label sectionLabel(String text, String color) {
        Label l = new Label(text);
        l.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        l.setStyle("-fx-text-fill: " + color + "; -fx-padding: 18 0 6 4;");
        return l;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTIONS MÉTIER
    // ─────────────────────────────────────────────────────────────────────────

    private void viewPdf(User user) {
        // 1. Récupérer le doctor_id via SELECT id FROM doctors WHERE user_id = ?
        int doctorId = getDoctorIdByUserId(user.getId());
        if (doctorId <= 0) {
            showError("Erreur", "Médecin introuvable en base de données.");
            return;
        }

        // 2. Récupérer le dernier document
        Doctor_documents dd;
        try {
            dd = serviceDoctorDoc.getLatestDocumentByDoctorId(doctorId);
        } catch (SQLException e) {
            showError("Erreur", "Erreur lors de la récupération du document : " + e.getMessage());
            return;
        }

        if (dd == null) {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Aucun document");
            a.setHeaderText(null);
            a.setContentText("Ce médecin n'a pas encore uploadé de document.");
            a.showAndWait();
            return;
        }

        // 3. Construire le chemin
        File pdfFile = new File(dd.getFolder_name() + "/" + dd.getStored_name());
        if (!pdfFile.exists()) {
            showError("Fichier introuvable", "Fichier PDF introuvable sur le serveur.");
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            showError("Non supporté", "Impossible d'ouvrir le PDF sur ce système.");
            return;
        }

        // 4. Copier avec extension .pdf si elle manque, puis ouvrir via browse()
        try {
            File fileToOpen = pdfFile;
            if (!pdfFile.getName().toLowerCase().endsWith(".pdf")) {
                fileToOpen = new File(pdfFile.getParent(), pdfFile.getName() + ".pdf");
                java.nio.file.Files.copy(
                    pdfFile.toPath(),
                    fileToOpen.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
            }
            Desktop.getDesktop().browse(fileToOpen.toURI());
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir le fichier PDF : " + e.getMessage());
        }
    }

    /** SELECT id FROM doctors WHERE user_id = ? */
    private int getDoctorIdByUserId(int userId) {
        try (java.sql.Connection conn = esprit.fx.utils.MyDB.getInstance().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM doctors WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) {
            System.err.println("[UserListController] getDoctorIdByUserId error: " + e.getMessage());
        }
        return -1;
    }

    private void approveDoctor(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Approuver le médecin");
        confirm.setHeaderText(null);
        confirm.setContentText("Approuver le compte de " + safe(user.getUsername()) + " ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            serviceDoctor.approveDoctorCertification(user.getId());
            refreshUsers();
            showInfo("Succès", "Médecin approuvé. Un email de confirmation a été envoyé.");
        } catch (SQLException e) {
            showError("Erreur", "Impossible d'approuver : " + e.getMessage());
        }
    }

    private void rejectDoctor(User user) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Refuser le médecin");
        dialog.setHeaderText("Motif du refus pour " + safe(user.getUsername()));
        TextArea reason = new TextArea();
        reason.setPromptText("Saisissez le motif...");
        reason.setPrefRowCount(3);
        dialog.getDialogPane().setContent(reason);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(b -> b == ButtonType.OK ? reason.getText().trim() : null);
        String motif = dialog.showAndWait().orElse(null);
        if (motif == null) return;
        if (motif.isBlank()) { showError("Erreur", "Le motif est obligatoire."); return; }
        try {
            serviceDoctor.rejectDoctorCertification(user.getId(), motif);
            refreshUsers();
            showInfo("Refus enregistré", "Le médecin a été notifié par email.");
        } catch (SQLException e) {
            showError("Erreur", "Impossible de refuser : " + e.getMessage());
        }
    }

    private void revokeDoctor(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Révoquer le médecin");
        confirm.setHeaderText(null);
        confirm.setContentText("Révoquer la certification de " + safe(user.getUsername()) + " ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            serviceDoctor.rejectDoctorCertification(user.getId(), "Certification révoquée par l'administrateur.");
            // Remettre is_active=false et is_certified=false
            try (java.sql.Connection conn = esprit.fx.utils.MyDB.getInstance().getConnection()) {
                java.sql.PreparedStatement ps = conn.prepareStatement(
                    "UPDATE doctors SET is_certified=false, updated_at=NOW() WHERE user_id=?");
                ps.setInt(1, user.getId());
                ps.executeUpdate();
                java.sql.PreparedStatement ps2 = conn.prepareStatement(
                    "UPDATE users SET is_active=false WHERE id=?");
                ps2.setInt(1, user.getId());
                ps2.executeUpdate();
            }
            refreshUsers();
        } catch (Exception e) {
            showError("Erreur", "Impossible de révoquer : " + e.getMessage());
        }
    }

    private void unlockUser(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Débloquer le compte");
        confirm.setHeaderText(null);
        confirm.setContentText("Débloquer " + safe(user.getUsername()) + " ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            serviceUser.unlockAccount(user.getId());
            refreshUsers();
        } catch (SQLException e) {
            showError("Déblocage impossible", e.getMessage());
        }
    }

    private void deleteUser(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer utilisateur");
        confirm.setHeaderText("Supprimer " + safe(user.getUsername()) + " ?");
        confirm.setContentText("Cette action est irréversible.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            serviceUser.supprimer(user.getId());
            refreshUsers();
        } catch (SQLException e) {
            showError("Suppression impossible", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DIALOG MODIFIER (logique existante conservée)
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void openPendingDoctorsPanel() {
        AdminDoctorValidationController.showAsStage();
        // Refresh the list after the validation window is closed so any
        // approvals/rejections are immediately reflected here.
        javafx.application.Platform.runLater(this::refreshUsers);
    }

    @FXML
    private void openAddUserDialog() {
        Optional<UserFormData> input = showUserDialog("Ajouter un utilisateur", null);
        if (input.isEmpty()) return;
        try {
            User user = new User();
            user.setUsername(input.get().username);
            user.setEmail(input.get().email);
            user.setPhoneNumber(input.get().phone);
            user.setPassword(input.get().password);
            user.setCreatedAt(LocalDateTime.now());
            user.setActive(input.get().active);
            user.setVerified(input.get().verified);
            user.setFailedAttempts(0);
            serviceUser.registerUser(user, input.get().roleName);
            refreshUsers();
        } catch (SQLException e) {
            showError("Ajout impossible", e.getMessage());
        }
    }

    private void openEditUserDialog(User original) {
        Optional<UserFormData> input = showUserDialog("Modifier #" + original.getId(), original);
        if (input.isEmpty()) return;
        try {
            User updated = new User();
            updated.setId(original.getId());
            updated.setUsername(input.get().username);
            updated.setEmail(input.get().email);
            updated.setPhoneNumber(input.get().phone);
            updated.setPassword(input.get().password);
            updated.setCreatedAt(original.getCreatedAt() != null ? original.getCreatedAt() : LocalDateTime.now());
            updated.setActive(input.get().active);
            updated.setVerified(input.get().verified);
            updated.setEmailVerificationToken(original.getEmailVerificationToken());
            updated.setEmailVerificationTokenExpiresAt(original.getEmailVerificationTokenExpiresAt());
            updated.setPasswordResetToken(original.getPasswordResetToken());
            updated.setPasswordResetTokenExpiresAt(original.getPasswordResetTokenExpiresAt());
            updated.setFailedAttempts(original.getFailedAttempts());
            serviceUser.modifier(updated);
            serviceUser.updateUserRole(updated.getId(), input.get().roleName);
            refreshUsers();
        } catch (SQLException e) {
            showError("Modification impossible", e.getMessage());
        }
    }

    private Optional<UserFormData> showUserDialog(String title, User existing) {
        Dialog<UserFormData> dialog = new Dialog<>();
        dialog.setTitle(title);
        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField usernameField = new TextField(existing != null ? safeValue(existing.getUsername()) : "");
        TextField emailField    = new TextField(existing != null ? safeValue(existing.getEmail()) : "");
        TextField phoneField    = new TextField(existing != null ? safeValue(existing.getPhoneNumber()) : "");
        PasswordField pwField   = new PasswordField();
        pwField.setPromptText(existing == null ? "Mot de passe" : "Laisser vide pour ne pas changer");
        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.setMaxWidth(Double.MAX_VALUE);
        roleBox.getItems().setAll(loadRoleOptions());
        String defRole = extractPrimaryRoleName(existing);
        if (defRole != null && roleBox.getItems().contains(defRole)) roleBox.setValue(defRole);
        else if (!roleBox.getItems().isEmpty()) roleBox.setValue(roleBox.getItems().get(0));
        CheckBox activeBox   = new CheckBox("Actif");   activeBox.setSelected(existing == null || existing.isActive());
        CheckBox verifiedBox = new CheckBox("Vérifié"); verifiedBox.setSelected(existing != null && existing.isVerified());

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(10));
        grid.add(new Label("Username"),  0, 0); grid.add(usernameField, 1, 0);
        grid.add(new Label("Email"),     0, 1); grid.add(emailField,    1, 1);
        grid.add(new Label("Téléphone"), 0, 2); grid.add(phoneField,    1, 2);
        grid.add(new Label("Rôle"),      0, 3); grid.add(roleBox,       1, 3);
        grid.add(new Label("Password"),  0, 4); grid.add(pwField,       1, 4);
        grid.add(activeBox,   1, 5);
        grid.add(verifiedBox, 1, 6);
        dialog.getDialogPane().setContent(grid);

        Node saveBtn = dialog.getDialogPane().lookupButton(saveType);
        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String u = usernameField.getText().trim();
            String em = emailField.getText().trim();
            String ph = phoneField.getText().trim();
            String pw = pwField.getText().trim();
            if (u.length() < 3 || u.length() > 80 || !u.matches("^[\\p{L}0-9_.\\-]+$")) { showError("Validation", "Username : 3-80 caractères (lettres, chiffres, point, tiret, underscore)."); ev.consume(); return; }
            if (!EMAIL_PATTERN.matcher(em).matches())                    { showError("Validation", "Email invalide."); ev.consume(); return; }
            if (!PHONE_PATTERN.matcher(ph).matches())                    { showError("Validation", "Téléphone : 8 chiffres locaux ou format international (ex: +21629110800)."); ev.consume(); return; }
            if (existing == null && pw.isEmpty())                        { showError("Validation", "Mot de passe obligatoire."); ev.consume(); return; }
            if (!pw.isEmpty() && pw.length() < 8)                        { showError("Validation", "Mot de passe : 8 caractères minimum."); ev.consume(); return; }
            if (!pw.isEmpty() && !PASSWORD_PATTERN.matcher(pw).matches()){ showError("Validation", "Mot de passe : lettres + chiffres."); ev.consume(); return; }
            if (roleBox.getValue() == null || roleBox.getValue().isBlank()){ showError("Validation", "Sélectionnez un rôle."); ev.consume(); }
        });
        dialog.setResultConverter(b -> b != saveType ? null :
            new UserFormData(usernameField.getText().trim(), emailField.getText().trim(),
                phoneField.getText().trim(), pwField.getText().trim(),
                roleBox.getValue(), activeBox.isSelected(), verifiedBox.isSelected()));
        return dialog.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FILTRES
    // ─────────────────────────────────────────────────────────────────────────

    private List<User> applyFilters(List<User> all) {
        String q      = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String role   = roleFilterComboBox.getValue();
        String status = statusFilterComboBox.getValue();

        return all.stream()
            .filter(u -> q.isEmpty()
                || safe(u.getUsername()).toLowerCase(Locale.ROOT).contains(q)
                || safe(u.getEmail()).toLowerCase(Locale.ROOT).contains(q))
            .filter(u -> matchesRole(u, role))
            .filter(u -> matchesStatus(u, status))
            .collect(Collectors.toList());
    }

    private boolean matchesRole(User u, String selected) {
        if (selected == null || "Tous".equalsIgnoreCase(selected)) return true;
        String exp = normalizeRoleForFilter(selected);
        if (u.getRoles() == null || u.getRoles().isEmpty()) return false;
        return u.getRoles().stream()
            .map(r -> normalizeRoleForFilter(r.getName()))
            .anyMatch(exp::equals);
    }

    private boolean matchesStatus(User u, String selected) {
        if (selected == null || "Tous".equalsIgnoreCase(selected)) return true;
        return switch (selected) {
            case "Actif"       -> u.isActive() && u.isVerified() && !isBlocked(u);
            case "En attente"  -> isPending(u);
            case "Bloqué"      -> isBlocked(u);
            default            -> true;
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS ÉTAT
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isPending(User u) {
        // Médecin en attente : is_verified=true, is_active=false, is_certified=false
        if (!"DOCTOR".equals(primaryRoleKey(u))) return false;
        if (u.isActive()) return false;
        if (!u.isVerified()) return false;
        Doctor d = findDoctor(u);
        return d != null && !d.isCertified();
    }

    private boolean isBlocked(User u) {
        // Bloqué : is_active=false ET failed_attempts >= 5 (pas un médecin en attente)
        return !u.isActive() && u.getFailedAttempts() >= 5;
    }

    private boolean isDoctorCertified(User u) {
        Doctor d = findDoctor(u);
        return d != null && d.isCertified();
    }

    private Doctor findDoctor(User u) {
        return doctorData.stream()
            .filter(d -> d.getUserId() == u.getId())
            .findFirst().orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS RÔLE / TEXTE
    // ─────────────────────────────────────────────────────────────────────────

    private String primaryRoleKey(User u) {
        if (u.getRoles() == null || u.getRoles().isEmpty()) return "PATIENT";
        String name = u.getRoles().get(0).getName();
        if (name == null) return "PATIENT";
        return normalizeRoleForFilter(name);
    }

    private String normalizeRoleForFilter(String role) {
        if (role == null || role.isBlank()) return "PATIENT";
        String n = role.trim().toUpperCase(Locale.ROOT);
        if (n.contains("DOCTOR") || n.contains("MEDECIN") || n.contains("PHYSICIAN")) return "DOCTOR";
        if (n.contains("ADMIN"))  return "ADMIN";
        return "PATIENT";
    }

    private String initials(String username) {
        if (username == null || username.isBlank()) return "?";
        String[] parts = username.trim().split("\\s+");
        if (parts.length >= 2)
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        return username.substring(0, Math.min(2, username.length())).toUpperCase();
    }

    private String safe(String v)      { return v == null || v.isBlank() ? "-" : v; }
    private String safeValue(String v) { return v == null ? "" : v; }

    private List<String> loadRoleOptions() {
        try {
            List<String> r = serviceUser.getAvailableRoleNames();
            return (r == null || r.isEmpty()) ? List.of("PATIENT") : r;
        } catch (SQLException e) {
            return List.of("PATIENT");
        }
    }

    private String extractPrimaryRoleName(User u) {
        if (u == null || u.getRoles() == null || u.getRoles().isEmpty()) return null;
        Role r = u.getRoles().get(0);
        return r == null ? null : r.getName();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null);
        a.setContentText(msg == null || msg.isBlank() ? "Opération invalide." : msg);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UserFormData (inchangé)
    // ─────────────────────────────────────────────────────────────────────────

    private static class UserFormData {
        final String username, email, phone, password, roleName;
        final boolean active, verified;
        UserFormData(String username, String email, String phone, String password,
                     String roleName, boolean active, boolean verified) {
            this.username = username; this.email = email; this.phone = phone;
            this.password = password; this.roleName = roleName;
            this.active = active; this.verified = verified;
        }
    }
}
