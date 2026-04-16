package esprit.fx.utils;

import esprit.fx.entities.FactureArij;
import esprit.fx.entities.LigneOrdonnanceArij;
import esprit.fx.entities.OrdonnanceArij;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

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
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                cs.beginText(); 
                cs.newLineAtOffset(50, y); 
                cs.showText("ORDONNANCE"); 
                cs.endText();
                y -= 25;
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                y = line(cs, "Numero: " + (o.getNumeroOrdonnance() != null ? o.getNumeroOrdonnance() : o.getId()), y);
                y = line(cs, "Date: " + (o.getDateEmission() != null ? o.getDateEmission().format(fmt) : ""), y);
                y = line(cs, "Medecin ID: " + o.getDoctorId(), y);
                y = line(cs, "Diagnostic: " + (o.getDiagnosis() != null ? o.getDiagnosis() : ""), y - 5);
                y = line(cs, "Medicaments:", y - 10);
                for (LigneOrdonnanceArij l : lignes)
                    y = line(cs, "- " + l.getNomMedicament() + " | " + l.getDosage() + " | qte: " + l.getQuantite() + " | duree: " + l.getDureeTraitement(), y - 5);
                y = line(cs, "Instructions: " + (o.getInstructions() != null ? o.getInstructions() : ""), y - 10);
            }
            doc.save(outputPath);
        } catch (IOException e) { 
            System.err.println("exportOrdonnance: " + e.getMessage()); 
        }
    }

    public static void exportFacture(FactureArij f, String outputPath) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PDRectangle.A4.getHeight() - 50;
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                cs.beginText(); 
                cs.newLineAtOffset(50, y); 
                cs.showText("FACTURE"); 
                cs.endText();
                y -= 25;
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                y = line(cs, "Numero: " + (f.getNumeroFacture() != null ? f.getNumeroFacture() : f.getId()), y);
                y = line(cs, "Date: " + (f.getDateEmission() != null ? f.getDateEmission().format(fmt) : ""), y);
                line(cs, "Montant: " + f.getMontant() + " TND", y - 5);
            }
            doc.save(outputPath);
        } catch (IOException e) { 
            System.err.println("exportFacture: " + e.getMessage()); 
        }
    }

    private static float line(PDPageContentStream cs, String text, float y) throws IOException {
        cs.beginText(); 
        cs.newLineAtOffset(50, y); 
        cs.showText(text); 
        cs.endText();
        return y - 18;
    }
}
