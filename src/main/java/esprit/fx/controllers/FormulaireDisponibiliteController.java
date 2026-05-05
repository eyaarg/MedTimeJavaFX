package esprit.fx.controllers;

import esprit.fx.entities.Disponibilite;
import esprit.fx.entities.User;
import esprit.fx.services.ServiceDisponibilite;
import esprit.fx.services.ServiceUser;
import esprit.fx.utils.UserSession;
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

public class FormulaireDisponibiliteController implements Initializable {

    @FXML private ComboBox<User> comboDocteur;
    @FXML private DatePicker datePickerDebut;
    @FXML private ComboBox<String> comboHeureDebut;
    @FXML private DatePicker datePickerFin;
    @FXML private ComboBox<String> comboHeureFin;
    @FXML private CheckBox checkBoxDisponible;
    @FXML private TextArea textAreaNotes;
    @FXML private Button btnSauvegarder;
    @FXML private Button btnAnnuler;

    private ServiceDisponibilite serviceDisponibilite;
    private ServiceUser serviceUser;
    private Disponibilite disponibiliteActuelle;
    private DisponibiliteController parentController;
    private String currentUserRole;
    private int currentUserId;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        serviceDisponibilite = new ServiceDisponibilite();
        serviceUser = new ServiceUser();
        
        User currentUser = UserSession.getCurrentUser();
        currentUserRole = UserSession.getCurrentRole();
        currentUserId = currentUser != null ? currentUser.getId() : 0;
        
        initializeComboBoxes();
        configureBasedOnRole();
        
        // Par défaut, disponible
        checkBoxDisponible.setSelected(true);
    }

    private void initializeComboBoxes() {
        // Heures disponibles (de 6h à 23h par créneaux de 30 minutes)
        for (int hour = 6; hour <= 23; hour++) {
            String heureStr = String.format("%02d:00", hour);
            comboHeureDebut.getItems().add(heureStr);
            comboHeureFin.getItems().add(heureStr);
            
            if (hour < 23) {
                String heureStr30 = String.format("%02d:30", hour);
                comboHeureDebut.getItems().add(heureStr30);
                comboHeureFin.getItems().add(heureStr30);
            }
        }
        
        // Valeurs par défaut
        comboHeureDebut.setValue("08:00");
        comboHeureFin.setValue("17:00");
        
        // Charger les médecins
        chargerMedecins();
    }

    private void configureBasedOnRole() {
        if ("DOCTOR".equals(currentUserRole)) {
            // Les médecins ne peuvent gérer que leurs propres disponibilités
            comboDocteur.setDisable(true);
        }
    }

    private void chargerMedecins() {
        try {
            List<User> users = serviceUser.getAll();
            
            // Filtrer seulement les médecins
            List<User> doctors = users.stream()
                .filter(user -> user.getRoles().stream()
                    .anyMatch(role -> "DOCTOR".equals(role.getName())))
                .toList();
            
            comboDocteur.setItems(FXCollections.observableArrayList(doctors));
            
            // Configurer l'affichage des noms dans la ComboBox
            comboDocteur.setCellFactory(listView -> new ListCell<User>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setText(null);
                    } else {
                        setText(user.getUsername() + " (" + user.getEmail() + ")");
                    }
                }
            });
            
            comboDocteur.setButtonCell(new ListCell<User>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setText(null);
                    } else {
                        setText(user.getUsername());
                    }
                }
            });
            
            // Pré-sélectionner le médecin actuel si c'est un médecin
            if ("DOCTOR".equals(currentUserRole)) {
                User currentUser = UserSession.getCurrentUser();
                comboDocteur.setValue(currentUser);
            }
            
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les médecins : " + e.getMessage());
        }
    }

    public void setDisponibilite(Disponibilite disponibilite) {
        this.disponibiliteActuelle = disponibilite;
        
        if (disponibilite != null) {
            // Mode modification
            remplirFormulaire(disponibilite);
        }
    }

    public void setParentController(DisponibiliteController parentController) {
        this.parentController = parentController;
    }

    private void remplirFormulaire(Disponibilite dispo) {
        try {
            // Trouver et sélectionner le médecin
            User doctor = serviceUser.afficherParId(dispo.getDoctorId());
            if (doctor != null) {
                comboDocteur.setValue(doctor);
            }
            
            // Date et heure de début
            if (dispo.getDateDebut() != null) {
                datePickerDebut.setValue(dispo.getDateDebut().toLocalDate());
                String heureDebut = String.format("%02d:%02d", 
                    dispo.getDateDebut().getHour(), 
                    dispo.getDateDebut().getMinute());
                comboHeureDebut.setValue(heureDebut);
            }
            
            // Date et heure de fin
            if (dispo.getDateFin() != null) {
                datePickerFin.setValue(dispo.getDateFin().toLocalDate());
                String heureFin = String.format("%02d:%02d", 
                    dispo.getDateFin().getHour(), 
                    dispo.getDateFin().getMinute());
                comboHeureFin.setValue(heureFin);
            }
            
            // Autres champs
            checkBoxDisponible.setSelected(dispo.isEstDisponible());
            textAreaNotes.setText(dispo.getNotes());
            
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement des données : " + e.getMessage());
        }
    }

    @FXML
    private void sauvegarder() {
        if (!validerFormulaire()) {
            return;
        }
        
        try {
            Disponibilite dispo = disponibiliteActuelle != null ? disponibiliteActuelle : new Disponibilite();
            
            // Remplir les données
            dispo.setDoctorId(comboDocteur.getValue().getId());
            
            // Construire la date/heure de début
            LocalDate dateDebut = datePickerDebut.getValue();
            String[] heureMinuteDebut = comboHeureDebut.getValue().split(":");
            LocalTime timeDebut = LocalTime.of(Integer.parseInt(heureMinuteDebut[0]), Integer.parseInt(heureMinuteDebut[1]));
            dispo.setDateDebut(LocalDateTime.of(dateDebut, timeDebut));
            
            // Construire la date/heure de fin
            LocalDate dateFin = datePickerFin.getValue();
            String[] heureMinuteFin = comboHeureFin.getValue().split(":");
            LocalTime timeFin = LocalTime.of(Integer.parseInt(heureMinuteFin[0]), Integer.parseInt(heureMinuteFin[1]));
            dispo.setDateFin(LocalDateTime.of(dateFin, timeFin));
            
            dispo.setEstDisponible(checkBoxDisponible.isSelected());
            dispo.setNotes(textAreaNotes.getText());
            
            // Sauvegarder
            if (disponibiliteActuelle == null) {
                serviceDisponibilite.ajouter(dispo);
                showInfo("Succès", "Disponibilité créée avec succès.");
            } else {
                serviceDisponibilite.modifier(dispo);
                showInfo("Succès", "Disponibilité modifiée avec succès.");
            }
            
            // Rafraîchir la liste parent et fermer
            if (parentController != null) {
                parentController.rafraichir();
            }
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

    @FXML
    private void copierDateDebut() {
        // Copier la date de début vers la date de fin pour faciliter la saisie
        if (datePickerDebut.getValue() != null) {
            datePickerFin.setValue(datePickerDebut.getValue());
        }
    }

    private boolean validerFormulaire() {
        if (comboDocteur.getValue() == null) {
            showAlert("Validation", "Veuillez sélectionner un médecin.");
            return false;
        }
        
        if (datePickerDebut.getValue() == null) {
            showAlert("Validation", "Veuillez sélectionner la date de début.");
            return false;
        }
        
        if (comboHeureDebut.getValue() == null) {
            showAlert("Validation", "Veuillez sélectionner l'heure de début.");
            return false;
        }
        
        if (datePickerFin.getValue() == null) {
            showAlert("Validation", "Veuillez sélectionner la date de fin.");
            return false;
        }
        
        if (comboHeureFin.getValue() == null) {
            showAlert("Validation", "Veuillez sélectionner l'heure de fin.");
            return false;
        }
        
        // Construire les dates pour validation
        LocalDate dateDebut = datePickerDebut.getValue();
        String[] heureMinuteDebut = comboHeureDebut.getValue().split(":");
        LocalTime timeDebut = LocalTime.of(Integer.parseInt(heureMinuteDebut[0]), Integer.parseInt(heureMinuteDebut[1]));
        LocalDateTime dateTimeDebut = LocalDateTime.of(dateDebut, timeDebut);
        
        LocalDate dateFin = datePickerFin.getValue();
        String[] heureMinuteFin = comboHeureFin.getValue().split(":");
        LocalTime timeFin = LocalTime.of(Integer.parseInt(heureMinuteFin[0]), Integer.parseInt(heureMinuteFin[1]));
        LocalDateTime dateTimeFin = LocalDateTime.of(dateFin, timeFin);
        
        // Vérifier que la date de fin est après la date de début
        if (dateTimeFin.isBefore(dateTimeDebut) || dateTimeFin.isEqual(dateTimeDebut)) {
            showAlert("Validation", "La date/heure de fin doit être après la date/heure de début.");
            return false;
        }
        
        // Vérifier que la date de début n'est pas dans le passé (sauf pour modification)
        if (disponibiliteActuelle == null && dateTimeDebut.isBefore(LocalDateTime.now())) {
            showAlert("Validation", "La date de début ne peut pas être dans le passé.");
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
}