module com.example.gestion_investissement {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.sql;
    requires jbcrypt;
    // IMPORTANT: Ouvrir les packages aux modules JavaFX
    opens com.bizhub.Investistment.Controllers to javafx.fxml;
    opens com.bizhub.Investistment.Entitites to javafx.base;

    // Exporter les packages
    exports com.bizhub.Investistment;
    exports com.bizhub.Investistment.Controllers;
    exports com.bizhub.Investistment.Entitites;

    opens com.bizhub.user.controller to javafx.fxml;
    exports com.bizhub.user.controller;
    opens com.bizhub.Investistment to javafx.fxml;


}