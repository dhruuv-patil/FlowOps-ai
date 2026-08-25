package com.flowops.execution;

/**
 * Runs one node type. Implementations are stateless Spring beans, one per node
 * type in the {@code NodeRegistry}; {@link NodeExecutorRegistry} indexes them by
 * {@link #type()}.
 *
 * <p>An executor is a pure function of its {@link NodeExecutionContext}: it reads
 * config and variables, does its work, and returns a {@link NodeResult}. Throwing
 * is allowed — the engine turns a thrown exception into a retryable failure with a
 * sanitized message.
 */
public interface NodeExecutor {

    /** The {@code NodeRegistry} type key this executor handles, e.g. {@code http_request}. */
    String type();

    NodeResult execute(NodeExecutionContext ctx) throws Exception;
}
