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

        // ========== AJOUT DES LISTENERS POUR VALIDATION EN TEMPS RÉEL ==========
        setupLiveValidation();
    }

    /**
     * Met en place la validation en temps réel (champs qui deviennent rouges)
     */
    private void setupLiveValidation() {
        // Validation du nom en temps réel
        txtNom.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                txtNom.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
            } else if (newVal.length() > 100) {
                txtNom.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
            } else {
                txtNom.setStyle("");
            }
        });

        // Validation du prix en temps réel
        txtPrix.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                txtPrix.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
            } else {
                try {
                    double prix = Double.parseDouble(newVal);
                    if (prix <= 0 || prix > 10000) {
                        txtPrix.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
                    } else {
                        txtPrix.setStyle("");
                    }
                } catch (NumberFormatException e) {
                    txtPrix.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
                }
            }
        });

        // Validation du stock en temps réel
        txtStock.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                try {
                    int stock = Integer.parseInt(newVal);
                    if (stock < 0 || stock > 999999) {
                        txtStock.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
                    } else {
                        txtStock.setStyle("");
                    }
                } catch (NumberFormatException e) {
                    txtStock.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
                }
            } else {
                txtStock.setStyle("");
            }
        });

        // Validation de la marque en temps réel
        txtMarque.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                txtMarque.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
            } else {
                txtMarque.setStyle("");
            }
        });

        // Validation de la date d'expiration
        dpDateExpiration.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.isBefore(LocalDate.now())) {
                dpDateExpiration.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
            } else {
                dpDateExpiration.setStyle("");
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
        cmbCategorie.setValue(p.getCategorie());
        txtPrix.setText(String.valueOf(p.getPrix()));
        txtStock.setText(String.valueOf(p.getStock()));
        txtImage.setText(p.getImage());
        chkDisponible.setSelected(p.getDisponible());
        chkPrescription.setSelected(p.getPrescriptionRequise());
        txtMarque.setText(p.getMarque());
        dpDateExpiration.setValue(p.getDateExpiration());
    }

    /**
     * VALIDATION COMPLÈTE DES CHAMPS AVANT ENVOI
     */
    private boolean validateInputs() {
        // ========== 1. VALIDATION DU NOM ==========
        if (txtNom.getText() == null || txtNom.getText().trim().isEmpty()) {
            showAlert("❌ Le nom du produit est obligatoire !");
            txtNom.requestFocus();
            txtNom.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
            return false;
        }
        if (txtNom.getText().length() > 100) {
            showAlert("❌ Le nom est trop long (maximum 100 caractères) !");
            txtNom.requestFocus();
            txtNom.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
            return false;
        }
        txtNom.setStyle("");

        // ========== 2. VALIDATION DE LA CATÉGORIE ==========
        if (cmbCategorie.getValue() == null) {
            showAlert("❌ Veuillez sélectionner une catégorie !");
            cmbCategorie.requestFocus();
            cmbCategorie.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
            return false;
        }
        cmbCategorie.setStyle("");

        // ========== 3. VALIDATION DU PRIX ==========
        double prix;
        try {
            if (txtPrix.getText() == null || txtPrix.getText().isEmpty()) {
                showAlert("❌ Le prix est obligatoire !");
                txtPrix.requestFocus();
                txtPrix.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
                return false;
            }
            prix = Double.parseDouble(txtPrix.getText().trim());
            if (prix <= 0) {
                showAlert("❌ Le prix doit être supérieur à 0 € !");
                txtPrix.requestFocus();
                txtPrix.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
                return false;
            }
            if (prix > 10000) {
                showAlert("❌ Le prix est trop élevé (maximum 10 000 €) !");
                txtPrix.requestFocus();
                txtPrix.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("❌ Le prix doit être un nombre valide (ex: 12.99) !");
            txtPrix.requestFocus();
            txtPrix.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
            return false;
        }
        txtPrix.setStyle("");

        // ========== 4. VALIDATION DU STOCK ==========
        if (txtStock.getText() != null && !txtStock.getText().isEmpty()) {
            try {
                int stock = Integer.parseInt(txtStock.getText().trim());
                if (stock < 0) {
                    showAlert("❌ Le stock ne peut pas être négatif !");
                    txtStock.requestFocus();
                    txtStock.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
                    return false;
                }
                if (stock > 999999) {
                    showAlert("❌ Le stock est trop élevé (maximum 999 999) !");
                    txtStock.requestFocus();
                    txtStock.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
                    return false;
                }
            } catch (NumberFormatException e) {
                showAlert("❌ Le stock doit être un nombre entier valide !");
                txtStock.requestFocus();
                txtStock.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
                return false;
            }
        }
        txtStock.setStyle("");

        // ========== 5. VALIDATION DE LA MARQUE ==========
        if (txtMarque.getText() == null || txtMarque.getText().trim().isEmpty()) {
            showAlert("❌ La marque est obligatoire !");
            txtMarque.requestFocus();
            txtMarque.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
            return false;
        }
        txtMarque.setStyle("");

        // ========== 6. VALIDATION DE LA DATE D'EXPIRATION ==========
        if (dpDateExpiration.getValue() != null) {
            if (dpDateExpiration.getValue().isBefore(LocalDate.now())) {
                showAlert("❌ La date d'expiration ne peut pas être dans le passé !");
                dpDateExpiration.requestFocus();
                dpDateExpiration.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
                return false;
            }
        }
        dpDateExpiration.setStyle("");

        // ========== 7. VALIDATION DE L'IMAGE (URL facultative mais format vérifié) ==========
        if (txtImage.getText() != null && !txtImage.getText().isEmpty()) {
            String imageUrl = txtImage.getText().trim();
            if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://") && !imageUrl.startsWith("/")) {
                showAlert("⚠️ L'URL de l'image devrait commencer par http://, https:// ou /");
                // Ce n'est pas bloquant, juste un avertissement
            }
        }

        return true;
    }

    @FXML
    private void handleValider() throws SQLException {
        ensureServiceProduit();

        // Validation complète avant d'enregistrer
        if (!validateInputs()) return;

        // Création du produit avec les données validées
        Produit produit = new Produit();
        produit.setNom(txtNom.getText().trim());
        produit.setDescription(txtDescription.getText() != null ? txtDescription.getText().trim() : "");
        produit.setCategorie(cmbCategorie.getValue());
        produit.setPrix(Double.parseDouble(txtPrix.getText().trim()));

        // Gestion du stock (si vide, mettre 0)
        int stock = 0;
        if (txtStock.getText() != null && !txtStock.getText().isEmpty()) {
            stock = Integer.parseInt(txtStock.getText().trim());
        }
        produit.setStock(stock);

        produit.setImage(txtImage.getText() != null ? txtImage.getText().trim() : "");
        produit.setDisponible(chkDisponible.isSelected());
        produit.setPrescriptionRequise(chkPrescription.isSelected());
        produit.setMarque(txtMarque.getText().trim());
        produit.setDateExpiration(dpDateExpiration.getValue());

        // Enregistrement selon le mode (ajout ou modification)
        if (!"modifier".equalsIgnoreCase(mode)) {
            try {
                serviceProduit.ajouter(produit);
                showAlert("✅ Produit ajouté avec succès !");
            } catch (SQLException e) {
                showAlert("❌ Erreur lors de l'ajout : " + e.getMessage());
                return;
            }
        } else {
            produit.setId(produitModification.getId());
            serviceProduit.modifier(produit);
            showAlert("✅ Produit modifié avec succès !");
        }

        // Rafraîchir la liste et fermer la fenêtre
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
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}