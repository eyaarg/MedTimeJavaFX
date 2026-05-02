package esprit.fx.test;

import esprit.fx.entities.LigneOrdonnanceArij;
import esprit.fx.entities.OrdonnanceArij;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class OrdonnanceArijTest {

    @Test
    void defaults_shouldInitializeLines() {
        OrdonnanceArij ordonnance = new OrdonnanceArij();

        assertEquals(0, ordonnance.getId());
        assertNotNull(ordonnance.getLignes());
        assertEquals(0, ordonnance.getLignes().size());
    }

    @Test
    void gettersAndSetters_shouldStoreValues() {
        OrdonnanceArij ordonnance = new OrdonnanceArij();
        LocalDateTime dateEmission = LocalDateTime.of(2026, 4, 15, 10, 0);
        LocalDateTime dateValidite = LocalDateTime.of(2026, 4, 22, 10, 0);
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 15, 10, 5);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 15, 10, 10);
        List<LigneOrdonnanceArij> lignes = new ArrayList<>();
        lignes.add(new LigneOrdonnanceArij());

        ordonnance.setId(7);
        ordonnance.setConsultationId(11);
        ordonnance.setDoctorId(13);
        ordonnance.setContent("Content");
        ordonnance.setDiagnosis("Diagnosis");
        ordonnance.setNumeroOrdonnance("ORD-001");
        ordonnance.setDateEmission(dateEmission);
        ordonnance.setDateValidite(dateValidite);
        ordonnance.setSignaturePath("/tmp/sign.png");
        ordonnance.setInstructions("Take after meal");
        ordonnance.setCreatedAt(createdAt);
        ordonnance.setUpdatedAt(updatedAt);
        ordonnance.setTokenVerification("token123");
        ordonnance.setDocumentNom("ordonnance.pdf");
        ordonnance.setDocumentSize(2048);
        ordonnance.setDocumentMimeType("application/pdf");
        ordonnance.setDocumentOriginalName("original.pdf");
        ordonnance.setLignes(lignes);

        assertEquals(7, ordonnance.getId());
        assertEquals(11, ordonnance.getConsultationId());
        assertEquals(13, ordonnance.getDoctorId());
        assertEquals("Content", ordonnance.getContent());
        assertEquals("Diagnosis", ordonnance.getDiagnosis());
        assertEquals("ORD-001", ordonnance.getNumeroOrdonnance());
        assertEquals(dateEmission, ordonnance.getDateEmission());
        assertEquals(dateValidite, ordonnance.getDateValidite());
        assertEquals("/tmp/sign.png", ordonnance.getSignaturePath());
        assertEquals("Take after meal", ordonnance.getInstructions());
        assertEquals(createdAt, ordonnance.getCreatedAt());
        assertEquals(updatedAt, ordonnance.getUpdatedAt());
        assertEquals("token123", ordonnance.getTokenVerification());
        assertEquals("ordonnance.pdf", ordonnance.getDocumentNom());
        assertEquals(2048, ordonnance.getDocumentSize());
        assertEquals("application/pdf", ordonnance.getDocumentMimeType());
        assertEquals("original.pdf", ordonnance.getDocumentOriginalName());
        assertSame(lignes, ordonnance.getLignes());
    }

    @Test
    void setLignes_shouldReplaceNullWithEmptyList() {
        OrdonnanceArij ordonnance = new OrdonnanceArij();

        ordonnance.setLignes(null);

        assertNotNull(ordonnance.getLignes());
        assertEquals(0, ordonnance.getLignes().size());
    }
}
