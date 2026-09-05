package com.paysentry.mockbank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MockLienResponse {

    private BigDecimal currentLienAmount;
    private BigDecimal previousLienAmount;
    private String reason;
    private String reference;
    private LocalDateTime timestamp;

    public BigDecimal getCurrentLienAmount() {
        return currentLienAmount;
    }

    public void setCurrentLienAmount(BigDecimal currentLienAmount) {
        this.currentLienAmount = currentLienAmount;
    }

    public BigDecimal getPreviousLienAmount() {
        return previousLienAmount;
    }

    public void setPreviousLienAmount(BigDecimal previousLienAmount) {
        this.previousLienAmount = previousLienAmount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
