package com.paysentry.backend.service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paysentry.backend.dto.scan.TransactionRiskRequest;
import com.paysentry.backend.dto.scan.TransactionRiskResponse;
import com.paysentry.backend.entity.RiskLevel;
import com.paysentry.backend.entity.ScanType;
import com.paysentry.backend.entity.TransactionRecord;
import com.paysentry.backend.entity.BankingEventType;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.monitoring.BankAccountService;
import com.paysentry.backend.monitoring.RiskEngineService;
import com.paysentry.backend.monitoring.RiskOutcome;
import com.paysentry.backend.repository.TransactionRecordRepository;
import com.paysentry.backend.repository.UserRepository;
import com.paysentry.backend.service.SensitiveContentService;

@Service
public class TransactionRiskService {

    private final TransactionRecordRepository transactionRecordRepository;
    private final UserRepository userRepository;
    private final FraudScoringService fraudScoringService;
    private final ReportService reportService;
    private final BankAccountService bankAccountService;
    private final RiskEngineService riskEngineService;
    private final SensitiveContentService sensitiveContentService;

    public TransactionRiskService(
        TransactionRecordRepository transactionRecordRepository,
        UserRepository userRepository,
        FraudScoringService fraudScoringService,
        ReportService reportService,
        BankAccountService bankAccountService,
        RiskEngineService riskEngineService,
        SensitiveContentService sensitiveContentService
    ) {
        this.transactionRecordRepository = transactionRecordRepository;
        this.userRepository = userRepository;
        this.fraudScoringService = fraudScoringService;
        this.reportService = reportService;
        this.bankAccountService = bankAccountService;
        this.riskEngineService = riskEngineService;
        this.sensitiveContentService = sensitiveContentService;
    }

    @Transactional
    public TransactionRiskResponse analyze(User user, TransactionRiskRequest request) {
        LocalDateTime transactionTime = request.getTransactionTime() == null
            ? LocalDateTime.now()
            : request.getTransactionTime();

        Set<String> trustedDevices = parseTrustedValues(user.getTrustedDevices());
        Set<String> trustedLocations = parseTrustedValues(user.getTrustedLocations());

        boolean newDevice = !containsIgnoreCase(trustedDevices, request.getDevice());
        boolean newLocation = !containsIgnoreCase(trustedLocations, request.getLocation());
        long recentTransactionCount = transactionRecordRepository.countByUserAndTransactionTimeBetween(
            user,
            transactionTime.minusHours(1),
            transactionTime
        );

        RiskAssessment assessment = fraudScoringService.assessTransaction(
            request.getAmount(),
            request.getReceiver(),
            request.getLocation(),
            request.getDevice(),
            transactionTime,
            newDevice,
            newLocation,
            recentTransactionCount
        );

        TransactionRecord record = new TransactionRecord();
        record.setUser(user);
        record.setAmount(request.getAmount());
        record.setReceiver(request.getReceiver().trim());
        record.setLocation(request.getLocation().trim());
        record.setDevice(request.getDevice().trim());
        record.setTransactionTime(transactionTime);
        record.setFlags(String.join(" | ", assessment.getFindings()));
        record.setRiskLevel(assessment.getRiskLevel());
        record.setRiskScore(assessment.getRiskScore());
        record.setExplanation(assessment.getExplanation());

        TransactionRecord savedRecord = transactionRecordRepository.save(record);
        RiskOutcome unifiedRisk = riskEngineService.recordSignal(
            user,
            bankAccountService.getForUser(user),
            BankingEventType.TRANSACTION,
            "transaction-analysis-" + UUID.randomUUID(),
            assessment.getRiskScore(),
            assessment.getFindings(),
            "amount=" + request.getAmount()
                + "; receiver=" + request.getReceiver().trim()
                + "; location=" + request.getLocation().trim()
                + "; device=" + request.getDevice().trim()
                + "; evidenceFingerprint=" + sensitiveContentService.fingerprint(
                    request.getReceiver() + "|" + request.getLocation() + "|" + request.getDevice()
                ),
            transactionTime
        );
        reportService.createUnifiedReport(
            user,
            ScanType.TRANSACTION,
            savedRecord.getId(),
            "Transaction Risk Analysis",
            unifiedRisk
        );

        if (assessment.getRiskLevel() == RiskLevel.SAFE) {
            trustedDevices.add(request.getDevice().trim());
            trustedLocations.add(request.getLocation().trim());
            user.setTrustedDevices(String.join(", ", trustedDevices));
            user.setTrustedLocations(String.join(", ", trustedLocations));
            userRepository.save(user);
        }

        TransactionRiskResponse response = new TransactionRiskResponse();
        response.setTransactionId(savedRecord.getId());
        response.setRiskLevel(unifiedRisk.getRiskLevel().name());
        response.setRiskScore(unifiedRisk.getScore());
        response.setExplanation(String.join("\n", unifiedRisk.getReasons()));
        response.setFlags(unifiedRisk.getReasons());
        response.setRecommendations(java.util.Collections.singletonList(unifiedRisk.getRecommendedAction()));
        response.setCreatedAt(savedRecord.getCreatedAt());
        return response;
    }

    private Set<String> parseTrustedValues(String source) {
        Set<String> values = new LinkedHashSet<>();
        if (source == null || source.isBlank()) {
            return values;
        }
        String[] tokens = source.split(",");
        for (String token : tokens) {
            String value = token.trim();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private boolean containsIgnoreCase(Set<String> values, String candidate) {
        String normalizedCandidate = candidate.trim().toLowerCase();
        return values.stream().anyMatch(value -> value.toLowerCase().equals(normalizedCandidate));
    }
}
