module MedTimeFX {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jakarta.mail;
    requires java.desktop;
    requires org.apache.pdfbox;
    requires jbcrypt;

    opens esprit.fx.controllers to javafx.fxml;
    opens esprit.fx.entities to javafx.base;
    exports esprit.fx;
}