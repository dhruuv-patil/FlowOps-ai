package com.flowops.execution;

/**
 * A node's window onto the run log. The engine supplies an implementation that
 * assigns the monotonic sequence number, persists the line, and pushes it to any
 * live SSE subscriber.
 *
 * <p>Contract §9: executors log outcomes and user-safe detail only — never a raw
 * credential, token, header value, or full response body.
 */
public interface NodeLogger {

    void debug(String message);

    void info(String message);

    void warn(String message);

    void error(String message);
}
