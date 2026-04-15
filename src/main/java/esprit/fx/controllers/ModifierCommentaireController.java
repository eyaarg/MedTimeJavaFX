package esprit.fx.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import esprit.fx.entities.Commentaire;
import esprit.fx.services.CommentaireService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class ModifierCommentaireController implements Initializable {

    @FXML
    private TextArea txtContenu;

    @FXML
    private Button btnModifier;

    @FXML
    private Button btnAnnuler;

    private CommentaireService commentaireService;
    private Commentaire commentaireAModifier;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        commentaireService = new CommentaireService();
    }

    // Méthode appelée depuis CommentaireController
    public void setCommentaire(Commentaire commentaire) {
        this.commentaireAModifier = commentaire;
        // Pré-remplir le champ
        txtContenu.setText(commentaire.getContenu());
    }

    @FXML
    public void modifierCommentaire() throws SQLException {
        // Validation
        if (txtContenu.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setContentText("Le contenu ne peut pas être vide !");
            alert.show();
            return;
        }

        // Modifier le commentaire
        commentaireAModifier.setContenu(txtContenu.getText());
        commentaireService.modifier(commentaireAModifier);

        // Message succès
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setContentText("Commentaire modifié avec succès !");
        alert.show();

        // Fermer la fenêtre
        Stage stage = (Stage) btnModifier.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void annuler() {
        Stage stage = (Stage) btnAnnuler.getScene().getWindow();
        stage.close();
    }
}