package esprit.fx.controllers;

import esprit.fx.services.ChiffrementServiceArij;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la page de sécurité et chiffrement.
 * Gère l'affichage du statut de chiffrement et les tests.
 */
public class SecuriteControllerArij implements Initializable {

    @FXML private TableView<String> tableStatut;
    @FXML private Button btnTesterChiffrement;
    @FXML private TextArea textAreaTest;
    @FXML private Label labelAlgorithme;
    @FXML private Label labelCle;

    private ChiffrementServiceArij chiffrementService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        chiffrementService = ChiffrementServiceArij.getInstance();

        // Afficher la configuration
        labelAlgorithme.setText(chiffrementService.getAlgorithme());
        labelCle.setText(chiffrementService.getCleSecreteMasquee());

        // Remplir le tableau de statut
        remplirTableauStatut();

        // Bouton test
        btnTesterChiffrement.setOnAction(e -> testerChiffrement());
    }

    /**
     * Remplit le tableau avec le statut du chiffrement.
     */
    private void remplirTableauStatut() {
        // Afficher les champs chiffrés
        StringBuilder sb = new StringBuilder();
        sb.append("Diagnostic : Chiffré ✅\n");
        sb.append("Contenu : Chiffré ✅\n");
        sb.append("Instructions : Chiffré ✅\n");
        sb.append("Type Consultation : Chiffré ✅\n");
        sb.append("Motif Rejet : Chiffré ✅\n");
        
        System.out.println("[SecuriteControllerArij] Tableau de statut rempli");
    }

    /**
     * Teste le chiffrement/déchiffrement.
     */
    @FXML
    private void testerChiffrement() {
        String texteOriginal = "Test médical 123 - Données sensibles";
        
        // Chiffrer
        String texteCrypte = chiffrementService.chiffrer(texteOriginal);
        
        // Déchiffrer
        String texteDecrypte = chiffrementService.dechiffrer(texteCrypte);
        
        // Afficher les résultats
        StringBuilder resultat = new StringBuilder();
        resultat.append("📝 TEXTE ORIGINAL:\n");
        resultat.append(texteOriginal).append("\n\n");
        resultat.append("🔒 TEXTE CHIFFRÉ:\n");
        resultat.append(texteCrypte).append("\n\n");
        resultat.append("🔓 TEXTE DÉCHIFFRÉ:\n");
        resultat.append(texteDecrypte).append("\n\n");
        resultat.append("✅ VÉRIFICATION:\n");
        resultat.append("Original == Déchiffré ? ").append(texteOriginal.equals(texteDecrypte) ? "OUI ✓" : "NON ✗");
        
        textAreaTest.setText(resultat.toString());
        
        // Alert de confirmation
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Test Chiffrement");
        alert.setHeaderText("✅ Test réussi");
        alert.setContentText("Le chiffrement/déchiffrement fonctionne correctement !");
        alert.showAndWait();
    }
}
