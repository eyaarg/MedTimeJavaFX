package esprit.fx.test;

import esprit.fx.entities.Doctor;
import esprit.fx.entities.User;
import esprit.fx.services.ServiceUser;

import java.sql.SQLException;
import java.time.LocalDateTime;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws SQLException {
        ServiceUser su = new ServiceUser();
        User u = new User(
                15,
                "test@esprit.tn",
                "eya",
                "test123",
                null,
                true,
                "29110800",
                true,
                null,
                null,
                null,
                null,
                0
        );
        Doctor doctor = new Doctor(
                0,
                "doctor@esprit.tn",
                "drAhmed",
                "pass123",
                null,
                true,
                "29110800",
                true,
                null,
                null,
                null,
                null,
                0,
                0,
                0,
                "LIC-2024-001",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
         //su.modifier(u);
        System.out.println(su.getAll());
    }
}