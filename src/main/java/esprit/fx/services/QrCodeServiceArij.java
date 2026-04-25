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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * ============================================================
 *  QrCodeServiceArij — Génération de QR Codes avec ZXing
 * ============================================================
 *
 *  Génère des QR codes pour les ordonnances médicales.
 *  Le QR code encode l'URL de scan Symfony :
 *    http://localhost:8000/ordonnance/scan/{accessToken}
 *
 *  Dépendances (déjà dans pom.xml) :
 *    com.google.zxing:core:3.5.2
 *    com.google.zxing:javase:3.5.2
 *    javafx.swing (pour SwingFXUtils)
 *
 *  Usage typique :
 *  ────────────────
 *    QrCodeServiceArij qr = new QrCodeServiceArij();
 *
 *    // 1. Obtenir une BufferedImage (pour intégration PDF ou ImageView)
 *    BufferedImage img = qr.genererQrCode(ordonnance, 200);
 *
 *    // 2. Convertir en JavaFX Image pour un ImageView
 *    ImageView iv = new ImageView(qr.toJavaFxImage(img));
 *
 *    // 3. Sauvegarder en fichier PNG
 *    qr.genererQrCodeFichier(ordonnance, "/chemin/qr.png", 300);
 */
public class QrCodeServiceArij {

    /** URL de base Symfony — modifiable selon l'environnement. */
    private static final String BASE_URL = "http://localhost:8000";

    /** Niveau de correction d'erreur : H = 30% de redondance (recommandé pour documents médicaux). */
    private static final ErrorCorrectionLevel ERROR_CORRECTION = ErrorCorrectionLevel.H;

    // ================================================================== //
    //  Méthode 1 : genererQrCode → BufferedImage                        //
    // ================================================================== //

    /**
     * Génère un QR Code à partir d'une ordonnance.
     *
     * Le contenu encodé est l'URL de scan :
     *   http://localhost:8000/ordonnance/scan/{accessToken}
     *
     * L'accessToken est auto-généré (UUID v4) si absent dans l'entité.
     *
     * @param ordonnance entité ordonnance (accessToken utilisé ou généré)
     * @param taille     taille en pixels (largeur = hauteur, ex: 200)
     * @return BufferedImage du QR code, ou null en cas d'erreur
     */
    public BufferedImage genererQrCode(OrdonnanceArij ordonnance, int taille) {
        if (ordonnance == null) {
            System.err.println("[QrCodeServiceArij] ordonnance null.");
            return null;
        }
        String url = ordonnance.buildScanUrl(BASE_URL);
        return genererQrCode(url, taille);
    }

    /**
     * Génère un QR Code à partir d'un contenu textuel libre.
     *
     * Utilise MultiFormatWriter (générique) plutôt que QRCodeWriter
     * pour permettre d'autres formats si besoin futur.
     *
     * @param contenu texte à encoder (URL, token, texte libre…)
     * @param taille  taille en pixels
     * @return BufferedImage du QR code, ou null en cas d'erreur
     */
    public BufferedImage genererQrCode(String contenu, int taille) {
        if (contenu == null || contenu.isBlank()) {
            System.err.println("[QrCodeServiceArij] Contenu vide.");
            return null;
        }
        if (taille <= 0) taille = 200;

        try {
            // ── Paramètres d'encodage ──────────────────────────────────
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET,       "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION,    ERROR_CORRECTION);
            hints.put(EncodeHintType.MARGIN,              1);  // marge minimale

            // ── Génération de la matrice de bits ──────────────────────
            // MultiFormatWriter détecte automatiquement le format QR_CODE
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix matrix = writer.encode(
                contenu,
                BarcodeFormat.QR_CODE,
                taille,
                taille,
                hints
            );

            // ── Conversion BitMatrix → BufferedImage ──────────────────
            // MatrixToImageWriter utilise noir (#000000) et blanc (#FFFFFF)
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);

            System.out.println("[QrCodeServiceArij] ✓ QR Code généré"
                + " | taille=" + taille + "px"
                + " | contenu=" + truncate(contenu, 60));

            return image;

        } catch (WriterException e) {
            System.err.println("[QrCodeServiceArij] ✗ Erreur génération QR : " + e.getMessage());
            return null;
        }
    }

    // ================================================================== //
    //  Méthode 2 : genererQrCodeFichier → PNG sur disque                //
    // ================================================================== //

    /**
     * Génère un QR Code et le sauvegarde en fichier PNG.
     *
     * @param ordonnance entité ordonnance
     * @param cheminPng  chemin absolu du fichier PNG (ex: "C:/qr/ord-123.png")
     * @param taille     taille en pixels
     * @return true si le fichier a été créé avec succès
     */
    public boolean genererQrCodeFichier(OrdonnanceArij ordonnance,
                                         String cheminPng,
                                         int taille) {
        if (ordonnance == null) return false;
        return genererQrCodeFichier(ordonnance.buildScanUrl(BASE_URL), cheminPng, taille);
    }

    /**
     * Génère un QR Code à partir d'un contenu et le sauvegarde en PNG.
     *
     * @param contenu   texte à encoder
     * @param cheminPng chemin absolu du fichier PNG
     * @param taille    taille en pixels
     * @return true si le fichier a été créé avec succès
     */
    public boolean genererQrCodeFichier(String contenu, String cheminPng, int taille) {
        if (contenu == null || contenu.isBlank()) {
            System.err.println("[QrCodeServiceArij] Contenu vide.");
            return false;
        }
        if (cheminPng == null || cheminPng.isBlank()) {
            System.err.println("[QrCodeServiceArij] Chemin PNG vide.");
            return false;
        }
        if (taille <= 0) taille = 300;

        try {
            // Créer les répertoires parents si nécessaire
            File fichier = new File(cheminPng);
            if (fichier.getParentFile() != null) {
                fichier.getParentFile().mkdirs();
            }

            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET,    "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ERROR_CORRECTION);
            hints.put(EncodeHintType.MARGIN,           1);

            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix matrix = writer.encode(
                contenu,
                BarcodeFormat.QR_CODE,
                taille,
                taille,
                hints
            );

            // Écriture directe en PNG via MatrixToImageWriter
            Path path = FileSystems.getDefault().getPath(cheminPng);
            MatrixToImageWriter.writeToPath(matrix, "PNG", path);

            System.out.println("[QrCodeServiceArij] ✓ QR Code sauvegardé : " + cheminPng);
            return true;

        } catch (WriterException | IOException e) {
            System.err.println("[QrCodeServiceArij] ✗ Erreur sauvegarde QR : " + e.getMessage());
            return false;
        }
    }

    // ================================================================== //
    //  Méthode 3 : conversion BufferedImage → JavaFX Image              //
    // ================================================================== //

    /**
     * Convertit une BufferedImage (AWT/Swing) en JavaFX Image.
     *
     * Utilise SwingFXUtils.toFXImage() — pont entre Swing et JavaFX.
     * Nécessite le module javafx.swing dans le classpath.
     *
     * Usage dans un controller :
     *   BufferedImage bi = qrService.genererQrCode(ordonnance, 200);
     *   ImageView iv = new ImageView(qrService.toJavaFxImage(bi));
     *
     * @param bufferedImage image AWT à convertir
     * @return JavaFX Image, ou null si bufferedImage est null
     */
    public Image toJavaFxImage(BufferedImage bufferedImage) {
        if (bufferedImage == null) return null;
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    /**
     * Raccourci : génère le QR Code d'une ordonnance et retourne
     * directement une JavaFX Image prête pour un ImageView.
     *
     * @param ordonnance entité ordonnance
     * @param taille     taille en pixels
     * @return JavaFX Image du QR code, ou null en cas d'erreur
     */
    public Image genererQrCodeJavaFx(OrdonnanceArij ordonnance, int taille) {
        BufferedImage bi = genererQrCode(ordonnance, taille);
        return toJavaFxImage(bi);
    }

    // ================================================================== //
    //  Helper                                                            //
    // ================================================================== //

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
