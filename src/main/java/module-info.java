module MedTimeFX {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires javafx.web;
    requires javafx.media;
    requires java.sql;
    requires java.net.http;
    requires jakarta.mail;
    requires java.desktop;
    requires org.apache.pdfbox;
    requires jbcrypt;
    requires bcrypt;

    // Third-party libraries used by teammates' modules
    requires org.json;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires com.calendarfx.view;
    requires kernel;
    requires layout;
    requires io;
    requires commons;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;

    opens esprit.fx.controllers to javafx.fxml;
    opens esprit.fx.entities to javafx.base;
    opens esprit.fx.services to javafx.base;
    exports esprit.fx;
}
