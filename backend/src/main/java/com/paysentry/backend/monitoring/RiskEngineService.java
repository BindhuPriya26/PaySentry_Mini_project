package com.paysentry.backend.monitoring;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paysentry.backend.entity.BankAccount;
import com.paysentry.backend.entity.BankingEventType;
import com.paysentry.backend.entity.FraudAlert;
import com.paysentry.backend.entity.FraudEvent;
import com.paysentry.backend.entity.UnifiedRiskLevel;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.notification.RealtimeAlertService;
import com.paysentry.backend.repository.FraudAlertRepository;
import com.paysentry.backend.repository.FraudEventRepository;

@Service
public class RiskEngineService {

    private final FraudEventRepository fraudEventRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final RealtimeAlertService realtimeAlertService;

    public RiskEngineService(
        FraudEventRepository fraudEventRepository,
        FraudAlertRepository fraudAlertRepository,
        RealtimeAlertService realtimeAlertService
    ) {
        this.fraudEventRepository = fraudEventRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.realtimeAlertService = realtimeAlertService;
    }

    @Transactional
    public RiskOutcome recordSignal(
        User user,
        BankAccount account,
        BankingEventType eventType,
        String externalEventId,
        int directRiskScore,
        List<String> directReasons,
        String eventDetails,
        LocalDateTime occurredAt
    ) {
        FraudEvent existing = fraudEventRepository.findByExternalEventId(externalEventId).orElse(null);
        if (existing != null) {
            return new RiskOutcome(
                existing,
                null,
                existing.getOverallRiskScore(),
                existing.getRiskLevel(),
                AlertResponseMapper.splitLines(existing.getReasons()),
                recommendationFor(existing.getRiskLevel())
            );
        }

        List<String> reasons = new ArrayList<>(directReasons);
        List<FraudEvent> recentEvents = fraudEventRepository.findByUserAndOccurredAtAfterOrderByOccurredAtDesc(
            user,
            occurredAt.minusHours(24)
        );
        int correlationBonus = calculateCorrelationBonus(eventType, directRiskScore, recentEvents, reasons);
        int overallScore = Math.min(100, directRiskScore + correlationBonus);
        UnifiedRiskLevel riskLevel = UnifiedRiskLevel.fromScore(overallScore);

        FraudEvent event = new FraudEvent();
        event.setUser(user);
        event.setAccount(account);
        event.setExternalEventId(externalEventId);
        event.setEventType(eventType);
        event.setDirectRiskScore(directRiskScore);
        event.setOverallRiskScore(overallScore);
        event.setRiskLevel(riskLevel);
        event.setReasons(String.join("\n", reasons));
        event.setEventDetails(eventDetails);
        event.setOccurredAt(occurredAt);
        FraudEvent savedEvent = fraudEventRepository.save(event);

        FraudAlert alert = null;
        if (riskLevel != UnifiedRiskLevel.LOW) {
            alert = new FraudAlert();
            alert.setUser(user);
            alert.setAccount(account);
            alert.setFraudEvent(savedEvent);
            alert.setTitle(buildAlertTitle(eventType, riskLevel));
            alert.setRiskScore(overallScore);
            alert.setRiskLevel(riskLevel);
            alert.setReasons(String.join("\n", reasons));
            alert.setRecommendedAction(recommendationFor(riskLevel));
            alert = fraudAlertRepository.save(alert);
            realtimeAlertService.publish(alert);
        }

        return new RiskOutcome(savedEvent, alert, overallScore, riskLevel, reasons, recommendationFor(riskLevel));
    }

    private int calculateCorrelationBonus(
        BankingEventType eventType,
        int directRiskScore,
        List<FraudEvent> recentEvents,
        List<String> reasons
    ) {
        Set<BankingEventType> suspiciousTypes = EnumSet.noneOf(BankingEventType.class);
        boolean recentHighRiskEvent = false;
        for (FraudEvent event : recentEvents) {
            if (event.getOverallRiskScore() >= 31) {
                suspiciousTypes.add(event.getEventType());
            }
            if (event.getOverallRiskScore() >= 71) {
                recentHighRiskEvent = true;
            }
        }

        int bonus = 0;
        if (!suspiciousTypes.isEmpty()) {
            int correlatedChannels = suspiciousTypes.contains(eventType)
                ? suspiciousTypes.size() : suspiciousTypes.size() + 1;
            bonus += Math.min(24, correlatedChannels * 8);
            reasons.add("Unified risk engine correlated this signal with " + suspiciousTypes.size()
                + " suspicious channel(s) from the previous 24 hours.");
        }

        Set<BankingEventType> combined = EnumSet.noneOf(BankingEventType.class);
        combined.addAll(suspiciousTypes);
        combined.add(eventType);
        if (combined.contains(BankingEventType.TRANSACTION)
            && combined.contains(BankingEventType.LIEN_CHANGE)
            && (combined.contains(BankingEventType.DEVICE_CHANGE) || combined.contains(BankingEventType.LOCATION_CHANGE))) {
            bonus += 18;
            reasons.add("Transaction, lien, and account-access changes form a high-risk combined pattern.");
        }
        if (combined.contains(BankingEventType.TRANSACTION)
            && (combined.contains(BankingEventType.SMS) || combined.contains(BankingEventType.URL) || combined.contains(BankingEventType.QR))) {
            bonus += 15;
            reasons.add("A suspicious digital message or link is correlated with banking activity.");
        }
        if (directRiskScore >= 71 && recentHighRiskEvent) {
            bonus += 10;
            reasons.add("A second high-risk signal was detected within the correlation window.");
        }
        return Math.min(bonus, 45);
    }

    private String buildAlertTitle(BankingEventType eventType, UnifiedRiskLevel riskLevel) {
        return riskLevel.name() + "-RISK " + eventType.name().replace('_', ' ') + " DETECTED";
    }

    private String recommendationFor(UnifiedRiskLevel riskLevel) {
        if (riskLevel == UnifiedRiskLevel.CRITICAL) {
            return "Do not approve related activity. Contact your bank through its official channel immediately and verify the account state.";
        }
        if (riskLevel == UnifiedRiskLevel.HIGH) {
            return "Verify this activity with your bank before taking action. Do not share OTPs, passwords, or PINs.";
        }
        if (riskLevel == UnifiedRiskLevel.MEDIUM) {
            return "Review the activity carefully and verify the sender, device, or receiver using an official channel.";
        }
        return "No immediate action is required; continue monitoring your banking activity.";
    }
}
