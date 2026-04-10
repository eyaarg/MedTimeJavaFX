package tn.esprit.utils.consultationonline;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import tn.esprit.entities.consultationonline.FactureArij;
import tn.esprit.entities.consultationonline.LigneOrdonnanceArij;
import tn.esprit.entities.consultationonline.OrdonnanceArij;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PdfExporterArij {

    public static void exportOrdonnance(OrdonnanceArij o, List<LigneOrdonnanceArij> lignes, String outputPath) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PDRectangle.A4.getHeight() - 50;
                cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
                cs.beginText();
                cs.newLineAtOffset(50, y);
                cs.showText("ORDONNANCE");
                cs.endText();
                y -= 25;

                cs.setFont(PDType1Font.HELVETICA, 12);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                y = writeLine(cs, "Numéro: " + (o.getNumeroOrdonnance() != null ? o.getNumeroOrdonnance() : o.getId()), y);
                y = writeLine(cs, "Date: " + (o.getDateEmission() != null ? o.getDateEmission().format(formatter) : ""), y);
                y = writeLine(cs, "Médecin ID: " + o.getDoctorId(), y);
                y = writeLine(cs, "Consultation ID: " + o.getConsultationId(), y);
                y = writeLine(cs, "Diagnostic: " + (o.getDiagnosis() != null ? o.getDiagnosis() : ""), y - 5);

                y = writeLine(cs, "Médicaments:", y - 10);
                for (LigneOrdonnanceArij l : lignes) {
                    y = writeLine(cs, "- " + l.getNomMedicament() + " | " + l.getDosage() + " | qty: " + l.getQuantite() + " | durée: " + l.getDureeTraitement(), y - 5);
                }

                y = writeLine(cs, "Instructions: " + (o.getInstructions() != null ? o.getInstructions() : ""), y - 10);
                y = writeLine(cs, "Token: " + (o.getTokenVerification() != null ? o.getTokenVerification() : ""), y - 10);
            }
            doc.save(outputPath);
        } catch (IOException e) {
            System.err.println("exportOrdonnance error: " + e.getMessage());
        }
    }

    public static void exportFacture(FactureArij f, String outputPath) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PDRectangle.A4.getHeight() - 50;
                cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
                cs.beginText();
                cs.newLineAtOffset(50, y);
                cs.showText("FACTURE");
                cs.endText();
                y -= 25;

                cs.setFont(PDType1Font.HELVETICA, 12);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                y = writeLine(cs, "Numéro: " + (f.getNumeroFacture() != null ? f.getNumeroFacture() : f.getId()), y);
                y = writeLine(cs, "Date: " + (f.getDateEmission() != null ? f.getDateEmission().format(formatter) : ""), y);
                y = writeLine(cs, "Montant: " + f.getMontant(), y - 5);
            }
            doc.save(outputPath);
        } catch (IOException e) {
            System.err.println("exportFacture error: " + e.getMessage());
        }
    }

    private static float writeLine(PDPageContentStream cs, String text, float y) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(50, y);
        cs.showText(text);
        cs.endText();
        return y - 18;
    }
}
