package esprit.fx.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class RendezVousSimpleController implements Initializable {

    @FXML private TableView<String> tableRendezVous;
    @FXML private Button btnAjouter;
    @FXML private Button btnActualiser;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("DEBUG: RendezVousSimpleController initialisé");
        
        // Configuration basique
        if (tableRendezVous != null) {
            System.out.println("DEBUG: Table trouvée et configurée");
        }
        
        if (btnAjouter != null) {
            btnAjouter.setOnAction(e -> {
                System.out.println("DEBUG: Bouton Ajouter cliqué");
                showAlert("Info", "Fonctionnalité en développement");
            });
        }
        
        if (btnActualiser != null) {
            btnActualiser.setOnAction(e -> {
                System.out.println("DEBUG: Bouton Actualiser cliqué");
                showAlert("Info", "Liste actualisée");
            });
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}