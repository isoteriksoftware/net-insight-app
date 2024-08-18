module com.eten.u17cm.netinsightapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.jnetpcap;


    opens com.eten.u17cm.netinsightapp to javafx.fxml;
    exports com.eten.u17cm.netinsightapp;
}