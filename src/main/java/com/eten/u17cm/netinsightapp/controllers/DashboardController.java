package com.eten.u17cm.netinsightapp.controllers;

import com.eten.u17cm.netinsightapp.NetInsightApplication;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapException;
import org.jnetpcap.PcapHeader;
import org.jnetpcap.PcapIf;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class DashboardController implements Controller {
    private static final long MONITOR_INTERVAL = 1000; // 1 second
    private static final String TARGET_DEVICE = "en0"; // The device to monitor
    private static final long DEVICE_LINK_SPEED_BYTES_PER_SECOND = 108_250_000; // 866 Mbps

    public Label currentBandwidth;

    private volatile boolean running = true; // Flag to stop the background thread
    private Thread monitoringThread; // The monitoring thread
    private Pcap pcap; // Pcap instance to be closed properly
    private long totalBytesReceived = 0;
    private long previousTime = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        previousTime = System.currentTimeMillis();

        // Start monitoring in a new thread
        monitoringThread = new Thread(() -> {
            try {
                startMonitoring();
            } catch (PcapException e) {
                NetInsightApplication.showError("Failed to start monitoring: " + e.getMessage());
                e.printStackTrace();
            }
        });

        monitoringThread.setDaemon(true); // Set the thread as daemon
        monitoringThread.start();
    }

    private void startMonitoring() throws PcapException {
        // Find all network devices
        List<PcapIf> devices = Pcap.findAllDevs();
        if (devices.isEmpty()) {
            NetInsightApplication.showError("No network devices found.");
            return;
        }

        // Select the target device
        PcapIf selectedDevice = devices.stream()
                .filter(device -> TARGET_DEVICE.equals(device.name()))
                .findFirst()
                .orElse(null);

        if (selectedDevice == null) {
            NetInsightApplication.showError("Device \"" + TARGET_DEVICE + "\" not found.");
            return;
        }

        // Create a Pcap object to capture packets
        try {
            pcap = Pcap.create(selectedDevice);
            pcap.setTimeout(1000);  // Set timeout to 1 second
            pcap.activate();

            // Schedule a timer to update bandwidth utilization
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (!running) {
                        timer.cancel(); // Stop the timer when the app is closing
                        return;
                    }
                    updateBandwidthUtilization();
                }
            }, MONITOR_INTERVAL, MONITOR_INTERVAL);

            // Start capturing packets in a loop
            pcap.loop(-1, (String _, PcapHeader header, byte[] packet) -> {
                if (!running) {
                    pcap.breakloop(); // Break the loop when the flag is false
                    return;
                }

                // Update total bytes received
                totalBytesReceived += header.wireLength();
            }, "NetInsight");

        } catch (Exception e) {
            NetInsightApplication.showError("Failed to start monitoring: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateBandwidthUtilization() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - previousTime;

        // Calculate bytes per second
        double bytesPerSecond = (totalBytesReceived * 1000.0) / elapsedTime;

        // Convert bytes per second to bits per second
        double bitsPerSecond = bytesPerSecond * 8;

        // Convert bits per second to megabits per second
        double megabitsPerSecond = bitsPerSecond / 1_000_000;

        // Update the label
        Platform.runLater(() -> currentBandwidth.setText(String.format("Current: %.2f Mbps", megabitsPerSecond)));

        // Reset counters
        totalBytesReceived = 0;
        previousTime = currentTime;
    }

    @Override
    public void dispose() {
        running = false; // Signal the background thread to stop
        if (pcap != null) {
            pcap.breakloop(); // Break the packet capture loop
            pcap.close(); // Properly close the Pcap instance to release resources
        }
        if (monitoringThread != null && monitoringThread.isAlive()) {
            try {
                monitoringThread.join(); // Wait for the thread to finish
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
