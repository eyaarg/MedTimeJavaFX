package esprit.fx.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import esprit.fx.entities.OrdonnanceArij;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

public class QrCodeServiceArij {

    private static final String BASE_URL = "http://localhost:8000";
    private static final ErrorCorrectionLevel ERROR_CORRECTION = ErrorCorrectionLevel.H;

    public BufferedImage genererQrCode(OrdonnanceArij ordonnance, int taille) {
        if (ordonnance == null) {
            System.err.println("[QrCodeServiceArij] ordonnance null.");
            return null;
        }
        return genererQrCode(buildOrdonnanceQrContent(ordonnance), taille);
    }

    public BufferedImage genererQrCode(String contenu, int taille) {
        if (contenu == null || contenu.isBlank()) {
            System.err.println("[QrCodeServiceArij] Contenu vide.");
            return null;
        }
        if (taille <= 0) taille = 200;

        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ERROR_CORRECTION);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = new MultiFormatWriter().encode(
                    contenu,
                    BarcodeFormat.QR_CODE,
                    taille,
                    taille,
                    hints
            );
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (WriterException e) {
            System.err.println("[QrCodeServiceArij] Erreur generation QR : " + e.getMessage());
            return null;
        }
    }

    public boolean genererQrCodeFichier(OrdonnanceArij ordonnance, String cheminPng, int taille) {
        if (ordonnance == null) return false;
        return genererQrCodeFichier(buildOrdonnanceQrContent(ordonnance), cheminPng, taille);
    }

    public boolean genererQrCodeFichier(String contenu, String cheminPng, int taille) {
        if (contenu == null || contenu.isBlank() || cheminPng == null || cheminPng.isBlank()) {
            return false;
        }
        if (taille <= 0) taille = 300;

        try {
            File fichier = new File(cheminPng);
            if (fichier.getParentFile() != null) {
                fichier.getParentFile().mkdirs();
            }

            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ERROR_CORRECTION);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = new MultiFormatWriter().encode(
                    contenu,
                    BarcodeFormat.QR_CODE,
                    taille,
                    taille,
                    hints
            );
            Path path = FileSystems.getDefault().getPath(cheminPng);
            MatrixToImageWriter.writeToPath(matrix, "PNG", path);
            return true;
        } catch (WriterException | IOException e) {
            System.err.println("[QrCodeServiceArij] Erreur sauvegarde QR : " + e.getMessage());
            return false;
        }
    }

    public Image toJavaFxImage(BufferedImage bufferedImage) {
        if (bufferedImage == null) return null;
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    public Image genererQrCodeJavaFx(OrdonnanceArij ordonnance, int taille) {
        return toJavaFxImage(genererQrCode(ordonnance, taille));
    }

    public Image genererQRCodeOrdonnance(int ordonnanceId, String accessToken) {
        if (accessToken == null || accessToken.isBlank()) return null;
        return toJavaFxImage(genererQrCode(BASE_URL + "/ordonnance/scan/" + accessToken, 160));
    }

    public Image genererQRCodeOrdonnance(OrdonnanceArij ordonnance, int taille) {
        return toJavaFxImage(genererQrCode(ordonnance, taille));
    }

    public boolean genererQRCodeFichier(String contenu, String cheminPng, int taille) {
        return genererQrCodeFichier(contenu, cheminPng, taille);
    }

    public String buildOrdonnanceQrContent(OrdonnanceArij ordonnance) {
        if (ordonnance == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("MEDTIME - ORDONNANCE\n");
        sb.append("Numero: ").append(valueOrDefault(ordonnance.getNumeroOrdonnance(), "ORD-" + ordonnance.getId())).append('\n');
        if (ordonnance.getDateEmission() != null) {
            sb.append("Date emission: ").append(ordonnance.getDateEmission()).append('\n');
        }
        if (ordonnance.getDateValidite() != null) {
            sb.append("Valide jusqu'au: ").append(ordonnance.getDateValidite().toLocalDate()).append('\n');
        }
        sb.append("Medecin ID: ").append(ordonnance.getDoctorId()).append('\n');
        sb.append("Consultation ID: ").append(ordonnance.getConsultationId()).append('\n');
        sb.append("Diagnostic: ").append(valueOrDefault(ordonnance.getDiagnosis(), "-")).append('\n');
        sb.append("Medicaments: ").append(valueOrDefault(ordonnance.getContent(), "-")).append('\n');
        sb.append("Instructions: ").append(valueOrDefault(ordonnance.getInstructions(), "-")).append('\n');
        sb.append("Token: ").append(valueOrDefault(ordonnance.getAccessToken(), ordonnance.getTokenVerification()));
        return sb.toString();
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
