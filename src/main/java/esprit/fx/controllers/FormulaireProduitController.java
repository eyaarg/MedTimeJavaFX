package esprit.fx.controllers;

import esprit.fx.entities.Produit;
import esprit.fx.services.ServiceProduit;
import esprit.fx.utils.MyDB;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class FormulaireProduitController implements Initializable {

    @FXML private TextField txtNom;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cmbCategorie;  // category names from DB
    @FXML private TextField txtPrix;
    @FXML private TextField txtStock;
    @FXML private TextField txtImage;
    @FXML private CheckBox chkDisponible;
    @FXML private CheckBox chkPrescription;
    @FXML private TextField txtMarque;
    @FXML private DatePicker dpDateExpiration;
    @FXML private Label lblTitre;
    @FXML private Button btnValider;

    private ServiceProduit serviceProduit;
    private ListeProduitController listeController;
    private String mode;
    private Produit produitModification;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chkDisponible.setSelected(true);
        ensureServiceProduit();
        loadCategories();
    }

    /** Populate the ComboBox with category names fetched from product_category. */
    private void loadCategories() {
        try {
            List<String> names = serviceProduit.getAllCategoryNames();
            cmbCategorie.getItems().setAll(names);
        } catch (SQLException e) {
            System.err.println("[FormulaireProduit] Erreur chargement catégories: " + e.getMessage());
        }
    }

    public void setServiceProduit(ServiceProduit serviceProduit) {
        this.serviceProduit = serviceProduit;
        loadCategories(); // reload with the injected service
    }

    public void setListeController(ListeProduitController listeController) {
        this.listeController = listeController;
    }

    public void setMode(String mode) {
        this.mode = mode;
        if ("ajouter".equalsIgnoreCase(mode)) {
            lblTitre.setText(" Ajouter un produit");
            btnValider.setText("Ô×ò");
        } else {
            lblTitre.setText("Ô£Å Modifier un produit");
            btnValider.setText("Ô£Å");
        }
    }

    public void setProduit(Produit produit) {
        this.produitModification = produit;
        remplirFormulaire(produit);
    }

    private void remplirFormulaire(Produit p) {
        txtNom.setText(p.getNom());
        txtDescription.setText(p.getDescription());
        // Select by category name (loaded from DB via JOIN in getAll/afficherParId)
        cmbCategorie.setValue(p.getCategorieName());
        txtPrix.setText(String.valueOf(p.getPrix()));
        txtStock.setText(String.valueOf(p.getStock()));
        txtImage.setText(p.getImage());
        chkDisponible.setSelected(Boolean.TRUE.equals(p.getDisponible()));
        chkPrescription.setSelected(Boolean.TRUE.equals(p.getPrescriptionRequise()));
        txtMarque.setText(p.getMarque() != null ? p.getMarque() : "");
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
            Double.parseDouble(txtPrix.getText());
        } catch (NumberFormatException e) {
            showAlert("Prix invalide !");
            return false;
        }
        return true;
    }

    @FXML
    private void handleValider() throws SQLException {
        ensureServiceProduit();
        if (!validateInputs()) return;

        // Resolve category name → id
        String selectedName = cmbCategorie.getValue();
        Integer categoryId = serviceProduit.resolveCategoryId(selectedName);
        if (categoryId == null) {
            showAlert("Catégorie introuvable en base : " + selectedName);
            return;
        }

        Produit produit = new Produit();
        produit.setNom(txtNom.getText());
        produit.setDescription(txtDescription.getText());
        produit.setCategoryId(categoryId);
        produit.setCategorieName(selectedName);
        produit.setPrix(Double.parseDouble(txtPrix.getText()));
        produit.setStock(Integer.parseInt(txtStock.getText()));
        produit.setImage(txtImage.getText());
        produit.setDisponible(chkDisponible.isSelected());
        produit.setPrescriptionRequise(chkPrescription.isSelected());
        produit.setMarque(txtMarque.getText());
        produit.setDateExpiration(dpDateExpiration.getValue());

        if (!"modifier".equalsIgnoreCase(mode)) {
            try {
                serviceProduit.ajouter(produit);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            showAlert("Produit ajouté avec succès !");
        } else {
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

    private void ensureServiceProduit() {
        if (mode == null || mode.isBlank()) {
            setMode("ajouter");
        }
        if (serviceProduit == null) {
            Connection conn = MyDB.getInstance().getConnection();
            serviceProduit = new ServiceProduit(conn);
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }
}