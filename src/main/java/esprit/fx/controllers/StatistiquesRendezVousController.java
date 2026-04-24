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

    private StatistiquesService statsService = new StatistiquesService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chargerStatistiques();
    }

    private void chargerStatistiques() {
        // 1. Taux de confirmation
        double taux = statsService.getTauxConfirmation();
        tauxConfirmationLabel.setText(String.format("%.2f %%", taux));

        // 2. RDV par Mois (BarChart)
        Map<String, Integer> moisMap = statsService.getNombreRDVParMois();
        XYChart.Series<String, Number> seriesMois = new XYChart.Series<>();
        seriesMois.setName("Nombre de RDV");
        for (Map.Entry<String, Integer> entry : moisMap.entrySet()) {
            seriesMois.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        rdvParMoisChart.getData().clear();
        rdvParMoisChart.getData().add(seriesMois);

        // 3. RDV par Statut (PieChart)
        Map<String, Integer> statutMap = statsService.getNombreRDVParStatut();
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : statutMap.entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
        }
        rdvParStatutChart.setData(pieChartData);

        // 4. RDV par Médecin (BarChart)
        Map<String, Integer> medecinMap = statsService.getNombreRDVParMedecin();
        XYChart.Series<String, Number> seriesMedecin = new XYChart.Series<>();
        seriesMedecin.setName("RDV par Médecin");
        for (Map.Entry<String, Integer> entry : medecinMap.entrySet()) {
            seriesMedecin.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        rdvParMedecinChart.getData().clear();
        rdvParMedecinChart.getData().add(seriesMedecin);
>>>>>>> 72dcbdb (Intégration du service dans le contrôleur et affichage des graphiques de statistiques)
    }
}
