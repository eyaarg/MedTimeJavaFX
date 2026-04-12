package esprit.fx.controllers;

import esprit.fx.entities.ConsultationsArij;
import esprit.fx.services.ServiceConsultationsArij;
import esprit.fx.utils.MyDB;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDate;

public class ConsultationFormControllerArij {
    private static final int CURRENT_USER_ID = 1;
    private static final String DEFAULT_TYPE = "ONLINE";

    @FXML private DatePicker consultationDatePicker;
    @FXML private ComboBox<Integer> doctorComboBox;
    @FXML private Label dateErrorLabel;
    @FXML private Label doctorErrorLabel;
    @FXML private Label errorLabel;

    private final ServiceConsultationsArij service = new ServiceConsultationsArij();
    private ConsultationsArij consultation;

    @FXML
    private void initialize() { loadDoctors(); }

    public void setConsultation(ConsultationsArij consultation) {
        this.consultation = consultation;
        if (consultation != null) {
            if (consultation.getConsultationDate() != null)
                consultationDatePicker.setValue(consultation.getConsultationDate().toLocalDate());
            if (consultation.getDoctorId() != 0)
                doctorComboBox.setValue(consultation.getDoctorId());
        }
    }

    @FXML
    private void handleSave() {
        hideErrors();
        LocalDate date = consultationDatePicker.getValue();
        Integer doctorId = doctorComboBox.getValue();
        boolean valid = true;

        if (date == null || date.isBefore(LocalDate.now())) {
            dateErrorLabel.setVisible(true); dateErrorLabel.setManaged(true); valid = false;
        }
        if (doctorId == null) {
            doctorErrorLabel.setVisible(true); doctorErrorLabel.setManaged(true); valid = false;
        }
        if (!valid) { errorLabel.setVisible(true); errorLabel.setManaged(true); return; }

        ConsultationsArij c = consultation != null ? consultation : new ConsultationsArij();
        c.setConsultationDate(date.atStartOfDay());
        c.setDoctorId(doctorId);
        c.setPatientId(CURRENT_USER_ID);
        c.setType(DEFAULT_TYPE);

        if (consultation != null && consultation.getId() != 0) service.updateConsultation(c);
        else service.createConsultation(c);
        closeWindow();
    }

    @FXML private void handleCancel() { closeWindow(); }

    private void loadDoctors() {
        ObservableList<Integer> doctors = FXCollections.observableArrayList();
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement("SELECT id FROM doctors");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) doctors.add(rs.getInt("id"));
        } catch (SQLException e) { System.err.println("loadDoctors: " + e.getMessage()); }
        if (doctors.isEmpty()) doctors.add(1);
        doctorComboBox.setItems(doctors);
    }

    private void hideErrors() {
        dateErrorLabel.setVisible(false); dateErrorLabel.setManaged(false);
        doctorErrorLabel.setVisible(false); doctorErrorLabel.setManaged(false);
        errorLabel.setVisible(false); errorLabel.setManaged(false);
    }

    private void closeWindow() {
        ((Stage) consultationDatePicker.getScene().getWindow()).close();
    }
}
