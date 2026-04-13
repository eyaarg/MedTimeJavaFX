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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        commentaireService = new CommentaireService();
        articleService = new ArticleService();

        // Charger les articles dans le ComboBox
        List<Article> articles = null;
        try {
            articles = articleService.getAll();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        cbArticle.setItems(FXCollections.observableArrayList(articles));

        // Afficher le titre de l'article dans le ComboBox
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
    }

    @FXML
    public void ajouterCommentaire() throws SQLException {
        // Validation
        if (txtContenu.getText().isEmpty() || cbArticle.getValue() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setContentText("Veuillez remplir tous les champs !");
            alert.show();
            return;
        }

        // Créer le commentaire
        Commentaire commentaire = new Commentaire();
        commentaire.setContenu(txtContenu.getText());
        commentaire.setDateCommentaire(new Date());
        commentaire.setNbLikes(0);
        commentaire.setArticle(cbArticle.getValue());

        // Ajouter en BD
        commentaireService.ajouter(commentaire);

        // Message succès
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setContentText("Commentaire ajouté avec succès !");
        alert.show();

        // Fermer la fenêtre
        Stage stage = (Stage) btnAjouter.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void annuler() {
        Stage stage = (Stage) btnAnnuler.getScene().getWindow();
        stage.close();
    }
}