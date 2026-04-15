package esprit.fx.controllers;

import esprit.fx.entities.Produit;
import esprit.fx.entities.CategorieEnum;
import esprit.fx.services.ServiceProduit;
import esprit.fx.utils.MyDB;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class FormulaireProduitController implements Initializable {

    @FXML private TextField txtNom;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<CategorieEnum> cmbCategorie;  // ← Type Enum
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
        // Charger les catégories Enum dans le ComboBox
        cmbCategorie.getItems().setAll(CategorieEnum.values());
        chkDisponible.setSelected(true);

        // Personnaliser l'affichage du ComboBox
        cmbCategorie.setCellFactory(param -> new ListCell<CategorieEnum>() {
            @Override
            protected void updateItem(CategorieEnum item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNom());
                }
            }
        });

        cmbCategorie.setButtonCell(new ListCell<CategorieEnum>() {
            @Override
            protected void updateItem(CategorieEnum item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Sélectionner une catégorie");
                } else {
                    setText(item.getNom());
                }
            }
        });
    }

    public void setServiceProduit(ServiceProduit serviceProduit) {
        this.serviceProduit = serviceProduit;
    }

    public void setListeController(ListeProduitController listeController) {
        this.listeController = listeController;
    }

    public void setMode(String mode) {
        this.mode = mode;
        if ("ajouter".equalsIgnoreCase(mode)) {
            lblTitre.setText(" Ajouter un produit");
            btnValider.setText("➕");
        } else {
            lblTitre.setText("✏ Modifier un produit");
            btnValider.setText("✏");
        }
    }

    public void setProduit(Produit produit) {
        this.produitModification = produit;
        remplirFormulaire(produit);
    }

    private void remplirFormulaire(Produit p) {
        txtNom.setText(p.getNom());
        txtDescription.setText(p.getDescription());
        cmbCategorie.setValue(p.getCategorie());  // ← L'Enum s'affiche bien
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

        Produit produit = new Produit();
        produit.setNom(txtNom.getText());
        produit.setDescription(txtDescription.getText());
        produit.setCategorie(cmbCategorie.getValue());  // ← L'Enum
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
