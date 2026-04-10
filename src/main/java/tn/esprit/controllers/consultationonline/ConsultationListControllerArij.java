package tn.esprit.controllers.consultationonline;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.consultationonline.ConsultationArij;
import tn.esprit.services.consultationonline.ConsultationServiceArij;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConsultationListControllerArij {
    private static final int CURRENT_USER_ID = 1;
    private static final String CURRENT_USER_ROLE = "PATIENT";

    @FXML
    private ComboBox<String> filterStatusCombo;
    @FXML
    private ComboBox<String> filterTypeCombo;
    @FXML
    private TextField searchField;
    @FXML
    private FlowPane cardsContainer;
    @FXML
    private Button newConsultationButton;

    private final ConsultationServiceArij consultationService = new ConsultationServiceArij();

    @FXML
    private void initialize() {
        filterStatusCombo.setItems(FXCollections.observableArrayList("EN_ATTENTE", "CONFIRMEE", "REFUSEE", "TERMINEE"));
        filterTypeCombo.setItems(FXCollections.observableArrayList("ONLINE", "IN_PERSON"));
        if (!"PATIENT".equalsIgnoreCase(CURRENT_USER_ROLE)) {
            newConsultationButton.setVisible(false);
        }
        loadConsultations(consultationService.getMyConsultations());
    }

    @FXML
    private void onStatusFilterChange() {
        filterConsultations();
    }

    @FXML
    private void onTypeFilterChange() {
        filterConsultations();
    }

    @FXML
    private void onSearch() {
        filterConsultations();
    }

    @FXML
    private void openNewConsultation() {
        openForm(null);
    }

    private void filterConsultations() {
        String status = filterStatusCombo.getValue();
        String type = filterTypeCombo.getValue();
        List<ConsultationArij> list = consultationService.filterConsultations(
                status == null ? "" : status,
                type == null ? "" : type
        );

        String search = searchField.getText();
        if (search != null && !search.isEmpty()) {
            String lower = search.toLowerCase();
            list = list.stream().filter(c ->
                    String.valueOf(c.getDoctorId()).contains(lower) ||
                            String.valueOf(c.getPatientId()).contains(lower) ||
                            (c.getStatus() != null && c.getStatus().toLowerCase().contains(lower)) ||
                            (c.getLienMeet() != null && c.getLienMeet().toLowerCase().contains(lower))
            ).collect(Collectors.toList());
        }
        loadConsultations(list);
    }

    private void loadConsultations(List<ConsultationArij> consultations) {
        cardsContainer.getChildren().clear();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (ConsultationArij c : consultations) {
            VBox card = new VBox(6);
            card.getStyleClass().add("card");
            card.setPrefWidth(240);

            Label dateLabel = new Label(c.getConsultationDate() != null ? c.getConsultationDate().format(formatter) : "N/A");
            Label statusLabel = new Label(c.getStatus() != null ? c.getStatus() : "N/A");
            statusLabel.setStyle("-fx-font-weight: bold;" + statusColor(c.getStatus()));
            Label counterpart = new Label("PATIENT".equalsIgnoreCase(CURRENT_USER_ROLE)
                    ? "Doctor ID: " + c.getDoctorId()
                    : "Patient ID: " + c.getPatientId());

            Button editBtn = new Button("Edit");
            editBtn.setVisible("EN_ATTENTE".equalsIgnoreCase(c.getStatus()));
            editBtn.setOnAction(e -> openForm(c));

            Button deleteBtn = new Button("Delete");
            deleteBtn.setOnAction(e -> {
                consultationService.deleteConsultation(c.getId());
                refreshList();
            });

            Button acceptBtn = new Button("Accept");
            Button refuseBtn = new Button("Refuse");
            boolean isDoctor = "DOCTOR".equalsIgnoreCase(CURRENT_USER_ROLE);
            acceptBtn.setVisible(isDoctor);
            refuseBtn.setVisible(isDoctor);
            acceptBtn.setOnAction(e -> {
                consultationService.acceptConsultation(c.getId());
                refreshList();
            });
            refuseBtn.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog("Unavailable");
                dialog.setHeaderText("Refuse consultation #" + c.getId());
                dialog.setContentText("Reason:");
                Optional<String> res = dialog.showAndWait();
                consultationService.rejectConsultation(c.getId(), res.orElse("No reason provided"));
                refreshList();
            });

            List<Button> buttons = new ArrayList<>();
            if (editBtn.isVisible()) buttons.add(editBtn);
            buttons.add(deleteBtn);
            if (acceptBtn.isVisible()) buttons.add(acceptBtn);
            if (refuseBtn.isVisible()) buttons.add(refuseBtn);

            card.getChildren().addAll(dateLabel, statusLabel, counterpart);
            card.getChildren().addAll(buttons);
            cardsContainer.getChildren().add(card);
        }
    }

    private void refreshList() {
        loadConsultations(consultationService.getMyConsultations());
    }

    private void openForm(ConsultationArij consultation) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/fxml/consultationonline/ConsultationFormArij.fxml")));
            Parent root = loader.load();
            ConsultationFormControllerArij controller = loader.getController();
            if (consultation != null) {
                controller.setConsultation(consultation);
            }
            Stage stage = new Stage();
            stage.setTitle("Consultation");
            stage.setScene(new Scene(root));
            stage.setOnHidden(e -> refreshList());
            stage.show();
        } catch (IOException e) {
            System.err.println("Error opening form: " + e.getMessage());
        }
    }

    private String statusColor(String status) {
        if (status == null) return "";
        switch (status.toUpperCase()) {
            case "EN_ATTENTE":
                return "-fx-text-fill: #e0a800;";
            case "CONFIRMEE":
                return "-fx-text-fill: #28a745;";
            case "REFUSEE":
                return "-fx-text-fill: #dc3545;";
            case "TERMINEE":
                return "-fx-text-fill: #17a2b8;";
            default:
                return "";
        }
    }
}
