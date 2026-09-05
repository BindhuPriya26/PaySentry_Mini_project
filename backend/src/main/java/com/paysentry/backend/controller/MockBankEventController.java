package com.paysentry.backend.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.paysentry.backend.dto.monitoring.EventProcessingResponse;
import com.paysentry.backend.dto.monitoring.MockBankEventRequest;
import com.paysentry.backend.monitoring.MonitoringEventService;

@RestController
@RequestMapping("/api/internal/mock-events")
public class MockBankEventController {

    private final MonitoringEventService monitoringEventService;
    private final String mockBankSharedKey;

    public MockBankEventController(
        MonitoringEventService monitoringEventService,
        @Value("${paysentry.mock-bank.shared-key}") String mockBankSharedKey
    ) {
        this.monitoringEventService = monitoringEventService;
        this.mockBankSharedKey = mockBankSharedKey;
    }

    @PostMapping
    public ResponseEntity<EventProcessingResponse> receiveMockEvent(
        @RequestHeader(value = "X-Mock-Bank-Key", required = false) String providedKey,
        @Valid @RequestBody MockBankEventRequest request
    ) {
        if (!mockBankSharedKey.equals(providedKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Mock bank key is invalid.");
        }
        return ResponseEntity.ok(monitoringEventService.process(request));
    }
}
