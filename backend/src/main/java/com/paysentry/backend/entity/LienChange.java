package com.paysentry.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "lien_changes")
public class LienChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private BankAccount account;

    @Column(name = "reference_id", nullable = false, unique = true, length = 80)
    private String referenceId;

    @Column(name = "previous_lien_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal previousLienAmount;

    @Column(name = "lien_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal lienAmount;

    @Column(name = "lien_reason", nullable = false, length = 300)
    private String lienReason;

    @Column(name = "expected_activity", nullable = false)
    private boolean expectedActivity;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 12)
    private UnifiedRiskLevel riskLevel;

    @Lob
    @Column(nullable = false)
    private String reasons;

    public Long getId() {
        return id;
    }

    public BankAccount getAccount() {
        return account;
    }

    public void setAccount(BankAccount account) {
        this.account = account;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
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

    public boolean isExpectedActivity() {
        return expectedActivity;
    }

    public void setExpectedActivity(boolean expectedActivity) {
        this.expectedActivity = expectedActivity;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public UnifiedRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(UnifiedRiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getReasons() {
        return reasons;
    }

    public void setReasons(String reasons) {
        this.reasons = reasons;
    }
}
