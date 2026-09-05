package com.paysentry.backend.dto.dashboard;

import java.util.List;
import java.util.Map;

public class DashboardResponse {

    private long totalFraudScans;
    private long fraudAlerts;
    private double averageRiskScore;
    private Map<String, Long> scanBreakdown;
    private Map<String, Long> riskBreakdown;
    private List<DashboardActivityResponse> recentActivity;

    public long getTotalFraudScans() {
        return totalFraudScans;
    }

    public void setTotalFraudScans(long totalFraudScans) {
        this.totalFraudScans = totalFraudScans;
    }

    public long getFraudAlerts() {
        return fraudAlerts;
    }

    public void setFraudAlerts(long fraudAlerts) {
        this.fraudAlerts = fraudAlerts;
    }

    public double getAverageRiskScore() {
        return averageRiskScore;
    }

    public void setAverageRiskScore(double averageRiskScore) {
        this.averageRiskScore = averageRiskScore;
    }

    public Map<String, Long> getScanBreakdown() {
        return scanBreakdown;
    }

    public void setScanBreakdown(Map<String, Long> scanBreakdown) {
        this.scanBreakdown = scanBreakdown;
    }

    public Map<String, Long> getRiskBreakdown() {
        return riskBreakdown;
    }

    public void setRiskBreakdown(Map<String, Long> riskBreakdown) {
        this.riskBreakdown = riskBreakdown;
    }

    public List<DashboardActivityResponse> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<DashboardActivityResponse> recentActivity) {
        this.recentActivity = recentActivity;
    }
}
