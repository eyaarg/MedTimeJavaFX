package esprit.fx.controllers;

import esprit.fx.utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainControllerArij {

    @FXML private StackPane contentArea;
    @FXML private Button btnConsultations;
    @FXML private Button btnBook;
    @FXML private Button btnPrescriptions;
    @FXML private Button btnInvoices;
    @FXML private Button btnNotifications;
    @FXML private Button btnUsers;
    @FXML private Button btnDashboard;
    @FXML private Button btnChat;

    @FXML
    private void initialize() {
        String currentRole = UserSession.getCurrentRole();
        boolean isPatient = "PATIENT".equalsIgnoreCase(currentRole);
        boolean isAdmin = UserSession.isAdmin();

        if (btnBook != null) {
            btnBook.setVisible(isPatient);
            btnBook.setManaged(isPatient);
        }
        if (btnUsers != null) {
            btnUsers.setVisible(isAdmin);
            btnUsers.setManaged(isAdmin);
        }

        showDashboard();
    }

    private void setActive(Button active) {
        for (Button b : new Button[]{btnConsultations, btnBook, btnPrescriptions, btnInvoices, btnNotifications, btnUsers, btnDashboard, btnChat}) {
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
    @FXML
    private void showUsers() {
        if (!UserSession.isAdmin()) {
            System.err.println("Acces refuse: Users reserve a l'admin.");
            showDashboard();
            return;
        }
        setActive(btnUsers);
        loadUsersView();
    }
    @FXML private void showChatAi()           { setActive(btnChat);          loadView("/fxml/ChatViewArij.fxml"); }
    @FXML
    private void handleLogout() {
        try {
            UserSession.clear();
            Parent loginRoot = FXMLLoader.load(Objects.requireNonNull(
                    MainControllerArij.class.getResource("/Login.fxml"),
                    "FXML not found: /Login.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(loginRoot));
            stage.setTitle("MedTimeFX — Connexion");
            stage.setMaximized(false);
            stage.centerOnScreen();
        } catch (IOException | NullPointerException e) {
            System.err.println("Erreur lors de la deconnexion: " + e.getMessage());
            e.printStackTrace();
        }
    }

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

    private void loadUsersView() {
        String preferred = "/fxml/UserList.fxml";
        String legacy = "/fxml/UserListArij.fxml";
        if (MainControllerArij.class.getResource(preferred) != null) {
            loadView(preferred);
            return;
        }
        if (MainControllerArij.class.getResource(legacy) != null) {
            loadView(legacy);
            return;
        }
        System.err.println("Aucune vue Users trouvee (UserList.fxml / UserListArij.fxml)");
    }
}
