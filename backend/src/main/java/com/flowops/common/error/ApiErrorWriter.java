package com.flowops.common.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Writes the contract §3 envelope straight to the servlet response.
 *
 * <p>Needed because Spring Security's {@code AuthenticationEntryPoint} /
 * {@code AccessDeniedHandler} and the JWT filter run outside the MVC dispatch, so
 * they cannot go through {@code @RestControllerAdvice}. Same shape, same codes —
 * a client only ever has to parse one body.
 */
@Component
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;

    public ApiErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            ErrorCode code,
            String message) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        String requestId = RequestIdFilter.currentRequestId(request);
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(RequestIdFilter.HEADER, requestId);

        ApiErrorResponse body = ApiErrorResponse.of(
                code, message, request.getRequestURI(), requestId, null);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
