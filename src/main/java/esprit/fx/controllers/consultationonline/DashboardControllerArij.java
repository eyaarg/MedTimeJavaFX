package esprit.fx.controllers.consultationonline;

import esprit.fx.services.ServiceConsultationsArij;
import esprit.fx.services.ServiceOrdonnanceArij;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class DashboardControllerArij {
    private static final int CURRENT_USER_ID = 1;
    private static final String CURRENT_USER_ROLE = "PATIENT";

    @FXML private VBox patientSection;
    @FXML private VBox doctorSection;
    @FXML private Label totalConsultationsLabel;
    @FXML private Label nextConsultationLabel;
    @FXML private PieChart patientStatusPie;
    @FXML private Label totalReceivedLabel;
    @FXML private Label acceptanceRateLabel;
    @FXML private Label totalOrdonnancesLabel;
    @FXML private BarChart<String, Number> weeklyBarChart;
    @FXML private PieChart doctorStatusPie;

    private final ServiceConsultationsArij consultationService = new ServiceConsultationsArij();
    private final ServiceOrdonnanceArij ordonnanceService = new ServiceOrdonnanceArij();

    @FXML
    private void initialize() {
        boolean isPatient = "PATIENT".equalsIgnoreCase(CURRENT_USER_ROLE);
        patientSection.setVisible(isPatient); patientSection.setManaged(isPatient);
        doctorSection.setVisible(!isPatient); doctorSection.setManaged(!isPatient);
        if (isPatient) loadPatientData(); else loadDoctorData();
    }

    private void loadPatientData() {
        int total = sumStatuses(CURRENT_USER_ID);
        totalConsultationsLabel.setText(String.valueOf(total));
        LocalDateTime next = consultationService.findNextConsultation(CURRENT_USER_ID);
        nextConsultationLabel.setText(next != null ? next.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) : "N/A");
        patientStatusPie.setData(FXCollections.observableArrayList(
            new PieChart.Data("En attente", consultationService.countByStatus("EN_ATTENTE", CURRENT_USER_ID)),
            new PieChart.Data("Confirmée",  consultationService.countByStatus("CONFIRMEE",  CURRENT_USER_ID)),
            new PieChart.Data("Refusée",    consultationService.countByStatus("REFUSEE",    CURRENT_USER_ID)),
            new PieChart.Data("Terminée",   consultationService.countByStatus("TERMINEE",   CURRENT_USER_ID))
        ));
    }

    private void loadDoctorData() {
        int total = sumStatuses(CURRENT_USER_ID);
        totalReceivedLabel.setText(String.valueOf(total));
        int confirmed = consultationService.countByStatus("CONFIRMEE", CURRENT_USER_ID);
        acceptanceRateLabel.setText(String.format("%.1f%%", total == 0 ? 0 : (confirmed * 100.0) / total));
        totalOrdonnancesLabel.setText(String.valueOf(ordonnanceService.countOrdonnancesByDoctor(CURRENT_USER_ID)));
        doctorStatusPie.setData(FXCollections.observableArrayList(
            new PieChart.Data("En attente", consultationService.countByStatus("EN_ATTENTE", CURRENT_USER_ID)),
            new PieChart.Data("Confirmée",  consultationService.countByStatus("CONFIRMEE",  CURRENT_USER_ID)),
            new PieChart.Data("Refusée",    consultationService.countByStatus("REFUSEE",    CURRENT_USER_ID)),
            new PieChart.Data("Terminée",   consultationService.countByStatus("TERMINEE",   CURRENT_USER_ID))
        ));
    }

    private int sumStatuses(int userId) {
        return consultationService.countByStatus("EN_ATTENTE", userId)
             + consultationService.countByStatus("CONFIRMEE",  userId)
             + consultationService.countByStatus("REFUSEE",    userId)
             + consultationService.countByStatus("TERMINEE",   userId);
    }
}
