package com.paysentry.backend.entity;

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
@Table(name = "fraud_events")
public class FraudEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private BankAccount account;

    @Column(name = "external_event_id", nullable = false, unique = true, length = 100)
    private String externalEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private BankingEventType eventType;

    @Column(name = "direct_risk_score", nullable = false)
    private int directRiskScore;

    @Column(name = "overall_risk_score", nullable = false)
    private int overallRiskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 12)
    private UnifiedRiskLevel riskLevel;

    @Lob
    @Column(nullable = false)
    private String reasons;

    @Lob
    @Column(nullable = false)
    private String eventDetails;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BankAccount getAccount() {
        return account;
    }

    public void setAccount(BankAccount account) {
        this.account = account;
    }

    public String getExternalEventId() {
        return externalEventId;
    }

    public void setExternalEventId(String externalEventId) {
        this.externalEventId = externalEventId;
    }

    public BankingEventType getEventType() {
        return eventType;
    }

    public void setEventType(BankingEventType eventType) {
        this.eventType = eventType;
    }

    public int getDirectRiskScore() {
        return directRiskScore;
    }

    public void setDirectRiskScore(int directRiskScore) {
        this.directRiskScore = directRiskScore;
    }

    public int getOverallRiskScore() {
        return overallRiskScore;
    }

    public void setOverallRiskScore(int overallRiskScore) {
        this.overallRiskScore = overallRiskScore;
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

    public String getEventDetails() {
        return eventDetails;
    }

    public void setEventDetails(String eventDetails) {
        this.eventDetails = eventDetails;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
