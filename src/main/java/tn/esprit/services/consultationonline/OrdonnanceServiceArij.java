package tn.esprit.services.consultationonline;

import tn.esprit.entities.consultationonline.LigneOrdonnanceArij;
import tn.esprit.entities.consultationonline.NotificationArij;
import tn.esprit.entities.consultationonline.OrdonnanceArij;
import tn.esprit.repositories.consultationonline.LigneOrdonnanceRepositoryArij;
import tn.esprit.repositories.consultationonline.NotificationRepositoryArij;
import tn.esprit.repositories.consultationonline.OrdonnanceRepositoryArij;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class OrdonnanceServiceArij {
    private static final int CURRENT_USER_ID = 1;

    private final OrdonnanceRepositoryArij ordonnanceRepository = new OrdonnanceRepositoryArij();
    private final LigneOrdonnanceRepositoryArij ligneOrdonnanceRepository = new LigneOrdonnanceRepositoryArij();
    private final NotificationRepositoryArij notificationRepository = new NotificationRepositoryArij();

    public OrdonnanceArij getByConsultationId(int consultationId) {
        return ordonnanceRepository.findByConsultationId(consultationId);
    }

    public void createOrdonnance(OrdonnanceArij o, List<LigneOrdonnanceArij> lignes) {
        LocalDateTime now = LocalDateTime.now();
        o.setNumeroOrdonnance("ORD-" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + randomChars(5));
        o.setTokenVerification(randomHex(16));
        o.setDateEmission(now);
        o.setCreatedAt(now);
        ordonnanceRepository.create(o);
        OrdonnanceArij saved = ordonnanceRepository.findByConsultationId(o.getConsultationId());
        int ordonnanceId = saved != null ? saved.getId() : o.getId();
        if (lignes != null) {
            for (LigneOrdonnanceArij l : lignes) {
                l.setOrdonnanceId(ordonnanceId);
                ligneOrdonnanceRepository.create(l);
            }
        }
        notifyPatient(o.getConsultationId(), ordonnanceId);
    }

    public void updateOrdonnance(OrdonnanceArij o, List<LigneOrdonnanceArij> lignes) {
        ligneOrdonnanceRepository.deleteByOrdonnanceId(o.getId());
        ordonnanceRepository.update(o);
        if (lignes != null) {
            for (LigneOrdonnanceArij l : lignes) {
                l.setOrdonnanceId(o.getId());
                ligneOrdonnanceRepository.create(l);
            }
        }
    }

    public void deleteOrdonnance(int id) {
        ligneOrdonnanceRepository.deleteByOrdonnanceId(id);
        ordonnanceRepository.delete(id);
    }

    private void notifyPatient(int consultationId, int ordonnanceId) {
        NotificationArij n = new NotificationArij();
        n.setUserId(CURRENT_USER_ID);
        n.setTitle("Nouvelle ordonnance");
        n.setMessage("Une ordonnance #" + ordonnanceId + " a été ajoutée pour votre consultation #" + consultationId);
        n.setType("ORDONNANCE");
        n.setLink("/ordonnances/" + ordonnanceId);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.create(n);
    }

    private String randomChars(int length) {
        String s = UUID.randomUUID().toString().replace("-", "");
        return s.substring(0, Math.min(length, s.length())).toUpperCase();
    }

    private String randomHex(int length) {
        String s = UUID.randomUUID().toString().replace("-", "");
        if (s.length() < length) {
            s = s + UUID.randomUUID().toString().replace("-", "");
        }
        return s.substring(0, length).toLowerCase();
    }
}
