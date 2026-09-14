package com.flowops.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flowops.config.CryptoProperties;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CredentialCipher} — the AES-256-GCM primitive every stored
 * credential relies on. No Spring context: a {@link CryptoProperties} with a known
 * 32-byte key is fed straight in.
 *
 * <p>These pin the security-relevant properties: a value round-trips, the same
 * plaintext never produces the same ciphertext twice (fresh IV per call → no equality
 * leak), the blob never contains the plaintext, and tampering is detected (GCM's
 * authentication tag) rather than silently returning corrupted data.
 */
class CredentialCipherTest {

    private static final String WEBHOOK_URL =
            "https://example.com/test-webhook";

    private static CredentialCipher cipher() {
        byte[] raw = new byte[CryptoProperties.KEY_BYTES];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) (i * 7 + 1);
        }
        String base64Key = Base64.getEncoder().encodeToString(raw);
        return new CredentialCipher(new CryptoProperties(base64Key));
    }

    @Test
    void encryptThenDecryptRoundTrips() {
        CredentialCipher cipher = cipher();
        String blob = cipher.encrypt(WEBHOOK_URL);
        assertThat(cipher.decrypt(blob)).isEqualTo(WEBHOOK_URL);
    }

    @Test
    void sameInputYieldsDistinctCiphertextButDecryptsToSamePlaintext() {
        CredentialCipher cipher = cipher();
        String first = cipher.encrypt(WEBHOOK_URL);
        String second = cipher.encrypt(WEBHOOK_URL);

        // Fresh IV per call → identical plaintext must not produce identical blobs.
        assertThat(first).isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo(WEBHOOK_URL);
        assertThat(cipher.decrypt(second)).isEqualTo(WEBHOOK_URL);
    }

    @Test
    void ciphertextNeverContainsThePlaintext() {
        CredentialCipher cipher = cipher();
        String blob = cipher.encrypt(WEBHOOK_URL);
        assertThat(blob).doesNotContain(WEBHOOK_URL);
        assertThat(blob).doesNotContain("hooks.slack.com");
    }

    @Test
    void tamperedCiphertextIsRejected() {
        CredentialCipher cipher = cipher();
        String blob = cipher.encrypt(WEBHOOK_URL);
        // Flip the final base64 character — GCM's tag check must reject the forgery.
        char last = blob.charAt(blob.length() - 1);
        String tampered = blob.substring(0, blob.length() - 1) + (last == 'A' ? 'B' : 'A');

        assertThatThrownBy(() -> cipher.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Credential decryption failed.");
    }

    @Test
    void garbageInputIsRejectedWithoutLeaking() {
        CredentialCipher cipher = cipher();
        assertThatThrownBy(() -> cipher.decrypt("not-a-real-blob"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Credential decryption failed.");
    }
}
