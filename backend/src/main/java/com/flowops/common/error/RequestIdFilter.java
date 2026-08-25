package com.flowops.common.error;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns every request a 16-hex-character id, exposes it as the
 * {@code X-Request-Id} response header, and stashes it for the error handlers.
 *
 * <p>Ordered ahead of the security filter chain so that a 401 produced by
 * Spring Security still carries a request id — the id is what ties a user's bug
 * report to the full exception in the server log.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String ATTRIBUTE = "flowops.requestId";
    public static final String HEADER = "X-Request-Id";

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String requestId = newRequestId();
        request.setAttribute(ATTRIBUTE, requestId);
        response.setHeader(HEADER, requestId);
        chain.doFilter(request, response);
    }

    /**
     * The id of the in-flight request, or a freshly generated one if the filter
     * did not run (which happens for errors raised inside the filter chain
     * itself). Never returns null, so the envelope's contract holds either way.
     */
    public static String currentRequestId(HttpServletRequest request) {
        Object existing = request == null ? null : request.getAttribute(ATTRIBUTE);
        return existing instanceof String id ? id : newRequestId();
    }

    private static String newRequestId() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
