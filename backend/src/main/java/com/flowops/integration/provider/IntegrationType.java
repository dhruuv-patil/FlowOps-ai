package com.flowops.integration.provider;

/**
 * Supported integration types. Wire values are lowercase stable identifiers
 * (stored in {@code integrations.type} and {@code execution_events.source}).
 * New providers are added here and a corresponding {@link WorkflowProvider}
 * bean; the registry auto-registers them.
 */
public enum IntegrationType {

    /** Slack incoming-webhook delivery (notification provider). */
    SLACK("slack"),

    /** Email (SMTP) delivery — generic SMTP, covers Gmail/Outlook/custom. */
    EMAIL("email"),

    /** Discord incoming-webhook delivery (notification provider). */
    DISCORD("discord"),

    /** Microsoft Teams incoming-webhook delivery (notification provider). */
    TEAMS("teams"),

    /** Outbound HTTP webhook delivery + event dispatch. */
    WEBHOOK("webhook"),

    /** n8n workflow automation. */
    N8N("n8n"),

    /** Make (formerly Integromat) workflow automation. */
    MAKE("make"),

    /** Zapier workflow automation (webhook connect; no public run-history API). */
    ZAPIER("zapier"),

    /** GitHub Actions workflow automation + reliability telemetry. */
    GITHUB("github"),

    /** Temporal workflow orchestration. */
    TEMPORAL("temporal"),

    /** Generic/custom HTTP-based provider. */
    CUSTOM("custom");

    private final String wire;

    IntegrationType(String wire) {
        this.wire = wire;
    }

    /** The value stored in the database and used in API contracts. */
    public String wire() {
        return wire;
    }

    /** Lookup by wire value (case-insensitive). */
    public static IntegrationType fromWire(String wire) {
        if (wire == null) {
            return null;
        }
        for (IntegrationType t : values()) {
            if (t.wire.equalsIgnoreCase(wire)) {
                return t;
            }
        }
        return null;
    }
}