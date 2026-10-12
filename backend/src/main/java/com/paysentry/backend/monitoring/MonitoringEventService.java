package com.paysentry.backend.monitoring;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paysentry.backend.dto.monitoring.EventProcessingResponse;
import com.paysentry.backend.dto.monitoring.MockBankEventRequest;
import com.paysentry.backend.entity.BankAccount;
import com.paysentry.backend.entity.BankingEventType;
import com.paysentry.backend.entity.FraudEvent;
import com.paysentry.backend.entity.LienChange;
import com.paysentry.backend.entity.MonitoredTransaction;
import com.paysentry.backend.exception.BadRequestException;
import com.paysentry.backend.repository.BankAccountRepository;
import com.paysentry.backend.repository.FraudEventRepository;
import com.paysentry.backend.repository.LienChangeRepository;
import com.paysentry.backend.repository.MonitoredTransactionRepository;

@Service
public class MonitoringEventService {

    private final BankAccountRepository bankAccountRepository;
    private final FraudEventRepository fraudEventRepository;
    private final MonitoredTransactionRepository monitoredTransactionRepository;
    private final LienChangeRepository lienChangeRepository;
    private final MonitoringRulesService monitoringRulesService;
    private final RiskEngineService riskEngineService;
    private final BankAccountService bankAccountService;

    public MonitoringEventService(
        BankAccountRepository bankAccountRepository,
        FraudEventRepository fraudEventRepository,
        MonitoredTransactionRepository monitoredTransactionRepository,
        LienChangeRepository lienChangeRepository,
        MonitoringRulesService monitoringRulesService,
        RiskEngineService riskEngineService,
        BankAccountService bankAccountService
    ) {
        this.bankAccountRepository = bankAccountRepository;
        this.fraudEventRepository = fraudEventRepository;
        this.monitoredTransactionRepository = monitoredTransactionRepository;
        this.lienChangeRepository = lienChangeRepository;
        this.monitoringRulesService = monitoringRulesService;
        this.riskEngineService = riskEngineService;
        this.bankAccountService = bankAccountService;
    }

    @Transactional
    public synchronized EventProcessingResponse process(MockBankEventRequest request) {
        FraudEvent existing = fraudEventRepository.findByExternalEventId(request.getEventId()).orElse(null);
        if (existing != null) {
            return toResponse(existing, false, "This simulated event was already processed.");
        }

        validateRequest(request);
        BankAccount account = bankAccountService.getByAccountId(request.getAccountId());
        LocalDateTime occurredAt = request.getOccurredAt() == null ? LocalDateTime.now() : request.getOccurredAt();
        long recentTransactions = monitoredTransactionRepository.countByAccountAndTransactionTimeAfter(
            account,
            occurredAt.minusHours(1)
        );
        long recentLienChanges = lienChangeRepository.countByAccountAndChangedAtAfter(account, occurredAt.minusHours(24));
        DirectRiskResult directRisk = monitoringRulesService.evaluate(
            request,
            account,
            recentTransactions,
            recentLienChanges
        );

        updateAccountSnapshot(account, request, occurredAt);
        bankAccountRepository.save(account);

        RiskOutcome outcome = riskEngineService.recordSignal(
            account.getUser(),
            account,
            request.getEventType(),
            request.getEventId(),
            directRisk.getScore(),
            directRisk.getReasons(),
            describe(request),
            occurredAt
        );

        if (request.getEventType() == BankingEventType.TRANSACTION) {
            storeTransaction(account, request, outcome, occurredAt);
        }
        if (request.getEventType() == BankingEventType.LIEN_CHANGE) {
            storeLienChange(account, request, outcome, occurredAt);
        }
        return toResponse(outcome, request.getAccountId(), request.getEventType().name(), occurredAt);
    }

    private void validateRequest(MockBankEventRequest request) {
        if (request.getEventType() == BankingEventType.TRANSACTION && request.getAmount() == null) {
            throw new BadRequestException("A transaction event requires an amount.");
        }
        if (request.getEventType() == BankingEventType.LIEN_CHANGE && request.getLienAmount() == null) {
            throw new BadRequestException("A lien change event requires the current lien amount.");
        }
        if (request.getEventType() == BankingEventType.SMS || request.getEventType() == BankingEventType.URL
            || request.getEventType() == BankingEventType.QR) {
            throw new BadRequestException("Mock Banking API events must represent banking activity, not content scans.");
        }
    }

    private void updateAccountSnapshot(BankAccount account, MockBankEventRequest request, LocalDateTime occurredAt) {
        if (account.getUpdatedAt() != null && occurredAt.isBefore(account.getUpdatedAt())) {
            return;
        }
        if (request.getTotalBalance() != null) {
            account.setTotalBalance(request.getTotalBalance());
        }
        if (request.getAvailableBalance() != null) {
            account.setAvailableBalance(request.getAvailableBalance());
        }
        if (request.getEventType() == BankingEventType.LIEN_CHANGE && request.getLienAmount() != null) {
            account.setLienAmount(request.getLienAmount());
        }
    }

    private void storeTransaction(
        BankAccount account,
        MockBankEventRequest request,
        RiskOutcome outcome,
        LocalDateTime occurredAt
    ) {
        MonitoredTransaction transaction = new MonitoredTransaction();
        transaction.setAccount(account);
        transaction.setExternalTransactionId(request.getEventId());
        transaction.setAmount(request.getAmount());
        transaction.setReceiver(defaultValue(request.getReceiver(), "Unknown receiver"));
        transaction.setLocation(defaultValue(request.getLocation(), "Unknown location"));
        transaction.setDevice(defaultValue(request.getDevice(), "Unknown device"));
        transaction.setNewDevice(request.isNewDevice());
        transaction.setNewLocation(request.isNewLocation());
        transaction.setTransactionTime(occurredAt);
        transaction.setRiskScore(outcome.getScore());
        transaction.setRiskLevel(outcome.getRiskLevel());
        transaction.setReasons(String.join("\n", outcome.getReasons()));
        monitoredTransactionRepository.save(transaction);
    }

    private void storeLienChange(
        BankAccount account,
        MockBankEventRequest request,
        RiskOutcome outcome,
        LocalDateTime occurredAt
    ) {
        LienChange lienChange = new LienChange();
        lienChange.setAccount(account);
        lienChange.setReferenceId(defaultValue(request.getTransactionReference(), request.getEventId()));
        lienChange.setPreviousLienAmount(defaultDecimal(request.getPreviousLienAmount()));
        lienChange.setLienAmount(request.getLienAmount());
        lienChange.setLienReason(defaultValue(request.getLienReason(), "No reason supplied by mock bank"));
        lienChange.setExpectedActivity(request.isExpectedActivity());
        lienChange.setChangedAt(occurredAt);
        lienChange.setRiskScore(outcome.getScore());
        lienChange.setRiskLevel(outcome.getRiskLevel());
        lienChange.setReasons(String.join("\n", outcome.getReasons()));
        lienChangeRepository.save(lienChange);
    }

    private EventProcessingResponse toResponse(
        RiskOutcome outcome,
        String accountId,
        String eventType,
        LocalDateTime occurredAt
    ) {
        EventProcessingResponse response = new EventProcessingResponse();
        response.setEventId(outcome.getFraudEvent().getExternalEventId());
        response.setAccountId(accountId);
        response.setEventType(eventType);
        response.setRiskScore(outcome.getScore());
        response.setRiskLevel(outcome.getRiskLevel().name());
        response.setReasons(outcome.getReasons());
        response.setRecommendedAction(outcome.getRecommendedAction());
        response.setAlertCreated(outcome.getAlert() != null);
        response.setOccurredAt(occurredAt);
        return response;
    }

    private EventProcessingResponse toResponse(FraudEvent event, boolean alertCreated, String reason) {
        EventProcessingResponse response = new EventProcessingResponse();
        response.setEventId(event.getExternalEventId());
        response.setAccountId(event.getAccount() == null ? null : event.getAccount().getAccountId());
        response.setEventType(event.getEventType().name());
        response.setRiskScore(event.getOverallRiskScore());
        response.setRiskLevel(event.getRiskLevel().name());
        response.setReasons(java.util.Collections.singletonList(reason));
        response.setRecommendedAction("Review the existing alert history for this event.");
        response.setAlertCreated(alertCreated);
        response.setOccurredAt(event.getOccurredAt());
        return response;
    }

    private String describe(MockBankEventRequest request) {
        return "eventType=" + request.getEventType()
            + "; amount=" + request.getAmount()
            + "; receiver=" + request.getReceiver()
            + "; location=" + request.getLocation()
            + "; device=" + request.getDevice()
            + "; lienAmount=" + request.getLienAmount()
            + "; lienReason=" + request.getLienReason();
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
