package com.paysentry.backend.monitoring;

import java.util.List;

public class DirectRiskResult {

    private final int score;
    private final List<String> reasons;

    public DirectRiskResult(int score, List<String> reasons) {
        this.score = score;
        this.reasons = reasons;
    }

    public int getScore() {
        return score;
    }

    public List<String> getReasons() {
        return reasons;
    }
}
