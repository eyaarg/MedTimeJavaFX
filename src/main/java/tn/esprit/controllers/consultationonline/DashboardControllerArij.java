package tn.esprit.controllers.consultationonline;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import tn.esprit.repositories.consultationonline.DashboardRepositoryArij;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class DashboardControllerArij {
    private static final int CURRENT_USER_ID = 1;
    private static final String CURRENT_USER_ROLE = "PATIENT"; // change to "DOCTOR" to test doctor dashboard

    @FXML
    private VBox patientSection;
    @FXML
    private VBox doctorSection;
    @FXML
    private Label totalConsultationsLabel;
    @FXML
    private Label nextConsultationLabel;
    @FXML
    private PieChart patientStatusPie;
    @FXML
    private Label totalReceivedLabel;
    @FXML
    private Label acceptanceRateLabel;
    @FXML
    private Label totalOrdonnancesLabel;
    @FXML
    private BarChart<String, Number> weeklyBarChart;
    @FXML
    private PieChart doctorStatusPie;

    private final DashboardRepositoryArij dashboardRepository = new DashboardRepositoryArij();

    @FXML
    private void initialize() {
        patientSection.setVisible("PATIENT".equalsIgnoreCase(CURRENT_USER_ROLE));
        patientSection.setManaged("PATIENT".equalsIgnoreCase(CURRENT_USER_ROLE));
        doctorSection.setVisible("DOCTOR".equalsIgnoreCase(CURRENT_USER_ROLE));
        doctorSection.setManaged("DOCTOR".equalsIgnoreCase(CURRENT_USER_ROLE));

        if (patientSection.isVisible()) {
            loadPatientData();
        }
        if (doctorSection.isVisible()) {
            loadDoctorData();
        }
    }

    private void loadPatientData() {
        int total = sumStatuses(CURRENT_USER_ID);
        totalConsultationsLabel.setText("Total consultations: " + total);

        LocalDateTime next = dashboardRepository.findNextConsultation(CURRENT_USER_ID);
        if (next != null) {
            nextConsultationLabel.setText("Next consultation: " + next.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        } else {
            nextConsultationLabel.setText("Next consultation: N/A");
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("EN_ATTENTE", dashboardRepository.countByStatus("EN_ATTENTE", CURRENT_USER_ID)),
                new PieChart.Data("CONFIRMEE", dashboardRepository.countByStatus("CONFIRMEE", CURRENT_USER_ID)),
                new PieChart.Data("REFUSEE", dashboardRepository.countByStatus("REFUSEE", CURRENT_USER_ID)),
                new PieChart.Data("TERMINEE", dashboardRepository.countByStatus("TERMINEE", CURRENT_USER_ID))
        );
        patientStatusPie.setData(pieData);
    }

    private void loadDoctorData() {
        int total = sumStatuses(CURRENT_USER_ID);
        totalReceivedLabel.setText("Total consultations received: " + total);

        int confirmed = dashboardRepository.countByStatus("CONFIRMEE", CURRENT_USER_ID);
        double acceptance = total == 0 ? 0 : (confirmed * 100.0) / total;
        acceptanceRateLabel.setText(String.format("Acceptance rate: %.1f%%", acceptance));

        int ordos = dashboardRepository.countOrdonnances(CURRENT_USER_ID);
        totalOrdonnancesLabel.setText("Total ordonnances: " + ordos);

        Map<String, Integer> weeks = dashboardRepository.countByWeek(CURRENT_USER_ID);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<String, Integer> entry : weeks.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        weeklyBarChart.getData().clear();
        weeklyBarChart.getData().add(series);
        CategoryAxis xAxis = (CategoryAxis) weeklyBarChart.getXAxis();
        NumberAxis yAxis = (NumberAxis) weeklyBarChart.getYAxis();
        xAxis.setLabel("Week");
        yAxis.setLabel("Count");

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("EN_ATTENTE", dashboardRepository.countByStatus("EN_ATTENTE", CURRENT_USER_ID)),
                new PieChart.Data("CONFIRMEE", dashboardRepository.countByStatus("CONFIRMEE", CURRENT_USER_ID)),
                new PieChart.Data("REFUSEE", dashboardRepository.countByStatus("REFUSEE", CURRENT_USER_ID)),
                new PieChart.Data("TERMINEE", dashboardRepository.countByStatus("TERMINEE", CURRENT_USER_ID))
        );
        doctorStatusPie.setData(pieData);
    }

    private int sumStatuses(int userId) {
        return dashboardRepository.countByStatus("EN_ATTENTE", userId)
                + dashboardRepository.countByStatus("CONFIRMEE", userId)
                + dashboardRepository.countByStatus("REFUSEE", userId)
                + dashboardRepository.countByStatus("TERMINEE", userId);
    }
}
