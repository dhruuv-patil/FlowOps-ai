package com.flowops.ai;

/**
 * The narrow contract the AI node family needs: a single-turn completion.
 *
 * <p>Mirrors the AI service's {@code /ai/complete} endpoint exactly. Splitting
 * this off {@link AiServiceClient} (which implements it) keeps every AI node
 * testable with a plain lambda — no transport, no Mockito — while production
 * wiring continues to inject the real {@code AiServiceClient}. Honesty rules
 * are shared: {@code configured} is false when the service has no provider
 * key, and {@code output} is then null/absent rather than fabricated.
 */
public interface AiCompleter {

    /**
     * Posts a single-turn completion to the AI service.
     *
     * @param systemPrompt system instructions (may be empty)
     * @param userPrompt   the concrete request the model answers
     * @param model        optional model override, or null for the provider default
     * @param jsonMode     when true the endpoint verifies the output parses as JSON
     */
    Completion complete(String systemPrompt, String userPrompt, String model, boolean jsonMode);

    /** The normalized completion: configured flag plus the model's text output. */
    record Completion(boolean configured, String output, String model) {
    }
}