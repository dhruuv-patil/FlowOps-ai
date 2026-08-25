package com.flowops.execution;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * The outcome of running one node.
 *
 * <ul>
 *   <li>{@link Kind#SUCCESS} — produced {@code output} and fired {@code handles}
 *       (the named output ports whose downstream edges become active);</li>
 *   <li>{@link Kind#WAIT} — suspended the run pending a human decision; no output
 *       yet, and the engine parks the execution in {@code WAITING};</li>
 *   <li>{@link Kind#FAIL} — failed with a user-safe {@code error} message; the
 *       engine retries per policy, then fails the run.</li>
 * </ul>
 */
public record NodeResult(Kind kind, JsonNode output, List<String> handles, String error) {

    public enum Kind {
        SUCCESS,
        WAIT,
        FAIL
    }

    public static NodeResult success(JsonNode output, List<String> handles) {
        return new NodeResult(Kind.SUCCESS, output, handles == null ? List.of() : handles, null);
    }

    /** Convenience for a single-output node whose only port is {@code out}. */
    public static NodeResult success(JsonNode output) {
        return success(output, List.of("out"));
    }

    public static NodeResult waiting() {
        return new NodeResult(Kind.WAIT, null, List.of(), null);
    }

    public static NodeResult fail(String error) {
        return new NodeResult(Kind.FAIL, null, List.of(), error);
    }
}
