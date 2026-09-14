package com.flowops.integration.delivery.support;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * HMAC-SHA256 signing for outbound webhook payloads. When a webhook integration is
 * configured with a {@code secret}, each delivery carries an
 * {@code X-Flowops-Signature: sha256=<hex>} header computed over the raw request
 * body, so the receiving endpoint can verify the request really came from FlowOps.
 * The secret is never logged or echoed.
 */
@Component
public class HmacSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /** Hex-encoded HMAC-SHA256 of {@code body} keyed by {@code secret}. */
    public String sign(String secret, String body) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception impossible) {
            // HmacSHA256 is universally available; a failure here is a JVM problem.
            throw new IllegalStateException("HMAC signing unavailable.", impossible);
        }
    }

    /** The {@code X-Flowops-Signature} header value for a signed delivery. */
    public String headerValue(String signature) {
        return "sha256=" + signature;
    }
}