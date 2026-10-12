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
import javax.persistence.UniqueConstraint;

@Entity
@Table(
    name = "gmail_message_analyses",
    uniqueConstraints = @UniqueConstraint(columnNames = {"gmail_connection_id", "gmail_message_id"})
)
public class GmailMessageAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gmail_connection_id", nullable = false)
    private GmailConnection connection;

    @Column(name = "gmail_message_id", nullable = false, length = 128)
    private String gmailMessageId;

    @Column(name = "sender_domain", length = 253)
    private String senderDomain;

    @Column(name = "subject_fingerprint", nullable = false, length = 64)
    private String subjectFingerprint;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 12)
    private UnifiedRiskLevel riskLevel;

    @Lob
    @Column(nullable = false)
    private String reasons;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public Long getId() { return id; }
    public GmailConnection getConnection() { return connection; }
    public void setConnection(GmailConnection connection) { this.connection = connection; }
    public String getGmailMessageId() { return gmailMessageId; }
    public void setGmailMessageId(String gmailMessageId) { this.gmailMessageId = gmailMessageId; }
    public String getSenderDomain() { return senderDomain; }
    public void setSenderDomain(String senderDomain) { this.senderDomain = senderDomain; }
    public String getSubjectFingerprint() { return subjectFingerprint; }
    public void setSubjectFingerprint(String subjectFingerprint) { this.subjectFingerprint = subjectFingerprint; }
    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    public UnifiedRiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(UnifiedRiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public String getReasons() { return reasons; }
    public void setReasons(String reasons) { this.reasons = reasons; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
