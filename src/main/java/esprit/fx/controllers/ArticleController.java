package esprit.fx.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import esprit.fx.entities.Article;
import esprit.fx.services.ArticleService;
import esprit.fx.utils.MyDB;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class ArticleController implements Initializable {

    @FXML
    private FlowPane flowArticles;

    @FXML
    private Button btnAjouter;

    private ArticleService articleService;
    private Map<Integer, String> specialiteMap;
    private boolean isDoctor = false;  // rôle injecté par MainControllerArij

    public void setRole(boolean isDoctor) {
        this.isDoctor = isDoctor;
        // Afficher/masquer le bouton Ajouter selon le rôle
        if (btnAjouter != null) {
            btnAjouter.setVisible(isDoctor);
            btnAjouter.setManaged(isDoctor);
        }
        // Recharger les cards avec les bons boutons
        try { chargerArticles(); } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        articleService = new ArticleService();
        specialiteMap = new HashMap<>();
        loadSpecialites();
        // Bouton Ajouter caché par défaut — visible seulement pour le médecin
        if (btnAjouter != null) {
            btnAjouter.setVisible(false);
            btnAjouter.setManaged(false);
        }
        try { chargerArticles(); } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private void loadSpecialites() {
        try {
            Connection con = MyDB.getInstance().getConnection();
            String sql = "SELECT id, nom FROM specialite";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                specialiteMap.put(rs.getInt("id"), rs.getString("nom"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement spécialités: " + e.getMessage());
        }
    }

    private String getSpecialiteNom(int id) {
        return specialiteMap.getOrDefault(id, "Inconnu");
    }

    private void chargerArticles() throws SQLException {
        List<Article> articles = articleService.getAll();
        
        flowArticles.getChildren().clear();
        
        for (Article article : articles) {
            VBox card = createArticleCard(article);
            flowArticles.getChildren().add(card);
        }
    }

    private VBox createArticleCard(Article article) {
        VBox card = new VBox(10);
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-border-radius:12;" +
                "-fx-border-color:#e2e8f0; -fx-border-width:1; -fx-padding:16;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,3); -fx-cursor:default;");

        // Titre + badge statut
        javafx.scene.layout.HBox top = new javafx.scene.layout.HBox(8);
        top.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label titreLabel = new Label("📰 " + article.getTitre());
        titreLabel.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#0f172a; -fx-wrap-text:true;");
        titreLabel.setMaxWidth(200);
        javafx.scene.layout.Region sp = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(sp, javafx.scene.layout.Priority.ALWAYS);
        String statut = article.getStatut() != null ? article.getStatut().toLowerCase() : "";
        Label badgeStatut = new Label("publié".equals(statut) ? "✅ Publié" : "📝 Brouillon");
        badgeStatut.setStyle("publié".equals(statut)
                ? "-fx-background-color:#f0fdf4; -fx-text-fill:#166534; -fx-font-size:10px; -fx-font-weight:bold; -fx-padding:2 8; -fx-background-radius:999;"
                : "-fx-background-color:#fff7ed; -fx-text-fill:#9a3412; -fx-font-size:10px; -fx-font-weight:bold; -fx-padding:2 8; -fx-background-radius:999;");
        top.getChildren().addAll(titreLabel, sp, badgeStatut);

        // Catégorie
        Label categorieLabel = new Label("🏷 " + getSpecialiteNom(article.getSpecialiteId()));
        categorieLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#475569;");

        // Date
        String dateStr = article.getDatePublication() != null
                ? new SimpleDateFormat("dd MMM yyyy").format(article.getDatePublication()) : "—";
        Label dateLabel = new Label("📅 " + dateStr + "   👁 " + article.getNbVues());
        dateLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#475569;");

        // Boutons icônes — selon le rôle
        javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(6);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // 💬 Commenter : visible pour tous
        Button btnCommenter = iconBtn("💬", "#f0fdf4", "#166534", "#bbf7d0", "Commenter");
        btnCommenter.setOnAction(e -> ouvrirAjouterCommentaire(article));
        buttonBox.getChildren().add(btnCommenter);

        if (isDoctor) {
            // ✏️ Modifier + 🗑 Supprimer : médecin uniquement
            Button btnModifier = iconBtn("✏️", "#e0f2fe", "#0369a1", "#bae6fd", "Modifier");
            btnModifier.setOnAction(e -> modifierArticle(article));

            Button btnSupprimer = iconBtn("🗑", "#fff1f2", "#be123c", "#fecdd3", "Supprimer");
            btnSupprimer.setOnAction(e -> supprimerArticle(article));

            buttonBox.getChildren().addAll(btnModifier, btnSupprimer);
        }

        javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
        card.getChildren().addAll(top, categorieLabel, dateLabel, sep, buttonBox);
        return card;
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

    private void ouvrirAjouterCommentaire(Article article) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterCommentaire.fxml"));
            Parent root = loader.load();
            AjouterCommentaireController controller = loader.getController();
            controller.setArticle(article);

            Stage stage = new Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle("Ajouter un commentaire");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            chargerArticles();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void ajouterArticle() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterArticle.fxml"));
            loader.setController(new AjouterArticleController());
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle("Ajouter Article");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            chargerArticles();
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
            chargerArticles();
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
                chargerArticles();
            } catch (SQLException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText(null);
                alert.setContentText("Impossible de supprimer l'article.");
                alert.show();
                e.printStackTrace();
            }
        }
    }
}
