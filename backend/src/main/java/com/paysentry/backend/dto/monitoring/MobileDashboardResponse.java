package com.paysentry.backend.dto.monitoring;

import java.math.BigDecimal;
import java.util.List;

public class MobileDashboardResponse {

    private String accountId;
    private BigDecimal totalBalance;
    private BigDecimal availableBalance;
    private BigDecimal lienAmount;
    private String currency;
    private int securityScore;
    private String protectionStatus;
    private List<MobileTransactionResponse> recentTransactions;
    private List<MobileAlertResponse> recentAlerts;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getLienAmount() {
        return lienAmount;
    }

    public void setLienAmount(BigDecimal lienAmount) {
        this.lienAmount = lienAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public int getSecurityScore() {
        return securityScore;
    }

    public void setSecurityScore(int securityScore) {
        this.securityScore = securityScore;
    }

    public String getProtectionStatus() {
        return protectionStatus;
    }

    public void setProtectionStatus(String protectionStatus) {
        this.protectionStatus = protectionStatus;
    }

    public List<MobileTransactionResponse> getRecentTransactions() {
        return recentTransactions;
    }

    public void setRecentTransactions(List<MobileTransactionResponse> recentTransactions) {
        this.recentTransactions = recentTransactions;
    }

    public List<MobileAlertResponse> getRecentAlerts() {
        return recentAlerts;
    }

    public void setRecentAlerts(List<MobileAlertResponse> recentAlerts) {
        this.recentAlerts = recentAlerts;
    }
}
