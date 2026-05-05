package esprit.fx.controllers;

import esprit.fx.entities.RendezVous;
import esprit.fx.entities.User;
import esprit.fx.services.ServiceRendezVous;
import esprit.fx.services.ServiceUser;
import esprit.fx.services.ServiceDisponibilite;
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

public class FormulaireRendezVousController implements Initializable {

    @FXML private ComboBox<User> comboPatient;
    @FXML private ComboBox<User> comboDocteur;
    @FXML private DatePicker datePickerRdv;
    @FXML private ComboBox<String> comboHeure;
    @FXML private TextArea textAreaMotif;
    @FXML private TextArea textAreaNotes;
    @FXML private ComboBox<String> comboStatut;
    @FXML private Button btnSauvegarder;
    @FXML private Button btnAnnuler;

    private ServiceRendezVous serviceRendezVous;
    private ServiceUser serviceUser;
    private ServiceDisponibilite serviceDisponibilite;
    private RendezVous rendezVousActuel;
    private RendezVousController parentController;
    private String currentUserRole;
    private int currentUserId;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        serviceRendezVous = new ServiceRendezVous();
        serviceUser = new ServiceUser();
        serviceDisponibilite = new ServiceDisponibilite();
        
        User currentUser = UserSession.getCurrentUser();
        currentUserRole = UserSession.getCurrentRole();
        currentUserId = currentUser != null ? currentUser.getId() : 0;
        
        initializeComboBoxes();
        configureBasedOnRole();
    }

    private void initializeComboBoxes() {
        // Heures disponibles (de 8h à 18h par créneaux de 30 minutes)
        for (int hour = 8; hour <= 18; hour++) {
            comboHeure.getItems().add(String.format("%02d:00", hour));
            if (hour < 18) {
                comboHeure.getItems().add(String.format("%02d:30", hour));
            }
        }
        
        // Statuts
        comboStatut.setItems(FXCollections.observableArrayList(
            "DEMANDE", "CONFIRME", "ANNULE", "TERMINE"
        ));
        comboStatut.setValue("DEMANDE");
        
        // Charger les utilisateurs
        chargerUtilisateurs();
    }

    private void configureBasedOnRole() {
        if ("PATIENT".equals(currentUserRole)) {
            // Les patients ne peuvent pas changer le patient (c'est eux)
            comboPatient.setDisable(true);
            // Ils ne peuvent pas changer le statut
            comboStatut.setDisable(true);
        } else if ("DOCTOR".equals(currentUserRole)) {
            // Les médecins ne peuvent pas changer le médecin (c'est eux)
            comboDocteur.setDisable(true);
        }
    }

    private void chargerUtilisateurs() {
        try {
            List<User> users = serviceUser.getAll();
            
            // Séparer patients et médecins
            List<User> patients = users.stream()
                .filter(user -> user.getRoles().stream()
                    .anyMatch(role -> "PATIENT".equals(role.getName())))
                .toList();
                
            List<User> doctors = users.stream()
                .filter(user -> user.getRoles().stream()
                    .anyMatch(role -> "DOCTOR".equals(role.getName())))
                .toList();
            
            comboPatient.setItems(FXCollections.observableArrayList(patients));
            comboDocteur.setItems(FXCollections.observableArrayList(doctors));
            
            // Configurer l'affichage des noms dans les ComboBox
            comboPatient.setCellFactory(listView -> new ListCell<User>() {
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
            
            comboPatient.setButtonCell(new ListCell<User>() {
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
            
            // Pré-sélectionner l'utilisateur actuel selon son rôle
            if ("PATIENT".equals(currentUserRole)) {
                User currentUser = UserSession.getCurrentUser();
                comboPatient.setValue(currentUser);
            } else if ("DOCTOR".equals(currentUserRole)) {
                User currentUser = UserSession.getCurrentUser();
                comboDocteur.setValue(currentUser);
            }
            
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les utilisateurs : " + e.getMessage());
        }
    }

    public void setRendezVous(RendezVous rendezVous) {
        this.rendezVousActuel = rendezVous;
        
        if (rendezVous != null) {
            // Mode modification
            remplirFormulaire(rendezVous);
        }
    }

    public void setParentController(RendezVousController parentController) {
        this.parentController = parentController;
    }

    private void remplirFormulaire(RendezVous rdv) {
        try {
            // Trouver et sélectionner le patient
            User patient = serviceUser.afficherParId(rdv.getPatientId());
            if (patient != null) {
                comboPatient.setValue(patient);
            }
            
            // Trouver et sélectionner le médecin
            User doctor = serviceUser.afficherParId(rdv.getDoctorId());
            if (doctor != null) {
                comboDocteur.setValue(doctor);
            }
            
            // Date et heure
            if (rdv.getDateHeure() != null) {
                datePickerRdv.setValue(rdv.getDateHeure().toLocalDate());
                String heure = String.format("%02d:%02d", 
                    rdv.getDateHeure().getHour(), 
                    rdv.getDateHeure().getMinute());
                comboHeure.setValue(heure);
            }
            
            // Autres champs
            textAreaMotif.setText(rdv.getMotif());
            textAreaNotes.setText(rdv.getNotes());
            comboStatut.setValue(rdv.getStatut());
            
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
            RendezVous rdv = rendezVousActuel != null ? rendezVousActuel : new RendezVous();
            
            // Remplir les données
            rdv.setPatientId(comboPatient.getValue().getId());
            rdv.setDoctorId(comboDocteur.getValue().getId());
            
            // Construire la date/heure
            LocalDate date = datePickerRdv.getValue();
            String[] heureMinute = comboHeure.getValue().split(":");
            LocalTime time = LocalTime.of(Integer.parseInt(heureMinute[0]), Integer.parseInt(heureMinute[1]));
            rdv.setDateHeure(LocalDateTime.of(date, time));
            
            rdv.setMotif(textAreaMotif.getText());
            rdv.setNotes(textAreaNotes.getText());
            rdv.setStatut(comboStatut.getValue());
            
            // Sauvegarder
            if (rendezVousActuel == null) {
                serviceRendezVous.ajouter(rdv);
                showInfo("Succès", "Rendez-vous créé avec succès.");
            } else {
                rdv.setDateModification(LocalDateTime.now());
                serviceRendezVous.modifier(rdv);
                showInfo("Succès", "Rendez-vous modifié avec succès.");
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

    private boolean validerFormulaire() {
        if (comboPatient.getValue() == null) {
            showAlert("Validation", "Veuillez sélectionner un patient.");
            return false;
        }
        
        if (comboDocteur.getValue() == null) {
            showAlert("Validation", "Veuillez sélectionner un médecin.");
            return false;
        }
        
        if (datePickerRdv.getValue() == null) {
            showAlert("Validation", "Veuillez sélectionner une date.");
            return false;
        }
        
        if (comboHeure.getValue() == null) {
            showAlert("Validation", "Veuillez sélectionner une heure.");
            return false;
        }
        
        if (textAreaMotif.getText().trim().isEmpty()) {
            showAlert("Validation", "Veuillez saisir le motif du rendez-vous.");
            return false;
        }
        
        // Vérifier que la date n'est pas dans le passé
        LocalDate selectedDate = datePickerRdv.getValue();
        if (selectedDate.isBefore(LocalDate.now())) {
            showAlert("Validation", "La date ne peut pas être dans le passé.");
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