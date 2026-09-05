package com.paysentry.backend.monitoring;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.paysentry.backend.dto.monitoring.MockBankEventRequest;
import com.paysentry.backend.entity.BankAccount;
import com.paysentry.backend.entity.BankingEventType;

@Service
public class MonitoringRulesService {

    public DirectRiskResult evaluate(
        MockBankEventRequest request,
        BankAccount account,
        long recentTransactionCount,
        long recentLienChanges
    ) {
        if (request.getEventType() == BankingEventType.LIEN_CHANGE) {
            return evaluateLien(request, account, recentLienChanges);
        }
        if (request.getEventType() == BankingEventType.TRANSACTION) {
            return evaluateTransaction(request, recentTransactionCount);
        }
        return evaluateAccessChange(request);
    }

    private DirectRiskResult evaluateTransaction(MockBankEventRequest request, long recentTransactionCount) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        BigDecimal amount = request.getAmount() == null ? BigDecimal.ZERO : request.getAmount();
        String receiver = safeLower(request.getReceiver());
        LocalDateTime occurredAt = request.getOccurredAt() == null ? LocalDateTime.now() : request.getOccurredAt();

        if (amount.compareTo(new BigDecimal("100000")) >= 0) {
            score += 40;
            reasons.add("Transaction amount is significantly above the high-risk threshold.");
        } else if (amount.compareTo(new BigDecimal("50000")) >= 0) {
            score += 28;
            reasons.add("Transaction amount is above the normal monitoring threshold.");
        } else if (amount.compareTo(new BigDecimal("20000")) >= 0) {
            score += 14;
            reasons.add("Transaction amount is higher than a routine transfer.");
        }
        if (request.isNewDevice()) {
            score += 25;
            reasons.add("Transaction originated from a newly observed device.");
        }
        if (request.isNewLocation()) {
            score += 20;
            reasons.add("Transaction originated from a newly observed location.");
        }
        if (occurredAt.getHour() < 6 || occurredAt.getHour() >= 23) {
            score += 12;
            reasons.add("Transaction occurred during an unusual hour.");
        }
        if (recentTransactionCount >= 3) {
            score += 18;
            reasons.add("Several transactions were observed in the last hour.");
        }
        if (receiver.contains("crypto") || receiver.contains("wallet") || receiver.contains("gift")) {
            score += 12;
            reasons.add("Receiver name matches a high-risk transfer pattern.");
        }
        if (reasons.isEmpty()) {
            reasons.add("Transaction aligns with the current simulated account pattern.");
        }
        return new DirectRiskResult(Math.min(score, 100), reasons);
    }

    private DirectRiskResult evaluateLien(MockBankEventRequest request, BankAccount account, long recentLienChanges) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        BigDecimal previous = request.getPreviousLienAmount() == null
            ? account.getLienAmount() : request.getPreviousLienAmount();
        BigDecimal current = request.getLienAmount() == null ? previous : request.getLienAmount();
        BigDecimal increase = current.subtract(previous);

        if (!request.isExpectedActivity()) {
            score += 35;
            reasons.add("Lien change has no matching expected activity.");
        }
        if (isBlank(request.getTransactionReference())) {
            score += 20;
            reasons.add("Lien change has no transaction or case reference.");
        }
        if (previous.compareTo(BigDecimal.ZERO) == 0 && current.compareTo(BigDecimal.ZERO) > 0) {
            score += 20;
            reasons.add("A new lien was created on the account.");
        }
        if (current.compareTo(new BigDecimal("50000")) >= 0) {
            score += 20;
            reasons.add("Lien amount is unusually large.");
        }
        if (increase.compareTo(new BigDecimal("25000")) >= 0
            || (previous.compareTo(BigDecimal.ZERO) > 0 && increase.multiply(new BigDecimal("2")).compareTo(previous) >= 0)) {
            score += 22;
            reasons.add("Lien amount increased sharply from the previous amount.");
        }
        if (recentLienChanges >= 2) {
            score += 15;
            reasons.add("Repeated lien modifications were observed within 24 hours.");
        }
        if (reasons.isEmpty()) {
            reasons.add("Lien change was recorded for monitoring; PaySentry will not alter the bank lien.");
        }
        return new DirectRiskResult(Math.min(score, 100), reasons);
    }

    private DirectRiskResult evaluateAccessChange(MockBankEventRequest request) {
        int score = request.getEventType() == BankingEventType.DEVICE_CHANGE ? 45 : 35;
        List<String> reasons = new ArrayList<>();
        if (request.getEventType() == BankingEventType.DEVICE_CHANGE) {
            reasons.add("A new device was registered against the monitored account.");
        } else {
            reasons.add("A new location was observed for the monitored account.");
        }
        if (request.isNewDevice() || request.isNewLocation()) {
            score += 10;
            reasons.add("The change is marked as previously unseen by the mock bank.");
        }
        return new DirectRiskResult(Math.min(score, 100), reasons);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
