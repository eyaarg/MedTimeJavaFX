package tn.esprit.services.consultationonline;

import tn.esprit.entities.consultationonline.FactureArij;
import tn.esprit.repositories.consultationonline.FactureRepositoryArij;

import java.util.List;

public class FactureServiceArij {
    private static final int CURRENT_USER_ID = 1;

    private final FactureRepositoryArij factureRepository = new FactureRepositoryArij();

    public List<FactureArij> getMyFactures() {
        return factureRepository.findByPatientId(CURRENT_USER_ID);
    }

    public FactureArij getById(int id) {
        return factureRepository.findById(id);
    }
}
