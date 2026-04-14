package esprit.fx.controllers;

import esprit.fx.entities.Produit;
import esprit.fx.services.ServiceProduit;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class FormulaireProduitController implements Initializable {

    // Tous les composants FXML
    @FXML private Label lblTitre;
    @FXML private TextField txtNom;           // nom
    @FXML private TextArea txtDescription;    // description
    @FXML private ComboBox<String> cmbCategorie; // categorie
    @FXML private TextField txtPrix;          // prix
    @FXML private TextField txtStock;         // stock
    @FXML private TextField txtImage;         // image
    @FXML private CheckBox chkDisponible;     // disponible
    @FXML private CheckBox chkPrescription;   // prescription_requise
    @FXML private TextField txtMarque;        // marque
    @FXML private DatePicker dpDateExpiration; // date_expiration
    @FXML private Button btnValider;

    // Variables
    private ServiceProduit serviceProduit;
    private ListeProduitController listeController;
    private String mode; // "ajouter" ou "modifier"
    private Produit produitModification;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupComboBox();
        setupDatePicker();
        setDefaultValues();
    }

    private void setupComboBox() {
        cmbCategorie.getItems().addAll(
                "MEDICAMENT",
                "MATERIEL_MEDICAL",
                "PARAPHARMACIE",
                "HYGIENE",
                "COMPLEMENT_ALIMENTAIRE"
        );
    }

    private void setupDatePicker() {
        dpDateExpiration.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
    }

    private void setDefaultValues() {
        chkDisponible.setSelected(true);  // Par défaut, produit disponible
        chkPrescription.setSelected(false); // Par défaut, pas de prescription
    }

    // Setters pour passer les données
    public void setServiceProduit(ServiceProduit serviceProduit) {
        this.serviceProduit = serviceProduit;
    }

    public void setListeController(ListeProduitController listeController) {
        this.listeController = listeController;
    }

    public void setMode(String mode) {
        this.mode = mode;
        if (mode.equals("ajouter")) {
            lblTitre.setText("Ajouter un produit");
            btnValider.setText("Ajouter");
        } else {
            lblTitre.setText("Modifier un produit");
            btnValider.setText(" Modifier");
        }
    }

    public void setProduit(Produit produit) {
        this.produitModification = produit;
        fillFormWithProduit(produit);
    }

    private void fillFormWithProduit(Produit p) {
        txtNom.setText(p.getNom());
        txtDescription.setText(p.getDescription());
        cmbCategorie.setValue(p.getCategorie());
        txtPrix.setText(String.valueOf(p.getPrix()));
        txtStock.setText(String.valueOf(p.getStock()));
        txtImage.setText(p.getImage());
        chkDisponible.setSelected(p.getDisponible());
        chkPrescription.setSelected(p.getPrescriptionRequise());
        txtMarque.setText(p.getMarque());
        dpDateExpiration.setValue(p.getDateExpiration());
    }

    private boolean validateInputs() {
        if (txtNom.getText().isEmpty()) {
            showAlert("Le nom est obligatoire !");
            return false;
        }

        if (cmbCategorie.getValue() == null) {
            showAlert("La catégorie est obligatoire !");
            return false;
        }

        try {
            double prix = Double.parseDouble(txtPrix.getText());
            if (prix <= 0) {
                showAlert("Le prix doit être positif !");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Prix invalide !");
            return false;
        }

        try {
            if (!txtStock.getText().isEmpty()) {
                int stock = Integer.parseInt(txtStock.getText());
                if (stock < 0) {
                    showAlert("Le stock ne peut pas être négatif !");
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            showAlert("Stock invalide !");
            return false;
        }

        return true;
    }

    private Produit createProduitFromForm() {
        Produit p = new Produit();
        p.setNom(txtNom.getText());                          // nom
        p.setDescription(txtDescription.getText());          // description
        p.setCategorie(cmbCategorie.getValue());             // categorie
        p.setPrix(Double.parseDouble(txtPrix.getText()));    // prix
        p.setStock(txtStock.getText().isEmpty() ? 0 : Integer.parseInt(txtStock.getText())); // stock
        p.setImage(txtImage.getText());                      // image
        p.setDisponible(chkDisponible.isSelected());         // disponible
        p.setPrescriptionRequise(chkPrescription.isSelected()); // prescription_requise
        p.setMarque(txtMarque.getText());                    // marque
        p.setDateExpiration(dpDateExpiration.getValue());    // date_expiration
        return p;
    }

    @FXML
    private void handleValider() throws SQLException {
        if (!validateInputs()) return;

        if (mode.equals("ajouter")) {
            Produit produit = createProduitFromForm();
            serviceProduit.ajouter(produit);
            showAlert("Produit ajouté avec succès !");
        } else {
            Produit produit = createProduitFromForm();
            produit.setId(produitModification.getId());
            serviceProduit.modifier(produit);
            showAlert("Produit modifié avec succès !");
        }

        if (listeController != null) {
            listeController.refreshList();
        }

        closeWindow();
    }

    @FXML
    private void handleAnnuler() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) txtNom.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}