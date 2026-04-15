package esprit.fx.controllers;

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
        if (btnBook != null) btnBook.setVisible("PATIENT".equalsIgnoreCase(CURRENT_USER_ROLE));
        showDashboard();
    }

    private void setActive(Button active) {
        for (Button b : new Button[]{btnConsultations, btnBook, btnPrescriptions, btnInvoices, btnNotifications, btnDashboard, btnChat}) {
            if (b != null) b.getStyleClass().remove("nav-btn-active");
        }
        if (active != null) active.getStyleClass().add("nav-btn-active");
    }

    @FXML private void showMyConsultations()  { setActive(btnConsultations); loadView("/fxml/ConsultationListArij.fxml"); }
    @FXML private void showBookAppointment()  { setActive(btnBook);          loadView("/fxml/ConsultationFormArij.fxml"); }
    @FXML private void showPrescriptions()    { setActive(btnPrescriptions); loadView("/fxml/OrdonnanceListArij.fxml"); }
    @FXML private void showInvoices()         { setActive(btnInvoices);      loadView("/fxml/FactureListArij.fxml"); }
    @FXML private void showDashboard()        { setActive(btnDashboard);     loadView("/fxml/DashboardArij.fxml"); }
    @FXML private void showNotifications()    { setActive(btnNotifications); loadView("/fxml/NotificationListArij.fxml"); }
    @FXML private void showChatAi()           { setActive(btnChat);          loadView("/fxml/ChatViewArij.fxml"); }
    @FXML private void handleLogout()         { System.out.println("Déconnexion"); }

    private void loadView(String fxmlPath) {
        try {
            Node view = FXMLLoader.load(Objects.requireNonNull(
                    MainControllerArij.class.getResource(fxmlPath),
                    "FXML not found: " + fxmlPath));
            contentArea.getChildren().setAll(view);
        } catch (IOException | NullPointerException e) {
            System.err.println("Erreur chargement vue " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
