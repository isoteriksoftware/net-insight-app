package com.eten.u17cm.netinsightapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapException;
import org.jnetpcap.PcapHeader;
import org.jnetpcap.PcapIf;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NetInsightApplication extends Application {
    private long totalBytesReceived = 0;
    private long previousTime = System.currentTimeMillis();
    private static final long MONITOR_INTERVAL = 2000; // 1 second

    @Override
    public void start(Stage stage) throws IOException, PcapException {
//        FXMLLoader fxmlLoader = new FXMLLoader(NetInsightApplication.class.getResource("home.fxml"));
//        Scene scene = new Scene(fxmlLoader.load(), 960, 600);
//        stage.setTitle("Net Insight");
//        stage.setResizable(false);
//        stage.setScene(scene);
        //stage.show();

        startMonitoring();
    }

    public static void main(String[] args) {
        launch();
    }

    private void startMonitoring() throws PcapException {
        List<PcapIf> devices = Pcap.findAllDevs();
        if (devices.isEmpty()) {
            System.out.println("No devices found.");
            return;
        }

        // Select the device named "en0" or your desired device
        PcapIf selectedDevice = devices.stream()
                .filter(device -> "en0".equals(device.name()))
                .findFirst()
                .orElse(null);

        if (selectedDevice == null) {
            System.out.println("Device en0 not found.");
            return;
        }

        // Create a Pcap object to capture packets
        try (Pcap pcap = Pcap.create(selectedDevice)) {
            //pcap.setSnaplen(65536); // Set snapshot length to capture full packets
            pcap.setTimeout(10);  // Set timeout to 1 second
            pcap.activate();
            System.out.println("Capturing packets on: " + selectedDevice.name());

            // Schedule a timer to update bandwidth utilization
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    updateBandwidthUtilization();
                }
            }, MONITOR_INTERVAL, MONITOR_INTERVAL);

            // Start capturing packets
            pcap.loop(-1, (String msg, PcapHeader header, byte[] packet) -> {
                // Update total bytes received
                totalBytesReceived += header.wireLength();
            }, "Hello World");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateBandwidthUtilization() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - previousTime;

        System.out.println("Total bytes received: " + totalBytesReceived);

        // Calculate bytes per second
        double bytesPerSecond = (totalBytesReceived * 1000.0) / elapsedTime;
        double bandwidthUtilization = (bytesPerSecond / 10000000.0) * 100; // Assuming 10 Mbps link

        // Update the label
        System.out.printf("Bandwidth Utilization: %.2f%%%n", bandwidthUtilization);

        // Reset counters
        totalBytesReceived = 0;
        previousTime = currentTime;
    }
}