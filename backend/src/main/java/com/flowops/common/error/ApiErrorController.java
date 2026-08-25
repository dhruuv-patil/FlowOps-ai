package com.flowops.common.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Replaces Spring Boot's {@code BasicErrorController} so that container-level
 * errors — the ones that never reach a {@code @RestControllerAdvice}, such as a
 * request rejected before dispatch — still emit the contract §3 envelope.
 *
 * <p>Declaring an {@link ErrorController} bean suppresses the auto-configured one,
 * which would otherwise answer with {@code {"error": "Not Found", ...}} where
 * {@code error} is a string rather than the envelope object.
 */
@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ApiErrorResponse> handleError(HttpServletRequest request) {
        int status = statusOf(request);
        ErrorCode code = ErrorCode.fromStatus(status);
        String requestId = RequestIdFilter.currentRequestId(request);

        ApiErrorResponse body = ApiErrorResponse.of(
                code, null, pathOf(request), requestId, null);

        return ResponseEntity.status(code.status())
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.noStore())
                .header(RequestIdFilter.HEADER, requestId)
                .body(body);
    }

    private static int statusOf(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        return status instanceof Integer value ? value : 500;
    }

    private static String pathOf(HttpServletRequest request) {
        Object original = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        return original instanceof String uri ? uri : request.getRequestURI();
    }
}
