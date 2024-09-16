package com.eten.u17cm.netinsightapp.controllers;

import com.eten.u17cm.netinsightapp.NetInsightApplication;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapException;
import org.jnetpcap.PcapHeader;
import org.jnetpcap.PcapIf;

import java.net.InetAddress;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class DashboardController implements Controller {
    private static final long MONITOR_INTERVAL = 1000; // 1 second
    private static final String TARGET_DEVICE = "en0"; // The device to monitor
    private static final String PING_TARGET = "8.8.8.8"; // Target for latency (Google DNS)

    public Label currentBandwidth;
    public Label averageBandwidth;
    public Label currentLatency;
    public Label averageLatency;

    private volatile boolean running = true; // Flag to stop the background thread
    private Thread monitoringThread; // The monitoring thread
    private Pcap pcap; // Pcap instance to be closed properly
    private long totalBytesReceived = 0;
    private long totalBytesReceivedAllTime = 0; // Total bytes received over the entire monitoring period
    private long previousTime = 0;
    private long startTime; // Start time of monitoring
    private double averageBandwidthMbps = 0; // Average bandwidth in Mbps

    private long totalLatencyTime = 0; // Total latency time for average calculation
    private int latencyCount = 0; // Count of latency measurements
    private double averageLatencyMs = 0; // Average latency in milliseconds

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        previousTime = System.currentTimeMillis();
        startTime = previousTime; // Initialize start time

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

            // Schedule a timer to update bandwidth and latency
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (!running) {
                        timer.cancel(); // Stop the timer when the app is closing
                        return;
                    }
                    updateBandwidthUtilization();
                    measureLatency(); // Measure latency every interval
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
        double bitsPerSecond = bytesPerSecond * 8;
        double megabitsPerSecond = bitsPerSecond / 1_000_000;

        // Update the current bandwidth label
        Platform.runLater(() -> currentBandwidth.setText(String.format("Current: %.2f Mbps", megabitsPerSecond)));

        // Update total bytes received all time and calculate average bandwidth
        totalBytesReceivedAllTime += totalBytesReceived;
        long totalElapsedTime = currentTime - startTime; // Total elapsed time in milliseconds
        double averageBytesPerSecond = (totalBytesReceivedAllTime * 1000.0) / totalElapsedTime;
        double averageBitsPerSecond = averageBytesPerSecond * 8;
        averageBandwidthMbps = averageBitsPerSecond / 1_000_000;

        // Update the average bandwidth label
        Platform.runLater(() -> averageBandwidth.setText(String.format("Average: %.2f Mbps", averageBandwidthMbps)));

        // Reset counters
        totalBytesReceived = 0;
        previousTime = currentTime;
    }

    private void measureLatency() {
        try {
            // Use ICMP ping to measure latency to the target (Google DNS in this case)
            InetAddress target = InetAddress.getByName(PING_TARGET);
            long start = System.currentTimeMillis();

            // Ping the target (timeout set to 1000ms)
            boolean reachable = target.isReachable(1000);
            System.out.println("Ping " + PING_TARGET + " reachable: " + reachable);
            long end = System.currentTimeMillis();

            if (reachable) {
                long rtt = end - start; // Round Trip Time (RTT) in milliseconds
                totalLatencyTime += rtt;
                latencyCount++;

                // Calculate average latency
                averageLatencyMs = totalLatencyTime / (double) latencyCount;

                // Update the current latency label
                Platform.runLater(() -> currentLatency.setText(String.format("Latency: %d ms", rtt)));

                // Update the average latency label
                Platform.runLater(() -> averageLatency.setText(String.format("Avg Latency: %.2f ms", averageLatencyMs)));
            }
        } catch (Exception e) {
            NetInsightApplication.showError("Failed to measure latency: " + e.getMessage());
            e.printStackTrace();
        }
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
