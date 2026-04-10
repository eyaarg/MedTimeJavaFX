package tn.esprit.controllers.consultationonline;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.Objects;

public class MainControllerArij {
    private static final String CURRENT_USER_ROLE = "PATIENT";

    @FXML
    private StackPane contentArea;

    @FXML
    private Button bookAppointmentButton;

    @FXML
    private void initialize() {
        bookAppointmentButton.setVisible("PATIENT".equalsIgnoreCase(CURRENT_USER_ROLE));
        showMyConsultations();
    }

    @FXML
    private void showMyConsultations() {
        loadView("/fxml/consultationonline/ConsultationListArij.fxml");
    }

    @FXML
    private void showBookAppointment() {
        loadView("/fxml/consultationonline/ConsultationFormArij.fxml");
    }

    @FXML
    private void showPrescriptions() {
        loadView("/fxml/consultationonline/ConsultationListArij.fxml");
    }

    @FXML
    private void showInvoices() {
        loadView("/fxml/consultationonline/ConsultationListArij.fxml");
    }

    @FXML
    private void showDashboard() {
        loadView("/fxml/consultationonline/ConsultationListArij.fxml");
    }

    @FXML
    private void showNotifications() {
        loadView("/fxml/consultationonline/ConsultationListArij.fxml");
    }

    @FXML
    private void showChatAi() {
        loadView("/fxml/consultationonline/ConsultationListArij.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            Node view = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            contentArea.getChildren().setAll(view);
        } catch (IOException | NullPointerException e) {
            System.err.println("Error loading view " + fxmlPath + ": " + e.getMessage());
        }
    }
}
