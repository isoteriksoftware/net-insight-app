module com.eten.u17cm.netinsightapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.jnetpcap;
    requires java.desktop;
    requires java.base;

    opens com.eten.u17cm.netinsightapp.models to javafx.base;
    opens com.eten.u17cm.netinsightapp to javafx.fxml;
    exports com.eten.u17cm.netinsightapp;
    exports com.eten.u17cm.netinsightapp.controllers;
    opens com.eten.u17cm.netinsightapp.controllers to javafx.fxml;
}