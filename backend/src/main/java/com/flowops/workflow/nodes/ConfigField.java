package com.flowops.workflow.nodes;

import java.util.List;

/**
 * A single configurable field on a node's config panel. This is the contract the
 * frontend renders the right-hand config form from, and the validator checks a
 * node's stored config against.
 *
 * @param key      config map key
 * @param label    human label for the form
 * @param type     input widget hint: {@code string}, {@code text}, {@code number},
 *                 {@code boolean}, {@code select}, {@code json}, {@code code},
 *                 {@code agent} (a picker of the org's saved AI agents)
 * @param required whether validation fails when the value is missing/blank
 * @param options  allowed values for {@code select}; empty otherwise
 * @param help     optional helper text shown under the field
 * @param placeholder optional input placeholder
 */
public record ConfigField(
        String key,
        String label,
        String type,
        boolean required,
        List<String> options,
        String help,
        String placeholder) {

    public static ConfigField text(String key, String label, boolean required, String help) {
        return new ConfigField(key, label, "string", required, List.of(), help, null);
    }

    public static ConfigField multiline(String key, String label, boolean required, String help) {
        return new ConfigField(key, label, "text", required, List.of(), help, null);
    }

    public static ConfigField number(String key, String label, boolean required, String help) {
        return new ConfigField(key, label, "number", required, List.of(), help, null);
    }

    public static ConfigField bool(String key, String label, String help) {
        return new ConfigField(key, label, "boolean", false, List.of(), help, null);
    }

    public static ConfigField select(String key, String label, boolean required, List<String> options) {
        return new ConfigField(key, label, "select", required, options, null, null);
    }

    public static ConfigField json(String key, String label, boolean required, String help) {
        return new ConfigField(key, label, "json", required, List.of(), help, null);
    }

    /**
     * A picker bound to the caller's saved AI agents. The stored value is an agent
     * id (a UUID string); the frontend resolves it to a name via the agents API.
     */
    public static ConfigField agent(String key, String label, boolean required, String help) {
        return new ConfigField(key, label, "agent", required, List.of(), help, null);
    }
}
