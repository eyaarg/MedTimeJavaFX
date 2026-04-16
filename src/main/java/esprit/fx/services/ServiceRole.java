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
        String req = "INSERT INTO `roles` (`name`) VALUES (?)";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setString(1, role.getName());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Role objet) throws SQLException {
        String req = "UPDATE `roles` SET `name`=? WHERE `id`=?";
        PreparedStatement preparedStatement = conn.prepareStatement(req);
        preparedStatement.setString(1, objet.getName());
        preparedStatement.setInt(2, objet.getId());
        preparedStatement.executeUpdate();


    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM `roles` WHERE `id`=?";
        PreparedStatement preparedStatement = conn.prepareStatement(req);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();

    }

    @Override
    public java.util.List<Role> getAll() throws SQLException {
        String req = "SELECT * FROM `roles`";
        Statement statement = conn.createStatement();
        ResultSet resultSet = statement.executeQuery(req);
        java.util.List<Role> roles = new java.util.ArrayList<Role>();
        while (resultSet.next()) {
            Role role = new Role(resultSet.getInt("id"), resultSet.getString("name"));
            roles.add(role);
        }
            return roles;
    }


    @Override
    public Role afficherParId(int id) throws SQLException {
        Role role = null;
        String sql = "SELECT * FROM roles WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            role = new Role(rs.getInt("id"), rs.getString("name"));
        }
        return role;
    }
}
