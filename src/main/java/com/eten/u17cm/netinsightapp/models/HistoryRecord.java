package com.eten.u17cm.netinsightapp.models;

import java.time.LocalDate;

public class HistoryRecord {

    private LocalDate date;
    private double bandwidth;
    private double latency;
    private double packetLoss;

    public HistoryRecord(LocalDate date, double bandwidth, double latency, double packetLoss) {
        this.date = date;
        this.bandwidth = bandwidth;
        this.latency = latency;
        this.packetLoss = packetLoss;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(double bandwidth) {
        this.bandwidth = bandwidth;
    }

    public double getLatency() {
        return latency;
    }

    public void setLatency(double latency) {
        this.latency = latency;
    }

    public double getPacketLoss() {
        return packetLoss;
    }

    public void setPacketLoss(double packetLoss) {
        this.packetLoss = packetLoss;
    }
}
