package esprit.fx.controllers;

import esprit.fx.entities.CategorieEnum;
import esprit.fx.entities.Produit;
import esprit.fx.services.ServiceProduit;
import esprit.fx.services.VisionService;
import esprit.fx.utils.MyDB;
import javafx.application.Platform;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.ResourceBundle;

public class FormulaireProduitController implements Initializable {

    @FXML private TextField txtNom;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<CategorieEnum> cmbCategorie;
    @FXML private TextField txtPrix;
    @FXML private TextField txtStock;
    @FXML private TextField txtImage;
    @FXML private ImageView imgPreview;
    @FXML private CheckBox chkDisponible;
    @FXML private CheckBox chkPrescription;
    @FXML private TextField txtMarque;
    @FXML private DatePicker dpDateExpiration;
    @FXML private Label lblTitre;
    @FXML private Button btnValider;

    private static final String IMAGES_DIR = "src/main/resources/images/produits/";
    private final VisionService visionService = new VisionService();

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

        if (produit.getImage() != null && !produit.getImage().isBlank()) {
            try {
                URL imageUrl = getClass().getResource(produit.getImage());
                if (imageUrl != null) {
                    imgPreview.setImage(new Image(imageUrl.toExternalForm()));
                }
            } catch (Exception ignored) {}
        }
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
    private void handleChoisirImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        Stage stage = (Stage) txtNom.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                Path destDir = Paths.get(IMAGES_DIR);
                Files.createDirectories(destDir);
                String fileName = System.currentTimeMillis() + "_" + selectedFile.getName();
                Path destPath = destDir.resolve(fileName);
                Files.copy(selectedFile.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
                txtImage.setText("/images/produits/" + fileName);
                imgPreview.setImage(new Image(selectedFile.toURI().toString()));

                // --- Préparation de l'IA Gemini ---
                txtDescription.setText("🤖 Gemini analyse l'image...");
                txtDescription.setDisable(true);

                // Redimensionnement et conversion Base64 (propre pour Gemini)
                String base64Image = encodeAndResizeImage(selectedFile);

                new Thread(() -> {
                    try {
                        // VisionService utilise maintenant Gemini
                        String description = visionService.genererDescription(base64Image);
                        Platform.runLater(() -> {
                            txtDescription.setText(description);
                            txtDescription.setDisable(false);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            txtDescription.setText("⚠️ Erreur Gemini : " + e.getMessage());
                            txtDescription.setDisable(false);
                        });
                    }
                }).start();

            } catch (IOException e) {
                showErrorAlert("Impossible de traiter l'image : " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleValider() {
        ensureServiceProduit();
        if (!validateInputs()) {
            return;
        }

        Produit produit;
        if ("modifier".equalsIgnoreCase(mode)) {
            produit = produitModification;
        } else {
            produit = new Produit();
        }

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
                showInfoAlert("Produit ajouté avec succès !");
            } else {
                serviceProduit.modifier(produit);
                showInfoAlert("Produit modifié avec succès !");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorAlert("Impossible d'enregistrer le produit : " + e.getMessage());
            return;
        }

        if (listeController != null) {
            try {
                listeController.refreshList();
            } catch (SQLException e) {
                showErrorAlert("Produit enregistré, mais la liste n'a pas pu être rechargée.");
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

    private String encodeAndResizeImage(File file) {
        try {
            java.awt.image.BufferedImage originalImage = javax.imageio.ImageIO.read(file);

            if (originalImage == null) {
                return Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
            }

            int targetWidth = 800;
            int targetHeight = (int) (originalImage.getHeight() * (double) targetWidth / originalImage.getWidth());
            int type = originalImage.getType() == 0 ? java.awt.image.BufferedImage.TYPE_INT_RGB : originalImage.getType();

            java.awt.Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, java.awt.Image.SCALE_SMOOTH);
            java.awt.image.BufferedImage outputImage = new java.awt.image.BufferedImage(targetWidth, targetHeight, type);
            outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(outputImage, "jpg", baos);
            
            // On retourne le Base64 pur sans préfixe pour Gemini
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            try {
                return Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
            } catch (Exception ex) {
                return "";
            }
        }
    }
}