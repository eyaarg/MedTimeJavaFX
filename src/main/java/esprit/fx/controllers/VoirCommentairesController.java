package esprit.fx.controllers;

import esprit.fx.entities.Article;
import esprit.fx.entities.Commentaire;
import esprit.fx.services.CommentaireService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;

public class VoirCommentairesController implements Initializable {

    @FXML private Label  lblTitreArticle;
    @FXML private VBox   vboxCommentaires;
    @FXML private Label  lblAucunCommentaire;
    @FXML private Button btnAjouterCommentaire;

    private Article article;
    private CommentaireService commentaireService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        commentaireService = new CommentaireService();
    }

    public void setArticle(Article article) {
        this.article = article;
        lblTitreArticle.setText("💬 Commentaires — " + article.getTitre());
        chargerCommentaires();
    }

    private void chargerCommentaires() {
        vboxCommentaires.getChildren().clear();
        try {
            List<Commentaire> commentaires = commentaireService.getByArticle(article.getId());
            if (commentaires.isEmpty()) {
                lblAucunCommentaire.setVisible(true);
                lblAucunCommentaire.setManaged(true);
            } else {
                lblAucunCommentaire.setVisible(false);
                lblAucunCommentaire.setManaged(false);
                for (Commentaire c : commentaires)
                    vboxCommentaires.getChildren().add(createCommentaireCard(c));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox createCommentaireCard(Commentaire commentaire) {
        VBox card = new VBox(6);
        card.setStyle(
            "-fx-background-color:white; -fx-background-radius:10; -fx-border-radius:10;" +
            "-fx-border-color:#e2e8f0; -fx-border-width:1; -fx-padding:14 16;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);"
        );

        Label lblContenu = new Label(commentaire.getContenu());
        lblContenu.setWrapText(true);
        lblContenu.setStyle("-fx-font-size:13px; -fx-text-fill:#1e293b;");

        String dateStr = commentaire.getDateCommentaire() != null
                ? new SimpleDateFormat("dd MMM yyyy à HH:mm").format(commentaire.getDateCommentaire()) : "—";

        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        Label lblDate = new Label("🕐 " + dateStr);
        lblDate.setStyle("-fx-font-size:11px; -fx-text-fill:#94a3b8;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnSupprimer = new Button("🗑");
        btnSupprimer.setStyle(
            "-fx-background-color:#fff1f2; -fx-text-fill:#be123c; -fx-font-size:12px;" +
            "-fx-background-radius:6; -fx-border-color:#fecdd3; -fx-border-width:1;" +
            "-fx-padding:4 8; -fx-cursor:hand;"
        );
        btnSupprimer.setOnAction(e -> {
            try {
                commentaireService.supprimer(commentaire.getId());
                chargerCommentaires();
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        footer.getChildren().addAll(lblDate, spacer, btnSupprimer);
        card.getChildren().addAll(lblContenu, footer);
        return card;
    }

    @FXML
    public void ouvrirAjouterCommentaire() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterCommentaire.fxml"));
            Parent root = loader.load();
            AjouterCommentaireController controller = loader.getController();
            controller.setArticle(article);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Ajouter un commentaire");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            chargerCommentaires();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void fermer() {
        Stage stage = (Stage) btnAjouterCommentaire.getScene().getWindow();
        stage.close();
    }
}
