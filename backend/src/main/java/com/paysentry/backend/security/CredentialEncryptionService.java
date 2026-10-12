package com.paysentry.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.paysentry.backend.exception.BadRequestException;

/** Encrypts third-party OAuth tokens before database persistence using AES-GCM. */
@Service
public class CredentialEncryptionService {

    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    private final String configuredKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public CredentialEncryptionService(@Value("${paysentry.gmail.encryption-key:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to encrypt provider credentials.", exception);
        }
    }

    public String decrypt(String payload) {
        try {
            byte[] encoded = Base64.getUrlDecoder().decode(payload);
            if (encoded.length <= IV_BYTES) {
                throw new BadRequestException("Stored provider credential is invalid.");
            }
            byte[] iv = Arrays.copyOfRange(encoded, 0, IV_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(encoded, IV_BYTES, encoded.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to decrypt provider credentials.", exception);
        }
    }

    private SecretKey key() {
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new BadRequestException("Gmail token encryption is not configured.");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(configuredKey);
            if (decoded.length != 16 && decoded.length != 24 && decoded.length != 32) {
                throw new BadRequestException("Gmail token encryption key must be a base64-encoded 128-, 192-, or 256-bit key.");
            }
            return new SecretKeySpec(decoded, "AES");
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Gmail token encryption key must be base64 encoded.");
        }
    }
}
