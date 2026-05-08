package esprit.fx.controllers;

import esprit.fx.entities.User;
import esprit.fx.services.ServiceProfilePhoto;
import esprit.fx.services.ServiceUser;
import esprit.fx.utils.UserSession;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;

public class ProfileController {

    private final ServiceUser         serviceUser   = new ServiceUser();
    private final ServiceProfilePhoto photoService  = new ServiceProfilePhoto();

    // ── Widgets partagés ──────────────────────────────────────────────────────
    private StackPane avatarPane;
    private Label     avatarInitialLabel;
    private ImageView avatarImageView;
    private Label     headerNameLabel;   // nom dans l'en-tête bleu
    private Stage     profileStage;      // référence à la fenêtre pour maj titre

    // ── Ouvrir l'écran ────────────────────────────────────────────────────────
    public static void showAsStage() {
        User user = UserSession.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun utilisateur connecté.");
            return;
        }
        ProfileController ctrl = new ProfileController();
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Mon Profil — " + user.getUsername());
        stage.setResizable(false);
        ctrl.profileStage = stage;
        ctrl.build(stage, user);
        stage.show();
    }

    // ── Construction de l'UI ──────────────────────────────────────────────────
    private void build(Stage stage, User user) {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #F5F7FA;");

        // ── En-tête avec avatar ───────────────────────────────────────────────
        VBox header = buildHeader(user);

        // ── Onglets ───────────────────────────────────────────────────────────
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color: white;");

        Tab tabInfo     = new Tab("✏  Mes informations", buildInfoSection(user, stage));
        Tab tabPhoto    = new Tab("📷  Photo de profil",  buildPhotoSection(user));
        Tab tabPassword = new Tab("🔒  Mot de passe",     buildPasswordSection(user));

        tabs.getTabs().addAll(tabInfo, tabPhoto, tabPassword);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        root.getChildren().addAll(header, tabs);

        Scene scene = new Scene(root, 520, 620);
        stage.setScene(scene);
    }

    // ── En-tête ───────────────────────────────────────────────────────────────
    private VBox buildHeader(User user) {
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(28, 20, 20, 20));
        header.setStyle("-fx-background-color: #1565C0;");

        avatarPane = buildAvatarPane(user, 52);

        headerNameLabel = new Label(safe(user.getUsername()));
        headerNameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        headerNameLabel.setStyle("-fx-text-fill: white;");

        Label roleLabel = new Label(formatRole(user));
        roleLabel.setStyle("-fx-text-fill: #BBDEFB; -fx-font-size: 12px;");

        header.getChildren().addAll(avatarPane, headerNameLabel, roleLabel);
        return header;
    }

    // ── Section 1 : Informations ──────────────────────────────────────────────
    private VBox buildInfoSection(User user, Stage stage) {
        VBox box = new VBox(14);
        box.setPadding(new Insets(24));

        Label title = sectionTitle("Modifier mes coordonnées");

        Label lUser  = fieldLabel("Nom d'utilisateur");
        TextField tfUser  = styledField(safe(user.getUsername()));

        Label lEmail = fieldLabel("Email");
        TextField tfEmail = styledField(safe(user.getEmail()));

        Label lPhone = fieldLabel("Téléphone");
        TextField tfPhone = styledField(safe(user.getPhoneNumber()));

        Label statusLabel = new Label("");
        statusLabel.setWrapText(true);

        Button btnSave = primaryBtn("Enregistrer les modifications");
        btnSave.setOnAction(e -> {
            String username = tfUser.getText().trim();
            String email    = tfEmail.getText().trim();
            String phone    = tfPhone.getText().trim();

            try {
                serviceUser.updateProfile(user.getId(), username, email, phone);

                // 1. Mettre à jour l'objet user et la session
                user.setUsername(username);
                user.setEmail(email);
                user.setPhoneNumber(phone);
                UserSession.setCurrentUser(user);

                // 2. Rafraîchir l'en-tête du profil (nom + titre fenêtre)
                headerNameLabel.setText(username);
                if (profileStage != null) {
                    profileStage.setTitle("Mon Profil — " + username);
                }

                // 3. Rafraîchir le footer de MainControllerArij
                refreshMainFooter(username);

                statusLabel.setStyle("-fx-text-fill: #2E7D32;");
                statusLabel.setText("✔ Informations mises à jour avec succès.");
            } catch (SQLException ex) {
                statusLabel.setStyle("-fx-text-fill: #C62828;");
                statusLabel.setText("✘ " + ex.getMessage());
            }
        });

        box.getChildren().addAll(title, lUser, tfUser, lEmail, tfEmail, lPhone, tfPhone,
                statusLabel, btnSave);
        return box;
    }

    // ── Section 2 : Photo de profil ───────────────────────────────────────────
    private VBox buildPhotoSection(User user) {
        VBox box = new VBox(16);
        box.setPadding(new Insets(24));
        box.setAlignment(Pos.TOP_LEFT);

        Label title = sectionTitle("Photo de profil");

        // Aperçu grande taille
        StackPane preview = buildAvatarPane(user, 70);
        preview.setAlignment(Pos.CENTER);
        VBox previewBox = new VBox(preview);
        previewBox.setAlignment(Pos.CENTER);
        previewBox.setPadding(new Insets(10, 0, 10, 0));

        Label hint = new Label("Formats acceptés : JPG, PNG — Max 5 Mo");
        hint.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 11px;");

        Label statusLabel = new Label("");
        statusLabel.setWrapText(true);

        Button btnChoose = primaryBtn("Choisir une photo");
        btnChoose.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Sélectionner une photo de profil");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images (JPG, PNG)", "*.jpg", "*.jpeg", "*.png"));
            File selected = fc.showOpenDialog(null);
            if (selected == null) return;

            // Vérifier taille max 5 Mo
            if (selected.length() > 5 * 1024 * 1024) {
                statusLabel.setStyle("-fx-text-fill: #C62828;");
                statusLabel.setText("✘ Fichier trop volumineux (max 5 Mo).");
                return;
            }

            try {
                String path = photoService.uploadProfilePhoto(user.getId(), selected);

                // Mettre à jour l'aperçu dans cet écran
                updateAvatarWithImage(preview, selected);
                // Mettre à jour l'avatar dans l'en-tête
                updateAvatarWithImage(avatarPane, selected);
                // Mettre à jour le sidebar de la fenêtre principale
                refreshSidebarPhoto(user.getId());

                // Stocker le chemin dans la session
                user.setProfilePhotoFile(path);
                UserSession.setCurrentUser(user);

                statusLabel.setStyle("-fx-text-fill: #2E7D32;");
                statusLabel.setText("✔ Photo mise à jour avec succès.");
            } catch (Exception ex) {
                statusLabel.setStyle("-fx-text-fill: #C62828;");
                statusLabel.setText("✘ Erreur : " + ex.getMessage());
            }
        });

        box.getChildren().addAll(title, previewBox, hint, btnChoose, statusLabel);
        return box;
    }

    // ── Section 3 : Changement de mot de passe ────────────────────────────────
    private VBox buildPasswordSection(User user) {
        VBox box = new VBox(14);
        box.setPadding(new Insets(24));

        Label title = sectionTitle("Changer mon mot de passe");

        Label lOld = fieldLabel("Ancien mot de passe");
        PasswordField pfOld = styledPasswordField("Ancien mot de passe");

        Label lNew = fieldLabel("Nouveau mot de passe");
        PasswordField pfNew = styledPasswordField("Nouveau mot de passe (lettres + chiffres)");

        Label lConfirm = fieldLabel("Confirmer le nouveau mot de passe");
        PasswordField pfConfirm = styledPasswordField("Confirmer le mot de passe");

        Label statusLabel = new Label("");
        statusLabel.setWrapText(true);

        Button btnChange = primaryBtn("Changer le mot de passe");
        btnChange.setOnAction(e -> {
            String oldPw  = pfOld.getText();
            String newPw  = pfNew.getText();
            String confirm = pfConfirm.getText();

            if (oldPw.isBlank() || newPw.isBlank() || confirm.isBlank()) {
                statusLabel.setStyle("-fx-text-fill: #C62828;");
                statusLabel.setText("✘ Tous les champs sont obligatoires.");
                return;
            }
            if (!newPw.equals(confirm)) {
                statusLabel.setStyle("-fx-text-fill: #C62828;");
                statusLabel.setText("✘ Le nouveau mot de passe et la confirmation ne correspondent pas.");
                return;
            }

            try {
                boolean ok = serviceUser.changePassword(user.getId(), oldPw, newPw);
                if (ok) {
                    statusLabel.setStyle("-fx-text-fill: #2E7D32;");
                    statusLabel.setText("✔ Mot de passe changé avec succès.");
                    pfOld.clear(); pfNew.clear(); pfConfirm.clear();
                } else {
                    statusLabel.setStyle("-fx-text-fill: #C62828;");
                    statusLabel.setText("✘ Ancien mot de passe incorrect.");
                }
            } catch (SQLException ex) {
                statusLabel.setStyle("-fx-text-fill: #C62828;");
                statusLabel.setText("✘ " + ex.getMessage());
            }
        });

        box.getChildren().addAll(title, lOld, pfOld, lNew, pfNew, lConfirm, pfConfirm,
                statusLabel, btnChange);
        return box;
    }

    // ── Avatar (cercle avec initiale ou image) ────────────────────────────────
    private StackPane buildAvatarPane(User user, double radius) {
        Circle circle = new Circle(radius);
        circle.setFill(Color.web("#1565C0"));

        avatarInitialLabel = new Label(initials(user.getUsername()));
        avatarInitialLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: "
                + (int)(radius * 0.55) + "px;");

        avatarImageView = new ImageView();
        avatarImageView.setFitWidth(radius * 2);
        avatarImageView.setFitHeight(radius * 2);
        avatarImageView.setPreserveRatio(false);
        // Clip circulaire pour l'image
        Circle clip = new Circle(radius, radius, radius);
        avatarImageView.setClip(clip);
        avatarImageView.setVisible(false);

        StackPane sp = new StackPane(circle, avatarInitialLabel, avatarImageView);
        sp.setMinSize(radius * 2, radius * 2);
        sp.setMaxSize(radius * 2, radius * 2);

        // Charger la photo existante si disponible
        try {
            File photo = photoService.getPhotoFile(user.getId());
            if (photo != null) {
                loadImageIntoAvatar(sp, photo);
            }
        } catch (Exception ignored) {}

        return sp;
    }

    private void updateAvatarWithImage(StackPane pane, File imageFile) {
        try {
            loadImageIntoAvatar(pane, imageFile);
        } catch (Exception e) {
            System.err.println("Erreur chargement image avatar: " + e.getMessage());
        }
    }

    private void loadImageIntoAvatar(StackPane pane, File imageFile) throws Exception {
        try (FileInputStream fis = new FileInputStream(imageFile)) {
            Image img = new Image(fis);
            // Trouver l'ImageView dans le StackPane
            pane.getChildren().stream()
                .filter(n -> n instanceof ImageView)
                .map(n -> (ImageView) n)
                .findFirst()
                .ifPresent(iv -> {
                    iv.setImage(img);
                    iv.setVisible(true);
                });
            // Cacher l'initiale
            pane.getChildren().stream()
                .filter(n -> n instanceof Label)
                .map(n -> (Label) n)
                .findFirst()
                .ifPresent(l -> l.setVisible(false));
        }
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────
    private Label sectionTitle(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        l.setStyle("-fx-text-fill: #1A1A2E;");
        return l;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #424242;");
        return l;
    }

    private TextField styledField(String value) {
        TextField tf = new TextField(value);
        tf.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-border-color: #E0E0E0; -fx-padding: 8 12 8 12;");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private PasswordField styledPasswordField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-border-color: #E0E0E0; -fx-padding: 8 12 8 12;");
        pf.setMaxWidth(Double.MAX_VALUE);
        return pf;
    }

    private Button primaryBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; " +
                "-fx-padding: 9 20 9 20; -fx-cursor: hand;");
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private String initials(String username) {
        if (username == null || username.isBlank()) return "?";
        String[] parts = username.trim().split("\\s+");
        if (parts.length >= 2)
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        return username.substring(0, Math.min(2, username.length())).toUpperCase();
    }

    private String safe(String v) { return v == null ? "" : v; }

    private String formatRole(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) return "Utilisateur";
        String name = user.getRoles().get(0).getName();
        if (name == null) return "Utilisateur";
        String n = name.toUpperCase();
        if (n.contains("DOCTOR") || n.contains("MEDECIN")) return "Médecin";
        if (n.contains("ADMIN")) return "Administrateur";
        return "Patient";
    }

    private static void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    /**
     * Rafraîchit le nom, le rôle et la photo dans le footer de la fenêtre principale.
     */
    private void refreshMainFooter(String newUsername) {
        try {
            for (javafx.stage.Window w : javafx.stage.Window.getWindows()) {
                if (w instanceof Stage s && s.getScene() != null) {
                    // Nom
                    javafx.scene.Node footerName = s.getScene().lookup("#footerNameLabel");
                    if (footerName instanceof Label fl) fl.setText(newUsername);

                    // Initiale avatar
                    javafx.scene.Node avatar = s.getScene().lookup("#avatarLabel");
                    if (avatar instanceof Label al) al.setText(initials(newUsername));
                }
            }
        } catch (Exception e) {
            System.err.println("refreshMainFooter: " + e.getMessage());
        }
    }

    /**
     * Rafraîchit la photo dans le sidebar de la fenêtre principale.
     */
    public static void refreshSidebarPhoto(int userId) {
        try {
            esprit.fx.services.ServiceProfilePhoto photoService =
                    new esprit.fx.services.ServiceProfilePhoto();
            java.io.File photo = photoService.getPhotoFile(userId);
            if (photo == null) return;

            for (javafx.stage.Window w : javafx.stage.Window.getWindows()) {
                if (w instanceof Stage s && s.getScene() != null) {
                    javafx.scene.Node ivNode = s.getScene().lookup("#avatarImageView");
                    javafx.scene.Node lblNode = s.getScene().lookup("#avatarLabel");
                    if (ivNode instanceof javafx.scene.image.ImageView iv) {
                        try (java.io.FileInputStream fis = new java.io.FileInputStream(photo)) {
                            javafx.scene.image.Image img = new javafx.scene.image.Image(fis);
                            javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(18, 18, 18);
                            iv.setClip(clip);
                            iv.setImage(img);
                            iv.setVisible(true);
                            if (lblNode instanceof Label l) l.setVisible(false);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("refreshSidebarPhoto: " + e.getMessage());
        }
    }
}
