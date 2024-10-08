package com.eten.u17cm.netinsightapp.controllers;

import com.eten.u17cm.netinsightapp.NetInsightApplication;
import javafx.application.Platform;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapException;
import org.jnetpcap.PcapHeader;
import org.jnetpcap.PcapIf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DashboardController implements Controller {
    private static final long MONITOR_INTERVAL = 500; // half second
    private static final String TARGET_DEVICE = "wlo1"; // The device to monitor
    private static final String PING_TARGET = "8.8.8.8"; // Target for latency (Google DNS)

    public Label currentBandwidth;
    public Label averageBandwidth;
    public Label currentLatency;
    public Label averageLatency;
    public Label currentPacketLoss;
    public Label averagePacketLoss;
    public LineChart<String, Number> bandwidthChart;
    public LineChart<String, Number> latencyChart;

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

    private int packetsSent = 0; // Total packets sent for ping
    private int packetsReceived = 0; // Total packets received for ping
    private double currentPacketLossPercentage = 0; // Current packet loss percentage
    private double averagePacketLossPercentage = 0; // Average packet loss percentage

    private static final int MAX_DATA_POINTS = 5; // Maximum number of data points to display on the chart
    private final XYChart.Series<String, Number> bandwidthSeries = new XYChart.Series<>();
    private final XYChart.Series<String, Number> latencySeries = new XYChart.Series<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        previousTime = System.currentTimeMillis();
        startTime = previousTime; // Initialize start time

        bandwidthChart.getData().clear();
        latencyChart.getData().clear();
        bandwidthChart.getData().add(bandwidthSeries);
        latencyChart.getData().add(latencySeries);

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

        // Format the time label with AM/PM
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss a")
                .withZone(ZoneId.systemDefault());
        String timeLabel = formatter.format(Instant.ofEpochMilli(currentTime));

        final XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(timeLabel, megabitsPerSecond);

        // Update chart data on the FX Application Thread
        Platform.runLater(() -> {
            if (bandwidthSeries.getData().size() > MAX_DATA_POINTS) {
                bandwidthSeries.getData().removeFirst(); // Remove the oldest data point
            }
            bandwidthSeries.getData().add(dataPoint);
        });

        // Reset counters
        totalBytesReceived = 0;
        previousTime = currentTime;
    }

    private void measureLatency() {
        try {
            // Detect the OS
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder processBuilder;

            if (os.contains("win")) {
                // Windows ping command
                processBuilder = new ProcessBuilder("ping", "-n", "4", "-w", "1000", PING_TARGET); // 1 second timeout
            } else {
                // Linux/macOS ping command
                processBuilder = new ProcessBuilder("ping", "-c", "4", "-W", "1", PING_TARGET); // 1 second timeout
            }

            packetsSent++; // Increment the total packets sent counter
            Process process = processBuilder.start();

            // Read the output of the ping command
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            long rtt = -1;

            // Parse the ping output to find the RTT
            while ((line = reader.readLine()) != null) {
                if (os.contains("win") && line.contains("Average =")) {
                    // Windows ping output parsing
                    String[] parts = line.split("Average = ")[1].split("ms")[0].trim().split(" ");
                    rtt = Long.parseLong(parts[0]);
                    break;
                } else if (!os.contains("win") && line.contains("rtt min/avg/max/mdev")) {
                    // Linux/macOS ping output parsing
                    String[] parts = line.split(" = ")[1].split("/");
                    rtt = Math.round(Double.parseDouble(parts[1])); // Extract average RTT
                    break;
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0 && rtt != -1) {
                // Successful ping and RTT found
                totalLatencyTime += rtt;
                latencyCount++;
                packetsReceived++; // Increment packets received counter

                // Calculate average latency
                averageLatencyMs = totalLatencyTime / (double) latencyCount;

                // Calculate packet loss
                currentPacketLossPercentage = ((double)(packetsSent - packetsReceived) / packetsSent) * 100;
                averagePacketLossPercentage = currentPacketLossPercentage;

                // Update the labels
                long finalRtt = rtt;
                Platform.runLater(() -> {
                    currentLatency.setText(String.format("Current: %d ms", finalRtt));
                    averageLatency.setText(String.format("Average: %.2f ms", averageLatencyMs));
                    currentPacketLoss.setText(String.format("Current: %.2f%%", currentPacketLossPercentage));
                    averagePacketLoss.setText(String.format("Average: %.2f%%", averagePacketLossPercentage));
                });
            } else {
                // Ping failed or RTT not found, mark as unreachable
                currentPacketLossPercentage = 100;

                Platform.runLater(() -> {
                    currentLatency.setText("Latency: Unreachable");
                    currentPacketLoss.setText("Packet Loss: 100%");
                });
            }

            final long finalRtt = rtt;
            Platform.runLater(() -> {
                // Format the time label with AM/PM
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss a")
                        .withZone(ZoneId.systemDefault());
                String timeLabel = formatter.format(Instant.ofEpochMilli(System.currentTimeMillis()));

                // Update latency chart
                if (latencySeries.getData().size() > MAX_DATA_POINTS) {
                    latencySeries.getData().removeFirst(); // Remove the oldest data point
                }
                latencySeries.getData().add(new XYChart.Data<>(timeLabel, finalRtt));
            });
        } catch (IOException e) {
            NetInsightApplication.showError("Error executing ping command: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            NetInsightApplication.showError("Ping process interrupted: " + e.getMessage());
            e.printStackTrace();
        } catch (NumberFormatException e) {
            NetInsightApplication.showError("Failed to parse RTT: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void dispose() {
        running = false; // Signal the background thread to stop
        if (pcap != null) {
            pcap.breakloop(); // Break the packet capture loop
            //pcap.close(); // Properly close the Pcap instance to release resources
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