package com.flowops.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WebhookTokens}, the mint/verify primitive behind inbound webhooks.
 * These pin the security-relevant properties: a minted token verifies against its own
 * hash, a wrong token does not, only the hash (never the token) is 64-char hex, and two
 * mints are independent.
 */
class WebhookTokensTest {

    private final WebhookTokens tokens = new WebhookTokens();

    @Test
    void aMintedTokenVerifiesAgainstItsHash() {
        WebhookTokens.Generated minted = tokens.generate();
        assertThat(tokens.matches(minted.token(), minted.hash())).isTrue();
    }

    @Test
    void aWrongTokenDoesNotVerify() {
        WebhookTokens.Generated minted = tokens.generate();
        assertThat(tokens.matches(minted.token() + "x", minted.hash())).isFalse();
        assertThat(tokens.matches("totally-wrong", minted.hash())).isFalse();
    }

    @Test
    void matchesIsNullSafe() {
        WebhookTokens.Generated minted = tokens.generate();
        assertThat(tokens.matches(null, minted.hash())).isFalse();
        assertThat(tokens.matches(minted.token(), null)).isFalse();
    }

    @Test
    void theHashIs64CharLowercaseHexAndTheTokenIsNot() {
        WebhookTokens.Generated minted = tokens.generate();
        assertThat(minted.hash()).matches("[0-9a-f]{64}");
        assertThat(minted.token()).isNotEqualTo(minted.hash());
        // The hint is the token's last 4 chars — non-secret, safe to display.
        assertThat(minted.token()).endsWith(minted.hint());
        assertThat(minted.hint()).hasSize(4);
    }

    @Test
    void twoMintsAreIndependent() {
        WebhookTokens.Generated a = tokens.generate();
        WebhookTokens.Generated b = tokens.generate();
        assertThat(a.token()).isNotEqualTo(b.token());
        assertThat(a.hash()).isNotEqualTo(b.hash());
    }

    @Test
    void dummyHashIsShaOfEmptyString() {
        assertThat(WebhookTokens.DUMMY_HASH).isEqualTo(WebhookTokens.sha256Hex(""));
    }
}
