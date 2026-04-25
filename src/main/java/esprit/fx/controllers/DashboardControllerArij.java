package esprit.fx.controllers;

import esprit.fx.services.ServiceConsultationsArij;
import esprit.fx.services.ServiceOrdonnanceArij;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class DashboardControllerArij {

    // ── FXML bindings ─────────────────────────────────────────────────────

    @FXML private VBox  patientSection;
    @FXML private VBox  doctorSection;

    // Patient KPIs
    @FXML private Label totalConsultationsLabel;
    @FXML private Label nextConsultationLabel;
    @FXML private Label patientConfirmRateLabel;
    @FXML private Label patientRefusedLabel;

    // Patient charts
    @FXML private PieChart              patientStatusPie;
    @FXML private BarChart<String, Number> patientWeeklyBar;

    // Doctor KPIs
    @FXML private Label totalReceivedLabel;
    @FXML private Label acceptanceRateLabel;
    @FXML private Label totalOrdonnancesLabel;
    @FXML private Label pendingLabel;

    // Doctor charts
    @FXML private BarChart<String, Number> weeklyBarChart;
    @FXML private PieChart                 doctorStatusPie;

    private final ServiceConsultationsArij consultationService = new ServiceConsultationsArij();
    private final ServiceOrdonnanceArij    ordonnanceService   = new ServiceOrdonnanceArij();

    private boolean isPatient = true;
    private int patientId = 0;
    private int doctorId = 0;

    // ── Init ──────────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        // Context (role/ids) is injected by MainControllerArij after login.
        patientSection.setVisible(false);  patientSection.setManaged(false);
        doctorSection.setVisible(false);  doctorSection.setManaged(false);
    }

    public void setContext(boolean isPatient, int patientId, int doctorId) {
        this.isPatient = isPatient;
        this.patientId = patientId;
        this.doctorId = doctorId;
        refresh();
    }

    private void refresh() {
        patientSection.setVisible(isPatient);  patientSection.setManaged(isPatient);
        doctorSection.setVisible(!isPatient);  doctorSection.setManaged(!isPatient);
        if (isPatient) {
            loadPatientData(patientId);
        } else {
            loadDoctorData(doctorId);
        }
    }

    // ── Patient dashboard ─────────────────────────────────────────────────

    private void loadPatientData(int patientId) {
        int uid = patientId;

        int total     = sumStatuses(uid);
        int confirmed = consultationService.countByStatus("CONFIRMEE", uid);
        int refused   = consultationService.countByStatus("REFUSEE",   uid);
        double rate   = total == 0 ? 0 : (confirmed * 100.0) / total;

        totalConsultationsLabel.setText(String.valueOf(total));
        patientConfirmRateLabel.setText(String.format("%.1f%%", rate));
        patientRefusedLabel.setText(String.valueOf(refused));

        LocalDateTime next = consultationService.findNextConsultation(uid);
        nextConsultationLabel.setText(next != null
                ? next.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
                : "Aucun");

        // Pie chart
        patientStatusPie.setData(FXCollections.observableArrayList(
            new PieChart.Data("En attente (" + consultationService.countByStatus("EN_ATTENTE", uid) + ")",
                               consultationService.countByStatus("EN_ATTENTE", uid)),
            new PieChart.Data("Confirmée ("  + confirmed + ")", confirmed),
            new PieChart.Data("Refusée ("    + refused   + ")", refused),
            new PieChart.Data("Terminée ("   + consultationService.countByStatus("TERMINEE", uid) + ")",
                               consultationService.countByStatus("TERMINEE", uid))
        ));

        // Bar chart — consultations par semaine
        loadWeeklyBar(patientWeeklyBar, uid);
    }

    // ── Doctor dashboard ──────────────────────────────────────────────────

    private void loadDoctorData(int doctorId) {
        int uid = doctorId;

        int total     = sumStatuses(uid);
        int confirmed = consultationService.countByStatus("CONFIRMEE",  uid);
        int pending   = consultationService.countByStatus("EN_ATTENTE", uid);
        int ordos     = ordonnanceService.countOrdonnancesByDoctor(uid);
        double rate   = total == 0 ? 0 : (confirmed * 100.0) / total;

        totalReceivedLabel.setText(String.valueOf(total));
        acceptanceRateLabel.setText(String.format("%.1f%%", rate));
        totalOrdonnancesLabel.setText(String.valueOf(ordos));
        pendingLabel.setText(String.valueOf(pending));

        // Bar chart
        loadWeeklyBar(weeklyBarChart, uid);

        // Pie chart
        doctorStatusPie.setData(FXCollections.observableArrayList(
            new PieChart.Data("En attente (" + pending + ")", pending),
            new PieChart.Data("Confirmée ("  + confirmed + ")", confirmed),
            new PieChart.Data("Refusée ("    + consultationService.countByStatus("REFUSEE",  uid) + ")",
                               consultationService.countByStatus("REFUSEE",  uid)),
            new PieChart.Data("Terminée ("   + consultationService.countByStatus("TERMINEE", uid) + ")",
                               consultationService.countByStatus("TERMINEE", uid))
        ));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void loadWeeklyBar(BarChart<String, Number> chart, int userId) {
        Map<String, Integer> weeks = consultationService.countByWeek(userId);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Consultations");
        if (weeks.isEmpty()) {
            // Données fictives pour la démo si aucune donnée
            series.getData().add(new XYChart.Data<>("S-4", 0));
            series.getData().add(new XYChart.Data<>("S-3", 0));
            series.getData().add(new XYChart.Data<>("S-2", 0));
            series.getData().add(new XYChart.Data<>("S-1", 0));
        } else {
            for (Map.Entry<String, Integer> e : weeks.entrySet())
                series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        }
        chart.getData().clear();
        chart.getData().add(series);
    }

    private int sumStatuses(int userId) {
        return consultationService.countByStatus("EN_ATTENTE", userId)
             + consultationService.countByStatus("CONFIRMEE",  userId)
             + consultationService.countByStatus("REFUSEE",    userId)
             + consultationService.countByStatus("TERMINEE",   userId);
    }
}
