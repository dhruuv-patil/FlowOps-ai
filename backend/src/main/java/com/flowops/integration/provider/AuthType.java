package com.flowops.integration.provider;

/**
 * Authentication type required by an integration provider.
 */
public enum AuthType {

    API_KEY("api_key", "API Key"),
    BEARER_TOKEN("bearer_token", "Bearer Token"),
    BASIC_AUTH("basic_auth", "Basic Authentication"),
    OAUTH2("oauth2", "OAuth 2.0"),
    WEBHOOK_SECRET("webhook_secret", "Webhook Secret"),
    HMAC_SIGNING("hmac_signing", "HMAC Signing"),
    CREDENTIALS("credentials", "Username / Password"),
    CUSTOM("custom", "Custom Auth");

    private final String wire;
    private final String displayName;

    AuthType(String wire, String displayName) {
        this.wire = wire;
        this.displayName = displayName;
    }

    public String wire() {
        return wire;
    }

    public String displayName() {
        return displayName;
    }
}
