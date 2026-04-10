package tn.esprit.services.consultationonline;

import tn.esprit.entities.consultationonline.ConsultationArij;
import tn.esprit.entities.consultationonline.NotificationArij;
import tn.esprit.repositories.consultationonline.ConsultationRepositoryArij;
import tn.esprit.repositories.consultationonline.NotificationRepositoryArij;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConsultationServiceArij {
    private static final int CURRENT_USER_ID = 1;
    private static final String CURRENT_USER_ROLE = "PATIENT";

    private final ConsultationRepositoryArij consultationRepository = new ConsultationRepositoryArij();
    private final NotificationRepositoryArij notificationRepository = new NotificationRepositoryArij();

    public List<ConsultationArij> getMyConsultations() {
        if ("PATIENT".equalsIgnoreCase(CURRENT_USER_ROLE)) {
            return consultationRepository.findByPatientId(CURRENT_USER_ID);
        } else {
            return consultationRepository.findByDoctorId(CURRENT_USER_ID);
        }
    }

    public void createConsultation(ConsultationArij c) {
        c.setStatus("EN_ATTENTE");
        c.setCreatedAt(LocalDateTime.now());
        c.setDeleted(false);
        consultationRepository.create(c);
        notifyDoctor(c.getDoctorId(), c.getId());
    }

    public void updateConsultation(ConsultationArij c) {
        c.setUpdatedAt(LocalDateTime.now());
        consultationRepository.update(c);
    }

    public void deleteConsultation(int id) {
        consultationRepository.softDelete(id);
    }

    public void acceptConsultation(int id) {
        ConsultationArij c = consultationRepository.findById(id);
        if (c == null) return;
        c.setStatus("CONFIRMEE");
        c.setUpdatedAt(LocalDateTime.now());
        consultationRepository.update(c);
        notifyPatient(c.getPatientId(), "CONFIRMATION", c.getId());
    }

    public void rejectConsultation(int id, String reason) {
        ConsultationArij c = consultationRepository.findById(id);
        if (c == null) return;
        c.setStatus("REFUSEE");
        c.setRejectionReason(reason);
        c.setUpdatedAt(LocalDateTime.now());
        consultationRepository.update(c);
        notifyPatient(c.getPatientId(), "REFUS", c.getId());
    }

    public List<ConsultationArij> filterConsultations(String status, String type) {
        List<ConsultationArij> result = new ArrayList<>();
        if (status != null && !status.isEmpty()) {
            result = consultationRepository.filterByStatus(status, CURRENT_USER_ID);
        } else if (type != null && !type.isEmpty()) {
            result = consultationRepository.filterByType(type, CURRENT_USER_ID);
        } else {
            result = getMyConsultations();
        }
        return result;
    }

    private void notifyDoctor(int doctorId, int consultationId) {
        NotificationArij n = new NotificationArij();
        n.setUserId(doctorId);
        n.setTitle("Nouvelle consultation");
        n.setMessage("Vous avez une nouvelle demande de consultation #" + consultationId);
        n.setType("CONSULTATION");
        n.setLink("/consultations/" + consultationId);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.create(n);
    }

    private void notifyPatient(int patientId, String type, int consultationId) {
        NotificationArij n = new NotificationArij();
        n.setUserId(patientId);
        n.setTitle("Mise à jour consultation");
        n.setMessage("Votre consultation #" + consultationId + " est " + type);
        n.setType(type);
        n.setLink("/consultations/" + consultationId);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.create(n);
    }
}
