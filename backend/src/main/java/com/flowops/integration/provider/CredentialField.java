package com.flowops.integration.provider;

/**
 * Describes a single field in a provider's connect form. Shared by workflow
 * providers and delivery (notification) providers so the connect dialog renders
 * identically for both. A {@code secret} field is rendered as a password input
 * and, if set, its value contributes to the masked last-4 {@code hint};
 * non-secret fields are display-only config (baseUrl, host, fromAddress, ...).
 */
public record CredentialField(
        String key,
        String label,
        boolean secret,
        String placeholder,
        String example,
        String help) {
}