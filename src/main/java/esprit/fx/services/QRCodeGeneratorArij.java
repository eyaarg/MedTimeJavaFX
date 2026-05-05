package esprit.fx.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * Service de génération de QR Code pour les ordonnances.
 * Utilise ZXing (Zebra Crossing) pour générer les codes QR.
 */
public class QRCodeGeneratorArij {

    private static final int QR_CODE_SIZE = 300;

    /**
     * Génère un QR Code à partir d'une URL et le retourne en tant qu'Image JavaFX.
     *
     * @param data données à encoder (URL de l'ordonnance)
     * @param size taille du QR Code en pixels
     * @return Image JavaFX du QR Code, ou null si erreur
     */
    public Image genererQRCodeImage(String data, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (WriterException e) {
            System.err.println("[QRCodeGeneratorArij] Erreur génération QR Code : " + e.getMessage());
            return null;
        }
    }

    /**
     * Génère un QR Code et le sauvegarde en fichier PNG.
     *
     * @param data données à encoder
     * @param filePath chemin du fichier de sortie
     * @param size taille du QR Code
     * @return true si succès, false sinon
     */
    public boolean genererQRCodeFichier(String data, String filePath, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size);
            Path path = FileSystems.getDefault().getPath(filePath);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
            System.out.println("[QRCodeGeneratorArij] QR Code généré : " + filePath);
            return true;
        } catch (WriterException | IOException e) {
            System.err.println("[QRCodeGeneratorArij] Erreur sauvegarde QR Code : " + e.getMessage());
            return false;
        }
    }

    /**
     * Génère un QR Code pour une ordonnance avec URL de scan.
     *
     * @param ordonnanceId ID de l'ordonnance
     * @param accessToken token d'accès unique
     * @param baseUrl URL de base (ex: http://localhost:8000)
     * @return Image JavaFX du QR Code
     */
    public Image genererQRCodeOrdonnance(int ordonnanceId, String accessToken, String baseUrl) {
        String scanUrl = baseUrl + "/ordonnance/scan/" + accessToken;
        return genererQRCodeImage(scanUrl, QR_CODE_SIZE);
    }
}
