package com.flowops.common.crypto;

import com.flowops.config.CryptoProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Symmetric encryption for credentials at rest, using AES-256-GCM (authenticated
 * encryption — tamper-evident, not just confidential).
 *
 * <p>The ciphertext blob is {@code base64( iv(12 bytes) ‖ ciphertext ‖ tag(16 bytes) )}.
 * A fresh random IV is drawn per {@link #encrypt} call, so encrypting the same
 * plaintext twice yields different blobs — no ciphertext equality leaks. The key is
 * loaded and validated once at construction (via {@link CryptoProperties#key()}), so
 * a missing or wrong-length {@code ENCRYPTION_KEY} aborts startup rather than the
 * first credential write.
 *
 * <p>This class never logs plaintext, ciphertext, or the key.
 */
@Component
public class CredentialCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;         // 96-bit nonce, the GCM standard
    private static final int TAG_BITS = 128;        // 16-byte authentication tag

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public CredentialCipher(CryptoProperties properties) {
        this.key = new SecretKeySpec(properties.key(), "AES");
    }

    /** Encrypts UTF-8 plaintext into a self-describing base64 blob ({@code iv ‖ ct ‖ tag}). */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Cannot encrypt null.");
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] sealed = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] blob = new byte[iv.length + sealed.length];
            System.arraycopy(iv, 0, blob, 0, iv.length);
            System.arraycopy(sealed, 0, blob, iv.length, sealed.length);
            return Base64.getEncoder().encodeToString(blob);
        } catch (Exception failure) {
            // Never include the plaintext or key in the message.
            throw new IllegalStateException("Credential encryption failed.", failure);
        }
    }

    /** Decrypts a blob produced by {@link #encrypt}. Fails if the ciphertext was tampered with. */
    public String decrypt(String blob) {
        if (blob == null || blob.isBlank()) {
            throw new IllegalArgumentException("Cannot decrypt an empty value.");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(blob);
            if (decoded.length <= IV_BYTES) {
                throw new IllegalArgumentException("Ciphertext blob is too short.");
            }
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(decoded, 0, iv, 0, IV_BYTES);
            byte[] sealed = new byte[decoded.length - IV_BYTES];
            System.arraycopy(decoded, IV_BYTES, sealed, 0, sealed.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(sealed), StandardCharsets.UTF_8);
        } catch (Exception failure) {
            throw new IllegalStateException("Credential decryption failed.", failure);
        }
    }

    /**
     * Decrypts a blob produced by {@link #encrypt} and parses it as JSON into a Map.
     * Used for provider credentials stored as JSON (e.g. {"apiKey": "...", "baseUrl": "..."}).
     */
    public Map<String, String> decryptToMap(String blob) {
        String json = decrypt(blob);
        try {
            return new ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse decrypted credentials as JSON", e);
        }
    }

    /**
     * Encrypts a Map as JSON into a self-describing base64 blob.
     * Used for provider credentials stored as JSON.
     */
    public String encryptJson(Map<String, String> map) {
        try {
            String json = new ObjectMapper().writeValueAsString(map);
            return encrypt(json);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt credentials as JSON", e);
        }
    }
}
