package esprit.fx.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class StatistiquesRendezVousController implements Initializable {

    @FXML
    private BarChart<String, Number> rdvParMoisChart;

    @FXML
    private PieChart rdvParStatutChart;

    @FXML
    private BarChart<String, Number> rdvParMedecinChart;

    @FXML
    private Label tauxConfirmationLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Logique d'initialisation à venir dans l'étape 3
    }
}
