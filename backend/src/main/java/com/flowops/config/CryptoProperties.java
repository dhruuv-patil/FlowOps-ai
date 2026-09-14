package com.flowops.config;

import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Binds the {@code flowops.crypto} tree: the symmetric key used to encrypt integration
 * credentials at rest (AES-256-GCM).
 *
 * <p>The key is a base64-encoded 32-byte value supplied via the {@code ENCRYPTION_KEY}
 * environment variable — never hardcoded, never logged. The compact constructor
 * fails fast with an actionable message if the key is missing or the wrong length,
 * so a misconfigured deployment cannot silently fall back to weak or absent
 * encryption.
 *
 * @param encryptionKey base64-encoded 32-byte (256-bit) AES key
 */
@ConfigurationProperties(prefix = "flowops.crypto")
public record CryptoProperties(@DefaultValue("") String encryptionKey) {

    /** The decoded key length AES-256 requires. */
    public static final int KEY_BYTES = 32;

    public CryptoProperties {
        encryptionKey = encryptionKey == null ? "" : encryptionKey.strip();
    }

    /**
     * Decodes and validates the key. Called once at startup (by {@code CredentialCipher})
     * so a bad key aborts boot rather than surfacing on the first credential write.
     */
    public byte[] key() {
        if (encryptionKey.isEmpty()) {
            throw new IllegalStateException(
                    "ENCRYPTION_KEY is not set. Provide a base64-encoded 32-byte key to enable "
                            + "credential encryption (see .env.example).");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(encryptionKey);
        } catch (IllegalArgumentException notBase64) {
            throw new IllegalStateException("ENCRYPTION_KEY must be valid base64.", notBase64);
        }
        if (decoded.length != KEY_BYTES) {
            throw new IllegalStateException(
                    "ENCRYPTION_KEY must decode to exactly " + KEY_BYTES + " bytes (AES-256); got "
                            + decoded.length + ".");
        }
        return decoded;
    }
}
