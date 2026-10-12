package com.paysentry.backend.service;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Service;

/** Produces auditable evidence without retaining message bodies, OTPs, or URL query values. */
@Service
public class SensitiveContentService {

    public String fingerprint(String content) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(content.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder encoded = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                encoded.append(String.format("%02x", value));
            }
            return encoded.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    public String redactedSmsEvidence(String content) {
        return "Message content was analyzed but is not retained. fingerprint=" + fingerprint(content);
    }

    public String safeUrlEvidence(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost() == null ? "invalid" : uri.getHost().toLowerCase();
            return (uri.getScheme() == null ? "unknown" : uri.getScheme().toLowerCase())
                + "://" + host + " (fingerprint=" + fingerprint(url) + ")";
        } catch (IllegalArgumentException exception) {
            return "invalid-url (fingerprint=" + fingerprint(url) + ")";
        }
    }
}
