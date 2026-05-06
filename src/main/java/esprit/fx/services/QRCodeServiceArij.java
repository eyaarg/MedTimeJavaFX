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

public class QRCodeServiceArij {

    private static final int DEFAULT_SIZE = 300;
    private static final String BASE_URL = "http://localhost:8000";

    public Image generateQRCodeImage(String text) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, DEFAULT_SIZE, DEFAULT_SIZE);
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    public Image genererQRCodeImage(String data, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (WriterException e) {
            System.err.println("[QRCodeServiceArij] Erreur generation QR Code : " + e.getMessage());
            return null;
        }
    }

    public Image genererQRCodeOrdonnance(int ordonnanceId, String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }
        String scanUrl = BASE_URL + "/ordonnance/scan/" + accessToken;
        return genererQRCodeImage(scanUrl, DEFAULT_SIZE);
    }

    public boolean genererQRCodeFichier(String data, String filePath, int size) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, size, size);
            Path path = FileSystems.getDefault().getPath(filePath);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
            return true;
        } catch (Exception e) {
            System.err.println("[QRCodeServiceArij] Erreur generation QR Code fichier : " + e.getMessage());
            return false;
        }
    }

    public void generateQRCodeFile(String text, String filePath) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, DEFAULT_SIZE, DEFAULT_SIZE);
        Path path = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
    }

    public String buildOrdonnanceUrl(int ordonnanceId) {
        return BASE_URL + "/ordonnances/" + ordonnanceId;
    }
}
