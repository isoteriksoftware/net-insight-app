package com.eten.u17cm.netinsightapp;

import com.eten.u17cm.netinsightapp.controllers.Controller;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.jnetpcap.PcapException;
import java.io.IOException;

public class NetInsightApplication extends Application {
    private Controller homeController;

    @Override
    public void start(Stage stage) throws IOException, PcapException {
        FXMLLoader fxmlLoader = new FXMLLoader(NetInsightApplication.class.getResource("home.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 960, 600);
        stage.setTitle("Net Insight");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        homeController = fxmlLoader.getController();
    }

    @Override
    public void stop() throws Exception {
        if (homeController != null) {
            homeController.dispose();
        }

        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }

    public static void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void showInfo(String message) {
        showAlert(Alert.AlertType.INFORMATION, "Information", message);
    }

    public static void showError(String message) {
        showAlert(Alert.AlertType.ERROR, "Error", message);
    }

    public static void showWarning(String message) {
        showAlert(Alert.AlertType.WARNING, "Warning", message);
    }
}