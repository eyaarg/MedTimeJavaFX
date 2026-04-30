package esprit.fx.test;

import esprit.fx.entities.ConsultationsArij;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultationsArijTest {

    @Test
    void defaults_shouldBeEmptyOrZero() {
        ConsultationsArij consultation = new ConsultationsArij();

        assertEquals(0, consultation.getId());
        assertEquals(0, consultation.getPatientId());
        assertEquals(0, consultation.getDoctorId());
        assertNull(consultation.getConsultationDate());
        assertNull(consultation.getType());
        assertNull(consultation.getStatus());
        assertFalse(consultation.isDeleted());
        assertNull(consultation.getCreatedAt());
        assertNull(consultation.getUpdatedAt());
        assertNull(consultation.getRejectionReason());
        assertEquals(0.0, consultation.getConsultationFee());
        assertNull(consultation.getLienMeet());
    }

    @Test
    void gettersAndSetters_shouldStoreValues() {
        ConsultationsArij consultation = new ConsultationsArij();
        LocalDateTime consultationDate = LocalDateTime.of(2026, 4, 15, 10, 30);
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 15, 9, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 15, 11, 0);

        consultation.setId(12);
        consultation.setPatientId(34);
        consultation.setDoctorId(56);
        consultation.setConsultationDate(consultationDate);
        consultation.setType("En ligne");
        consultation.setStatus("CONFIRMEE");
        consultation.setDeleted(true);
        consultation.setCreatedAt(createdAt);
        consultation.setUpdatedAt(updatedAt);
        consultation.setRejectionReason("Reason");
        consultation.setConsultationFee(75.5);
        consultation.setLienMeet("https://meet.example.com/abc");

        assertEquals(12, consultation.getId());
        assertEquals(34, consultation.getPatientId());
        assertEquals(56, consultation.getDoctorId());
        assertEquals(consultationDate, consultation.getConsultationDate());
        assertEquals("En ligne", consultation.getType());
        assertEquals("CONFIRMEE", consultation.getStatus());
        assertTrue(consultation.isDeleted());
        assertEquals(createdAt, consultation.getCreatedAt());
        assertEquals(updatedAt, consultation.getUpdatedAt());
        assertEquals("Reason", consultation.getRejectionReason());
        assertEquals(75.5, consultation.getConsultationFee(), 0.0001);
        assertEquals("https://meet.example.com/abc", consultation.getLienMeet());
    }
}
