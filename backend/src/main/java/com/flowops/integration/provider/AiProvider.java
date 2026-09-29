package com.flowops.integration.provider;

import java.util.List;

/**
 * SPI for AI capability providers (OpenAI, Anthropic).
 */
public interface AiProvider extends IntegrationProvider {

    /** Model listing for the provider. */
    List<String> listModels(IntegrationContext context);

    /** Generate text/completion. */
    String generateContent(IntegrationContext context, String prompt, String model);

    /** Test connection. */
    ConnectionTestResult testConnection(IntegrationContext context);
}
