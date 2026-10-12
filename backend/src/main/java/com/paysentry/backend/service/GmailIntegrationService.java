package com.paysentry.backend.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paysentry.backend.dto.gmail.GmailConnectResponse;
import com.paysentry.backend.dto.gmail.GmailConnectionStatusResponse;
import com.paysentry.backend.dto.gmail.GmailSyncResponse;
import com.paysentry.backend.entity.BankingEventType;
import com.paysentry.backend.entity.GmailConnection;
import com.paysentry.backend.entity.GmailConnectionStatus;
import com.paysentry.backend.entity.GmailMessageAnalysis;
import com.paysentry.backend.entity.GmailOauthState;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.exception.BadRequestException;
import com.paysentry.backend.exception.ResourceNotFoundException;
import com.paysentry.backend.monitoring.BankAccountService;
import com.paysentry.backend.monitoring.RiskEngineService;
import com.paysentry.backend.monitoring.RiskOutcome;
import com.paysentry.backend.repository.GmailConnectionRepository;
import com.paysentry.backend.repository.GmailMessageAnalysisRepository;
import com.paysentry.backend.repository.GmailOauthStateRepository;
import com.paysentry.backend.security.CredentialEncryptionService;

/**
 * Consent-based Gmail integration. Gmail bodies are read only in memory for scoring and are never
 * written to the PaySentry database. OAuth tokens are AES-GCM encrypted before persistence.
 */
@Service
public class GmailIntegrationService {

    private static final String GMAIL_READONLY_SCOPE = "https://www.googleapis.com/auth/gmail.readonly";
    private static final String OAUTH_SCOPES = "openid email " + GMAIL_READONLY_SCOPE;
    private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\b(?:https?://|www\\.)[^\\s<>'\\\"]+");
    private static final Pattern SUSPICIOUS_ATTACHMENT = Pattern.compile(
        "(?i).+\\.(exe|dll|msi|bat|cmd|scr|js|jse|vbs|vbe|ps1|apk|iso|img|html?|zip|rar)$"
    );

    private final GmailConnectionRepository connectionRepository;
    private final GmailOauthStateRepository oauthStateRepository;
    private final GmailMessageAnalysisRepository messageAnalysisRepository;
    private final CredentialEncryptionService credentialEncryptionService;
    private final FraudScoringService fraudScoringService;
    private final SensitiveContentService sensitiveContentService;
    private final RiskEngineService riskEngineService;
    private final BankAccountService bankAccountService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final int maxMessagesPerSync;

    public GmailIntegrationService(
        GmailConnectionRepository connectionRepository,
        GmailOauthStateRepository oauthStateRepository,
        GmailMessageAnalysisRepository messageAnalysisRepository,
        CredentialEncryptionService credentialEncryptionService,
        FraudScoringService fraudScoringService,
        SensitiveContentService sensitiveContentService,
        RiskEngineService riskEngineService,
        BankAccountService bankAccountService,
        ObjectMapper objectMapper,
        @Value("${paysentry.gmail.enabled}") boolean enabled,
        @Value("${paysentry.gmail.client-id:}") String clientId,
        @Value("${paysentry.gmail.client-secret:}") String clientSecret,
        @Value("${paysentry.gmail.redirect-uri:}") String redirectUri,
        @Value("${paysentry.gmail.max-messages-per-sync:25}") int maxMessagesPerSync
    ) {
        this.connectionRepository = connectionRepository;
        this.oauthStateRepository = oauthStateRepository;
        this.messageAnalysisRepository = messageAnalysisRepository;
        this.credentialEncryptionService = credentialEncryptionService;
        this.fraudScoringService = fraudScoringService;
        this.sensitiveContentService = sensitiveContentService;
        this.riskEngineService = riskEngineService;
        this.bankAccountService = bankAccountService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        this.enabled = enabled;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.maxMessagesPerSync = Math.max(1, Math.min(100, maxMessagesPerSync));
    }

    @Transactional(readOnly = true)
    public GmailConnectionStatusResponse status(User user) {
        GmailConnectionStatusResponse response = new GmailConnectionStatusResponse();
        response.setConfigured(isConfigured());
        connectionRepository.findByUser(user).ifPresent(connection -> {
            response.setConnected(connection.getStatus() == GmailConnectionStatus.CONNECTED);
            response.setMailboxEmail(connection.getMailboxEmail());
            response.setStatus(connection.getStatus().name());
            response.setLastSyncAt(connection.getLastSyncAt());
            response.setMessage(connection.getStatus() == GmailConnectionStatus.ERROR
                ? "Gmail access needs to be reconnected or retried."
                : "Gmail monitoring is connected with read-only access.");
        });
        if (response.getStatus() == null) {
            response.setConnected(false);
            response.setStatus("DISCONNECTED");
            response.setMessage(isConfigured()
                ? "No Gmail mailbox is connected."
                : "Gmail integration is not configured on this server.");
        }
        return response;
    }

    @Transactional
    public GmailConnectResponse startConnection(User user) {
        requireConfiguration();
        oauthStateRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        oauthStateRepository.deleteByUser(user);

        GmailOauthState state = new GmailOauthState();
        state.setUser(user);
        state.setState(Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
        state.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        oauthStateRepository.save(state);

        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("client_id", clientId);
        parameters.put("redirect_uri", redirectUri);
        parameters.put("response_type", "code");
        parameters.put("scope", OAUTH_SCOPES);
        parameters.put("access_type", "offline");
        parameters.put("prompt", "consent");
        parameters.put("include_granted_scopes", "true");
        parameters.put("state", state.getState());

        GmailConnectResponse response = new GmailConnectResponse();
        response.setAuthorizationUrl("https://accounts.google.com/o/oauth2/v2/auth?" + formEncode(parameters));
        response.setMessage("Continue in the browser, approve read-only Gmail access, then return to PaySentry and refresh Gmail status.");
        return response;
    }

    @Transactional
    public void completeAuthorization(String stateValue, String authorizationCode, String providerError) {
        requireConfiguration();
        GmailOauthState state = oauthStateRepository.findByState(stateValue)
            .orElseThrow(() -> new BadRequestException("The Gmail authorization request is invalid or has expired."));
        oauthStateRepository.delete(state);
        if (state.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("The Gmail authorization request expired. Start again from PaySentry.");
        }
        if (providerError != null && !providerError.isBlank()) {
            throw new BadRequestException("Gmail permission was not granted. No mailbox was connected.");
        }
        if (authorizationCode == null || authorizationCode.isBlank()) {
            throw new BadRequestException("Google did not return an authorization code.");
        }

        JsonNode tokenResponse = postForm("https://oauth2.googleapis.com/token", Map.of(
            "code", authorizationCode,
            "client_id", clientId,
            "client_secret", clientSecret,
            "redirect_uri", redirectUri,
            "grant_type", "authorization_code"
        ));
        String accessToken = requiredText(tokenResponse, "access_token", "Google did not return an access token.");
        String refreshToken = requiredText(tokenResponse, "refresh_token", "Google did not return a refresh token. Reconnect and approve consent again.");
        String mailboxEmail = fetchMailboxEmail(accessToken);

        GmailConnection connection = connectionRepository.findByUser(state.getUser()).orElseGet(GmailConnection::new);
        connection.setUser(state.getUser());
        connection.setMailboxEmail(mailboxEmail);
        connection.setEncryptedAccessToken(credentialEncryptionService.encrypt(accessToken));
        connection.setEncryptedRefreshToken(credentialEncryptionService.encrypt(refreshToken));
        connection.setAccessTokenExpiresAt(expiryFrom(tokenResponse));
        connection.setScopes(OAUTH_SCOPES);
        connection.setStatus(GmailConnectionStatus.CONNECTED);
        connection.setLastErrorAt(null);
        connection.setLastErrorCode(null);
        connectionRepository.save(connection);
    }

    @Transactional
    public GmailSyncResponse synchronize(User user) {
        requireConfiguration();
        GmailConnection connection = connectionRepository.findByUser(user)
            .orElseThrow(() -> new ResourceNotFoundException("No Gmail mailbox is connected for this account."));
        return synchronizeConnection(connection);
    }

    @Transactional
    public void disconnect(User user) {
        GmailConnection connection = connectionRepository.findByUser(user)
            .orElseThrow(() -> new ResourceNotFoundException("No Gmail mailbox is connected for this account."));
        if (isConfigured()) {
            try {
                String refreshToken = credentialEncryptionService.decrypt(connection.getEncryptedRefreshToken());
                revokeAtGoogle(refreshToken);
            } catch (RuntimeException ignored) {
                // Stored credentials are deleted even if Google is temporarily unreachable.
            }
        }
        messageAnalysisRepository.deleteByConnection(connection);
        connectionRepository.delete(connection);
    }

    @Scheduled(fixedDelayString = "${paysentry.gmail.sync-interval-ms:900000}")
    @Transactional
    public void synchronizeConnectedMailboxes() {
        if (!isConfigured()) {
            return;
        }
        for (GmailConnection connection : connectionRepository.findByStatus(GmailConnectionStatus.CONNECTED)) {
            try {
                synchronizeConnection(connection);
            } catch (RuntimeException exception) {
                markError(connection, exception);
            }
        }
    }

    private GmailSyncResponse synchronizeConnection(GmailConnection connection) {
        try {
            String accessToken = usableAccessToken(connection);
            JsonNode listing = getJson(
                "https://gmail.googleapis.com/gmail/v1/users/me/messages?labelIds=INBOX&maxResults=" + maxMessagesPerSync,
                accessToken
            );
            int processed = 0;
            int alertsCreated = 0;
            for (JsonNode messageRef : listing.path("messages")) {
                String messageId = messageRef.path("id").asText();
                if (messageId.isBlank() || messageAnalysisRepository.existsByConnectionAndGmailMessageId(connection, messageId)) {
                    continue;
                }
                if (processMessage(connection, accessToken, messageId)) {
                    alertsCreated++;
                }
                processed++;
            }
            connection.setStatus(GmailConnectionStatus.CONNECTED);
            connection.setLastSyncAt(LocalDateTime.now());
            connection.setLastErrorAt(null);
            connection.setLastErrorCode(null);
            connectionRepository.save(connection);

            GmailSyncResponse response = new GmailSyncResponse();
            response.setMessagesProcessed(processed);
            response.setAlertsCreated(alertsCreated);
            response.setMessage("Authorized Gmail messages were analyzed without retaining message bodies.");
            return response;
        } catch (RuntimeException exception) {
            markError(connection, exception);
            throw exception;
        }
    }

    private boolean processMessage(GmailConnection connection, String accessToken, String messageId) {
        JsonNode message = getJson(
            "https://gmail.googleapis.com/gmail/v1/users/me/messages/" + encode(messageId) + "?format=full",
            accessToken
        );
        DecodedGmailMessage decoded = decodeMessage(message);
        RiskAssessment assessment = fraudScoringService.assessEmail(
            decoded.sender,
            decoded.subject,
            decoded.body,
            decoded.urls,
            decoded.hasSuspiciousAttachment
        );
        LocalDateTime occurredAt = decoded.receivedAt == null ? LocalDateTime.now() : decoded.receivedAt;
        RiskOutcome outcome = riskEngineService.recordSignal(
            connection.getUser(),
            bankAccountService.getForUser(connection.getUser()),
            BankingEventType.EMAIL,
            "gmail-" + connection.getId() + "-" + messageId,
            assessment.getRiskScore(),
            assessment.getFindings(),
            "gmailMessageId=" + messageId + "; senderDomain=" + senderDomain(decoded.sender)
                + "; subjectFingerprint=" + sensitiveContentService.fingerprint(decoded.subject),
            occurredAt
        );

        GmailMessageAnalysis analysis = new GmailMessageAnalysis();
        analysis.setConnection(connection);
        analysis.setGmailMessageId(messageId);
        analysis.setSenderDomain(senderDomain(decoded.sender));
        analysis.setSubjectFingerprint(sensitiveContentService.fingerprint(decoded.subject));
        analysis.setRiskScore(outcome.getScore());
        analysis.setRiskLevel(outcome.getRiskLevel());
        analysis.setReasons(String.join("\n", outcome.getReasons()));
        analysis.setReceivedAt(occurredAt);
        analysis.setProcessedAt(LocalDateTime.now());
        messageAnalysisRepository.save(analysis);
        return outcome.getAlert() != null;
    }

    private String usableAccessToken(GmailConnection connection) {
        if (connection.getAccessTokenExpiresAt().isAfter(LocalDateTime.now().plusMinutes(1))) {
            return credentialEncryptionService.decrypt(connection.getEncryptedAccessToken());
        }
        String refreshToken = credentialEncryptionService.decrypt(connection.getEncryptedRefreshToken());
        JsonNode tokenResponse = postForm("https://oauth2.googleapis.com/token", Map.of(
            "client_id", clientId,
            "client_secret", clientSecret,
            "refresh_token", refreshToken,
            "grant_type", "refresh_token"
        ));
        String accessToken = requiredText(tokenResponse, "access_token", "Google did not return a refreshed access token.");
        connection.setEncryptedAccessToken(credentialEncryptionService.encrypt(accessToken));
        connection.setAccessTokenExpiresAt(expiryFrom(tokenResponse));
        connectionRepository.save(connection);
        return accessToken;
    }

    private String fetchMailboxEmail(String accessToken) {
        JsonNode profile = getJson("https://www.googleapis.com/oauth2/v2/userinfo", accessToken);
        String email = profile.path("email").asText().trim().toLowerCase();
        if (email.isEmpty() || !email.contains("@")) {
            throw new GmailIntegrationException(502, "GMAIL_PROFILE_ERROR", "Google did not return the mailbox email address.");
        }
        return email;
    }

    private JsonNode getJson(String url, String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw providerError(response.statusCode(), "Gmail API request failed");
            }
            return objectMapper.readTree(response.body());
        } catch (GmailIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new GmailIntegrationException(502, "GMAIL_UNAVAILABLE", "Gmail is temporarily unavailable. Try again later.");
        }
    }

    private JsonNode postForm(String url, Map<String, String> values) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formEncode(values)))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw providerError(response.statusCode(), "Google OAuth request failed");
            }
            return objectMapper.readTree(response.body());
        } catch (GmailIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new GmailIntegrationException(502, "GOOGLE_OAUTH_UNAVAILABLE", "Google OAuth is temporarily unavailable. Try again later.");
        }
    }

    private GmailIntegrationException providerError(int statusCode, String ignoredDetail) {
        if (statusCode == 401 || statusCode == 403) {
            return new GmailIntegrationException(401, "GMAIL_REAUTH_REQUIRED", "Gmail permission expired or was revoked. Reconnect Gmail to continue.");
        }
        if (statusCode == 429) {
            return new GmailIntegrationException(429, "GMAIL_RATE_LIMITED", "Gmail rate limit reached. Try again later.");
        }
        return new GmailIntegrationException(502, "GMAIL_PROVIDER_ERROR", "The Gmail provider request could not be completed.");
    }

    private void revokeAtGoogle(String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/revoke"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("token=" + encode(token)))
                .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // The encrypted credential is still deleted locally by the caller.
        }
    }

    private void markError(GmailConnection connection, RuntimeException exception) {
        connection.setStatus(GmailConnectionStatus.ERROR);
        connection.setLastErrorAt(LocalDateTime.now());
        connection.setLastErrorCode(exception instanceof GmailIntegrationException
            ? ((GmailIntegrationException) exception).getErrorCode()
            : "GMAIL_SYNC_ERROR");
        connectionRepository.save(connection);
    }

    private DecodedGmailMessage decodeMessage(JsonNode message) {
        JsonNode payload = message.path("payload");
        String sender = header(payload, "From");
        String subject = header(payload, "Subject");
        List<String> bodies = new ArrayList<>();
        List<String> filenames = new ArrayList<>();
        collectPayload(payload, bodies, filenames);
        String body = String.join("\n", bodies);
        if (body.length() > 30000) {
            body = body.substring(0, 30000);
        }
        List<String> urls = extractUrls(body);
        long internalDate = message.path("internalDate").asLong(0);
        LocalDateTime receivedAt = internalDate > 0
            ? LocalDateTime.ofInstant(Instant.ofEpochMilli(internalDate), ZoneId.systemDefault())
            : null;
        boolean suspiciousAttachment = filenames.stream().anyMatch(name -> SUSPICIOUS_ATTACHMENT.matcher(name).matches());
        return new DecodedGmailMessage(sender, subject, body, urls, suspiciousAttachment, receivedAt);
    }

    private void collectPayload(JsonNode payload, List<String> bodies, List<String> filenames) {
        if (payload == null || payload.isMissingNode()) {
            return;
        }
        String filename = payload.path("filename").asText();
        if (!filename.isBlank()) {
            filenames.add(filename);
        }
        String mimeType = payload.path("mimeType").asText("");
        String data = payload.path("body").path("data").asText();
        if (!data.isBlank() && (mimeType.startsWith("text/plain") || mimeType.startsWith("text/html"))) {
            String decoded = decodeBase64Url(data);
            if (mimeType.startsWith("text/html")) {
                decoded = decoded.replaceAll("<[^>]+>", " ");
            }
            bodies.add(decoded);
        }
        for (JsonNode part : payload.path("parts")) {
            collectPayload(part, bodies, filenames);
        }
    }

    private String decodeBase64Url(String value) {
        String padded = value + "====".substring(0, (4 - value.length() % 4) % 4);
        try {
            return new String(Base64.getUrlDecoder().decode(padded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private String header(JsonNode payload, String expectedName) {
        for (JsonNode header : payload.path("headers")) {
            if (expectedName.equalsIgnoreCase(header.path("name").asText())) {
                return header.path("value").asText("");
            }
        }
        return "";
    }

    private List<String> extractUrls(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        List<String> urls = new ArrayList<>();
        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find() && urls.size() < 20) {
            String url = matcher.group().replaceFirst("[.,!?;:)]$", "");
            if (url.regionMatches(true, 0, "www.", 0, 4)) {
                url = "https://" + url;
            }
            urls.add(url);
        }
        return urls;
    }

    private String senderDomain(String sender) {
        int at = sender == null ? -1 : sender.lastIndexOf('@');
        if (at < 0 || at == sender.length() - 1) {
            return "unknown";
        }
        return sender.substring(at + 1).replace(">", "").trim().toLowerCase();
    }

    private LocalDateTime expiryFrom(JsonNode tokenResponse) {
        return LocalDateTime.now().plusSeconds(Math.max(60, tokenResponse.path("expires_in").asLong(3600) - 60));
    }

    private String requiredText(JsonNode node, String key, String message) {
        String value = node.path(key).asText();
        if (value == null || value.isBlank()) {
            throw new GmailIntegrationException(502, "GOOGLE_OAUTH_RESPONSE_INVALID", message);
        }
        return value;
    }

    private boolean isConfigured() {
        return enabled && nonBlank(clientId) && nonBlank(clientSecret) && nonBlank(redirectUri);
    }

    private void requireConfiguration() {
        if (!isConfigured()) {
            throw new BadRequestException("Gmail integration is not configured on this server.");
        }
    }

    private boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String formEncode(Map<String, String> values) {
        StringBuilder encoded = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (encoded.length() > 0) {
                encoded.append('&');
            }
            encoded.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
        }
        return encoded.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static final class DecodedGmailMessage {
        private final String sender;
        private final String subject;
        private final String body;
        private final List<String> urls;
        private final boolean hasSuspiciousAttachment;
        private final LocalDateTime receivedAt;

        private DecodedGmailMessage(
            String sender,
            String subject,
            String body,
            List<String> urls,
            boolean hasSuspiciousAttachment,
            LocalDateTime receivedAt
        ) {
            this.sender = sender;
            this.subject = subject;
            this.body = body;
            this.urls = urls;
            this.hasSuspiciousAttachment = hasSuspiciousAttachment;
            this.receivedAt = receivedAt;
        }
    }
}
