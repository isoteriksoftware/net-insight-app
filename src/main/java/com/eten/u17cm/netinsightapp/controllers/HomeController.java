package com.eten.u17cm.netinsightapp.controllers;

import com.eten.u17cm.netinsightapp.NetInsightApplication;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class HomeController implements Controller {
    public Button btnMonitoring;
    public Button btnHistory;
    public StackPane contentView;

    private Controller currentController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        showDashboard();
        btnMonitoring.setOnAction(_ -> showDashboard());
        btnHistory.setOnAction(_ -> showHistory());
    }

    @Override
    public void dispose() {
        if (currentController != null) {
            currentController.dispose();
        }
    }

    private void showDashboard() {
        Node dashboardView = loadView("dashboard.fxml");

        setView(dashboardView);
    }

    private void showHistory() {
        Node historyView = loadView("history.fxml");
        setView(historyView);
    }

    private void setView(Node view) {
        if (view != null) {
            contentView.getChildren().clear();
            contentView.getChildren().add(view);
        }
    }

    private Node loadView(String fxmlFileName) {
        // Dispose the current controller in a new thread
        if (currentController != null) {
            new Thread(() -> {
                currentController.dispose();
                currentController = null;
            }).start();
        }

        try {
            FXMLLoader loader = new FXMLLoader(NetInsightApplication.class.getResource(fxmlFileName));
            Node view = loader.load();
            currentController = loader.getController();
            return view;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
