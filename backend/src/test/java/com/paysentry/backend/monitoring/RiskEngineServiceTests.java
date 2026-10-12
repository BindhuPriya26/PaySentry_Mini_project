package com.paysentry.backend.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.paysentry.backend.entity.BankingEventType;
import com.paysentry.backend.entity.FraudAlert;
import com.paysentry.backend.entity.FraudEvent;
import com.paysentry.backend.entity.UnifiedRiskLevel;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.notification.RealtimeAlertService;
import com.paysentry.backend.repository.FraudAlertRepository;
import com.paysentry.backend.repository.FraudEventRepository;

@ExtendWith(MockitoExtension.class)
class RiskEngineServiceTests {

    @Mock
    private FraudEventRepository fraudEventRepository;

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private RealtimeAlertService realtimeAlertService;

    @Mock
    private RiskThresholds riskThresholds;

    private RiskEngineService riskEngineService;

    @BeforeEach
    void setUp() {
        riskEngineService = new RiskEngineService(
            fraudEventRepository,
            fraudAlertRepository,
            realtimeAlertService,
            riskThresholds
        );
        when(fraudEventRepository.findByExternalEventId(any())).thenReturn(Optional.empty());
    }

    @Test
    void correlatesRecentChannelSignalsAndPersistsAnExplainableAlert() {
        User user = new User();
        FraudEvent priorSmsEvent = new FraudEvent();
        priorSmsEvent.setEventType(BankingEventType.SMS);
        priorSmsEvent.setOverallRiskScore(80);
        when(fraudEventRepository.findByUserAndOccurredAtBetweenOrderByOccurredAtDesc(
            any(),
            any(),
            any()
        )).thenReturn(Collections.singletonList(priorSmsEvent));
        when(riskThresholds.classify(76)).thenReturn(UnifiedRiskLevel.HIGH);
        when(fraudEventRepository.save(any(FraudEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RiskOutcome outcome = riskEngineService.recordSignal(
            user,
            null,
            BankingEventType.URL,
            "url-test-1",
            60,
            Collections.singletonList("Suspicious URL indicators found."),
            "fingerprint=synthetic",
            LocalDateTime.now()
        );

        assertEquals(76, outcome.getScore());
        assertEquals(UnifiedRiskLevel.HIGH, outcome.getRiskLevel());
        assertNotNull(outcome.getAlert());
        assertTrue(outcome.getReasons().stream().anyMatch(reason -> reason.contains("previous 24 hours")));
        verify(fraudEventRepository).save(any(FraudEvent.class));
        verify(fraudAlertRepository).save(any(FraudAlert.class));
        verify(realtimeAlertService).publish(outcome.getAlert());
    }

    @Test
    void lowRiskSignalIsStoredWithoutCreatingAnAlert() {
        when(fraudEventRepository.findByUserAndOccurredAtBetweenOrderByOccurredAtDesc(
            any(),
            any(),
            any()
        )).thenReturn(Collections.emptyList());
        when(riskThresholds.classify(10)).thenReturn(UnifiedRiskLevel.LOW);
        when(fraudEventRepository.save(any(FraudEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RiskOutcome outcome = riskEngineService.recordSignal(
            new User(),
            null,
            BankingEventType.URL,
            "url-test-2",
            10,
            Arrays.asList("No high-risk indicators found."),
            "fingerprint=synthetic",
            LocalDateTime.now()
        );

        assertEquals(UnifiedRiskLevel.LOW, outcome.getRiskLevel());
        assertEquals(null, outcome.getAlert());
        verify(fraudAlertRepository, never()).save(any(FraudAlert.class));
        verify(realtimeAlertService, never()).publish(any(FraudAlert.class));
    }
}
