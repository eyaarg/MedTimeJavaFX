package esprit.fx.services;

import com.itextpdf.io.font.constants.StandardFonts;
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
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import esprit.fx.entities.ConsultationsArij;
import esprit.fx.utils.MyDB;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ============================================================
 *  FacturePdfServiceArij — Génération de facture PDF iText 7
 * ============================================================
 *
 *  Génère une facture professionnelle pour une consultation payée.
 *
 *  Structure du document :
 *  ┌─────────────────────────────────────────────────────────┐
 *  │  EN-TÊTE  : logo MedTime + titre FACTURE               │
 *  │  NUMÉRO   : FAC-00042  |  Date du paiement             │
 *  │  ÉMETTEUR : Cabinet MedTime (coordonnées)              │
 *  │  CLIENT   : Nom patient + coordonnées                  │
 *  │  MÉDECIN  : Dr {nom}, spécialité                       │
 *  │  TABLEAU  : Description | Qté | Prix unitaire | Total  │
 *  │  TOTAUX   : Sous-total | TVA 0% | TOTAL TND            │
 *  │  PAIEMENT : "Payé via paiement sécurisé Stripe"        │
 *  │  PIED     : Mention légale + date génération           │
 *  └─────────────────────────────────────────────────────────┘
 *
 *  Après génération : ouvre le PDF avec Desktop.open()
 *
 *  Usage :
 *  ────────
 *    FacturePdfServiceArij service = new FacturePdfServiceArij();
 *    service.genererFacture(consultation, "/chemin/facture.pdf");
 */
public class FacturePdfServiceArij {

    // ------------------------------------------------------------------ //
    //  Palette de couleurs MedTime                                        //
    // ------------------------------------------------------------------ //
    private static final DeviceRgb BLEU_PRIMAIRE = new DeviceRgb(29,  78, 216);  // #1d4ed8
    private static final DeviceRgb BLEU_CLAIR    = new DeviceRgb(239, 246, 255); // #eff6ff
    private static final DeviceRgb VERT_PAYE     = new DeviceRgb(22,  163,  74); // #16a34a
    private static final DeviceRgb VERT_CLAIR    = new DeviceRgb(240, 253, 244); // #f0fdf4
    private static final DeviceRgb GRIS_TEXTE    = new DeviceRgb(71,  85,  105); // #475569
    private static final DeviceRgb GRIS_BORDURE  = new DeviceRgb(226, 232, 240); // #e2e8f0
    private static final DeviceRgb GRIS_CLAIR    = new DeviceRgb(248, 250, 252); // #f8fafc
    private static final DeviceRgb BLANC         = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb NOIR          = new DeviceRgb(15,  23,  42);  // #0f172a

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    // ================================================================== //
    //  Méthode principale                                                 //
    // ================================================================== //

    /**
     * Génère la facture PDF d'une consultation et l'ouvre avec le lecteur système.
     *
     * Les informations patient et médecin sont chargées depuis la BDD
     * via l'id de la consultation (table partagée avec Symfony).
     *
     * @param consultation  entité consultation (id, fee, date, patientId, doctorId)
     * @param cheminFichier chemin absolu du fichier PDF à créer
     *                      (ex: "C:/factures/FAC-00042.pdf")
     */
    public void genererFacture(ConsultationsArij consultation, String cheminFichier) {
        if (consultation == null) {
            System.err.println("[FacturePdfServiceArij] consultation null.");
            return;
        }

        File fichier = new File(cheminFichier);
        try {
            if (fichier.getParentFile() != null) fichier.getParentFile().mkdirs();

            // ── Charger les infos depuis la BDD ───────────────────────
            InfosFacture infos = chargerInfos(consultation);

            // ── Initialisation iText 7 ────────────────────────────────
            PdfWriter   writer = new PdfWriter(cheminFichier);
            PdfDocument pdf    = new PdfDocument(writer);
            Document    doc    = new Document(pdf, PageSize.A4);
            doc.setMargins(40, 50, 60, 50);

            PdfFont fontBold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont fontItalic  = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            // ── Sections ──────────────────────────────────────────────
            ajouterEntete(doc, consultation, infos, fontBold, fontRegular);
            doc.add(espaceur(8));

            ajouterInfosEmetteurClient(doc, consultation, infos, fontBold, fontRegular);
            doc.add(espaceur(10));

            ajouterTableauPrestations(doc, consultation, infos, fontBold, fontRegular);
            doc.add(espaceur(8));

            ajouterTotaux(doc, consultation, fontBold, fontRegular, fontItalic);
            doc.add(espaceur(10));

            ajouterMentionPaiement(doc, fontBold, fontRegular, fontItalic);
            doc.add(espaceur(10));

            ajouterPiedDePage(doc, consultation, fontRegular, fontItalic);

            doc.close();

            System.out.println("[FacturePdfServiceArij] ✓ Facture générée : " + cheminFichier);
            ouvrirPdf(fichier);

        } catch (IOException e) {
            System.err.println("[FacturePdfServiceArij] ✗ Erreur génération : " + e.getMessage());
        }
    }

    // ================================================================== //
    //  Sections du document                                               //
    // ================================================================== //

    /**
     * EN-TÊTE : bandeau bleu + numéro facture + date paiement.
     */
    private void ajouterEntete(Document doc, ConsultationsArij c,
                                InfosFacture infos,
                                PdfFont fontBold, PdfFont fontRegular) throws IOException {

        // Numéro facture : FAC-{id padded 5 chiffres}
        String numeroFacture = "FAC-" + String.format("%05d", c.getId());

        Table header = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
            .setWidth(UnitValue.createPercentValue(100))
            .setBackgroundColor(BLEU_PRIMAIRE)
            .setBorder(Border.NO_BORDER);

        // Gauche : nom plateforme
        Cell cellG = new Cell().setBorder(Border.NO_BORDER).setPadding(16);
        cellG.add(new Paragraph("MedTime")
            .setFont(fontBold).setFontSize(22).setFontColor(BLANC));
        cellG.add(new Paragraph("Plateforme de consultation médicale en ligne")
            .setFont(fontRegular).setFontSize(10)
            .setFontColor(new DeviceRgb(191, 219, 254)));
        header.addCell(cellG);

        // Droite : FACTURE + numéro + date
        Cell cellD = new Cell().setBorder(Border.NO_BORDER).setPadding(16)
            .setTextAlignment(TextAlignment.RIGHT);
        cellD.add(new Paragraph("FACTURE")
            .setFont(fontBold).setFontSize(20).setFontColor(BLANC));
        cellD.add(new Paragraph(numeroFacture)
            .setFont(fontBold).setFontSize(13)
            .setFontColor(new DeviceRgb(191, 219, 254)));
        cellD.add(new Paragraph("Date : " + (infos.datePaiement != null
                ? infos.datePaiement.format(DATE_FMT)
                : LocalDateTime.now().format(DATE_FMT)))
            .setFont(fontRegular).setFontSize(10)
            .setFontColor(new DeviceRgb(191, 219, 254)));
        header.addCell(cellD);

        doc.add(header);
    }

    /**
     * ÉMETTEUR + CLIENT + MÉDECIN en deux colonnes.
     */
    private void ajouterInfosEmetteurClient(Document doc, ConsultationsArij c,
                                             InfosFacture infos,
                                             PdfFont fontBold, PdfFont fontRegular) throws IOException {

        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
            .setWidth(UnitValue.createPercentValue(100))
            .setBorder(Border.NO_BORDER);

        // ── Colonne ÉMETTEUR (Cabinet MedTime) ────────────────────────
        Cell cellEmetteur = new Cell()
            .setBorder(new SolidBorder(GRIS_BORDURE, 1))
            .setBackgroundColor(GRIS_CLAIR)
            .setPadding(12)
            .setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(8));

        cellEmetteur.add(new Paragraph("ÉMETTEUR")
            .setFont(fontBold).setFontSize(10).setFontColor(GRIS_TEXTE)
            .setMarginBottom(6));
        cellEmetteur.add(new Paragraph("Cabinet MedTime")
            .setFont(fontBold).setFontSize(12).setFontColor(NOIR));
        cellEmetteur.add(new Paragraph("Plateforme médicale en ligne")
            .setFont(fontRegular).setFontSize(10).setFontColor(GRIS_TEXTE));
        cellEmetteur.add(new Paragraph("contact@medtime.app")
            .setFont(fontRegular).setFontSize(10).setFontColor(GRIS_TEXTE));
        cellEmetteur.add(new Paragraph("www.medtime.app")
            .setFont(fontRegular).setFontSize(10).setFontColor(BLEU_PRIMAIRE));

        // Médecin dans le bloc émetteur
        if (infos.medecinNom != null && !infos.medecinNom.isBlank()) {
            cellEmetteur.add(espaceurParagraphe(6));
            cellEmetteur.add(new Paragraph("Médecin : Dr. " + infos.medecinNom)
                .setFont(fontBold).setFontSize(10).setFontColor(NOIR));
            if (infos.medecinSpecialite != null && !infos.medecinSpecialite.isBlank()) {
                cellEmetteur.add(new Paragraph("Spécialité : " + infos.medecinSpecialite)
                    .setFont(fontRegular).setFontSize(10).setFontColor(GRIS_TEXTE));
            }
        }

        // ── Colonne CLIENT (Patient) ───────────────────────────────────
        Cell cellClient = new Cell()
            .setBorder(new SolidBorder(GRIS_BORDURE, 1))
            .setBackgroundColor(BLEU_CLAIR)
            .setPadding(12)
            .setMarginLeft(8)
            .setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(8));

        cellClient.add(new Paragraph("FACTURÉ À")
            .setFont(fontBold).setFontSize(10).setFontColor(BLEU_PRIMAIRE)
            .setMarginBottom(6));
        cellClient.add(new Paragraph(safe(infos.patientNom))
            .setFont(fontBold).setFontSize(12).setFontColor(NOIR));
        if (infos.patientEmail != null && !infos.patientEmail.isBlank()) {
            cellClient.add(new Paragraph(infos.patientEmail)
                .setFont(fontRegular).setFontSize(10).setFontColor(GRIS_TEXTE));
        }
        if (infos.patientTelephone != null && !infos.patientTelephone.isBlank()) {
            cellClient.add(new Paragraph("Tél : " + infos.patientTelephone)
                .setFont(fontRegular).setFontSize(10).setFontColor(GRIS_TEXTE));
        }
        cellClient.add(espaceurParagraphe(6));
        cellClient.add(new Paragraph("Consultation #" + c.getId())
            .setFont(fontRegular).setFontSize(10).setFontColor(GRIS_TEXTE));
        if (c.getConsultationDate() != null) {
            cellClient.add(new Paragraph("Date : " + c.getConsultationDate().format(DATE_FMT))
                .setFont(fontRegular).setFontSize(10).setFontColor(GRIS_TEXTE));
        }

        table.addCell(cellEmetteur);
        table.addCell(cellClient);
        doc.add(table);
    }

    /**
     * TABLEAU DES PRESTATIONS : Description | Qté | Prix unitaire | Total.
     */
    private void ajouterTableauPrestations(Document doc, ConsultationsArij c,
                                            InfosFacture infos,
                                            PdfFont fontBold, PdfFont fontRegular) throws IOException {

        doc.add(new Paragraph("Détail de la prestation")
            .setFont(fontBold).setFontSize(12).setFontColor(BLEU_PRIMAIRE)
            .setMarginBottom(8));

        Table table = new Table(UnitValue.createPercentArray(new float[]{55, 10, 17, 18}))
            .setWidth(UnitValue.createPercentValue(100))
            .setBorder(Border.NO_BORDER);

        // En-têtes
        String[] headers = {"Description", "Qté", "Prix unitaire", "Total"};
        for (String h : headers) {
            table.addHeaderCell(
                new Cell()
                    .setBackgroundColor(BLEU_PRIMAIRE)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(9)
                    .add(new Paragraph(h)
                        .setFont(fontBold).setFontSize(10).setFontColor(BLANC)
                        .setTextAlignment(TextAlignment.CENTER))
            );
        }

        // Ligne de prestation
        String description = "Consultation médicale en ligne";
        if (infos.medecinNom != null && !infos.medecinNom.isBlank()) {
            description += " — Dr. " + infos.medecinNom;
        }
        String montantStr = String.format("%.2f TND", c.getConsultationFee());

        table.addCell(cellPrestation(description, fontRegular, BLANC, false));
        table.addCell(cellPrestation("1", fontRegular, BLANC, false)
            .setTextAlignment(TextAlignment.CENTER));
        table.addCell(cellPrestation(montantStr, fontRegular, BLANC, false)
            .setTextAlignment(TextAlignment.RIGHT));
        table.addCell(cellPrestation(montantStr, fontBold, BLANC, true)
            .setTextAlignment(TextAlignment.RIGHT));

        doc.add(table);
    }

    /**
     * TOTAUX : Sous-total | TVA 0% | TOTAL.
     */
    private void ajouterTotaux(Document doc, ConsultationsArij c,
                                PdfFont fontBold, PdfFont fontRegular,
                                PdfFont fontItalic) throws IOException {

        String montantStr = String.format("%.2f TND", c.getConsultationFee());

        // Tableau aligné à droite (50% vide + 50% totaux)
        Table table = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
            .setWidth(UnitValue.createPercentValue(100))
            .setBorder(Border.NO_BORDER);

        // Colonne gauche vide
        table.addCell(new Cell().setBorder(Border.NO_BORDER));

        // Colonne droite : totaux
        Cell cellTotaux = new Cell()
            .setBorder(new SolidBorder(GRIS_BORDURE, 1))
            .setBackgroundColor(GRIS_CLAIR)
            .setPadding(12)
            .setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(8));

        // Sous-total
        cellTotaux.add(ligneTotal("Sous-total", montantStr, fontRegular, GRIS_TEXTE));

        // TVA 0% — service médical
        cellTotaux.add(ligneTotal("TVA (0% — service médical)", "0.00 TND",
            fontItalic, GRIS_TEXTE));

        // Séparateur
        cellTotaux.add(new Paragraph("─────────────────────────")
            .setFont(fontRegular).setFontSize(8).setFontColor(GRIS_BORDURE)
            .setMarginTop(4).setMarginBottom(4));

        // TOTAL en vert
        Paragraph totalPara = new Paragraph()
            .add(new Text("TOTAL  ").setFont(fontBold).setFontSize(13).setFontColor(VERT_PAYE))
            .add(new Text(montantStr).setFont(fontBold).setFontSize(14).setFontColor(VERT_PAYE))
            .setTextAlignment(TextAlignment.RIGHT);
        cellTotaux.add(totalPara);

        table.addCell(cellTotaux);
        doc.add(table);
    }

    /**
     * MENTION PAIEMENT : badge vert "Payé via Stripe".
     */
    private void ajouterMentionPaiement(Document doc,
                                         PdfFont fontBold, PdfFont fontRegular,
                                         PdfFont fontItalic) throws IOException {

        Table box = new Table(UnitValue.createPercentArray(new float[]{100}))
            .setWidth(UnitValue.createPercentValue(100))
            .setBorder(new SolidBorder(new DeviceRgb(187, 247, 208), 1.5f)) // #bbf7d0
            .setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(8));

        Cell cell = new Cell()
            .setBorder(Border.NO_BORDER)
            .setBackgroundColor(VERT_CLAIR)
            .setPadding(12);

        cell.add(new Paragraph()
            .add(new Text("✅  ").setFont(fontBold).setFontSize(13))
            .add(new Text("Payé via paiement sécurisé Stripe")
                .setFont(fontBold).setFontSize(12).setFontColor(VERT_PAYE)));

        cell.add(new Paragraph(
            "Ce paiement a été traité de manière sécurisée par Stripe. " +
            "Aucune donnée bancaire n'est stockée sur nos serveurs.")
            .setFont(fontItalic).setFontSize(9).setFontColor(GRIS_TEXTE)
            .setMarginTop(4));

        box.addCell(cell);
        doc.add(box);
    }

    /**
     * PIED DE PAGE : mention légale + date de génération.
     */
    private void ajouterPiedDePage(Document doc, ConsultationsArij c,
                                    PdfFont fontRegular, PdfFont fontItalic) throws IOException {

        SolidLine line = new SolidLine(0.5f);
        line.setColor(GRIS_BORDURE);
        doc.add(new LineSeparator(line).setMarginTop(10).setMarginBottom(6));

        Table footer = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
            .setWidth(UnitValue.createPercentValue(100))
            .setBorder(Border.NO_BORDER);

        Cell cellG = new Cell().setBorder(Border.NO_BORDER);
        cellG.add(new Paragraph("N° Consultation : #" + c.getId())
            .setFont(fontRegular).setFontSize(8).setFontColor(GRIS_TEXTE));
        cellG.add(new Paragraph("MedTime — Plateforme de consultation médicale en ligne")
            .setFont(fontItalic).setFontSize(8).setFontColor(GRIS_TEXTE));

        Cell cellD = new Cell().setBorder(Border.NO_BORDER)
            .setTextAlignment(TextAlignment.RIGHT);
        cellD.add(new Paragraph("Généré le " + LocalDateTime.now().format(DATETIME_FMT))
            .setFont(fontItalic).setFontSize(8).setFontColor(GRIS_TEXTE));
        cellD.add(new Paragraph("Document généré par MedTimeFX")
            .setFont(fontItalic).setFontSize(8).setFontColor(GRIS_TEXTE));

        footer.addCell(cellG);
        footer.addCell(cellD);
        doc.add(footer);
    }

    // ================================================================== //
    //  Chargement des données depuis la BDD                              //
    // ================================================================== //

    /**
     * Charge les informations patient et médecin depuis la BDD partagée.
     *
     * Interroge directement la table consultations + patients + doctors + users
     * pour récupérer les données nécessaires à la facture.
     *
     * La date de paiement est lue depuis la table paiement (mise à jour
     * par le webhook Stripe via Symfony).
     */
    private InfosFacture chargerInfos(ConsultationsArij c) {
        InfosFacture infos = new InfosFacture();

        String sql = """
            SELECT
                u_p.username        AS patient_nom,
                u_p.email           AS patient_email,
                u_p.phone_number    AS patient_tel,
                u_d.username        AS medecin_nom,
                d.specialite        AS medecin_specialite,
                p.created_at        AS date_paiement
            FROM consultations con
            JOIN patients  pat ON pat.id  = con.patient_id
            JOIN users     u_p ON u_p.id  = pat.user_id
            JOIN doctors   d   ON d.id    = con.doctor_id
            JOIN users     u_d ON u_d.id  = d.user_id
            LEFT JOIN paiement p ON p.consultation_id = con.id
            WHERE con.id = ?
            LIMIT 1
            """;

        try (PreparedStatement ps =
                 MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, c.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                infos.patientNom       = rs.getString("patient_nom");
                infos.patientEmail     = rs.getString("patient_email");
                infos.patientTelephone = rs.getString("patient_tel");
                infos.medecinNom       = rs.getString("medecin_nom");
                infos.medecinSpecialite = rs.getString("medecin_specialite");
                java.sql.Timestamp dp  = rs.getTimestamp("date_paiement");
                infos.datePaiement     = dp != null ? dp.toLocalDateTime() : null;
            }
        } catch (SQLException e) {
            System.err.println("[FacturePdfServiceArij] chargerInfos: " + e.getMessage());
        }

        return infos;
    }

    /**
     * Vérifie en BDD si une consultation est au statut "payee".
     *
     * Interroge directement la table consultations partagée avec Symfony.
     * Le webhook Stripe (côté Symfony) met à jour ce statut après paiement.
     *
     * @param consultationId id de la consultation
     * @return true si status = "payee" (insensible à la casse)
     */
    public static boolean estPayee(int consultationId) {
        String sql = "SELECT status FROM consultations WHERE id = ? AND is_deleted = 0";
        try (PreparedStatement ps =
                 MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, consultationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String status = rs.getString("status");
                return "payee".equalsIgnoreCase(status)
                    || "paid".equalsIgnoreCase(status)
                    || "PAYEE".equals(status);
            }
        } catch (SQLException e) {
            System.err.println("[FacturePdfServiceArij] estPayee: " + e.getMessage());
        }
        return false;
    }

    // ================================================================== //
    //  Ouverture du PDF                                                   //
    // ================================================================== //

    private void ouvrirPdf(File fichier) {
        if (!fichier.exists()) {
            System.err.println("[FacturePdfServiceArij] Fichier introuvable : "
                + fichier.getAbsolutePath());
            return;
        }
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(fichier);
                System.out.println("[FacturePdfServiceArij] ✓ PDF ouvert.");
            }
        } catch (IOException e) {
            System.err.println("[FacturePdfServiceArij] Impossible d'ouvrir le PDF : "
                + e.getMessage());
        }
    }

    // ================================================================== //
    //  Helpers de mise en forme                                           //
    // ================================================================== //

    private Cell cellPrestation(String texte, PdfFont font,
                                 DeviceRgb bg, boolean gras) {
        return new Cell()
            .setBackgroundColor(bg)
            .setBorder(new SolidBorder(GRIS_BORDURE, 0.5f))
            .setPadding(9)
            .add(new Paragraph(texte)
                .setFont(font).setFontSize(10)
                .setFontColor(gras ? VERT_PAYE : GRIS_TEXTE));
    }

    private Paragraph ligneTotal(String label, String valeur,
                                  PdfFont font, DeviceRgb couleur) {
        return new Paragraph()
            .add(new Text(label + "  ").setFont(font).setFontSize(10).setFontColor(couleur))
            .add(new Text(valeur).setFont(font).setFontSize(10).setFontColor(couleur))
            .setTextAlignment(TextAlignment.RIGHT)
            .setMarginBottom(3);
    }

    private Paragraph espaceurParagraphe(float height) {
        return new Paragraph("").setFontSize(height).setMarginBottom(0);
    }

    private Paragraph espaceur(float height) {
        return new Paragraph("\n").setFontSize(height);
    }

    private String safe(String val) {
        return (val != null && !val.isBlank()) ? val.trim() : "—";
    }

    // ================================================================== //
    //  DTO interne                                                        //
    // ================================================================== //

    /** Données chargées depuis la BDD pour construire la facture. */
    private static class InfosFacture {
        String        patientNom;
        String        patientEmail;
        String        patientTelephone;
        String        medecinNom;
        String        medecinSpecialite;
        LocalDateTime datePaiement;
    }
}
