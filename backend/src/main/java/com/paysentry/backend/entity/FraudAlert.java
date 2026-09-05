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
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "fraud_alerts")
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private BankAccount account;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fraud_event_id", nullable = false, unique = true)
    private FraudEvent fraudEvent;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 12)
    private UnifiedRiskLevel riskLevel;

    @Lob
    @Column(nullable = false)
    private String reasons;

    @Lob
    @Column(name = "recommended_action", nullable = false)
    private String recommendedAction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status = AlertStatus.NEW;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

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

    public FraudEvent getFraudEvent() {
        return fraudEvent;
    }

    public void setFraudEvent(FraudEvent fraudEvent) {
        this.fraudEvent = fraudEvent;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }
}
