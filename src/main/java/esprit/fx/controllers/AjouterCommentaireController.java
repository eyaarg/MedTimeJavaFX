package esprit.fx.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import esprit.fx.entities.Article;
import esprit.fx.entities.Commentaire;
import esprit.fx.services.ArticleService;
import esprit.fx.services.CommentaireService;

import java.net.URL;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class AjouterCommentaireController implements Initializable {

    @FXML
    private TextArea txtContenu;

    @FXML
    private ComboBox<Article> cbArticle;

    @FXML
    private Button btnAjouter;

    @FXML
    private Button btnAnnuler;

    private CommentaireService commentaireService;
    private ArticleService articleService;
    private Article articleSelectionne;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        commentaireService = new CommentaireService();
        articleService = new ArticleService();

        loadArticles();
    }

    private void loadArticles() {
        try {
            List<Article> articles = articleService.getAll();
            cbArticle.setItems(FXCollections.observableArrayList(articles));

            cbArticle.setCellFactory(param -> new ListCell<Article>() {
                @Override
                protected void updateItem(Article item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getTitre());
                }
            });
            cbArticle.setButtonCell(new ListCell<Article>() {
                @Override
                protected void updateItem(Article item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getTitre());
                }
            });

            if (articleSelectionne != null) {
                cbArticle.setValue(articleSelectionne);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setArticle(Article article) {
        this.articleSelectionne = article;
        if (cbArticle != null) {
            cbArticle.setValue(article);
        }
    }

    @FXML
    public void ajouterCommentaire() throws SQLException {
        Article selectedArticle = cbArticle.getValue();
        
        if (txtContenu.getText().isEmpty() || selectedArticle == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setContentText("Veuillez remplir tous les champs !");
            alert.show();
            return;
        }

        Commentaire commentaire = new Commentaire();
        commentaire.setContenu(txtContenu.getText());
        commentaire.setDateCommentaire(new Date());
        commentaire.setNbLikes(0);
        commentaire.setArticle(selectedArticle);

        commentaireService.ajouter(commentaire);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setContentText("Commentaire ajouté avec succès !");
        alert.show();

        Stage stage = (Stage) btnAjouter.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void annuler() {
        Stage stage = (Stage) btnAnnuler.getScene().getWindow();
        stage.close();
    }
}