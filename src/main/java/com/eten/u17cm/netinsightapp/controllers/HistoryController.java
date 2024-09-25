package com.eten.u17cm.netinsightapp.controllers;

import com.eten.u17cm.netinsightapp.models.HistoryRecord;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.LocalDate;
import java.util.Random;
import java.util.ResourceBundle;

public class HistoryController implements Controller {
    @FXML
    private TableView<HistoryRecord> tableView;

    @FXML
    private TableColumn<HistoryRecord, LocalDate> dateColumn;

    @FXML
    private TableColumn<HistoryRecord, Double> bandwidthColumn;

    @FXML
    private TableColumn<HistoryRecord, Double> latencyColumn;

    @FXML
    private TableColumn<HistoryRecord, Double> packetLossColumn;

    private final ObservableList<HistoryRecord> historyData = FXCollections.observableArrayList();

    @Override
    public void dispose() {

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set up the columns with the corresponding property in HistoryRecord class
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        bandwidthColumn.setCellValueFactory(new PropertyValueFactory<>("bandwidth"));
        latencyColumn.setCellValueFactory(new PropertyValueFactory<>("latency"));
        packetLossColumn.setCellValueFactory(new PropertyValueFactory<>("packetLoss"));

        // Customize the table cell factory to format values without decimals
        setCellFactoryForDoubleColumn(bandwidthColumn);
        setCellFactoryForDoubleColumn(latencyColumn);
        setCellFactoryForDoubleColumn(packetLossColumn);

        // Populate the table with random data
        populateTableWithRandomData();
    }

    // Method to format the Double columns without decimals
    private void setCellFactoryForDoubleColumn(TableColumn<HistoryRecord, Double> column) {
        column.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("%.0f", value));  // Formats to no decimal places
                }
            }
        });
    }

    private void populateTableWithRandomData() {
        LocalDate today = LocalDate.now();
        Random random = new Random();

        for (int i = 0; i < 10; i++) { // Generates 10 rows of random data
            LocalDate date = today.minusDays(i);
            double bandwidth = 50 + (150 - 50) * random.nextDouble(); // Random value between 50 and 150 Mbps
            double latency = 10 + (100 - 10) * random.nextDouble();   // Random value between 10 and 100 ms
            double packetLoss = random.nextDouble() * 5;              // Random value between 0 and 5%

            historyData.add(new HistoryRecord(date, bandwidth, latency, packetLoss));
        }

        tableView.setItems(historyData);
    }
}
