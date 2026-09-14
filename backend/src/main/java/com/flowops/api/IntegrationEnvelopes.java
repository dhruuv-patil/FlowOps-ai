package com.flowops.api;

import java.util.List;

/** Object envelopes for the integration collection endpoints (never bare arrays). */
public final class IntegrationEnvelopes {

    private IntegrationEnvelopes() {
    }

    public record Integrations(List<IntegrationResponse> integrations) {
    }
}
