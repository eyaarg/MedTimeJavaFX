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

    @FXML private StackPane contentArea;
    @FXML private Button btnConsultations;
    @FXML private Button btnBook;
    @FXML private Button btnPrescriptions;
    @FXML private Button btnInvoices;
    @FXML private Button btnNotifications;
    @FXML private Button btnDashboard;
    @FXML private Button btnChat;

    @FXML
    private void initialize() {
        if (btnBook != null) {
            btnBook.setVisible("PATIENT".equalsIgnoreCase(CURRENT_USER_ROLE));
        }
        showMyConsultations();
    }

    /** Highlights the active nav button and clears others. */
    private void setActive(Button active) {
        Button[] all = {btnConsultations, btnBook, btnPrescriptions,
                        btnInvoices, btnNotifications, btnDashboard, btnChat};
        for (Button b : all) {
            if (b == null) continue;
            b.getStyleClass().remove("nav-btn-active");
        }
        if (active != null) active.getStyleClass().add("nav-btn-active");
    }

    @FXML
    private void showMyConsultations() {
        setActive(btnConsultations);
        loadView("/fxml/consultationonline/ConsultationListArij.fxml");
    }

    @FXML
    private void showBookAppointment() {
        setActive(btnBook);
        loadView("/fxml/consultationonline/ConsultationFormArij.fxml");
    }

    @FXML
    private void showPrescriptions() {
        setActive(btnPrescriptions);
        loadView("/fxml/consultationonline/OrdonnanceListArij.fxml");
    }

    @FXML
    private void showInvoices() {
        setActive(btnInvoices);
        loadView("/fxml/consultationonline/FactureListArij.fxml");
    }

    @FXML
    private void showDashboard() {
        setActive(btnDashboard);
        loadView("/fxml/consultationonline/DashboardArij.fxml");
    }

    @FXML
    private void showNotifications() {
        setActive(btnNotifications);
        loadView("/fxml/consultationonline/NotificationListArij.fxml");
    }

    @FXML
    private void showChatAi() {
        setActive(btnChat);
        loadView("/fxml/consultationonline/ChatViewArij.fxml");
    }

    @FXML
    private void handleLogout() {
        System.out.println("Logout requested");
        // TODO: navigate to login screen
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
