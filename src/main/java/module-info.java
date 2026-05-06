module MedTimeFX {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires javafx.web;
    requires java.sql;
    requires java.net.http;
    requires java.desktop;
    requires jakarta.mail;
    requires org.apache.pdfbox;
    requires jbcrypt;
    requires bcrypt;

    // Apache POI - Export Excel (non-modulaire, accès via classpath)
    // requires org.apache.poi.ooxml;

    // JSON
    requires org.json;

    // QR Code
    requires com.google.zxing;
    requires com.google.zxing.javase;

    // Calendar
    requires com.calendarfx.view;

    // iText PDF
    requires kernel;
    requires layout;
    requires io;
    requires commons;

    opens esprit.fx to javafx.fxml;
    opens esprit.fx.controllers to javafx.fxml;
    opens esprit.fx.entities to javafx.base;
    opens esprit.fx.services to javafx.base;
    opens esprit.fx.utils to javafx.base;
    opens esprit.fx.models to javafx.base;
    exports esprit.fx;
}
