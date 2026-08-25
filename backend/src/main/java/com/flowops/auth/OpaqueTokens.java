package com.flowops.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Mints and hashes the opaque refresh token (contract §1.2).
 *
 * <p>Deliberately not a JWT: there is nothing for the client to read, and an
 * opaque handle can be revoked server-side, which is the whole point of the split
 * with the stateless access token.
 *
 * <p>Only the SHA-256 hash is ever persisted, so a database dump does not yield
 * usable refresh tokens. SHA-256 without a salt or work factor is the right choice
 * here — unlike a password, the token is 256 bits of {@link SecureRandom} output,
 * so it has no guessable structure for an offline attack to exploit, and lookup by
 * hash must be a single indexed query.
 */
final class OpaqueTokens {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** 256 bits, matching the SHA-256 output the hash column is sized for. */
    private static final int TOKEN_BYTES = 32;

    private OpaqueTokens() {
    }

    /** A fresh token: 43 base64url characters, unpadded. */
    static String mint() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Lowercase hex SHA-256 — 64 characters, matching {@code refresh_tokens.token_hash}. */
    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is mandated by the JDK; absence means a broken runtime.
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
