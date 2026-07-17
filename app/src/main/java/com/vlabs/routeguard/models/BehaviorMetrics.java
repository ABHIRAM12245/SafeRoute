package com.vlabs.routeguard.models;

public class BehaviorMetrics {
    private int positiveVotesGiven;
    private int negativeVotesGiven;
    private int positiveVotesReceived;
    private int negativeVotesReceived;
    private int validReports;
    private int invalidReports;
    private int totalVotesGiven;
    private int totalVotesReceived;
    private int totalReports;
    private int reportsToday;
    private int votesToday;
    private int accountAgeDays;

    public BehaviorMetrics() {
    }

    public BehaviorMetrics(int positiveVotesGiven, int negativeVotesGiven, int positiveVotesReceived, int negativeVotesReceived, int validReports, int invalidReports, int totalVotesGiven, int totalVotesReceived, int totalReports, int reportsToday, int votesToday, int accountAgeDays) {
        this.positiveVotesGiven = positiveVotesGiven;
        this.negativeVotesGiven = negativeVotesGiven;
        this.positiveVotesReceived = positiveVotesReceived;
        this.negativeVotesReceived = negativeVotesReceived;
        this.validReports = validReports;
        this.invalidReports = invalidReports;
        this.totalVotesGiven = totalVotesGiven;
        this.totalVotesReceived = totalVotesReceived;
        this.totalReports = totalReports;
        this.reportsToday = reportsToday;
        this.votesToday = votesToday;
        this.accountAgeDays = accountAgeDays;
    }

    public int getPositiveVotesGiven() {
        return positiveVotesGiven;
    }

    public void setPositiveVotesGiven(int positiveVotesGiven) {
        this.positiveVotesGiven = positiveVotesGiven;
    }

    public int getNegativeVotesGiven() {
        return negativeVotesGiven;
    }

    public void setNegativeVotesGiven(int negativeVotesGiven) {
        this.negativeVotesGiven = negativeVotesGiven;
    }

    public int getPositiveVotesReceived() {
        return positiveVotesReceived;
    }

    public void setPositiveVotesReceived(int positiveVotesReceived) {
        this.positiveVotesReceived = positiveVotesReceived;
    }

    public int getNegativeVotesReceived() {
        return negativeVotesReceived;
    }

    public void setNegativeVotesReceived(int negativeVotesReceived) {
        this.negativeVotesReceived = negativeVotesReceived;
    }

    public int getValidReports() {
        return validReports;
    }

    public void setValidReports(int validReports) {
        this.validReports = validReports;
    }

    public int getInvalidReports() {
        return invalidReports;
    }

    public void setInvalidReports(int invalidReports) {
        this.invalidReports = invalidReports;
    }

    public int getTotalVotesGiven() {
        return totalVotesGiven;
    }

    public void setTotalVotesGiven(int totalVotesGiven) {
        this.totalVotesGiven = totalVotesGiven;
    }

    public int getTotalVotesReceived() {
        return totalVotesReceived;
    }

    public void setTotalVotesReceived(int totalVotesReceived) {
        this.totalVotesReceived = totalVotesReceived;
    }

    public int getTotalReports() {
        return totalReports;
    }

    public void setTotalReports(int totalReports) {
        this.totalReports = totalReports;
    }

    public int getReportsToday() {
        return reportsToday;
    }

    public void setReportsToday(int reportsToday) {
        this.reportsToday = reportsToday;
    }

    public int getVotesToday() {
        return votesToday;
    }

    public void setVotesToday(int votesToday) {
        this.votesToday = votesToday;
    }

    public int getAccountAgeDays() {
        return accountAgeDays;
    }

    public void setAccountAgeDays(int accountAgeDays) {
        this.accountAgeDays = accountAgeDays;
    }
}
