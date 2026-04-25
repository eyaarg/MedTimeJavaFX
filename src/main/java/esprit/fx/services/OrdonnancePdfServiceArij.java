package esprit.fx.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.io.font.constants.StandardFonts;
import esprit.fx.entities.LigneOrdonnanceArij;
import esprit.fx.entities.OrdonnanceArij;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * ============================================================
 *  OrdonnancePdfServiceArij — Génération PDF iText 7
 * ============================================================
 *
 *  Génère un PDF médical professionnel pour une ordonnance :
 *
 *  Structure du document :
 *  ┌─────────────────────────────────────────────┐
 *  │  EN-TÊTE  : logo + nom cabinet + bandeau    │
 *  │  PATIENT  : nom, prénom, date de naissance  │
 *  │  MÉDECIN  : Dr {nom}, spécialité            │
 *  │  DATE     : date d'émission                 │
 *  │  TABLEAU  : médicaments | posologie | durée │
 *  │  RECOMMANDATIONS du médecin                 │
 *  │  QR CODE  : token de vérification           │
 *  │  PIED DE PAGE : n° ordonnance + mention     │
 *  └─────────────────────────────────────────────┘
 *
 *  Après génération : ouvre le PDF avec Desktop.open()
 *
 *  Usage :
 *  ────────
 *    OrdonnancePdfServiceArij service = new OrdonnancePdfServiceArij();
 *    service.genererPdf(ordonnance, lignes, patientNom, patientPrenom,
 *                       patientDateNaissance, medecinNom, medecinSpecialite,
 *                       "/chemin/ordonnance.pdf");
 */
public class OrdonnancePdfServiceArij {

    // ------------------------------------------------------------------ //
    //  Palette de couleurs MedTime                                        //
    // ------------------------------------------------------------------ //
    private static final DeviceRgb BLEU_PRIMAIRE  = new DeviceRgb(29,  78, 216);  // #1d4ed8
    private static final DeviceRgb BLEU_CLAIR     = new DeviceRgb(239, 246, 255); // #eff6ff
    private static final DeviceRgb GRIS_TEXTE     = new DeviceRgb(71,  85, 105);  // #475569
    private static final DeviceRgb GRIS_BORDURE   = new DeviceRgb(226, 232, 240); // #e2e8f0
    private static final DeviceRgb BLANC          = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb ROUGE_ALERTE   = new DeviceRgb(220,  38,  38); // #dc2626

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    // ================================================================== //
    //  Méthode principale                                                 //
    // ================================================================== //

    /**
     * Génère le PDF de l'ordonnance et l'ouvre avec le lecteur système.
     *
     * @param ordonnance         entité ordonnance (numéro, dates, instructions…)
     * @param lignes             liste des médicaments prescrits
     * @param patientNom         nom de famille du patient
     * @param patientPrenom      prénom du patient
     * @param patientDateNaissance date de naissance (format dd/MM/yyyy ou null)
     * @param medecinNom         nom complet du médecin (sans "Dr.")
     * @param medecinSpecialite  spécialité médicale (ex: "Cardiologue")
     * @param cheminFichier      chemin absolu du fichier PDF à créer
     */
    public void genererPdf(OrdonnanceArij ordonnance,
                           List<LigneOrdonnanceArij> lignes,
                           String patientNom,
                           String patientPrenom,
                           String patientDateNaissance,
                           String medecinNom,
                           String medecinSpecialite,
                           String cheminFichier) {

        File fichier = new File(cheminFichier);

        try {
            // Créer les répertoires parents si nécessaire
            if (fichier.getParentFile() != null) {
                fichier.getParentFile().mkdirs();
            }

            // ── Initialisation iText 7 ─────────────────────────────────
            PdfWriter   writer  = new PdfWriter(cheminFichier);
            PdfDocument pdf     = new PdfDocument(writer);
            Document    doc     = new Document(pdf, PageSize.A4);
            doc.setMargins(40, 50, 60, 50); // top, right, bottom, left

            // Charger les polices standard (pas de fichier .ttf requis)
            PdfFont fontBold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont fontItalic  = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            // ── 1. EN-TÊTE ─────────────────────────────────────────────
            ajouterEntete(doc, ordonnance, fontBold, fontRegular);

            doc.add(separateur());
            doc.add(new Paragraph("\n").setFontSize(4));

            // ── 2. INFOS PATIENT + MÉDECIN (2 colonnes) ───────────────
            ajouterInfosPatientMedecin(doc, patientNom, patientPrenom,
                patientDateNaissance, medecinNom, medecinSpecialite,
                fontBold, fontRegular);

            doc.add(new Paragraph("\n").setFontSize(6));

            // ── 3. DATE DE L'ORDONNANCE ────────────────────────────────
            ajouterDateOrdonnance(doc, ordonnance, fontRegular, fontItalic);

            doc.add(new Paragraph("\n").setFontSize(8));

            // ── 4. TABLEAU DES MÉDICAMENTS ─────────────────────────────
            ajouterTableauMedicaments(doc, lignes, fontBold, fontRegular);

            doc.add(new Paragraph("\n").setFontSize(8));

            // ── 5. RECOMMANDATIONS ─────────────────────────────────────
            ajouterRecommandations(doc, ordonnance, fontBold, fontRegular, fontItalic);

            doc.add(new Paragraph("\n").setFontSize(8));

            // ── 6. QR CODE de vérification ─────────────────────────────
            ajouterQrCode(doc, ordonnance, fontRegular, fontItalic);

            // ── 7. PIED DE PAGE ────────────────────────────────────────
            ajouterPiedDePage(doc, ordonnance, fontRegular, fontItalic);

            // ── Fermeture ──────────────────────────────────────────────
            doc.close();

            System.out.println("[OrdonnancePdfServiceArij] ✓ PDF généré : " + cheminFichier);

            // ── Ouvrir le PDF avec le lecteur système ──────────────────
            ouvrirPdf(fichier);

        } catch (IOException e) {
            System.err.println("[OrdonnancePdfServiceArij] ✗ Erreur génération PDF : "
                + e.getMessage());
        }
    }

    // ================================================================== //
    //  Sections du document                                               //
    // ================================================================== //

    /**
     * EN-TÊTE : bandeau bleu avec nom du cabinet + logo si disponible.
     */
    private void ajouterEntete(Document doc, OrdonnanceArij ordonnance,
                                PdfFont fontBold, PdfFont fontRegular) throws IOException {

        // Bandeau bleu en-tête
        Table header = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
            .setWidth(UnitValue.createPercentValue(100))
            .setBackgroundColor(BLEU_PRIMAIRE)
            .setBorder(Border.NO_BORDER);

        // Colonne gauche : nom du cabinet
        Cell cellGauche = new Cell()
            .setBorder(Border.NO_BORDER)
            .setPadding(14);

        cellGauche.add(new Paragraph("🏥  Cabinet MedTime")
            .setFont(fontBold)
            .setFontSize(18)
            .setFontColor(BLANC));

        cellGauche.add(new Paragraph("Plateforme de consultation médicale en ligne")
            .setFont(fontRegular)
            .setFontSize(10)
            .setFontColor(new DeviceRgb(191, 219, 254))); // #bfdbfe

        header.addCell(cellGauche);

        // Colonne droite : logo ou badge ORDONNANCE
        Cell cellDroite = new Cell()
            .setBorder(Border.NO_BORDER)
            .setPadding(14)
            .setTextAlignment(TextAlignment.RIGHT);

        // Essayer de charger le logo depuis les ressources
        boolean logoCharge = false;
        try {
            var logoStream = getClass().getResourceAsStream("/images/logo.png");
            if (logoStream != null) {
                byte[] logoBytes = logoStream.readAllBytes();
                Image logo = new Image(ImageDataFactory.create(logoBytes))
                    .setWidth(60).setHeight(60);
                cellDroite.add(logo);
                logoCharge = true;
            }
        } catch (Exception ignored) {}

        if (!logoCharge) {
            // Fallback : badge texte si pas de logo
            cellDroite.add(new Paragraph("ORDONNANCE")
                .setFont(fontBold)
                .setFontSize(11)
                .setFontColor(BLANC)
                .setBackgroundColor(new DeviceRgb(30, 64, 175))
                .setPadding(6)
                .setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(6)));
        }

        header.addCell(cellDroite);
        doc.add(header);
    }

    /**
     * INFOS PATIENT + MÉDECIN en deux colonnes côte à côte.
     */
    private void ajouterInfosPatientMedecin(Document doc,
                                             String patientNom,
                                             String patientPrenom,
                                             String patientDateNaissance,
                                             String medecinNom,
                                             String medecinSpecialite,
                                             PdfFont fontBold,
                                             PdfFont fontRegular) throws IOException {

        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
            .setWidth(UnitValue.createPercentValue(100))
            .setBorder(Border.NO_BORDER);

        // ── Colonne PATIENT ──────────────────────────────────────────
        Cell cellPatient = new Cell()
            .setBorder(new SolidBorder(GRIS_BORDURE, 1))
            .setBackgroundColor(BLEU_CLAIR)
            .setPadding(12)
            .setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(8));

        cellPatient.add(new Paragraph("👤  Patient")
            .setFont(fontBold).setFontSize(11)
            .setFontColor(BLEU_PRIMAIRE));

        cellPatient.add(ligneInfo("Nom",     safe(patientNom),            fontBold, fontRegular));
        cellPatient.add(ligneInfo("Prénom",  safe(patientPrenom),         fontBold, fontRegular));
        cellPatient.add(ligneInfo("Né(e) le", safe(patientDateNaissance), fontBold, fontRegular));

        // ── Colonne MÉDECIN ──────────────────────────────────────────
        Cell cellMedecin = new Cell()
            .setBorder(new SolidBorder(GRIS_BORDURE, 1))
            .setBackgroundColor(new DeviceRgb(240, 253, 244)) // vert très clair
            .setPadding(12)
            .setMarginLeft(8)
            .setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(8));

        cellMedecin.add(new Paragraph("🩺  Médecin prescripteur")
            .setFont(fontBold).setFontSize(11)
            .setFontColor(new DeviceRgb(22, 101, 52))); // vert foncé

        String nomMedecin = (medecinNom != null && !medecinNom.isBlank())
            ? "Dr. " + medecinNom : "Dr. —";
        cellMedecin.add(ligneInfo("Médecin",    nomMedecin,                fontBold, fontRegular));
        cellMedecin.add(ligneInfo("Spécialité", safe(medecinSpecialite),   fontBold, fontRegular));

        table.addCell(cellPatient);
        table.addCell(cellMedecin);
        doc.add(table);
    }

    /**
     * DATE DE L'ORDONNANCE.
     */
    private void ajouterDateOrdonnance(Document doc, OrdonnanceArij ordonnance,
                                        PdfFont fontRegular, PdfFont fontItalic) throws IOException {
        String dateStr = ordonnance.getDateEmission() != null
            ? ordonnance.getDateEmission().format(DATETIME_FMT)
            : "—";

        doc.add(new Paragraph()
            .add(new Text("Date de l'ordonnance : ").setFont(fontRegular).setFontSize(10)
                .setFontColor(GRIS_TEXTE))
            .add(new Text(dateStr).setFont(fontItalic).setFontSize(10)
                .setFontColor(GRIS_TEXTE)));
    }

    /**
     * TABLEAU DES MÉDICAMENTS : Médicament | Posologie/Dosage | Durée.
     */
    private void ajouterTableauMedicaments(Document doc,
                                            List<LigneOrdonnanceArij> lignes,
                                            PdfFont fontBold,
                                            PdfFont fontRegular) throws IOException {

        doc.add(new Paragraph("💊  Médicaments prescrits")
            .setFont(fontBold).setFontSize(13)
            .setFontColor(BLEU_PRIMAIRE)
            .setMarginBottom(6));

        if (lignes == null || lignes.isEmpty()) {
            doc.add(new Paragraph("Aucun médicament prescrit.")
                .setFont(fontRegular).setFontSize(10)
                .setFontColor(GRIS_TEXTE));
            return;
        }

        // Tableau : 4 colonnes — Médicament | Dosage | Durée | Instructions
        Table table = new Table(UnitValue.createPercentArray(new float[]{35, 20, 20, 25}))
            .setWidth(UnitValue.createPercentValue(100))
            .setBorder(Border.NO_BORDER);

        // En-têtes
        String[] headers = {"Médicament", "Posologie / Dosage", "Durée", "Instructions"};
        for (String h : headers) {
            table.addHeaderCell(
                new Cell()
                    .setBackgroundColor(BLEU_PRIMAIRE)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(8)
                    .add(new Paragraph(h)
                        .setFont(fontBold)
                        .setFontSize(10)
                        .setFontColor(BLANC)
                        .setTextAlignment(TextAlignment.CENTER))
            );
        }

        // Lignes de données (alternance de couleurs)
        boolean pair = false;
        for (LigneOrdonnanceArij ligne : lignes) {
            DeviceRgb bg = pair
                ? new DeviceRgb(248, 250, 252)  // #f8fafc
                : BLANC;
            pair = !pair;

            table.addCell(cellTableau(safe(ligne.getNomMedicament()), fontBold,   bg, true));
            table.addCell(cellTableau(
                safe(ligne.getDosage()) + (ligne.getQuantite() > 0
                    ? "  (qté: " + ligne.getQuantite() + ")" : ""),
                fontRegular, bg, false));
            table.addCell(cellTableau(safe(ligne.getDureeTraitement()), fontRegular, bg, false));
            table.addCell(cellTableau(safe(ligne.getInstructions()),    fontRegular, bg, false));
        }

        doc.add(table);
    }

    /**
     * RECOMMANDATIONS du médecin (instructions générales + diagnostic).
     */
    private void ajouterRecommandations(Document doc, OrdonnanceArij ordonnance,
                                         PdfFont fontBold, PdfFont fontRegular,
                                         PdfFont fontItalic) throws IOException {

        boolean hasInstructions = ordonnance.getInstructions() != null
            && !ordonnance.getInstructions().isBlank();
        boolean hasDiagnosis    = ordonnance.getDiagnosis() != null
            && !ordonnance.getDiagnosis().isBlank();

        if (!hasInstructions && !hasDiagnosis) return;

        doc.add(new Paragraph("📋  Recommandations du médecin")
            .setFont(fontBold).setFontSize(13)
            .setFontColor(BLEU_PRIMAIRE)
            .setMarginBottom(6));

        Table box = new Table(UnitValue.createPercentArray(new float[]{100}))
            .setWidth(UnitValue.createPercentValue(100))
            .setBorder(new SolidBorder(GRIS_BORDURE, 1))
            .setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(8));

        Cell cell = new Cell()
            .setBorder(Border.NO_BORDER)
            .setBackgroundColor(new DeviceRgb(255, 251, 235)) // jaune très clair
            .setPadding(12);

        if (hasDiagnosis) {
            cell.add(new Paragraph()
                .add(new Text("Diagnostic : ").setFont(fontBold).setFontSize(10))
                .add(new Text(ordonnance.getDiagnosis()).setFont(fontItalic).setFontSize(10))
                .setMarginBottom(6));
        }

        if (hasInstructions) {
            cell.add(new Paragraph()
                .add(new Text("Instructions : ").setFont(fontBold).setFontSize(10))
                .add(new Text(ordonnance.getInstructions()).setFont(fontRegular).setFontSize(10)));
        }

        box.addCell(cell);
        doc.add(box);
    }

    /**
     * QR CODE de vérification généré avec ZXing.
     *
     * Le QR code encode le tokenVerification de l'ordonnance.
     * Permet à un pharmacien ou médecin de scanner et vérifier
     * l'authenticité du document via l'application web Symfony.
     */
    private void ajouterQrCode(Document doc, OrdonnanceArij ordonnance,
                                PdfFont fontRegular, PdfFont fontItalic) throws IOException {

        String token = ordonnance.getTokenVerification();
        if (token == null || token.isBlank()) return;

        // Contenu du QR code : URL de vérification ou token brut
        String qrContent = "https://medtime.app/verify/" + token;

        try {
            byte[] qrBytes = genererQrCodePng(qrContent, 120);

            doc.add(separateur());
            doc.add(new Paragraph("\n").setFontSize(4));

            Table qrTable = new Table(UnitValue.createPercentArray(new float[]{20, 80}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBorder(Border.NO_BORDER);

            // Image QR
            Cell cellQr = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(4);
            Image qrImage = new Image(ImageDataFactory.create(qrBytes))
                .setWidth(80).setHeight(80);
            cellQr.add(qrImage);

            // Texte explicatif
            Cell cellTexte = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(8)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);

            cellTexte.add(new Paragraph("🔐  QR Code de vérification")
                .setFont(fontRegular).setFontSize(10)
                .setFontColor(GRIS_TEXTE));
            cellTexte.add(new Paragraph("Scannez ce code pour vérifier l'authenticité de ce document.")
                .setFont(fontItalic).setFontSize(9)
                .setFontColor(GRIS_TEXTE));
            cellTexte.add(new Paragraph("Token : " + token)
                .setFont(fontRegular).setFontSize(8)
                .setFontColor(new DeviceRgb(148, 163, 184))); // gris clair

            qrTable.addCell(cellQr);
            qrTable.addCell(cellTexte);
            doc.add(qrTable);

        } catch (WriterException e) {
            System.err.println("[OrdonnancePdfServiceArij] QR Code non généré : " + e.getMessage());
        }
    }

    /**
     * PIED DE PAGE : numéro d'ordonnance + mention confidentielle.
     */
    private void ajouterPiedDePage(Document doc, OrdonnanceArij ordonnance,
                                    PdfFont fontRegular, PdfFont fontItalic) throws IOException {

        doc.add(separateur());

        Table footer = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
            .setWidth(UnitValue.createPercentValue(100))
            .setBorder(Border.NO_BORDER)
            .setMarginTop(6);

        // Gauche : numéro d'ordonnance
        Cell cellGauche = new Cell()
            .setBorder(Border.NO_BORDER);
        cellGauche.add(new Paragraph(
            "N° Ordonnance : " + safe(ordonnance.getNumeroOrdonnance()))
            .setFont(fontRegular).setFontSize(9)
            .setFontColor(GRIS_TEXTE));

        // Droite : mention confidentielle
        Cell cellDroite = new Cell()
            .setBorder(Border.NO_BORDER)
            .setTextAlignment(TextAlignment.RIGHT);
        cellDroite.add(new Paragraph("🔒  Document médical confidentiel")
            .setFont(fontItalic).setFontSize(9)
            .setFontColor(ROUGE_ALERTE));

        footer.addCell(cellGauche);
        footer.addCell(cellDroite);
        doc.add(footer);

        // Ligne de copyright
        doc.add(new Paragraph("Généré par MedTimeFX — " +
            java.time.LocalDateTime.now().format(DATETIME_FMT))
            .setFont(fontItalic).setFontSize(8)
            .setFontColor(new DeviceRgb(148, 163, 184))
            .setTextAlignment(TextAlignment.CENTER));
    }

    // ================================================================== //
    //  Ouverture du PDF                                                   //
    // ================================================================== //

    /**
     * Ouvre le PDF généré avec l'application système par défaut.
     * Utilise java.awt.Desktop (compatible Windows, macOS, Linux).
     *
     * Appelé après la fermeture du Document iText pour s'assurer
     * que le fichier est complètement écrit avant ouverture.
     */
    private void ouvrirPdf(File fichier) {
        if (!fichier.exists()) {
            System.err.println("[OrdonnancePdfServiceArij] Fichier introuvable : "
                + fichier.getAbsolutePath());
            return;
        }

        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
                    desktop.open(fichier);
                    System.out.println("[OrdonnancePdfServiceArij] ✓ PDF ouvert : "
                        + fichier.getAbsolutePath());
                } else {
                    System.err.println("[OrdonnancePdfServiceArij] Action OPEN non supportée.");
                }
            } else {
                System.err.println("[OrdonnancePdfServiceArij] Desktop non supporté sur ce système.");
            }
        } catch (IOException e) {
            System.err.println("[OrdonnancePdfServiceArij] Impossible d'ouvrir le PDF : "
                + e.getMessage());
        }
    }

    // ================================================================== //
    //  Génération QR Code (ZXing)                                        //
    // ================================================================== //

    /**
     * Génère un QR Code PNG en mémoire (byte[]) via ZXing.
     *
     * @param contenu texte à encoder dans le QR code
     * @param taille  taille en pixels (largeur = hauteur)
     * @return tableau de bytes PNG
     */
    private byte[] genererQrCodePng(String contenu, int taille)
            throws WriterException, IOException {

        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1); // marge minimale

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(contenu, BarcodeFormat.QR_CODE, taille, taille, hints);

        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    // ================================================================== //
    //  Helpers de mise en forme                                           //
    // ================================================================== //

    /** Ligne "Label : Valeur" pour les blocs d'informations. */
    private Paragraph ligneInfo(String label, String valeur,
                                 PdfFont fontBold, PdfFont fontRegular) {
        return new Paragraph()
            .add(new Text(label + " : ").setFont(fontBold).setFontSize(10))
            .add(new Text(valeur).setFont(fontRegular).setFontSize(10))
            .setMarginBottom(3);
    }

    /** Cellule de tableau avec style alterné. */
    private Cell cellTableau(String texte, PdfFont font,
                              DeviceRgb bg, boolean gras) {
        return new Cell()
            .setBackgroundColor(bg)
            .setBorder(new SolidBorder(GRIS_BORDURE, 0.5f))
            .setPadding(7)
            .add(new Paragraph(texte)
                .setFont(font)
                .setFontSize(10)
                .setFontColor(gras ? ColorConstants.BLACK : GRIS_TEXTE));
    }

    /** Ligne de séparation horizontale bleue. */
    private LineSeparator separateur() {
        SolidLine line = new SolidLine(1f);
        line.setColor(BLEU_PRIMAIRE);
        return new LineSeparator(line).setMarginTop(8).setMarginBottom(4);
    }

    /** Retourne "—" si la valeur est null ou vide. */
    private String safe(String val) {
        return (val != null && !val.isBlank()) ? val.trim() : "—";
    }
}
