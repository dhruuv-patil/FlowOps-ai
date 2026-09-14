package com.flowops.webhook;

import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Mints and verifies inbound-webhook tokens.
 *
 * <p>A token is 256 bits from {@link SecureRandom}, rendered base64url without
 * padding. Because it is non-guessable high-entropy material — not a human-chosen
 * password — the correct store/verify is a <em>fast</em> one-way hash (SHA-256) plus a
 * constant-time compare, not bcrypt: bcrypt's work factor defends against brute-forcing
 * low-entropy secrets and would only add per-request latency here for no security gain.
 *
 * <p>Only the lowercase-hex hash is ever persisted; the token itself exists just long
 * enough to be handed back to the admin once. This is deliberately separate from
 * {@code CredentialCipher} (reversible AES-GCM): a webhook token must be one-way, so
 * that a database read can never recover a working URL.
 */
@Component
public class WebhookTokens {

    /** SHA-256 of the empty string — a fixed, valid 64-char hex hash to compare against
     * on the miss path so a missing/disabled webhook takes the same time as a wrong token. */
    public static final String DUMMY_HASH =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private static final int TOKEN_BYTES = 32;
    private static final int HINT_LENGTH = 4;

    private final SecureRandom random = new SecureRandom();

    /** A freshly-minted token together with what gets stored: its hash and a display hint. */
    public record Generated(String token, String hash, String hint) {
    }

    public Generated generate() {
        byte[] raw = new byte[TOKEN_BYTES];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        return new Generated(token, sha256Hex(token), hint(token));
    }

    /**
     * Constant-time check that {@code presented} hashes to {@code storedHash}. Compares
     * the fixed-length hex encodings via {@link MessageDigest#isEqual}, so a wrong token
     * and a right one take the same time regardless of where they first differ.
     */
    public boolean matches(String presented, String storedHash) {
        if (presented == null || storedHash == null) {
            return false;
        }
        byte[] a = sha256Hex(presented).getBytes(StandardCharsets.US_ASCII);
        byte[] b = storedHash.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(a, b);
    }

    /** Last-4 of the token — non-secret, safe to display so an admin can recognize it. */
    private static String hint(String token) {
        return token.length() <= HINT_LENGTH ? token : token.substring(token.length() - HINT_LENGTH);
    }

    static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            // Every JVM ships SHA-256; treat its absence as a fatal misconfiguration.
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
