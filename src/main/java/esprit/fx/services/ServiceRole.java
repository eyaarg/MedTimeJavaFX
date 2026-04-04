package esprit.fx.services;

import esprit.fx.entities.Role;
import esprit.fx.utils.MyDB;

import java.sql.*;

public class ServiceRole implements IService<Role> {
    private Connection conn;
    public ServiceRole() {
        conn = MyDB.getInstance().getConnection();
    }
    @Override
    public void ajouter(Role role) throws SQLException {
        String req = "INSERT INTO `role` ( `name`) VALUES ( '"+role.getName() + ")";
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(req);
    }

    @Override
    public void modifier(Role objet) throws SQLException {
        String req = "UPDATE `role` SET `name`=? WHERE `id`=?";
        PreparedStatement preparedStatement = conn.prepareStatement(req);
        preparedStatement.setString(1, objet.getName());
        preparedStatement.setInt(2, objet.getId());
        preparedStatement.executeUpdate();


    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM `role` WHERE `id`=?";
        PreparedStatement preparedStatement = conn.prepareStatement(req);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();

    }

    @Override
    public java.util.List<Role> getAll() throws SQLException {
        String req = "SELECT * FROM `role`";
        Statement statement = conn.createStatement();
        ResultSet resultSet = statement.executeQuery(req);
        java.util.List<Role> roles = new java.util.ArrayList<Role>();
        while (resultSet.next()) {
            Role role = new Role(resultSet.getInt("id"), resultSet.getString("name"));
            roles.add(role);
        }
            return roles;
    }
}
