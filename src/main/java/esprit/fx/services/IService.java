package esprit.fx.services;

import java.sql.SQLException;
import java.util.List;

public interface IService<T> {
    void ajouter(T t) throws SQLException;
    void modifier(T t) throws SQLException;
    void supprimer(int id) throws SQLException;
    List<T> getAll() throws SQLException;
    T afficherParId(int id) throws SQLException;
}
