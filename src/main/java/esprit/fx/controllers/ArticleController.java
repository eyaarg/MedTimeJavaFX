package esprit.fx.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        articleService = new ArticleService();
        specialiteMap = new HashMap<>();

        loadSpecialites();

        try {
            chargerArticles();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
        VBox card = new VBox();
        card.setPrefWidth(220);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2),5,0,0,2);");
        
        Label titreLabel = new Label(article.getTitre());
        titreLabel.setFont(new Font(16));
        titreLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
        titreLabel.setWrapText(true);
        
        Label categorieLabel = new Label("Catégorie: " + getSpecialiteNom(article.getSpecialiteId()));
        categorieLabel.setStyle("-fx-text-fill: #666;");
        
        String dateStr = "";
        if (article.getDatePublication() != null) {
            dateStr = new SimpleDateFormat("dd/MM/yyyy").format(article.getDatePublication());
        }
        Label dateLabel = new Label("Date: " + dateStr);
        dateLabel.setStyle("-fx-text-fill: #666;");
        
        Label statutLabel = new Label("Statut: " + article.getStatut());
        statutLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        
        Label likesLabel = new Label("Vues: " + article.getNbVues());
        likesLabel.setStyle("-fx-text-fill: #666;");
        
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        
        Button btnCommenter = new Button("Commenter");
        btnCommenter.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-background-radius: 5;");
        btnCommenter.setOnAction(e -> ouvrirAjouterCommentaire(article));
        
        Button btnModifier = new Button("Modifier");
        btnModifier.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-background-radius: 5;");
        btnModifier.setOnAction(e -> modifierArticle(article));
        
        Button btnSupprimer = new Button("Supprimer");
        btnSupprimer.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-background-radius: 5;");
        btnSupprimer.setOnAction(e -> supprimerArticle(article));
        
        buttonBox.getChildren().addAll(btnCommenter, btnModifier, btnSupprimer);
        
        card.getChildren().addAll(titreLabel, categorieLabel, dateLabel, likesLabel, statutLabel, buttonBox);
        card.setPadding(new Insets(15));
        
        return card;
    }

    private void ouvrirAjouterCommentaire(Article article) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterCommentaire.fxml"));
            Parent root = loader.load();
            
            AjouterCommentaireController controller = loader.getController();
            controller.setArticle(article);
            
            Stage stage = new Stage();
            stage.setTitle("Ajouter un commentaire");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            chargerArticles();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void supprimerArticle(Article article) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer cet article?");
        
        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            try {
                articleService.supprimer(article.getId());
                chargerArticles();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @FXML
    public void ajouterArticle() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterArticle.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter Article");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            chargerArticles();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void modifierArticle(Article article) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ModifierArticle.fxml"));
            Parent root = loader.load();
            ModifierArticleController controller = loader.getController();
            controller.setArticle(article);
            Stage stage = new Stage();
            stage.setTitle("Modifier Article");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            chargerArticles();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}