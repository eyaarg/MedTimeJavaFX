package esprit.fx.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * Service de g├®n├®ration de QR Code pour les ordonnances.
 * Utilise ZXing (Zebra Crossing) pour g├®n├®rer les codes QR.
 */
public class QRCodeServiceArij {

    private static final int DEFAULT_SIZE = 300;
    private static final String BASE_URL = "http://localhost:8000";

    /**
     * G├®n├¿re un QR Code pour une ordonnance et le retourne en tant qu'Image JavaFX.
     *
     * @param ordonnanceId ID de l'ordonnance
     * @param accessToken  token d'acc├¿s unique
     * @return Image JavaFX du QR Code, ou null si token manquant ou erreur
     */
    public Image genererQRCodeOrdonnance(int ordonnanceId, String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }
        String scanUrl = BASE_URL + "/ordonnance/scan/" + accessToken;
        return genererQRCodeImage(scanUrl, DEFAULT_SIZE);
    }

    /**
     * G├®n├¿re un QR Code ├á partir d'une URL et le retourne en tant qu'Image JavaFX.
     *
     * @param data donn├®es ├á encoder
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
            System.err.println("[QRCodeServiceArij] Erreur g├®n├®ration QR Code : " + e.getMessage());
            return null;
        }
    }

    /**
     * G├®n├¿re un QR Code et le sauvegarde en fichier PNG.
     *
     * @param data     donn├®es ├á encoder
     * @param filePath chemin du fichier de sortie
     * @param size     taille du QR Code
     * @return true si succ├¿s, false sinon
     */
    public boolean genererQRCodeFichier(String data, String filePath, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size);
            Path path = FileSystems.getDefault().getPath(filePath);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
            System.out.println("[QRCodeServiceArij] QR Code g├®n├®r├® : " + filePath);
            return true;
        } catch (WriterException | IOException e) {
            System.err.println("[QRCodeServiceArij] Erreur sauvegarde QR Code : " + e.getMessage());
            return false;
        }
    }
}
