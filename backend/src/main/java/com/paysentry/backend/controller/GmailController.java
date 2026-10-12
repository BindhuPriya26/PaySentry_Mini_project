package com.paysentry.backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paysentry.backend.dto.gmail.GmailConnectResponse;
import com.paysentry.backend.dto.gmail.GmailConnectionStatusResponse;
import com.paysentry.backend.dto.gmail.GmailSyncResponse;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.service.GmailIntegrationService;

@RestController
@RequestMapping("/api/gmail")
public class GmailController {

    private final GmailIntegrationService gmailIntegrationService;

    public GmailController(GmailIntegrationService gmailIntegrationService) {
        this.gmailIntegrationService = gmailIntegrationService;
    }

    @GetMapping("/status")
    public ResponseEntity<GmailConnectionStatusResponse> status(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(gmailIntegrationService.status(user));
    }

    @PostMapping("/connect")
    public ResponseEntity<GmailConnectResponse> connect(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(gmailIntegrationService.startConnection(user));
    }

    @PostMapping("/sync")
    public ResponseEntity<GmailSyncResponse> synchronize(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(gmailIntegrationService.synchronize(user));
    }

    @DeleteMapping("/connection")
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal User user) {
        gmailIntegrationService.disconnect(user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/callback", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> callback(
        @RequestParam(required = false) String state,
        @RequestParam(required = false, name = "code") String authorizationCode,
        @RequestParam(required = false, name = "error") String providerError
    ) {
        try {
            gmailIntegrationService.completeAuthorization(state, authorizationCode, providerError);
            return ResponseEntity.ok(callbackPage("Gmail connected", "You can safely return to PaySentry and refresh Gmail status."));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(callbackPage(
                "Gmail was not connected",
                "No mailbox was connected. Return to PaySentry and try again after checking your Google consent settings."
            ));
        }
    }

    private String callbackPage(String title, String message) {
        return "<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>PaySentry</title></head>"
            + "<body style=\"font-family:system-ui;max-width:42rem;margin:4rem auto;padding:0 1rem\"><h1>"
            + title + "</h1><p>" + message + "</p></body></html>";
    }
}
