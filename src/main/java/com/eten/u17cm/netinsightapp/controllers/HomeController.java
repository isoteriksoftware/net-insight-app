package com.eten.u17cm.netinsightapp.controllers;

import com.eten.u17cm.netinsightapp.NetInsightApplication;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class HomeController implements Initializable {
    @FXML
    public Button btnDashboard;

    @FXML
    public StackPane contentView;

    private Node dashboardView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        showDashboard();
    }

    private void showDashboard() {
        if (dashboardView == null) {
            dashboardView = loadView("dashboard.fxml");
        }

        setView(dashboardView);
    }

    private void setView(Node view) {
        if (view != null) {
            contentView.getChildren().clear();
            contentView.getChildren().add(view);
        }
    }

    private Node loadView(String fxmlFileName) {
        try {
            FXMLLoader loader = new FXMLLoader(NetInsightApplication.class.getResource(fxmlFileName));
            return loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
