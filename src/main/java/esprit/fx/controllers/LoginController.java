package esprit.fx.controllers;

import esprit.fx.entities.User;
import esprit.fx.services.ServiceUser;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    private ServiceUser serviceUser = new ServiceUser();

    @FXML
    private void handleLogin() {
        try {
            String email    = usernameField.getText().trim();
            String password = passwordField.getText();

            User user = serviceUser.login(email, password);

            if (user != null) {
                // Résoudre le rôle
                String role = "PATIENT";
                if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                    role = user.getRoles().get(0).getName().toUpperCase();
                }

                // Résoudre patientId / doctorId
                int patientId = 0;
                int doctorId  = 0;
                java.sql.Connection conn = esprit.fx.utils.MyDB.getInstance().getConnection();

                if (role.contains("DOCTOR")) {
                    java.sql.PreparedStatement ps = conn.prepareStatement(
                            "SELECT id FROM doctors WHERE user_id = ?");
                    ps.setInt(1, user.getId());
                    java.sql.ResultSet rs = ps.executeQuery();
                    if (rs.next()) doctorId = rs.getInt("id");
                    role = "DOCTOR";
                } else {
                    java.sql.PreparedStatement ps = conn.prepareStatement(
                            "SELECT id FROM patients WHERE user_id = ?");
                    ps.setInt(1, user.getId());
                    java.sql.ResultSet rs = ps.executeQuery();
                    if (rs.next()) patientId = rs.getInt("id");
                    role = "PATIENT";
                }

                // Charger MainViewArij
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/MainViewArij.fxml"));
                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.setScene(new Scene(loader.load()));
                stage.setTitle("MedTime");
                stage.setMaximized(true);

                // Passer le contexte
                MainControllerArij mainCtrl = loader.getController();
                mainCtrl.setUserContext(user.getId(), patientId, doctorId, role);

            } else {
                showAlert("Erreur", "Email ou mot de passe incorrect");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", e.getMessage());
        }
    }

    @FXML
    private void goToRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Register.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void devLoginPatient() { devLogin("PATIENT"); }

    @FXML
    private void devLoginDoctor() { devLogin("DOCTOR"); }

    @FXML
    private void devLoginAdmin() { devLogin("ADMIN"); }

    private void devLogin(String role) {
        try {
            java.sql.Connection conn = esprit.fx.utils.MyDB.getInstance().getConnection();
            int userId = 0, patientId = 0, doctorId = 0;

            if ("DOCTOR".equals(role)) {
                java.sql.PreparedStatement ps = conn.prepareStatement(
                        "SELECT d.id as doctor_id, d.user_id FROM doctors d LIMIT 1");
                java.sql.ResultSet rs = ps.executeQuery();
                if (rs.next()) { doctorId = rs.getInt("doctor_id"); userId = rs.getInt("user_id"); }
            } else if ("PATIENT".equals(role)) {
                java.sql.PreparedStatement ps = conn.prepareStatement(
                        "SELECT p.id as patient_id, p.user_id FROM patients p LIMIT 1");
                java.sql.ResultSet rs = ps.executeQuery();
                if (rs.next()) { patientId = rs.getInt("patient_id"); userId = rs.getInt("user_id"); }
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/MainViewArij.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("MedTime");
            stage.setMaximized(true);

            MainControllerArij mainCtrl = loader.getController();
            mainCtrl.setUserContext(userId, patientId, doctorId, role);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur dev", e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }
}
