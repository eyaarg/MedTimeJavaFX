package esprit.fx.controllers;

import esprit.fx.entities.ConsultationsArij;
import esprit.fx.services.ServiceConsultationsArij;
import esprit.fx.utils.MyDB;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConsultationFormControllerArij {

    private static final String DEFAULT_TYPE = "ONLINE";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final class DoctorChoice {
        private final int id;
        private final String label;

        DoctorChoice(int id, String label) {
            this.id = id;
            this.label = label;
        }

        int getId() {
            return id;
        }

        String getLabel() {
            return label;
        }
    }

    @FXML private DatePicker consultationDatePicker;
    @FXML private ComboBox<DoctorChoice> doctorComboBox;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private ComboBox<String> timeComboBox;
    @FXML private Label dateErrorLabel;
    @FXML private Label doctorErrorLabel;
    @FXML private Label uniqueErrorLabel;
    @FXML private Label availabilityLabel;
    @FXML private Label errorLabel;

    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Button chooseFilesButton;
    @FXML private Label selectedFilesLabel;

    @FXML private ComboBox<String> durationComboBox;
    @FXML private TextField reasonField;
    @FXML private CheckBox urgentCheckBox;
    @FXML private Label suggestionsLabel;

    private final ServiceConsultationsArij service = new ServiceConsultationsArij();
    private ConsultationsArij consultation;

    private int patientId = 0; // patients.id (provided by MainControllerArij)

    private List<File> selectedFiles = new ArrayList<>();

    @FXML
    private void initialize() {
        initStaticFields();
        initTimeSlots();
        loadDoctors();
    }

    public void setConsultation(ConsultationsArij consultation) {
        this.consultation = consultation;
        if (consultation == null) {
            return;
        }
        LocalDateTime dt = consultation.getConsultationDate();
        if (dt != null) {
            consultationDatePicker.setValue(dt.toLocalDate());
            timeComboBox.setValue(dt.toLocalTime().format(TIME_FMT));
        }
        int doctorId = consultation.getDoctorId();
        if (doctorId != 0 && doctorComboBox.getItems() != null) {
            for (DoctorChoice d : doctorComboBox.getItems()) {
                if (d != null && d.getId() == doctorId) {
                    doctorComboBox.setValue(d);
                    break;
                }
            }
        }
    }

    @FXML
    private void handleSave() {
        hideErrors();

        if (patientId <= 0) {
            new Alert(Alert.AlertType.ERROR, "Veuillez vous connecter en tant que patient.").showAndWait();
            return;
        }

        DoctorChoice doctor = doctorComboBox.getValue();
        LocalDate date = consultationDatePicker.getValue();
        String timeStr = timeComboBox.getValue();

        boolean valid = true;
        if (doctor == null) {
            doctorErrorLabel.setVisible(true);
            doctorErrorLabel.setManaged(true);
            valid = false;
        }

        LocalDateTime selectedDateTime = null;
        if (date == null || timeStr == null || timeStr.isBlank()) {
            dateErrorLabel.setText("Please select a date and time.");
            dateErrorLabel.setVisible(true);
            dateErrorLabel.setManaged(true);
            valid = false;
        } else {
            try {
                LocalTime t = LocalTime.parse(timeStr, TIME_FMT);
                selectedDateTime = LocalDateTime.of(date, t);
                if (selectedDateTime.isBefore(LocalDateTime.now())) {
                    dateErrorLabel.setText("The consultation date cannot be in the past.");
                    dateErrorLabel.setVisible(true);
                    dateErrorLabel.setManaged(true);
                    valid = false;
                }
            } catch (Exception e) {
                dateErrorLabel.setText("Invalid time format.");
                dateErrorLabel.setVisible(true);
                dateErrorLabel.setManaged(true);
                valid = false;
            }
        }

        if (!valid) {
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            return;
        }

        int excludeId = (consultation != null ? consultation.getId() : 0);
        if (service.isDoctorSlotTaken(doctor.getId(), selectedDateTime, excludeId)) {
            uniqueErrorLabel.setVisible(true);
            uniqueErrorLabel.setManaged(true);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            return;
        }

        boolean isNew = (consultation == null || consultation.getId() == 0);
        ConsultationsArij c = isNew ? new ConsultationsArij() : consultation;
        c.setPatientId(patientId);
        c.setDoctorId(doctor.getId());
        c.setConsultationDate(selectedDateTime);
        c.setType(DEFAULT_TYPE);
        if (c.getStatus() == null || c.getStatus().isBlank()) {
            c.setStatus("EN_ATTENTE");
        }

        if (isNew) {
            service.createConsultation(c);
        } else {
            service.updateConsultation(c);
        }

        goBackToListOrClose();
    }

    @FXML
    private void handleCancel() {
        goBackToListOrClose();
    }

    @FXML
    private void handleCheckAvailability() {
        hideErrors();
        availabilityLabel.setVisible(false);
        availabilityLabel.setManaged(false);

        DoctorChoice doctor = doctorComboBox.getValue();
        LocalDate date = consultationDatePicker.getValue();
        String timeStr = timeComboBox.getValue();

        boolean valid = true;
        if (doctor == null) {
            doctorErrorLabel.setVisible(true);
            doctorErrorLabel.setManaged(true);
            valid = false;
        }
        if (date == null || timeStr == null || timeStr.isBlank()) {
            dateErrorLabel.setText("Please select a date and time.");
            dateErrorLabel.setVisible(true);
            dateErrorLabel.setManaged(true);
            valid = false;
        }
        if (!valid) {
            return;
        }

        LocalDateTime dt;
        try {
            dt = LocalDateTime.of(date, LocalTime.parse(timeStr, TIME_FMT));
        } catch (Exception e) {
            dateErrorLabel.setText("Invalid time format.");
            dateErrorLabel.setVisible(true);
            dateErrorLabel.setManaged(true);
            return;
        }

        int excludeId = (consultation != null ? consultation.getId() : 0);
        boolean taken = service.isDoctorSlotTaken(doctor.getId(), dt, excludeId);
        if (taken) {
            uniqueErrorLabel.setVisible(true);
            uniqueErrorLabel.setManaged(true);
            return;
        }

        availabilityLabel.setText("✅ Slot available.");
        availabilityLabel.setStyle("-fx-text-fill:#166534; -fx-font-weight:bold;");
        availabilityLabel.setVisible(true);
        availabilityLabel.setManaged(true);
    }

    @FXML
    private void handleChooseFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select files");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx"),
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );

        List<File> files = chooser.showOpenMultipleDialog(getWindow());
        if (files == null || files.isEmpty()) {
            selectedFiles = new ArrayList<>();
            selectedFilesLabel.setText("No file chosen");
            return;
        }
        selectedFiles = new ArrayList<>(files);
        selectedFilesLabel.setText(files.size() == 1 ? files.get(0).getName() : (files.size() + " files selected"));
    }

    @FXML
    private void handleSuggestSlots() {
        DoctorChoice doctor = doctorComboBox.getValue();
        LocalDate date = consultationDatePicker.getValue();

        if (doctor == null || date == null) {
            suggestionsLabel.setText("Select doctor and date to run availability checks.");
            return;
        }

        int excludeId = (consultation != null ? consultation.getId() : 0);
        List<String> suggestions = new ArrayList<>();
        for (String timeStr : timeComboBox.getItems()) {
            LocalTime t = LocalTime.parse(timeStr, TIME_FMT);
            LocalDateTime dt = LocalDateTime.of(date, t);
            if (dt.isBefore(LocalDateTime.now())) {
                continue;
            }
            if (!service.isDoctorSlotTaken(doctor.getId(), dt, excludeId)) {
                suggestions.add(timeStr);
                if (suggestions.size() >= 3) {
                    break;
                }
            }
        }

        if (suggestions.isEmpty()) {
            suggestionsLabel.setText("No slots available for this date.");
        } else {
            suggestionsLabel.setText("Suggested: " + String.join(", ", suggestions));
        }
    }

    private void initStaticFields() {
        // Type fixed to "En ligne" (ONLINE in DB mapping)
        typeComboBox.setItems(FXCollections.observableArrayList("En ligne"));
        typeComboBox.getSelectionModel().selectFirst();

        // Attachment categories (optional)
        if (categoryComboBox != null) {
            categoryComboBox.setItems(FXCollections.observableArrayList(
                    "Rapport médical",
                    "Ordonnance",
                    "Résultat labo",
                    "Autre"
            ));
            categoryComboBox.getSelectionModel().selectFirst();
        }

        // Assistant (UI only)
        if (durationComboBox != null) {
            durationComboBox.setItems(FXCollections.observableArrayList("30 min", "45 min", "60 min"));
            durationComboBox.getSelectionModel().selectFirst();
        }
    }

    private void initTimeSlots() {
        List<String> slots = new ArrayList<>();
        LocalTime start = LocalTime.of(7, 0);
        LocalTime end = LocalTime.of(19, 0);
        for (LocalTime t = start; !t.isAfter(end); t = t.plusMinutes(10)) {
            slots.add(t.format(TIME_FMT));
        }
        timeComboBox.setItems(FXCollections.observableArrayList(slots));

        LocalDate today = LocalDate.now();
        consultationDatePicker.setValue(today);
        timeComboBox.getSelectionModel().select(nearestSlot(LocalTime.now(), slots));
    }

    private int nearestSlot(LocalTime now, List<String> slots) {
        LocalTime rounded = now.plusMinutes(10 - (now.getMinute() % 10)).withSecond(0).withNano(0);
        String target = rounded.format(TIME_FMT);
        int idx = slots.indexOf(target);
        return idx >= 0 ? idx : 0;
    }

    private void loadDoctors() {
        ObservableList<DoctorChoice> doctors = FXCollections.observableArrayList();
        String sql = """
                SELECT d.id AS id,
                       COALESCE(u.username, CONCAT('Doctor #', d.id)) AS name
                FROM doctors d
                LEFT JOIN users u ON u.id = d.user_id
                ORDER BY name
                """;
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                doctors.add(new DoctorChoice(id, name));
            }
        } catch (SQLException e) {
            System.err.println("loadDoctors: " + e.getMessage());
        }

        doctorComboBox.setItems(doctors);
        doctorComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(DoctorChoice object) {
                return object == null ? "" : object.getLabel();
            }

            @Override
            public DoctorChoice fromString(String string) {
                return null;
            }
        });
        doctorComboBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(DoctorChoice item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLabel());
            }
        });
        doctorComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(DoctorChoice item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Select doctor" : item.getLabel());
            }
        });

        if (doctors.isEmpty()) {
            doctorComboBox.setDisable(true);
            doctorComboBox.setPromptText("No doctors available");
        }
    }

    private void hideErrors() {
        dateErrorLabel.setVisible(false);
        dateErrorLabel.setManaged(false);
        doctorErrorLabel.setVisible(false);
        doctorErrorLabel.setManaged(false);
        uniqueErrorLabel.setVisible(false);
        uniqueErrorLabel.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void goBackToListOrClose() {
        if (tryNavigateMainContent("/fxml/ConsultationListArij.fxml")) {
            return;
        }
        closeWindow();
    }

    private boolean tryNavigateMainContent(String fxmlPath) {
        try {
            Parent sceneRoot = consultationDatePicker.getScene() != null ? consultationDatePicker.getScene().getRoot() : null;
            if (!(sceneRoot instanceof BorderPane bp)) {
                return false;
            }
            Node center = bp.getCenter();
            if (!(center instanceof StackPane contentArea)) {
                return false;
            }

            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            Node view = loader.load();
            Object ctrl = loader.getController();
            if (ctrl instanceof ConsultationListControllerArij c) {
                c.setPatientId(patientId);
            }
            contentArea.getChildren().setAll(view);
            return true;
        } catch (IOException | NullPointerException e) {
            System.err.println("Navigation error: " + e.getMessage());
            return false;
        }
    }

    private void closeWindow() {
        Stage stage = getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    private Stage getWindow() {
        return consultationDatePicker != null && consultationDatePicker.getScene() != null
                ? (Stage) consultationDatePicker.getScene().getWindow()
                : null;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }
}
