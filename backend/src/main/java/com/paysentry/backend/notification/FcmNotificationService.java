package com.paysentry.backend.notification;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paysentry.backend.entity.FraudAlert;
import com.paysentry.backend.entity.MobileDeviceToken;
import com.paysentry.backend.repository.MobileDeviceTokenRepository;

@Service
public class FcmNotificationService {

    private static final Logger LOGGER = Logger.getLogger(FcmNotificationService.class.getName());

    private final MobileDeviceTokenRepository mobileDeviceTokenRepository;
    private final boolean fcmEnabled;
    private final String serviceAccountPath;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private String cachedAccessToken;
    private Instant accessTokenExpiresAt = Instant.EPOCH;

    public FcmNotificationService(
        MobileDeviceTokenRepository mobileDeviceTokenRepository,
        @Value("${paysentry.fcm.enabled:false}") boolean fcmEnabled,
        @Value("${paysentry.fcm.service-account-path:}") String serviceAccountPath,
        ObjectMapper objectMapper
    ) {
        this.mobileDeviceTokenRepository = mobileDeviceTokenRepository;
        this.fcmEnabled = fcmEnabled;
        this.serviceAccountPath = serviceAccountPath;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public void sendImportantAlert(FraudAlert alert) {
        if (!fcmEnabled) {
            LOGGER.info("FCM is disabled. Alert " + alert.getId() + " remains available through the mobile dashboard.");
            return;
        }

        try {
            JsonNode serviceAccount = loadServiceAccount();
            String accessToken = getAccessToken(serviceAccount);
            String projectId = serviceAccount.path("project_id").asText();
            if (projectId.isEmpty()) {
                throw new IllegalStateException("Firebase service account is missing project_id.");
            }
            List<MobileDeviceToken> tokens = mobileDeviceTokenRepository.findByUserAndActiveTrue(alert.getUser());
            for (MobileDeviceToken token : tokens) {
                sendMessage(projectId, accessToken, token, alert);
            }
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Unable to deliver FCM alert to registered devices.", exception);
        }
    }

    private JsonNode loadServiceAccount() throws Exception {
        if (serviceAccountPath == null || serviceAccountPath.trim().isEmpty()) {
            throw new IllegalStateException("paysentry.fcm.service-account-path is required when FCM is enabled.");
        }
        return objectMapper.readTree(Files.readString(Paths.get(serviceAccountPath)));
    }

    private synchronized String getAccessToken(JsonNode serviceAccount) throws Exception {
        if (cachedAccessToken != null && Instant.now().isBefore(accessTokenExpiresAt)) {
            return cachedAccessToken;
        }

        Instant now = Instant.now();
        String header = encodeJson(Map.of("alg", "RS256", "typ", "JWT"));
        String claims = encodeJson(Map.of(
            "iss", serviceAccount.path("client_email").asText(),
            "scope", "https://www.googleapis.com/auth/firebase.messaging",
            "aud", "https://oauth2.googleapis.com/token",
            "iat", now.getEpochSecond(),
            "exp", now.plusSeconds(3600).getEpochSecond()
        ));
        String unsignedToken = header + "." + claims;
        String signedToken = unsignedToken + "." + sign(unsignedToken, serviceAccount.path("private_key").asText());
        String form = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", "UTF-8")
            + "&assertion=" + URLEncoder.encode(signedToken, "UTF-8");
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Firebase OAuth token request failed with HTTP " + response.statusCode());
        }
        JsonNode tokenResponse = objectMapper.readTree(response.body());
        cachedAccessToken = tokenResponse.path("access_token").asText();
        if (cachedAccessToken.isEmpty()) {
            throw new IllegalStateException("Firebase OAuth response did not include an access token.");
        }
        accessTokenExpiresAt = now.plusSeconds(Math.max(60, tokenResponse.path("expires_in").asLong(3600) - 60));
        return cachedAccessToken;
    }

    private void sendMessage(String projectId, String accessToken, MobileDeviceToken token, FraudAlert alert) throws Exception {
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("title", alert.getTitle());
        notification.put("body", "Risk score " + alert.getRiskScore() + "/100. " + alert.getRecommendedAction());
        Map<String, String> data = new LinkedHashMap<>();
        data.put("alertId", alert.getId().toString());
        data.put("riskLevel", alert.getRiskLevel().name());
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("token", token.getFcmToken());
        message.put("notification", notification);
        message.put("data", data);
        String body = objectMapper.writeValueAsString(Map.of("message", message));
        HttpRequest request = HttpRequest.newBuilder(
            URI.create("https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send")
        )
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("FCM delivery failed with HTTP " + response.statusCode());
        }
    }

    private String encodeJson(Map<String, Object> value) throws Exception {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
            objectMapper.writeValueAsBytes(value)
        );
    }

    private String sign(String value, String privateKeyValue) throws Exception {
        String encodedKey = privateKeyValue
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(
            new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encodedKey.getBytes(StandardCharsets.UTF_8)))
        );
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(value.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());
    }
}
