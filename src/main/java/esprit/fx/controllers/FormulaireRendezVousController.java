package esprit.fx.controllers;

import esprit.fx.entities.RendezVous;
import esprit.fx.entities.User;
import esprit.fx.services.ServiceRendezVous;
import esprit.fx.services.ServiceUser;
import esprit.fx.services.ServiceDisponibilite;
import esprit.fx.services.WeatherService;
import esprit.fx.utils.UserSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ResourceBundle;

public class FormulaireRendezVousController implements Initializable {

    @FXML private ComboBox<User> comboPatient;
    @FXML private ComboBox<User> comboDocteur;
    @FXML private DatePicker datePickerRdv;
    @FXML private Label labelPatient;
    @FXML private Label labelMeteo;
    @FXML private ComboBox<String> comboHeure;
    @FXML private TextArea textAreaMotif;
    @FXML private TextArea textAreaNotes;
    @FXML private ComboBox<String> comboStatut;
    @FXML private Button btnSauvegarder;
    @FXML private Button btnAnnuler;

    private ServiceRendezVous serviceRendezVous;
    private ServiceUser serviceUser;
    private ServiceDisponibilite serviceDisponibilite;
    private WeatherService weatherService;
    private RendezVous rendezVousActuel;
    private RendezVousController parentController;
    private String currentUserRole;
    private int currentUserId;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            serviceRendezVous = new ServiceRendezVous();
            serviceUser = new ServiceUser();
            serviceDisponibilite = new ServiceDisponibilite();
            weatherService = new WeatherService();

            User currentUser = UserSession.getCurrentUser();
            currentUserRole = UserSession.getCurrentRole();
            currentUserId = currentUser != null ? currentUser.getId() : 0;

            initializeComboBoxes();
            configureBasedOnRole();
            setupWeatherListener();
        } catch (Exception e) {
            System.err.println("Erreur initialisation FormulaireRendezVousController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupWeatherListener() {
        datePickerRdv.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate == null) {
                labelMeteo.setVisible(false);
                labelMeteo.setManaged(false);
                return;
            }
            labelMeteo.setText("Chargement de la meteo...");
            labelMeteo.setStyle(
                    "-fx-font-size: 12px; -fx-text-fill: #6b7280;" +
                    "-fx-background-color: #f3f4f6; -fx-background-radius: 6;" +
                    "-fx-padding: 6 10; -fx-border-color: #e5e7eb; -fx-border-radius: 6;");
            labelMeteo.setVisible(true);
            labelMeteo.setManaged(true);

            Thread thread = new Thread(() -> {
                WeatherService.MeteoResult result = weatherService.getMeteo(newDate);
                Platform.runLater(() -> afficherMeteo(newDate, result));
            });
            thread.setDaemon(true);
            thread.start();
        });
    }

    private void afficherMeteo(LocalDate date, WeatherService.MeteoResult result) {
        if (result == null) {
            labelMeteo.setText("Meteo indisponible pour cette date (hors plage 5 jours)");
            labelMeteo.setStyle(
                    "-fx-font-size: 12px; -fx-text-fill: #92400e;" +
                    "-fx-background-color: #fef3c7; -fx-background-radius: 6;" +
                    "-fx-padding: 6 10; -fx-border-color: #fde68a; -fx-border-radius: 6;");
        } else {
            labelMeteo.setText(result.toDisplayString(date));
            boolean isBad = result.icone.startsWith("09") || result.icone.startsWith("10")
                         || result.icone.startsWith("11") || result.icone.startsWith("13");
            if (isBad) {
                labelMeteo.setStyle(
                        "-fx-font-size: 12px; -fx-text-fill: #1e40af;" +
                        "-fx-background-color: #dbeafe; -fx-background-radius: 6;" +
                        "-fx-padding: 6 10; -fx-border-color: #93c5fd; -fx-border-radius: 6;");
            } else {
                labelMeteo.setStyle(
                        "-fx-font-size: 12px; -fx-text-fill: #065f46;" +
                        "-fx-background-color: #d1fae5; -fx-background-radius: 6;" +
                        "-fx-padding: 6 10; -fx-border-color: #6ee7b7; -fx-border-radius: 6;");
            }
        }
        labelMeteo.setVisible(true);
        labelMeteo.setManaged(true);
    }

    private void initializeComboBoxes() {
        for (int hour = 8; hour <= 18; hour++) {
            comboHeure.getItems().add(String.format("%02d:00", hour));
            if (hour < 18) {
                comboHeure.getItems().add(String.format("%02d:30", hour));
            }
        }

        comboStatut.setItems(FXCollections.observableArrayList(
            "DEMANDE", "CONFIRME", "ANNULE", "TERMINE"
        ));
        comboStatut.setValue("DEMANDE");

        chargerUtilisateurs();
    }

    private void configureBasedOnRole() {
        if ("PATIENT".equals(currentUserRole)) {
            labelPatient.setVisible(false);
            labelPatient.setManaged(false);
            comboPatient.setVisible(false);
            comboPatient.setManaged(false);
            comboPatient.setDisable(true);
            comboStatut.setDisable(true);
        } else if ("DOCTOR".equals(currentUserRole)) {
            labelPatient.setVisible(true);
            labelPatient.setManaged(true);
            comboPatient.setVisible(true);
            comboPatient.setManaged(true);
            comboDocteur.setDisable(true);
        }
    }

    private void chargerUtilisateurs() {
        try {
            List<User> users = serviceUser.getAll();

            List<User> patients = users.stream().filter(this::isPatient).toList();
            List<User> doctors  = users.stream().filter(this::isDoctor).toList();

            comboPatient.setItems(FXCollections.observableArrayList(patients));
            comboDocteur.setItems(FXCollections.observableArrayList(doctors));

            comboPatient.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(User u, boolean empty) {
                    super.updateItem(u, empty);
                    setText(empty || u == null ? null : u.getUsername());
                }
            });
            comboPatient.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(User u, boolean empty) {
                    super.updateItem(u, empty);
                    setText(empty || u == null ? null : u.getUsername());
                }
            });
            comboDocteur.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(User u, boolean empty) {
                    super.updateItem(u, empty);
                    setText(empty || u == null ? null : u.getUsername());
                }
            });
            comboDocteur.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(User u, boolean empty) {
                    super.updateItem(u, empty);
                    setText(empty || u == null ? null : u.getUsername());
                }
            });

            if (currentUserId > 0) {
                if ("PATIENT".equals(currentUserRole)) {
                    User p = findUserById(patients, currentUserId);
                    if (p != null) comboPatient.setValue(p);
                } else if ("DOCTOR".equals(currentUserRole)) {
                    User d = findUserById(doctors, currentUserId);
                    if (d != null) comboDocteur.setValue(d);
                }
            }

        } catch (Exception e) {
            // Ne pas faire crasher initialize() - afficher l'erreur après que la scène soit prête
            System.err.println("Erreur chargement utilisateurs: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> showAlert("Erreur", "Impossible de charger les utilisateurs : " + e.getMessage()));
        }
    }

    public void setRendezVous(RendezVous rendezVous) {
        this.rendezVousActuel = rendezVous;
        if (rendezVous != null) remplirFormulaire(rendezVous);
    }

    public void setParentController(RendezVousController parentController) {
        this.parentController = parentController;
    }

    private void remplirFormulaire(RendezVous rdv) {
        try {
            User patient = serviceUser.afficherParId(rdv.getPatientId());
            if (patient != null) comboPatient.setValue(patient);

            User doctor = serviceUser.afficherParId(rdv.getDoctorId());
            if (doctor != null) comboDocteur.setValue(doctor);

            if (rdv.getDateHeure() != null) {
                datePickerRdv.setValue(rdv.getDateHeure().toLocalDate());
                comboHeure.setValue(String.format("%02d:%02d",
                    rdv.getDateHeure().getHour(), rdv.getDateHeure().getMinute()));
            }

            textAreaMotif.setText(rdv.getMotif());
            textAreaNotes.setText(rdv.getNotes());
            comboStatut.setValue(rdv.getStatut());

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement : " + e.getMessage());
        }
    }

    @FXML
    private void sauvegarder() {
        if (!validerFormulaire()) return;

        try {
            RendezVous rdv = rendezVousActuel != null ? rendezVousActuel : new RendezVous();

            rdv.setPatientId(comboPatient.getValue().getId());
            rdv.setDoctorId(comboDocteur.getValue().getId());

            LocalDate date = datePickerRdv.getValue();
            String[] hm = comboHeure.getValue().split(":");
            rdv.setDateHeure(LocalDateTime.of(date, LocalTime.of(Integer.parseInt(hm[0]), Integer.parseInt(hm[1]))));

            rdv.setMotif(textAreaMotif.getText());
            rdv.setNotes(textAreaNotes.getText());
            rdv.setStatut(comboStatut.getValue());

            if (rendezVousActuel == null) {
                serviceRendezVous.ajouter(rdv);
                showInfo("Succes", "Rendez-vous cree avec succes.");
            } else {
                rdv.setDateModification(LocalDateTime.now());
                serviceRendezVous.modifier(rdv);
                showInfo("Succes", "Rendez-vous modifie avec succes.");
            }

            if (parentController != null) parentController.rafraichir();
            fermer();

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la sauvegarde : " + e.getMessage());
        } catch (Exception e) {
            showAlert("Erreur", "Erreur inattendue : " + e.getMessage());
        }
    }

    @FXML
    private void annuler() {
        fermer();
    }

    private boolean validerFormulaire() {
        if (comboPatient.getValue() == null) {
            showAlert("Validation", "Veuillez selectionner un patient.");
            return false;
        }
        if (comboDocteur.getValue() == null) {
            showAlert("Validation", "Veuillez selectionner un medecin.");
            return false;
        }
        if (datePickerRdv.getValue() == null) {
            showAlert("Validation", "Veuillez selectionner une date.");
            return false;
        }
        if (comboHeure.getValue() == null) {
            showAlert("Validation", "Veuillez selectionner une heure.");
            return false;
        }
        if (textAreaMotif.getText().trim().isEmpty()) {
            showAlert("Validation", "Veuillez saisir le motif du rendez-vous.");
            return false;
        }
        if (datePickerRdv.getValue().isBefore(LocalDate.now())) {
            showAlert("Validation", "La date ne peut pas etre dans le passe.");
            return false;
        }
        return true;
    }

    private void fermer() {
        Stage stage = (Stage) btnAnnuler.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean isPatient(User user) {
        if (user == null || user.getRoles() == null) return false;
        return user.getRoles().stream().anyMatch(role -> {
            if (role == null || role.getName() == null) return false;
            String r = role.getName().trim().toUpperCase();
            return "PATIENT".equals(r) || "ROLE_PATIENT".equals(r);
        });
    }

    private boolean isDoctor(User user) {
        if (user == null || user.getRoles() == null) return false;
        return user.getRoles().stream().anyMatch(role -> {
            if (role == null || role.getName() == null) return false;
            String r = role.getName().trim().toUpperCase();
            return "DOCTOR".equals(r) || "ROLE_DOCTOR".equals(r)
                || "PHYSICIAN".equals(r) || "ROLE_PHYSICIAN".equals(r)
                || "MEDECIN".equals(r);
        });
    }

    private User findUserById(List<User> users, int userId) {
        if (users == null || users.isEmpty()) return null;
        return users.stream()
            .filter(u -> u != null && u.getId() == userId)
            .findFirst().orElse(null);
    }
}
