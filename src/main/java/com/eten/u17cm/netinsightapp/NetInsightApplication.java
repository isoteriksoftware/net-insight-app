package com.eten.u17cm.netinsightapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapException;
import org.jnetpcap.PcapHeader;
import org.jnetpcap.PcapIf;
import org.jnetpcap.util.PcapUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

public class NetInsightApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(NetInsightApplication.class.getResource("home.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 960, 600);
        stage.setTitle("Net Insight");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) throws PcapException {
        launch();

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

            // Capture packets indefinitely (or specify a count)
            pcap.loop(-1, (String msg, PcapHeader header, byte[] packet) -> {
                // Extract timestamp
                Instant timestamp = Instant.ofEpochMilli(header.toEpochMilli());

                // Extract packet length
                int wireLength = header.wireLength();
                int capLength = header.captureLength();

                // Format packet data
                String hexDump = PcapUtils.toHexCurleyString(packet, 0, Math.min(64, packet.length));

                // Print packet info
                System.out.printf("Packet [timestamp=%s, wirelen=%-4d caplen=%-4d %s]%n",
                        timestamp, wireLength, capLength, hexDump);
            }, "Hello World");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}