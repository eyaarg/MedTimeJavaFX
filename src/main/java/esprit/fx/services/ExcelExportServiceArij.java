package esprit.fx.services;

import esprit.fx.entities.ConsultationsArij;
import esprit.fx.entities.OrdonnanceArij;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service pour exporter l'historique médical en Excel.
 * Utilise Apache POI pour créer des fichiers .xlsx professionnels.
 */
public class ExcelExportServiceArij {

    private final ServiceConsultationsArij consultationService = new ServiceConsultationsArij();
    private final ServiceOrdonnanceArij ordonnanceService = new ServiceOrdonnanceArij();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Exporte l'historique médical d'un médecin en Excel.
     */
    public boolean exportHistoriqueMedical(int medecinId, String filePath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            
            // Créer les feuilles
            Sheet consultationsSheet = workbook.createSheet("Consultations");
            Sheet ordonnancesSheet = workbook.createSheet("Ordonnances");
            Sheet resumeSheet = workbook.createSheet("Résumé");

            // Remplir les feuilles
            remplirConsultations(consultationsSheet, medecinId);
            remplirOrdonnances(ordonnancesSheet, medecinId);
            remplirResume(resumeSheet, medecinId);

            // Sauvegarder le fichier
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
                System.out.println("[ExcelExportServiceArij] ✅ Fichier Excel créé: " + filePath);
                return true;
            }

        } catch (IOException e) {
            System.err.println("[ExcelExportServiceArij] ❌ Erreur lors de l'export: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Remplit la feuille des consultations.
     */
    private void remplirConsultations(Sheet sheet, int medecinId) {
        // En-têtes
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Patient ID", "Date", "Statut", "Type", "Frais"};
        
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Données
        List<ConsultationsArij> consultations = consultationService.findByDoctor(medecinId);
        int rowNum = 1;

        CellStyle dataStyle = sheet.getWorkbook().createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        for (ConsultationsArij consultation : consultations) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(consultation.getId());
            row.createCell(1).setCellValue(consultation.getPatientId());
            row.createCell(2).setCellValue(consultation.getConsultationDate() != null 
                ? consultation.getConsultationDate().format(dateFormatter) : "");
            row.createCell(3).setCellValue(consultation.getStatus() != null ? consultation.getStatus() : "");
            row.createCell(4).setCellValue(consultation.getType() != null ? consultation.getType() : "");
            row.createCell(5).setCellValue(consultation.getConsultationFee());

            for (int i = 0; i < 6; i++) {
                row.getCell(i).setCellStyle(dataStyle);
            }
        }

        // Ajuster la largeur des colonnes
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Remplit la feuille des ordonnances.
     */
    private void remplirOrdonnances(Sheet sheet, int medecinId) {
        // En-têtes
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Consultation ID", "Numéro", "Date Émission", "Date Validité", "Diagnostic", "Statut"};
        
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Données
        List<OrdonnanceArij> ordonnances = ordonnanceService.findByDoctor(medecinId);
        int rowNum = 1;

        CellStyle dataStyle = sheet.getWorkbook().createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setWrapText(true);

        for (OrdonnanceArij ordonnance : ordonnances) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(ordonnance.getId());
            row.createCell(1).setCellValue(ordonnance.getConsultationId());
            row.createCell(2).setCellValue(ordonnance.getNumeroOrdonnance() != null ? ordonnance.getNumeroOrdonnance() : "");
            row.createCell(3).setCellValue(ordonnance.getDateEmission() != null 
                ? ordonnance.getDateEmission().format(dateFormatter) : "");
            row.createCell(4).setCellValue(ordonnance.getDateValidite() != null 
                ? ordonnance.getDateValidite().format(dateFormatter) : "");
            row.createCell(5).setCellValue(ordonnance.getDiagnosis() != null ? ordonnance.getDiagnosis() : "");
            row.createCell(6).setCellValue("Validée");

            for (int i = 0; i < 7; i++) {
                row.getCell(i).setCellStyle(dataStyle);
            }
        }

        // Ajuster la largeur des colonnes
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Remplit la feuille de résumé.
     */
    private void remplirResume(Sheet sheet, int medecinId) {
        CellStyle titleStyle = sheet.getWorkbook().createCellStyle();
        titleStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font titleFont = sheet.getWorkbook().createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        CellStyle labelStyle = sheet.getWorkbook().createCellStyle();
        labelStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        labelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font labelFont = sheet.getWorkbook().createFont();
        labelFont.setBold(true);
        labelStyle.setFont(labelFont);

        // Titre
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Résumé de l'Historique Médical");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 2));

        // Statistiques
        int rowNum = 2;

        // Total consultations
        Row row1 = sheet.createRow(rowNum++);
        Cell label1 = row1.createCell(0);
        label1.setCellValue("Total Consultations:");
        label1.setCellStyle(labelStyle);
        int totalConsultations = consultationService.countByDoctor(medecinId);
        row1.createCell(1).setCellValue(totalConsultations);

        // Consultations confirmées
        Row row2 = sheet.createRow(rowNum++);
        Cell label2 = row2.createCell(0);
        label2.setCellValue("Consultations Confirmées:");
        label2.setCellStyle(labelStyle);
        int confirmed = consultationService.countByStatus("CONFIRMEE", medecinId);
        row2.createCell(1).setCellValue(confirmed);

        // Consultations en attente
        Row row3 = sheet.createRow(rowNum++);
        Cell label3 = row3.createCell(0);
        label3.setCellValue("Consultations En Attente:");
        label3.setCellStyle(labelStyle);
        int pending = consultationService.countByStatus("EN_ATTENTE", medecinId);
        row3.createCell(1).setCellValue(pending);

        // Ordonnances émises
        Row row4 = sheet.createRow(rowNum++);
        Cell label4 = row4.createCell(0);
        label4.setCellValue("Ordonnances Émises:");
        label4.setCellStyle(labelStyle);
        int ordonnances = ordonnanceService.countOrdonnancesByDoctor(medecinId);
        row4.createCell(1).setCellValue(ordonnances);

        // Taux d'acceptation
        Row row5 = sheet.createRow(rowNum++);
        Cell label5 = row5.createCell(0);
        label5.setCellValue("Taux d'Acceptation:");
        label5.setCellStyle(labelStyle);
        double rate = totalConsultations == 0 ? 0 : (confirmed * 100.0) / totalConsultations;
        row5.createCell(1).setCellValue(String.format("%.1f%%", rate));

        // Ajuster la largeur des colonnes
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }
}
