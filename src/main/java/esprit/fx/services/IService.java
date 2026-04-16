package esprit.fx.services;

import java.sql.SQLException;
import java.util.List;

public interface IService<Objet> {
    void ajouter(Objet objet) throws SQLException;
    void modifier(Objet objet) throws SQLException;
    void supprimer(int id) throws SQLException;
    List<Objet> getAll() throws SQLException;
    Objet afficherParId(int id) throws SQLException;
}