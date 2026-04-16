package esprit.fx.controllers;

import esprit.fx.entities.CategorieEnum;
import esprit.fx.entities.Produit;
import esprit.fx.services.ServiceProduit;
import esprit.fx.utils.MyDB;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class FormulaireProduitController implements Initializable {

    @FXML private TextField txtNom;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<CategorieEnum> cmbCategorie;
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
        cmbCategorie.getItems().setAll(CategorieEnum.values());
        chkDisponible.setSelected(true);

        cmbCategorie.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(CategorieEnum item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNom());
            }
        });

        cmbCategorie.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(CategorieEnum item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Selectionner une categorie" : item.getNom());
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

    private void remplirFormulaire(Produit produit) {
        txtNom.setText(produit.getNom());
        txtDescription.setText(produit.getDescription());
        cmbCategorie.setValue(produit.getCategorie());
        txtPrix.setText(String.valueOf(produit.getPrix()));
        txtStock.setText(String.valueOf(produit.getStock()));
        txtImage.setText(produit.getImage());
        chkDisponible.setSelected(Boolean.TRUE.equals(produit.getDisponible()));
        chkPrescription.setSelected(Boolean.TRUE.equals(produit.getPrescriptionRequise()));
        txtMarque.setText(produit.getMarque());
        dpDateExpiration.setValue(produit.getDateExpiration());
    }

    private boolean validateInputs() {
        if (txtNom.getText() == null || txtNom.getText().trim().isEmpty()) {
            showWarningAlert("Le nom est obligatoire !");
            return false;
        }

        if (cmbCategorie.getValue() == null) {
            showWarningAlert("La categorie est obligatoire !");
            return false;
        }

        try {
            double prix = Double.parseDouble(txtPrix.getText().trim());
            if (prix <= 0) {
                showWarningAlert("Le prix doit etre superieur a 0 !");
                return false;
            }
        } catch (NumberFormatException e) {
            showWarningAlert("Prix invalide !");
            return false;
        }

        try {
            int stock = Integer.parseInt(txtStock.getText().trim());
            if (stock < 0) {
                showWarningAlert("Le stock ne peut pas etre inferieur a 0 !");
                return false;
            }
        } catch (NumberFormatException e) {
            showWarningAlert("Stock invalide !");
            return false;
        }

        if (dpDateExpiration.getValue() == null) {
            showWarningAlert("La date d'expiration est obligatoire !");
            return false;
        }

        if (dpDateExpiration.getValue().isBefore(LocalDate.now())) {
            showWarningAlert("La date d'expiration ne peut pas etre dans le passe !");
            return false;
        }

        return true;
    }

    @FXML
    private void handleValider() {
        ensureServiceProduit();
        if (!validateInputs()) {
            return;
        }

        Produit produit = new Produit();
        produit.setNom(txtNom.getText().trim());
        produit.setDescription(txtDescription.getText());
        produit.setCategorie(cmbCategorie.getValue());
        produit.setPrix(Double.parseDouble(txtPrix.getText().trim()));
        produit.setStock(Integer.parseInt(txtStock.getText().trim()));
        produit.setImage(txtImage.getText());
        produit.setDisponible(chkDisponible.isSelected());
        produit.setPrescriptionRequise(chkPrescription.isSelected());
        produit.setMarque(txtMarque.getText());
        produit.setDateExpiration(dpDateExpiration.getValue());

        try {
            if (!"modifier".equalsIgnoreCase(mode)) {
                serviceProduit.ajouter(produit);
                showInfoAlert("Produit ajoute avec succes !");
            } else {
                produit.setId(produitModification.getId());
                serviceProduit.modifier(produit);
                showInfoAlert("Produit modifie avec succes !");
            }
        } catch (SQLException e) {
            showErrorAlert("Impossible d'enregistrer le produit.");
            return;
        }

        if (listeController != null) {
            try {
                listeController.refreshList();
            } catch (SQLException e) {
                showErrorAlert("Produit enregistre, mais la liste n'a pas pu etre rechargee.");
                return;
            }
        }

        goBackToListOrClose();
    }

    @FXML
    private void handleAnnuler() {
        goBackToListOrClose();
    }

    private void goBackToListOrClose() {
        if (tryNavigateMainContent("/fxml/ListProd.fxml")) {
            return;
        }
        closeWindow();
    }

    private boolean tryNavigateMainContent(String fxmlPath) {
        try {
            Parent sceneRoot = txtNom.getScene() != null ? txtNom.getScene().getRoot() : null;
            if (!(sceneRoot instanceof BorderPane borderPane)) {
                return false;
            }

            Node center = borderPane.getCenter();
            if (!(center instanceof StackPane contentArea)) {
                return false;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
            return true;
        } catch (IOException | NullPointerException e) {
            return false;
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) txtNom.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
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

    private void showWarningAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfoAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
