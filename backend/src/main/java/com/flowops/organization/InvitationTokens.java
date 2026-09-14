package com.flowops.organization;

import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Mints and verifies organization-invitation tokens.
 *
 * <p>A token is 256 bits from {@link SecureRandom}, rendered base64url without
 * padding. Because it is non-guessable high-entropy material — not a human-chosen
 * secret — a fast one-way hash (SHA-256) plus a constant-time compare is the correct
 * store/verify, exactly as for webhook and refresh tokens. Only the lowercase-hex
 * hash is ever persisted; the token itself is handed back to the admin once.
 */
@Component
public class InvitationTokens {

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

    /** Constant-time check that {@code presented} hashes to {@code storedHash}. */
    public boolean matches(String presented, String storedHash) {
        if (presented == null || storedHash == null) {
            return false;
        }
        byte[] a = sha256Hex(presented).getBytes(StandardCharsets.US_ASCII);
        byte[] b = storedHash.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(a, b);
    }

    public String hash(String token) {
        return sha256Hex(token);
    }

    private static String hint(String token) {
        return token.length() <= HINT_LENGTH ? token : token.substring(token.length() - HINT_LENGTH);
    }

    private static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
