package esprit.fx.controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import esprit.fx.entities.Article;
import esprit.fx.entities.Commentaire;
import esprit.fx.services.ArticleService;
import esprit.fx.services.CommentaireService;
import esprit.fx.services.ModerationService;
import esprit.fx.services.ReactionService;
import esprit.fx.services.TranslationService;
import esprit.fx.utils.MyDB;
import esprit.fx.utils.Session;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.ResourceBundle;

public class ArticleController implements Initializable {

    @FXML private VBox   feedContainer;
    @FXML private Button btnAjouter;
    @FXML private Button btnPrecedent;
    @FXML private Button btnSuivant;
    @FXML private Button btnPremiere;
    @FXML private Button btnDerniere;
    @FXML private HBox   pageNumbersBox;
    @FXML private Label  lblPageInfo;
    @FXML private ScrollPane scrollPane;

    private static final int ARTICLES_PAR_PAGE = 3;

    private ArticleService     articleService;
    private CommentaireService commentaireService;
    private ReactionService    reactionService;
    private TranslationService translationService;
    private ModerationService  moderationService;
    private Map<Integer, String> specialiteMap;
    private boolean isDoctor = false;

    private List<Article> tousLesArticles = new ArrayList<>();
    private int pageActuelle = 1;
    private int totalPages   = 1;

    public void setRole(boolean isDoctor) {
        this.isDoctor = isDoctor;
        if (btnAjouter != null) {
            btnAjouter.setVisible(isDoctor);
            btnAjouter.setManaged(isDoctor);
        }
        try { chargerFeed(); } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        articleService     = new ArticleService();
        commentaireService = new CommentaireService();
        reactionService    = new ReactionService();
        translationService = new TranslationService();
        moderationService  = new ModerationService();
        specialiteMap      = new HashMap<>();
        loadSpecialites();
        if (btnAjouter != null) { btnAjouter.setVisible(false); btnAjouter.setManaged(false); }
        try { chargerFeed(); } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private void loadSpecialites() {
        try {
            Connection con = MyDB.getInstance().getConnection();
            ResultSet rs = con.prepareStatement("SELECT id, nom FROM specialite").executeQuery();
            while (rs.next()) specialiteMap.put(rs.getInt("id"), rs.getString("nom"));
        } catch (SQLException e) { System.err.println("Spécialités: " + e.getMessage()); }
    }
    private String getSpecialiteNom(int id) { return specialiteMap.getOrDefault(id, "Général"); }

    // ── Pagination ─────────────────────────────────────────────────────────
    private void chargerFeed() throws SQLException {
        tousLesArticles = articleService.getAll();
        totalPages = Math.max(1, (int) Math.ceil((double) tousLesArticles.size() / ARTICLES_PAR_PAGE));
        if (pageActuelle > totalPages) pageActuelle = 1;
        afficherPage(pageActuelle);
    }

    private void afficherPage(int page) {
        feedContainer.getChildren().clear();
        int debut = (page - 1) * ARTICLES_PAR_PAGE;
        int fin   = Math.min(debut + ARTICLES_PAR_PAGE, tousLesArticles.size());
        for (int i = debut; i < fin; i++)
            feedContainer.getChildren().add(buildPost(tousLesArticles.get(i)));
        mettreAJourPagination();
        if (scrollPane != null) scrollPane.setVvalue(0);
    }

    private void mettreAJourPagination() {
        if (btnPrecedent == null) return;
        btnPrecedent.setDisable(pageActuelle <= 1);
        btnPremiere.setDisable(pageActuelle <= 1);
        btnSuivant.setDisable(pageActuelle >= totalPages);
        btnDerniere.setDisable(pageActuelle >= totalPages);
        int debut = (pageActuelle - 1) * ARTICLES_PAR_PAGE + 1;
        int fin   = Math.min(pageActuelle * ARTICLES_PAR_PAGE, tousLesArticles.size());
        lblPageInfo.setText("Articles " + debut + "–" + fin + " sur " + tousLesArticles.size());
        pageNumbersBox.getChildren().clear();
        int fenetre = 2;
        for (int i = 1; i <= totalPages; i++) {
            if (i == 1 || i == totalPages || (i >= pageActuelle - fenetre && i <= pageActuelle + fenetre)) {
                final int page = i;
                Button btn = new Button(String.valueOf(i));
                if (i == pageActuelle) {
                    btn.setStyle("-fx-background-color:#1d4ed8;-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:8;-fx-border-color:transparent;-fx-padding:7 12;-fx-cursor:hand;-fx-min-width:36px;");
                } else {
                    btn.setStyle("-fx-background-color:#f1f5f9;-fx-text-fill:#475569;-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:8;-fx-border-color:#e2e8f0;-fx-border-width:1;-fx-padding:7 12;-fx-cursor:hand;-fx-min-width:36px;");
                }
                btn.setOnAction(e -> { pageActuelle = page; afficherPage(pageActuelle); });
                pageNumbersBox.getChildren().add(btn);
            } else if (i == pageActuelle - fenetre - 1 || i == pageActuelle + fenetre + 1) {
                Label dots = new Label("…");
                dots.setStyle("-fx-font-size:13px;-fx-text-fill:#94a3b8;-fx-padding:7 4;");
                pageNumbersBox.getChildren().add(dots);
            }
        }
    }

    @FXML private void pagePrecedente()    { if (pageActuelle > 1)         { pageActuelle--; afficherPage(pageActuelle); } }
    @FXML private void pageSuivante()      { if (pageActuelle < totalPages) { pageActuelle++; afficherPage(pageActuelle); } }
    @FXML private void allerPremierePage() { pageActuelle = 1;          afficherPage(pageActuelle); }
    @FXML private void allerDernierePage() { pageActuelle = totalPages; afficherPage(pageActuelle); }

    // ── Construction d'un post complet (style Facebook) ───────────────────
    private VBox buildPost(Article article) {
        VBox post = new VBox(0);
        post.setStyle("-fx-background-color:white;-fx-background-radius:12;-fx-border-radius:12;-fx-border-color:#e2e8f0;-fx-border-width:1;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,3);");
        post.setMaxWidth(680);

        // En-tête
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16,16,10,16));
        Label avatar = new Label("👨‍⚕️");
        avatar.setStyle("-fx-background-color:#eff6ff;-fx-background-radius:50%;-fx-font-size:22px;-fx-min-width:46px;-fx-min-height:46px;-fx-max-width:46px;-fx-max-height:46px;-fx-alignment:center;");
        VBox authorInfo = new VBox(2);
        Label lblAuteur = new Label("Dr. Médecin");
        lblAuteur.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#0f172a;");
        String dateStr = article.getDatePublication() != null ? new SimpleDateFormat("dd MMM yyyy 'à' HH:mm").format(article.getDatePublication()) : "—";
        Label lblMeta = new Label("🏷 " + getSpecialiteNom(article.getSpecialiteId()) + "  ·  📅 " + dateStr);
        lblMeta.setStyle("-fx-font-size:11px;-fx-text-fill:#64748b;");
        authorInfo.getChildren().addAll(lblAuteur, lblMeta);
        Region sp1 = new Region(); HBox.setHgrow(sp1, Priority.ALWAYS);
        String statut = article.getStatut() != null ? article.getStatut().toLowerCase() : "";
        Label badge = new Label("publié".equals(statut) ? "✅ Publié" : "📝 Brouillon");
        badge.setStyle("publié".equals(statut)
            ? "-fx-background-color:#f0fdf4;-fx-text-fill:#166534;-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:3 10;-fx-background-radius:999;"
            : "-fx-background-color:#fff7ed;-fx-text-fill:#9a3412;-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:3 10;-fx-background-radius:999;");
        header.getChildren().addAll(avatar, authorInfo, sp1, badge);

        // Titre & Contenu
        Label lblTitre = new Label(article.getTitre());
        lblTitre.setWrapText(true);
        lblTitre.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#0f172a;");
        lblTitre.setPadding(new Insets(0,16,6,16));
        Label lblContenu = new Label(article.getContenu());
        lblContenu.setWrapText(true);
        lblContenu.setStyle("-fx-font-size:13px;-fx-text-fill:#334155;-fx-line-spacing:3;");
        lblContenu.setPadding(new Insets(0,16,12,16));

        // ── Image de l'article ────────────────────────────────────────────
        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
        imageView.setVisible(false);
        imageView.setManaged(false);
        if (article.getImage() != null && !article.getImage().isBlank()) {
            try {
                javafx.scene.image.Image img = new javafx.scene.image.Image(
                    article.getImage(), 680, 320, true, true, true);
                img.progressProperty().addListener((obs, ov, nv) -> {
                    if (nv.doubleValue() == 1.0 && !img.isError()) {
                        imageView.setImage(img);
                        imageView.setFitWidth(680);
                        imageView.setFitHeight(320);
                        imageView.setPreserveRatio(true);
                        imageView.setStyle("-fx-background-radius:0;");
                        imageView.setVisible(true);
                        imageView.setManaged(true);
                    }
                });
            } catch (Exception ex) {
                System.err.println("Erreur chargement image: " + ex.getMessage());
            }
        }

        // Stats bar
        HBox statsBar = new HBox(12);
        statsBar.setAlignment(Pos.CENTER_LEFT);
        statsBar.setPadding(new Insets(6,16,6,16));
        statsBar.setStyle("-fx-border-color:#f1f5f9;-fx-border-width:1 0 1 0;");
        Label lblReactionSummary = new Label();
        lblReactionSummary.setStyle("-fx-font-size:12px;-fx-text-fill:#64748b;");
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        Label lblNbComments = new Label("💬  0 commentaire");
        lblNbComments.setStyle("-fx-font-size:12px;-fx-text-fill:#64748b;");
        statsBar.getChildren().addAll(lblReactionSummary, sp2, lblNbComments);

        // Action bar
        StackPane reactionContainer = new StackPane();
        reactionContainer.setAlignment(Pos.BOTTOM_LEFT);
        Button btnLike = actionBarBtn("👍  J'aime");
        btnLike.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnLike, Priority.ALWAYS);
        HBox emojiPopup = buildEmojiPopup(article, btnLike, lblReactionSummary);
        emojiPopup.setVisible(false); emojiPopup.setManaged(false);
        emojiPopup.setTranslateY(-52);
        reactionContainer.getChildren().addAll(btnLike, emojiPopup);
        btnLike.setOnMouseEntered(e -> showPopup(emojiPopup));
        btnLike.setOnMouseExited(e -> {
            javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(Duration.millis(300));
            p.setOnFinished(ev -> { if (!emojiPopup.isHover()) hidePopup(emojiPopup); });
            p.play();
        });
        emojiPopup.setOnMouseExited(e -> hidePopup(emojiPopup));
        btnLike.setOnAction(e -> {
            int uid = Session.getCurrentUserId();
            if (uid == 0) return;
            try { reactionService.upsertReaction(article.getId(), uid, "LIKE"); rafraichirReactions(article, lblReactionSummary, btnLike); }
            catch (SQLException ex) { ex.printStackTrace(); }
        });

        HBox actionBar = new HBox(0);
        actionBar.setAlignment(Pos.CENTER);
        actionBar.setPadding(new Insets(4,8,4,8));
        actionBar.getChildren().add(reactionContainer);

        Button btnCommenter = actionBarBtn("💬  Commenter");
        btnCommenter.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnCommenter, Priority.ALWAYS);
        actionBar.getChildren().add(btnCommenter);

        Button btnTraduire = actionBarBtn("🌍  Traduire");
        btnTraduire.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnTraduire, Priority.ALWAYS);
        actionBar.getChildren().add(btnTraduire);

        if (isDoctor) {
            Button btnModifier = actionBarBtn("✏️  Modifier");
            HBox.setHgrow(btnModifier, Priority.ALWAYS); btnModifier.setMaxWidth(Double.MAX_VALUE);
            btnModifier.setOnAction(e -> modifierArticle(article));
            Button btnSupprimer = actionBarBtn("🗑  Supprimer");
            HBox.setHgrow(btnSupprimer, Priority.ALWAYS); btnSupprimer.setMaxWidth(Double.MAX_VALUE);
            btnSupprimer.setOnAction(e -> supprimerArticle(article));
            actionBar.getChildren().addAll(btnModifier, btnSupprimer);
        }

        // Panneau traduction
        VBox translatePanel = buildTranslatePanel(article);
        translatePanel.setVisible(false); translatePanel.setManaged(false);
        btnTraduire.setOnAction(e -> {
            boolean v = !translatePanel.isVisible();
            translatePanel.setVisible(v); translatePanel.setManaged(v);
            btnTraduire.setText(v ? "🌍  Fermer traduction" : "🌍  Traduire");
        });

        // Zone commentaires
        VBox commentSection = new VBox(0);
        commentSection.setStyle("-fx-background-color:#f8fafc;-fx-background-radius:0 0 12 12;");
        VBox listeCommentaires = new VBox(0);
        listeCommentaires.setPadding(new Insets(8,16,0,16));

        HBox inputRow = new HBox(10);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.setPadding(new Insets(10,16,12,16));
        Label avatarUser = new Label(getUserInitial());
        avatarUser.setStyle("-fx-background-color:#1d4ed8;-fx-background-radius:50%;-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;-fx-min-width:34px;-fx-min-height:34px;-fx-max-width:34px;-fx-max-height:34px;-fx-alignment:center;");
        TextField txtComment = new TextField();
        txtComment.setPromptText("Écrire un commentaire...");
        txtComment.setStyle("-fx-background-color:white;-fx-border-color:#e2e8f0;-fx-border-radius:20;-fx-background-radius:20;-fx-padding:8 14;-fx-font-size:13px;");
        HBox.setHgrow(txtComment, Priority.ALWAYS);
        Button btnEnvoyer = new Button("Envoyer");
        btnEnvoyer.setStyle("-fx-background-color:#1d4ed8;-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:20;-fx-border-color:transparent;-fx-padding:8 16;-fx-cursor:hand;");
        inputRow.getChildren().addAll(avatarUser, txtComment, btnEnvoyer);

        Runnable envoyerAction = () -> {
            String texte = txtComment.getText().trim();
            if (texte.isEmpty()) return;
            // ── Modération IA ──
            ModerationService.AnalyseResult analyse = moderationService.analyser(texte);
            if (analyse.rejected) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("🛡️ Commentaire rejeté");
                alert.setHeaderText("Votre commentaire a été bloqué par la modération IA");
                alert.setContentText(
                    analyse.getSummary() + "\n\n" +
                    "❌ Raison : " + analyse.rejectionReason + "\n\n" +
                    "Veuillez reformuler votre commentaire de manière respectueuse."
                );
                alert.showAndWait();
                return;
            }
            try {
                Commentaire c = new Commentaire();
                c.setContenu(texte);
                c.setDateCommentaire(new java.util.Date());
                c.setNbLikes(0);
                c.setArticle(article);
                commentaireService.ajouter(c);
                txtComment.clear();
                rafraichirCommentaires(article, listeCommentaires, lblNbComments);
            } catch (SQLException ex) { ex.printStackTrace(); }
        };
        btnEnvoyer.setOnAction(e -> envoyerAction.run());
        txtComment.setOnAction(e -> envoyerAction.run());
        btnCommenter.setOnAction(e -> txtComment.requestFocus());
        commentSection.getChildren().addAll(listeCommentaires, inputRow);

        // Assemblage
        post.getChildren().addAll(header, lblTitre, lblContenu, imageView, statsBar, new Separator(), actionBar, new Separator(), translatePanel, commentSection);
        rafraichirCommentaires(article, listeCommentaires, lblNbComments);
        rafraichirReactions(article, lblReactionSummary, btnLike);
        return post;
    }

    // ── Popup emoji ────────────────────────────────────────────────────────
    private HBox buildEmojiPopup(Article article, Button btnLike, Label lblReactionSummary) {
        HBox popup = new HBox(2);
        popup.setAlignment(Pos.CENTER_LEFT);
        popup.setPadding(new Insets(8, 12, 8, 12));
        popup.setStyle(
            "-fx-background-color:white;" +
            "-fx-background-radius:30;" +
            "-fx-border-radius:30;" +
            "-fx-border-color:#e2e8f0;" +
            "-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),12,0,4,4);"
        );

        // type, label affiché, couleur fond, couleur texte
        String[][] reactions = {
            {"LIKE",  "J'aime",  "#dbeafe", "#1d4ed8"},
            {"LOVE",  "J'adore", "#fee2e2", "#dc2626"},
            {"HAHA",  "Haha",    "#fef9c3", "#ca8a04"},
            {"WOW",   "Wow",     "#f3e8ff", "#7c3aed"},
            {"SAD",   "Triste",  "#f1f5f9", "#475569"},
            {"ANGRY", "Grrr",    "#ffedd5", "#ea580c"}
        };

        for (String[] r : reactions) {
            String type   = r[0];
            String label  = r[1];
            String bgColor = r[2];
            String fgColor = r[3];

            Button btn = new Button(label);
            btn.setTooltip(new Tooltip(label));
            btn.setStyle(
                "-fx-background-color:" + bgColor + ";" +
                "-fx-text-fill:" + fgColor + ";" +
                "-fx-font-size:11px;" +
                "-fx-font-weight:bold;" +
                "-fx-background-radius:20;" +
                "-fx-border-color:transparent;" +
                "-fx-padding:6 10;" +
                "-fx-cursor:hand;"
            );
            btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color:" + fgColor + ";" +
                "-fx-text-fill:white;" +
                "-fx-font-size:11px;" +
                "-fx-font-weight:bold;" +
                "-fx-background-radius:20;" +
                "-fx-border-color:transparent;" +
                "-fx-padding:6 10;" +
                "-fx-cursor:hand;"
            ));
            btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color:" + bgColor + ";" +
                "-fx-text-fill:" + fgColor + ";" +
                "-fx-font-size:11px;" +
                "-fx-font-weight:bold;" +
                "-fx-background-radius:20;" +
                "-fx-border-color:transparent;" +
                "-fx-padding:6 10;" +
                "-fx-cursor:hand;"
            ));
            btn.setOnAction(e -> {
                int uid = Session.getCurrentUserId();
                if (uid == 0) return;
                try {
                    reactionService.upsertReaction(article.getId(), uid, type);
                    rafraichirReactions(article, lblReactionSummary, btnLike);
                    hidePopup((HBox) btn.getParent());
                } catch (SQLException ex) { ex.printStackTrace(); }
            });
            popup.getChildren().add(btn);
        }
        return popup;
    }

    private void showPopup(HBox p) { p.setVisible(true); p.setManaged(true); FadeTransition ft = new FadeTransition(Duration.millis(150), p); ft.setFromValue(0); ft.setToValue(1); ft.play(); }
    private void hidePopup(HBox p) { FadeTransition ft = new FadeTransition(Duration.millis(150), p); ft.setFromValue(1); ft.setToValue(0); ft.setOnFinished(e -> { p.setVisible(false); p.setManaged(false); }); ft.play(); }

    private void rafraichirReactions(Article article, Label lblSummary, Button btnLike) {
        try {
            int uid = Session.getCurrentUserId();
            Map<String, Integer> counts = reactionService.getCountsByArticle(article.getId());
            int total = counts.values().stream().mapToInt(Integer::intValue).sum();
            StringBuilder sb = new StringBuilder();
            int shown = 0;
            for (Map.Entry<String, Integer> e : counts.entrySet()) { if (shown++ >= 3) break; sb.append(ReactionService.toEmoji(e.getKey())); }
            if (total > 0) sb.append("  ").append(total);
            lblSummary.setText(sb.toString());
            if (uid != 0) {
                String myReaction = reactionService.getUserReaction(article.getId(), uid);
                if (myReaction != null) { btnLike.setText(ReactionService.toEmoji(myReaction) + "  " + ReactionService.toLabel(myReaction)); }
                else { btnLike.setText("👍  J'aime"); }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── Commentaires ───────────────────────────────────────────────────────
    private void rafraichirCommentaires(Article article, VBox liste, Label lblNb) {
        liste.getChildren().clear();
        try {
            List<Commentaire> commentaires = commentaireService.getByArticle(article.getId());
            int nb = commentaires.size();
            lblNb.setText("💬  " + nb + (nb <= 1 ? " commentaire" : " commentaires"));
            for (Commentaire c : commentaires) liste.getChildren().add(buildCommentRow(c, article, liste, lblNb));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private HBox buildCommentRow(Commentaire commentaire, Article article, VBox liste, Label lblNb) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(6,0,6,0));
        String initial = (commentaire.getUsername() != null && !commentaire.getUsername().isEmpty()) ? commentaire.getUsername().substring(0,1).toUpperCase() : "U";
        Label avatarC = new Label(initial);
        avatarC.setStyle("-fx-background-color:#e2e8f0;-fx-background-radius:50%;-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#475569;-fx-min-width:32px;-fx-min-height:32px;-fx-max-width:32px;-fx-max-height:32px;-fx-alignment:center;");
        VBox bubble = new VBox(3);
        bubble.setStyle("-fx-background-color:white;-fx-background-radius:12;-fx-border-radius:12;-fx-border-color:#e2e8f0;-fx-border-width:1;-fx-padding:8 12;");
        HBox.setHgrow(bubble, Priority.ALWAYS);
        String dateStr = commentaire.getDateCommentaire() != null ? new SimpleDateFormat("dd MMM 'à' HH:mm").format(commentaire.getDateCommentaire()) : "";
        HBox bubbleHeader = new HBox(8);
        bubbleHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblUser = new Label(commentaire.getUsername() != null ? commentaire.getUsername() : "Utilisateur");
        lblUser.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#0f172a;");
        Label lblDate = new Label(dateStr);
        lblDate.setStyle("-fx-font-size:11px;-fx-text-fill:#94a3b8;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        bubbleHeader.getChildren().addAll(lblUser, lblDate, sp);
        Label lblContenu = new Label(commentaire.getContenu());
        lblContenu.setWrapText(true);
        lblContenu.setStyle("-fx-font-size:13px;-fx-text-fill:#1e293b;");
        int currentUserId = Session.getCurrentUserId();
        if (currentUserId != 0 && currentUserId == commentaire.getUtilisateurId()) {
            Button btnEdit = new Button("✏️");
            btnEdit.setStyle("-fx-background-color:transparent;-fx-text-fill:#3b82f6;-fx-font-size:11px;-fx-cursor:hand;-fx-border-color:transparent;-fx-padding:0 4;");
            Button btnDel = new Button("✕");
            btnDel.setStyle("-fx-background-color:transparent;-fx-text-fill:#ef4444;-fx-font-size:11px;-fx-cursor:hand;-fx-border-color:transparent;-fx-padding:0 4;");
            btnDel.setOnAction(e -> { try { commentaireService.supprimer(commentaire.getId()); rafraichirCommentaires(article, liste, lblNb); } catch (SQLException ex) { ex.printStackTrace(); } });
            bubbleHeader.getChildren().addAll(btnEdit, btnDel);
            bubble.getChildren().addAll(bubbleHeader, lblContenu);
            btnEdit.setOnAction(e -> {
                bubble.getChildren().remove(lblContenu);
                TextField editField = new TextField(commentaire.getContenu());
                editField.setStyle("-fx-background-color:#f8fafc;-fx-border-color:#3b82f6;-fx-border-radius:6;-fx-background-radius:6;-fx-padding:6 10;-fx-font-size:13px;");
                HBox editActions = new HBox(8); editActions.setAlignment(Pos.CENTER_RIGHT);
                Button btnSave = new Button("Enregistrer");
                btnSave.setStyle("-fx-background-color:#1d4ed8;-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:6;-fx-border-color:transparent;-fx-padding:5 12;-fx-cursor:hand;");
                Button btnCancel = new Button("Annuler");
                btnCancel.setStyle("-fx-background-color:#f1f5f9;-fx-text-fill:#475569;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:6;-fx-border-color:#e2e8f0;-fx-padding:5 12;-fx-cursor:hand;");
                editActions.getChildren().addAll(btnCancel, btnSave);
                bubble.getChildren().addAll(editField, editActions);
                btnSave.setOnAction(ev -> { String t = editField.getText().trim(); if (!t.isEmpty()) { try { commentaire.setContenu(t); commentaireService.modifier(commentaire); rafraichirCommentaires(article, liste, lblNb); } catch (SQLException ex) { ex.printStackTrace(); } } });
                btnCancel.setOnAction(ev -> { bubble.getChildren().removeAll(editField, editActions); bubble.getChildren().add(lblContenu); });
            });
        } else {
            bubble.getChildren().addAll(bubbleHeader, lblContenu);
        }
        row.getChildren().addAll(avatarC, bubble);
        return row;
    }

    // ── Panneau traduction ─────────────────────────────────────────────────
    private VBox buildTranslatePanel(Article article) {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(14,16,14,16));
        panel.setStyle("-fx-background-color:#f0f9ff;-fx-border-color:#bae6fd;-fx-border-width:1 0 1 0;");
        Label lblTitle = new Label("🌍  Traduction de l'article");
        lblTitle.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#0369a1;");
        HBox langRow = new HBox(10); langRow.setAlignment(Pos.CENTER_LEFT);
        ComboBox<TranslationService.Langue> cbSource = new ComboBox<>();
        cbSource.getItems().addAll(TranslationService.Langue.values()); cbSource.setValue(TranslationService.Langue.FRANCAIS);
        ComboBox<TranslationService.Langue> cbCible = new ComboBox<>();
        cbCible.getItems().addAll(TranslationService.Langue.values()); cbCible.setValue(TranslationService.Langue.ANGLAIS);
        Button btnGo = new Button("Traduire");
        btnGo.setStyle("-fx-background-color:#0369a1;-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:8;-fx-border-color:transparent;-fx-padding:7 16;-fx-cursor:hand;");
        langRow.getChildren().addAll(new Label("De :"), cbSource, new Label("→  Vers :"), cbCible, btnGo);
        Label lblLoading = new Label("⏳  Traduction en cours...");
        lblLoading.setStyle("-fx-font-size:12px;-fx-text-fill:#64748b;"); lblLoading.setVisible(false); lblLoading.setManaged(false);
        VBox resultBox = new VBox(6); resultBox.setVisible(false); resultBox.setManaged(false);
        Label lblResultTitle = new Label(); lblResultTitle.setStyle("-fx-font-size:11px;-fx-text-fill:#64748b;");
        Label lblResultText = new Label(); lblResultText.setWrapText(true);
        lblResultText.setStyle("-fx-font-size:13px;-fx-text-fill:#1e293b;-fx-background-color:white;-fx-background-radius:8;-fx-border-radius:8;-fx-border-color:#e2e8f0;-fx-border-width:1;-fx-padding:10 12;");
        resultBox.getChildren().addAll(lblResultTitle, lblResultText);
        btnGo.setOnAction(e -> {
            TranslationService.Langue src = cbSource.getValue(), cible = cbCible.getValue();
            if (src == cible) { lblResultTitle.setText("⚠️ Choisissez deux langues différentes"); resultBox.setVisible(true); resultBox.setManaged(true); return; }
            btnGo.setDisable(true); lblLoading.setVisible(true); lblLoading.setManaged(true); resultBox.setVisible(false); resultBox.setManaged(false);
            Thread t = new Thread(() -> {
                String traduit = translationService.traduire(article.getTitre() + " " + article.getContenu(), src, cible);
                Platform.runLater(() -> { lblLoading.setVisible(false); lblLoading.setManaged(false); btnGo.setDisable(false); lblResultTitle.setText(src.label + "  →  " + cible.label); lblResultText.setText(traduit); resultBox.setVisible(true); resultBox.setManaged(true); });
            }); t.setDaemon(true); t.start();
        });
        panel.getChildren().addAll(lblTitle, langRow, lblLoading, resultBox);
        return panel;
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private Button actionBarBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:transparent;-fx-text-fill:#475569;-fx-font-size:13px;-fx-font-weight:bold;-fx-cursor:hand;-fx-border-color:transparent;-fx-padding:8 12;-fx-background-radius:8;");
        b.setOnMouseEntered(e -> b.setStyle(b.getStyle().replace("-fx-background-color:transparent;","-fx-background-color:#f1f5f9;")));
        b.setOnMouseExited(e -> b.setStyle(b.getStyle().replace("-fx-background-color:#f1f5f9;","-fx-background-color:transparent;")));
        return b;
    }

    private String getUserInitial() {
        if (Session.getCurrentUser() != null && Session.getCurrentUser().getUsername() != null)
            return Session.getCurrentUser().getUsername().substring(0,1).toUpperCase();
        return "U";
    }
    @FXML
    public void ajouterArticle() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterArticle.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle("Nouvel article");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            chargerFeed();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void modifierArticle(Article article) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ModifierArticle.fxml"));
            Parent root = loader.load();
            ModifierArticleController controller = loader.getController();
            controller.setArticle(article);
            Stage stage = new Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier Article");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            chargerFeed();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void supprimerArticle(Article article) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment supprimer cet article ?");
        if (confirm.showAndWait().isPresent() && confirm.getResult() == ButtonType.OK) {
            try {
                articleService.supprimer(article.getId());
                chargerFeed();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Impossible de supprimer l'article.").show();
                e.printStackTrace();
            }
        }
    }
}
