package com.paysentry.backend.dto.monitoring;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.paysentry.backend.entity.BankingEventType;

public class MockBankEventRequest {

    @NotBlank
    private String eventId;

    @NotBlank
    private String accountId;

    @NotNull
    private BankingEventType eventType;

    private BigDecimal amount;
    private String receiver;
    private String location;
    private String device;
    private boolean newDevice;
    private boolean newLocation;
    private BigDecimal totalBalance;
    private BigDecimal availableBalance;
    private BigDecimal previousLienAmount;
    private BigDecimal lienAmount;
    private String lienReason;
    private String transactionReference;
    private boolean expectedActivity;
    private LocalDateTime occurredAt;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BankingEventType getEventType() {
        return eventType;
    }

    public void setEventType(BankingEventType eventType) {
        this.eventType = eventType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public boolean isNewDevice() {
        return newDevice;
    }

    public void setNewDevice(boolean newDevice) {
        this.newDevice = newDevice;
    }

    public boolean isNewLocation() {
        return newLocation;
    }

    public void setNewLocation(boolean newLocation) {
        this.newLocation = newLocation;
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

    public BigDecimal getPreviousLienAmount() {
        return previousLienAmount;
    }

    public void setPreviousLienAmount(BigDecimal previousLienAmount) {
        this.previousLienAmount = previousLienAmount;
    }

    public BigDecimal getLienAmount() {
        return lienAmount;
    }

    public void setLienAmount(BigDecimal lienAmount) {
        this.lienAmount = lienAmount;
    }

    public String getLienReason() {
        return lienReason;
    }

    public void setLienReason(String lienReason) {
        this.lienReason = lienReason;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public boolean isExpectedActivity() {
        return expectedActivity;
    }

    public void setExpectedActivity(boolean expectedActivity) {
        this.expectedActivity = expectedActivity;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
