package com.paysentry.backend.monitoring;

import java.util.List;

import com.paysentry.backend.entity.FraudAlert;
import com.paysentry.backend.entity.FraudEvent;
import com.paysentry.backend.entity.UnifiedRiskLevel;

public class RiskOutcome {

    private final FraudEvent fraudEvent;
    private final FraudAlert alert;
    private final int score;
    private final UnifiedRiskLevel riskLevel;
    private final List<String> reasons;
    private final String recommendedAction;

    public RiskOutcome(
        FraudEvent fraudEvent,
        FraudAlert alert,
        int score,
        UnifiedRiskLevel riskLevel,
        List<String> reasons,
        String recommendedAction
    ) {
        this.fraudEvent = fraudEvent;
        this.alert = alert;
        this.score = score;
        this.riskLevel = riskLevel;
        this.reasons = reasons;
        this.recommendedAction = recommendedAction;
    }

    public FraudEvent getFraudEvent() {
        return fraudEvent;
    }

    public FraudAlert getAlert() {
        return alert;
    }

    public int getScore() {
        return score;
    }

    public UnifiedRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }
}
