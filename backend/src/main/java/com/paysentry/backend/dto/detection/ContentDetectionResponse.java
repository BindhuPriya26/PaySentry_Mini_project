package com.paysentry.backend.dto.detection;

import java.time.LocalDateTime;
import java.util.List;

public class ContentDetectionResponse {

    private String channel;
    private int riskScore;
    private String riskLevel;
    private List<String> reasons;
    private String explanation;
    private String recommendedAction;
    private String extractedContent;
    private LocalDateTime analyzedAt;

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public String getExtractedContent() {
        return extractedContent;
    }

    public void setExtractedContent(String extractedContent) {
        this.extractedContent = extractedContent;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }
}
