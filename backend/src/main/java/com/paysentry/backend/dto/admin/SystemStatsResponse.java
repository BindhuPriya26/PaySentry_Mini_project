package com.paysentry.backend.dto.admin;

public class SystemStatsResponse {

    private long totalUsers;
    private long activeUsers;
    private long totalReports;
    private long highRiskReports;
    private long totalSmsScans;
    private long totalQrScans;
    private long totalUrlScans;
    private long totalTransactions;

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public long getTotalReports() {
        return totalReports;
    }

    public void setTotalReports(long totalReports) {
        this.totalReports = totalReports;
    }

    public long getHighRiskReports() {
        return highRiskReports;
    }

    public void setHighRiskReports(long highRiskReports) {
        this.highRiskReports = highRiskReports;
    }

    public long getTotalSmsScans() {
        return totalSmsScans;
    }

    public void setTotalSmsScans(long totalSmsScans) {
        this.totalSmsScans = totalSmsScans;
    }

    public long getTotalQrScans() {
        return totalQrScans;
    }

    public void setTotalQrScans(long totalQrScans) {
        this.totalQrScans = totalQrScans;
    }

    public long getTotalUrlScans() {
        return totalUrlScans;
    }

    public void setTotalUrlScans(long totalUrlScans) {
        this.totalUrlScans = totalUrlScans;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }
}
